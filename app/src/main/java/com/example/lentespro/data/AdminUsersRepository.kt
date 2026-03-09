package com.example.lentespro.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class AdminUsersRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {
    suspend fun isAdmin(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("role") == "ADMIN"
    }

    suspend fun listUsers(): List<RemoteUserRow> {
        val snap = db.collection("users").get().await()
        return snap.documents.map { d ->
            RemoteUserRow(
                uid = d.id,
                displayName = d.getString("displayName") ?: "Usuario",
                role = d.getString("role") ?: "SELLER",
                active = d.getBoolean("active") ?: true
            )
        }.sortedWith(compareBy({ it.role }, { it.displayName.lowercase() }))
    }

    suspend fun setActive(uid: String, active: Boolean) {
        // ✅ Solo cambia active
        db.collection("users").document(uid).update("active", active).await()
    }
}