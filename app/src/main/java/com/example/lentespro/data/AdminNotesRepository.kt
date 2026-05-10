package com.example.lentespro.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AdminNotesRepository(private val db: FirebaseFirestore) {
    private val docRef = db.collection("settings").document("admin_notes")

    fun observeNotes(): Flow<String> = callbackFlow {
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val text = snapshot?.getString("content") ?: ""
            trySend(text)
        }
        awaitClose { subscription.remove() }
    }

    suspend fun updateNotes(text: String) {
        docRef.set(mapOf("content" to text)).await()
    }
}
