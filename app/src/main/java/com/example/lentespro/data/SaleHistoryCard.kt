package com.example.lentespro.data

data class SaleHistoryCard(
    val saleId: String,
    val saleNumber: Int,
    val soldAtEpochMillis: Long,
    val messengerName: String?,
    val total: Double,
    val firstItemName: String,
    val soldQty: Int,
    val customerName: String?,
    val customerPhone1: String?,
    val customerNeighborhood: String?, // ✅ Añadido barrio para el historial
    val sellerUid: String?,
    val sellerName: String?
)
