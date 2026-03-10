package com.example.lentespro.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MessengerRepository(
    private val db: FirebaseFirestore
) {
    private val collection = db.collection("messengers")

    /**
     * Observa todos los mensajeros en tiempo real desde Firestore.
     */
    fun observeAll(): Flow<List<MessengerEntity>> = callbackFlow {
        val subscription = collection
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(MessengerEntity::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Crea un nuevo mensajero en la nube.
     */
    suspend fun create(name: String, phone: String, address: String?) {
        val docRef = collection.document() // Genera un ID automático
        val messenger = MessengerEntity(
            id = docRef.id,
            name = name.trim(),
            phone = phone.trim(),
            address = address?.trim()?.ifBlank { null }
        )
        docRef.set(messenger).await()
    }

    /**
     * Elimina un mensajero de la nube.
     */
    suspend fun delete(entity: MessengerEntity) {
        if (entity.id.isNotBlank()) {
            collection.document(entity.id).delete().await()
        }
    }
}
