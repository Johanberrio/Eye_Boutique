package com.example.lentespro.util

import com.example.lentespro.data.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StockMonitor(
    private val repo: ProductRepository,
    private val notificationHelper: NotificationHelper,
    private val scope: CoroutineScope
) {
    // Mantener registro de qué productos ya notificamos en esta sesión para no repetir
    private val notifiedIds = mutableSetOf<String>()
    
    // Registro de productos que tenían stock para detectar el cambio a 0
    private val productsWithStock = mutableSetOf<String>()

    fun start() {
        scope.launch(Dispatchers.IO) {
            repo.observeAll().collectLatest { products ->
                products.forEach { product ->
                    val isOutOfStock = product.cantidad <= 0
                    
                    if (!isOutOfStock) {
                        // El producto tiene stock, lo marcamos para monitorear cuando llegue a 0
                        productsWithStock.add(product.id)
                        notifiedIds.remove(product.id)
                    } else {
                        // El producto está agotado. 
                        // Verificamos si antes tenía stock (para detectar el momento exacto en que se agota)
                        // o si no ha sido notificado aún en esta sesión.
                        if (productsWithStock.contains(product.id) && !notifiedIds.contains(product.id)) {
                            notificationHelper.notifyStockOut(product.nombre)
                            notifiedIds.add(product.id)
                            productsWithStock.remove(product.id)
                        }
                    }
                }
            }
        }
    }
}
