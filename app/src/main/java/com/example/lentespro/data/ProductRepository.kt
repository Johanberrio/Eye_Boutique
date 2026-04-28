package com.example.lentespro.data

import android.util.Log
import com.example.lentespro.util.Formatters
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

    /**
     * Búsqueda optimizada que ignora tildes y mayúsculas/minúsculas.
     * Normaliza tanto la consulta como los datos almacenados para la comparación.
     */
    fun observeSearch(q: String): Flow<List<ProductEntity>> = callbackFlow {
        val normalizedQuery = Formatters.normalize(q)
        
        val subscription = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close()
                return@addSnapshotListener
            }
            
            val products = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val p = doc.toObject(ProductEntity::class.java) ?: return@mapNotNull null
                    
                    val normalizedNombre = Formatters.normalize(p.nombre)
                    val normalizedMarca = Formatters.normalize(p.marca)
                    
                    if (normalizedNombre.contains(normalizedQuery) || normalizedMarca.contains(normalizedQuery)) {
                        p
                    } else {
                        null
                    }
                } catch (e: Exception) { 
                    null 
                }
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
