package com.example.lentespro.data

import com.google.firebase.firestore.DocumentId

/**
 * Entidad que representa un mensajero.
 * Migrada a Firestore para sincronización multi-dispositivo.
 */
data class MessengerEntity(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String? = null
)
