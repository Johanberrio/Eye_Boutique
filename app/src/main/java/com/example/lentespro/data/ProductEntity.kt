package com.example.lentespro.data

import com.google.firebase.firestore.DocumentId

/**
 * Entidad que representa un producto en el inventario.
 * Ahora está preparada para funcionar directamente con Cloud Firestore.
 */
data class ProductEntity(
    @DocumentId val id: String = "", // El ID del documento de Firestore

    // Identidad del producto
    val nombre: String = "",
    val marca: String = "",
    val color: String = "",
    val tipo: String = "", // diaria / quincenal / mensual / anual / etc.

    // Parámetros ópticos
    val potenciaEsferica: Double = 0.0,
    val cilindro: Double? = null,
    val eje: Int? = null,
    val curvaBase: Double? = null,
    val diametro: Double? = null,

    // Inventario y precios
    val cantidad: Int = 0,
    val stockMinimo: Int = 0,
    val precioCompra: Double = 0.0,
    val precioVenta: Double = 0.0,

    // Trazabilidad
    val fechaCaducidadEpochMillis: Long? = null,
    val lote: String? = null,
    val notas: String? = null,

    val actualizadoEnEpochMillis: Long = System.currentTimeMillis()
)
