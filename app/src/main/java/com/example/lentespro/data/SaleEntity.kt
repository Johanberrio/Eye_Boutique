package com.example.lentespro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    val createdAtEpochMillis: Long = System.currentTimeMillis(),

    // 🛵 logística
    val status: SaleStatus = SaleStatus.EN_RUTA,
    val dispatchedAtEpochMillis: Long = System.currentTimeMillis(),
    val finalizedAtEpochMillis: Long? = null,
    val messengerName: String? = null,

    // info opcional
    val customerName: String? = null,
    val notes: String? = null,

    // total final cobrado (solo vendido)
    val total: Double = 0.0
)
