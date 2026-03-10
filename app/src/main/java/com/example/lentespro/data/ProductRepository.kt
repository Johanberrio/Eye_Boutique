package com.example.lentespro.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository(
    private val db: FirebaseFirestore
) {
    private val collection = db.collection("products")

    /**
     * Observa todos los productos en tiempo real desde Firestore.
     */
    fun observeAll(): Flow<List<ProductEntity>> = callbackFlow {
        val subscription = collection
            .orderBy("nombre", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val products = snapshot?.documents?.mapNotNull { doc ->
                    // Use doc.id directly as it is already a String
                    doc.toObject(ProductEntity::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(products)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Búsqueda simple (en Firestore es mejor filtrar en local si la lista no es gigante,
     * o usar servicios externos. Por ahora, filtramos todos).
     */
    fun observeSearch(q: String): Flow<List<ProductEntity>> = callbackFlow {
        val subscription = collection.addSnapshotListener { snapshot, _ ->
            val products = snapshot?.documents?.mapNotNull { it.toObject(ProductEntity::class.java) }
                ?.filter { 
                    it.nombre.contains(q, true) || it.marca.contains(q, true) 
                } ?: emptyList()
            trySend(products)
        }
        awaitClose { subscription.remove() }
    }

    suspend fun getById(id: String): ProductEntity? {
        val doc = collection.document(id).get().await()
        return doc.toObject(ProductEntity::class.java)
    }

    suspend fun upsert(entity: ProductEntity) {
        // Si el nombre se usa como ID o generamos uno nuevo
        val id = if (entity.nombre.isNotBlank()) entity.nombre + "_" + entity.marca else db.collection("products").document().id
        collection.document(id).set(entity).await()
    }

    suspend fun delete(entity: ProductEntity) {
        val id = entity.nombre + "_" + entity.marca
        collection.document(id).delete().await()
    }
}
