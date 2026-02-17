package com.example.lentespro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    // Identidad del producto
    val nombre: String,
    val marca: String,
    val color: String,
    val tipo: String, // diaria / quincenal / mensual / anual / etc.

    // Parámetros ópticos (según aplique)
    val potenciaEsferica: Double,   // Sphere (D)
    val cilindro: Double?,          // Cylinder (D)
    val eje: Int?,                  // Axis (0..180)
    val curvaBase: Double?,         // BC
    val diametro: Double?,          // DIA

    // Inventario y precios
    val cantidad: Int,
    val stockMinimo: Int,
    val precioCompra: Double,
    val precioVenta: Double,

    // Trazabilidad
    val fechaCaducidadEpochMillis: Long?, // null si no aplica
    val lote: String?,
    val notas: String?,

    val actualizadoEnEpochMillis: Long = System.currentTimeMillis()
)











