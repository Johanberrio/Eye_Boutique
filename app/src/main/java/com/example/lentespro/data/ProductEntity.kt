package com.example.lentespro.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Entidad que representa un producto en el inventario.
 */
data class ProductEntity(
    @DocumentId val id: String = "",

    // Identidad del producto
    val nombre: String = "",
    val marca: String = "",
    val color: String = "",
    val tipo: String = "",
    val imageUrl: String? = null, // ✅ Nueva: URL de la imagen en Firebase Storage

    // Parámetros ópticos (Solicitados: Esfera y Diámetro)
    val potenciaEsferica: Double = 0.0,
    val diametro: Double? = null,

    // Inventario y precios
    val cantidad: Int = 0,
    val stockMinimo: Int = 0,
    val precioCompra: Double = 0.0,
    val precioVenta: Double = 0.0,

    @get:PropertyName("isHalloween")
    @set:PropertyName("isHalloween")
    var isHalloween: Boolean = false, // ✅ Etiqueta para lentes de Halloween

    // Trazabilidad
    val fechaCaducidadEpochMillis: Long? = null,
    val lote: String? = null,
    val notas: String? = null,

    val actualizadoEnEpochMillis: Long = System.currentTimeMillis()
)
