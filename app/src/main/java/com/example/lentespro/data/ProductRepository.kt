package com.example.lentespro.data

import android.util.Log
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

    fun observeAll(): Flow<List<ProductEntity>> = callbackFlow {
        val subscription = collection
            .orderBy("nombre", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Al cerrar sesión, Firestore lanza un error de permisos.
                    // Cerramos el canal de forma segura para evitar el crash.
                    close() 
                    return@addSnapshotListener
                }
                
                val products = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ProductEntity::class.java)
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error deserializando producto ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()
                
                trySend(products)
            }
        awaitClose { subscription.remove() }
    }

    fun observeSearch(q: String): Flow<List<ProductEntity>> = callbackFlow {
        val subscription = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close()
                return@addSnapshotListener
            }
            val products = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val p = doc.toObject(ProductEntity::class.java)
                    if (p != null && (p.nombre.contains(q, true) || p.marca.contains(q, true))) p else null
                } catch (e: Exception) { null }
            } ?: emptyList()
            trySend(products)
        }
        awaitClose { subscription.remove() }
    }

    suspend fun getById(id: String): ProductEntity? {
        return try {
            val doc = collection.document(id).get().await()
            doc.toObject(ProductEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun upsert(entity: ProductEntity) {
        val id = if (entity.id.isNotBlank()) entity.id else collection.document().id
        collection.document(id).set(entity).await()
    }

    suspend fun delete(entity: ProductEntity) {
        if (entity.id.isNotBlank()) {
            collection.document(entity.id).delete().await()
        }
    }
}
