package com.example.lentespro.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class SaleEntity(
    @DocumentId val id: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val status: SaleStatus = SaleStatus.EN_RUTA,
    val dispatchedAtEpochMillis: Long = System.currentTimeMillis(),
    val finalizedAtEpochMillis: Long? = null,
    val messengerName: String? = null,
    val notes: String? = null,
    val total: Double = 0.0,
    val customerName: String = "",
    val customerPhone1: String = "",
    val customerPhone2: String? = null,
    val customerAddress: String = "",
    val customerNeighborhood: String = "",
    val sellerUid: String? = null,
    val sellerName: String? = null,
    val items: List<SaleItemEntity> = emptyList()
)

data class SaleItemEntity(
    val productId: String = "",
    val productName: String = "",
    val lensName: String = "", // ✅ Nombre comercial (ej: Pattaya Blue)
    @get:PropertyName("isHalloween")
    @set:PropertyName("isHalloween")
    var isHalloween: Boolean = false, // ✅ Para identificar que es de Halloween en las ventas
    val unitPrice: Double = 0.0,
    val dispatchedQty: Int = 0,
    val soldQty: Int? = null,
    val returnedQty: Int? = null
)
