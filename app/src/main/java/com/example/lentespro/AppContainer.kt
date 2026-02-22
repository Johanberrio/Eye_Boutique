package com.example.lentespro

import android.content.Context
import com.example.lentespro.data.AppDatabase
import com.example.lentespro.data.MessengerRepository
import com.example.lentespro.data.ProductRepository
import com.example.lentespro.data.SaleRepository

class AppContainer(context: Context) {
    private val db = AppDatabase.get(context)
    val productRepository: ProductRepository = ProductRepository(db.productDao())
    val saleRepository: SaleRepository = SaleRepository(
        db = db,
        saleDao = db.saleDao()
    )

    val messengerRepository: MessengerRepository by lazy {
        MessengerRepository(db.messengerDao())
    }

}














