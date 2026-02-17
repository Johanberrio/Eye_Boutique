package com.example.lentespro.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("saleId"), Index("productId")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val saleId: Long,
    val productId: Long,

    val productName: String,  // snapshot
    val unitPrice: Double,

    // ✅ Cantidad que salió a ruta (descuenta inventario)
    val dispatchedQty: Int,

    // ✅ Cantidad que sí se vendió (la define el mensajero al final)
    val soldQty: Int? = null,

    // ✅ Cantidad devuelta (si no se vendió)
    val returnedQty: Int? = null
)
