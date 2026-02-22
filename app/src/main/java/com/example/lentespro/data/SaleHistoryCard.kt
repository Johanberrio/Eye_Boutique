package com.example.lentespro.data

data class SaleHistoryCard(
    val saleId: Long,
    val soldAtEpochMillis: Long,
    val messengerName: String?,
    val total: Double,
    val productName: String,
    val soldQty: Int,
    val customerName: String?,
    val customerPhone1: String?
)


