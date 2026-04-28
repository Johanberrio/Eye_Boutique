package com.example.lentespro.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SaleRepository(
    private val db: FirebaseFirestore
) {
    private val salesCollection = db.collection("sales")
    private val productsCollection = db.collection("products")

    fun observeSales(): Flow<List<SaleEntity>> = callbackFlow {
        val subscription = salesCollection
            .orderBy("createdAtEpochMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close()
                    return@addSnapshotListener
                }
                val sales = snapshot?.documents?.mapNotNull { it.toObject(SaleEntity::class.java) } ?: emptyList()
                trySend(sales)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getSaleOnce(saleId: String): SaleEntity? {
        return try {
            val doc = salesCollection.document(saleId).get().await()
            doc.toObject(SaleEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * ✅ CREAR RUTA (DISPATCH):
     * Corregido: Lecturas primero, luego escrituras.
     */
    suspend fun createRouteDispatch(
        messengerName: String?,
        notes: String?,
        items: List<CreateRouteItem>,
        customerName: String,
        customerPhone1: String,
        customerPhone2: String?,
        customerAddress: String,
        customerNeighborhood: String
    ): String {
        return db.runTransaction { transaction ->
            // 1. OBTENER REFERENCIAS
            val saleDocRef = salesCollection.document()
            val productRefs = items.map { productsCollection.document(it.productId) }

            // 2. REALIZAR TODAS LAS LECTURAS (READS) PRIMERO
            val productSnapshots = productRefs.map { transaction.get(it) }
            val products = productSnapshots.map { snap ->
                snap.toObject(ProductEntity::class.java) ?: error("Producto no encontrado")
            }

            // 3. REALIZAR TODAS LAS ESCRITURAS (WRITES) DESPUÉS
            val saleItems = items.mapIndexed { index, item ->
                val product = products[index]
                if (product.cantidad < item.quantity) {
                    error("Stock insuficiente para ${product.nombre}")
                }

                // Programar actualización de stock
                transaction.update(productRefs[index], "cantidad", product.cantidad - item.quantity)

                SaleItemEntity(
                    productId = item.productId,
                    productName = "${product.nombre} (${product.marca})",
                    unitPrice = item.unitPrice,
                    dispatchedQty = item.quantity
                )
            }

            val sale = SaleEntity(
                status = SaleStatus.EN_RUTA,
                messengerName = messengerName,
                notes = notes,
                customerName = customerName,
                customerPhone1 = customerPhone1,
                customerPhone2 = customerPhone2,
                customerAddress = customerAddress,
                customerNeighborhood = customerNeighborhood,
                items = saleItems
            )
            
            transaction.set(saleDocRef, sale)
            saleDocRef.id
        }.await()
    }

    /**
     * ✅ FINALIZAR RUTA:
     * Corregido: Lecturas primero, luego escrituras.
     */
    suspend fun finalizeDispatch(
        saleId: String,
        soldByProductId: Map<String, Int>,
        sellerUid: String,
        sellerName: String
    ) {
        db.runTransaction { transaction ->
            // 1. LECTURA DE LA VENTA
            val saleRef = salesCollection.document(saleId)
            val saleSnap = transaction.get(saleRef)
            val sale = saleSnap.toObject(SaleEntity::class.java) ?: error("Venta no encontrada")

            if (sale.status != SaleStatus.EN_RUTA) error("Esta venta ya fue finalizada")

            // 2. IDENTIFICAR PRODUCTOS QUE NECESITAN REINGRESO DE STOCK
            val itemsConDevolucion = sale.items.filter { 
                (it.dispatchedQty - (soldByProductId[it.productId] ?: 0)) > 0 
            }
            val productRefs = itemsConDevolucion.map { productsCollection.document(it.productId) }

            // 3. REALIZAR TODAS LAS LECTURAS DE PRODUCTOS
            val productSnaps = productRefs.map { transaction.get(it) }
            val productsMap = productSnaps.associate { it.id to (it.toObject(ProductEntity::class.java) ?: error("Producto no encontrado")) }

            // 4. REALIZAR TODAS LAS ESCRITURAS
            val updatedItems = sale.items.map { item ->
                val sold = soldByProductId[item.productId] ?: 0
                val returned = item.dispatchedQty - sold
                
                if (returned > 0) {
                    val currentProduct = productsMap[item.productId] ?: error("Error de sincronización")
                    transaction.update(productsCollection.document(item.productId), "cantidad", currentProduct.cantidad + returned)
                }

                item.copy(soldQty = sold, returnedQty = returned)
            }

            val totalSold = updatedItems.sumOf { (it.soldQty ?: 0) * it.unitPrice }

            transaction.update(saleRef, mapOf(
                "status" to SaleStatus.FINALIZADA,
                "finalizedAtEpochMillis" to System.currentTimeMillis(),
                "total" to totalSold,
                "sellerUid" to sellerUid,
                "sellerName" to sellerName,
                "items" to updatedItems
            ))
        }.await()
    }
}

data class CreateRouteItem(
    val productId: String,
    val unitPrice: Double,
    val quantity: Int
)
