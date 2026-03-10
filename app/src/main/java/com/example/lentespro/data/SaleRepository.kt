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
                    close(error)
                    return@addSnapshotListener
                }
                val sales = snapshot?.documents?.mapNotNull { it.toObject(SaleEntity::class.java) } ?: emptyList()
                trySend(sales)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getSaleOnce(saleId: String): SaleEntity? {
        val doc = salesCollection.document(saleId).get().await()
        return doc.toObject(SaleEntity::class.java)
    }

    /**
     * ✅ CREAR RUTA (DISPATCH):
     * - Resta stock en Firestore.
     * - Guarda la venta con sus items embebidos.
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
            val saleDocRef = salesCollection.document()
            
            // 1. Validar y actualizar stock de productos
            val saleItems = items.map { item ->
                val productRef = productsCollection.document(item.productId)
                val productSnap = transaction.get(productRef)
                val product = productSnap.toObject(ProductEntity::class.java) ?: error("Producto no encontrado")
                
                if (product.cantidad < item.quantity) {
                    error("Stock insuficiente para ${product.nombre}")
                }
                
                // Restar stock
                transaction.update(productRef, "cantidad", product.cantidad - item.quantity)
                
                SaleItemEntity(
                    productId = item.productId,
                    productName = "${product.nombre} (${product.marca})",
                    unitPrice = item.unitPrice,
                    dispatchedQty = item.quantity
                )
            }

            // 2. Crear la entidad de venta
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
     * - Calcula vendidos y devueltos.
     * - Reingresa stock de devueltos a Firestore.
     * - Marca la venta como FINALIZADA.
     */
    suspend fun finalizeDispatch(
        saleId: String,
        soldByProductId: Map<String, Int>,
        sellerUid: String,
        sellerName: String
    ) {
        db.runTransaction { transaction ->
            val saleRef = salesCollection.document(saleId)
            val saleSnap = transaction.get(saleRef)
            val sale = saleSnap.toObject(SaleEntity::class.java) ?: error("Venta no encontrada")

            if (sale.status != SaleStatus.EN_RUTA) error("Esta venta ya fue finalizada")

            val updatedItems = sale.items.map { item ->
                val sold = soldByProductId[item.productId] ?: 0
                val returned = item.dispatchedQty - sold
                
                // Reingresar stock de devueltos
                if (returned > 0) {
                    val productRef = productsCollection.document(item.productId)
                    val productSnap = transaction.get(productRef)
                    val product = productSnap.toObject(ProductEntity::class.java) ?: error("Producto no encontrado")
                    transaction.update(productRef, "cantidad", product.cantidad + returned)
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
