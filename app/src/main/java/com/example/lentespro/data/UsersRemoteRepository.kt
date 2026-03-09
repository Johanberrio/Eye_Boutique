package com.example.lentespro.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class SellerOption(
    val uid: String,
    val displayName: String
)

class UsersRemoteRepository(
    private val db: FirebaseFirestore
) {
    suspend fun getSellers(): List<SellerOption> {
        val snap = db.collection("users")
            .whereEqualTo("role", "SELLER")
            .get()
            .await()

        return snap.documents.map { d ->
            SellerOption(
                uid = d.id,
                displayName = d.getString("displayName") ?: "Vendedor"
            )
        }.sortedBy { it.displayName.lowercase() }
    }
}