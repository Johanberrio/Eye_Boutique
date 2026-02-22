package com.example.lentespro.data

import androidx.room.withTransaction

class SaleRepository(
    private val db: AppDatabase,
    private val saleDao: SaleDao
) {
    fun observeSales() = saleDao.observeSales()
    fun observeSaleWithItems(id: Long) = saleDao.observeSaleWithItems(id)
    fun observeSaleHistoryCards() = saleDao.observeSaleHistoryCards()

    suspend fun getSaleWithItemsOnce(saleId: Long) = db.withTransaction {
        saleDao.getSaleWithItems(saleId)
    }

    /**
     * ✅ 1) SALIDA A RUTA:
     * - Inserta Sale con estado EN_RUTA
     * - Inserta items con dispatchedQty
     * - Descuenta inventario (cantidad -= dispatchedQty)
     * - ✅ Guarda datos del CLIENTE en SaleEntity
     */
    suspend fun createRouteDispatch(
        messengerName: String?,
        notes: String?,
        items: List<CreateRouteItem>,

        // ✅ NUEVO: Datos del cliente
        customerName: String,
        customerPhone1: String,
        customerPhone2: String?,
        customerAddress: String,
        customerNeighborhood: String
    ): Long {
        require(items.isNotEmpty()) { "La salida a ruta no puede estar vacía." }

        // Validaciones mínimas (las fuertes ya las haces en el ViewModel)
        /*require(customerName.trim().isNotBlank()) { "Nombre del cliente es obligatorio." }
        require(customerPhone1.trim().isNotBlank()) { "Celular 1 del cliente es obligatorio." }
        require(customerAddress.trim().isNotBlank()) { "Dirección del cliente es obligatoria." }
        require(customerNeighborhood.trim().isNotBlank()) { "Barrio del cliente es obligatorio." }*/

        return db.withTransaction {
            val normalized = items.map { it.copy(quantity = it.quantity.coerceAtLeast(1)) }

            val products = normalized.associate { i ->
                i.productId to (saleDao.getProductById(i.productId)
                    ?: error("Producto no encontrado (id=${i.productId})"))
            }

            normalized.forEach { i ->
                val p = products.getValue(i.productId)
                if (p.cantidad < i.quantity) {
                    error("Stock insuficiente para '${p.nombre}'. Disponible: ${p.cantidad}, pedido: ${i.quantity}")
                }
            }

            val saleId = saleDao.insertSale(
                SaleEntity(
                    status = SaleStatus.EN_RUTA,
                    dispatchedAtEpochMillis = System.currentTimeMillis(),
                    messengerName = messengerName?.trim()?.ifBlank { null },
                    notes = notes?.trim()?.ifBlank { null },
                    total = 0.0,

                    // ✅ NUEVO: cliente
                    customerName = customerName.trim(),
                    customerPhone1 = customerPhone1.trim(),
                    customerPhone2 = customerPhone2?.trim()?.ifBlank { null },
                    customerAddress = customerAddress.trim(),
                    customerNeighborhood = customerNeighborhood.trim()
                )
            )

            val itemEntities = normalized.map { i ->
                val p = products.getValue(i.productId)
                SaleItemEntity(
                    saleId = saleId,
                    productId = p.id,
                    productName = "${p.nombre} (${p.marca})",
                    unitPrice = i.unitPrice,
                    dispatchedQty = i.quantity,
                    soldQty = null,
                    returnedQty = null
                )
            }
            saleDao.insertItems(itemEntities)

            // Descuenta inventario al salir a ruta
            normalized.forEach { i ->
                val p = products.getValue(i.productId)
                saleDao.updateProduct(p.copy(cantidad = p.cantidad - i.quantity))
            }

            saleId
        }
    }

    /**
     * ✅ 2) FINALIZAR:
     * - Recibe "soldQty" por producto (o por item)
     * - Calcula devueltos = dispatched - sold
     * - Reingresa devueltos al inventario
     * - Actualiza items (soldQty/returnedQty)
     * - Actualiza Sale a FINALIZADA con total de vendido
     */
    suspend fun finalizeDispatch(
        saleId: Long,
        soldByProductId: Map<Long, Int>
    ) {
        db.withTransaction {
            val sw = saleDao.getSaleWithItems(saleId) ?: error("Salida no encontrada (id=$saleId).")
            if (sw.sale.status != SaleStatus.EN_RUTA) {
                error("Esta salida ya fue finalizada.")
            }

            val updatedItems = sw.items.map { item ->
                val sold = (soldByProductId[item.productId] ?: 0).coerceAtLeast(0)
                if (sold > item.dispatchedQty) {
                    error("No puedes marcar vendido más de lo despachado para ${item.productName}.")
                }
                val returned = item.dispatchedQty - sold
                item.copy(
                    soldQty = sold,
                    returnedQty = returned
                )
            }

            // Reingresar inventario de devueltos
            updatedItems.forEach { it ->
                val returned = it.returnedQty ?: 0
                if (returned > 0) {
                    val p = saleDao.getProductById(it.productId)
                        ?: error("Producto no encontrado al finalizar (id=${it.productId})")
                    saleDao.updateProduct(p.copy(cantidad = p.cantidad + returned))
                }
            }

            saleDao.updateItems(updatedItems)

            val totalSold = updatedItems.sumOf { (it.soldQty ?: 0) * it.unitPrice }

            saleDao.updateSale(
                sw.sale.copy(
                    status = SaleStatus.FINALIZADA,
                    finalizedAtEpochMillis = System.currentTimeMillis(),
                    total = totalSold
                )
            )
        }
    }
}

data class CreateRouteItem(
    val productId: Long,
    val unitPrice: Double,
    val quantity: Int
)