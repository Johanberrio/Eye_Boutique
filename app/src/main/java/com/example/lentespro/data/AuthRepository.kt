package com.example.lentespro.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()

        // ✅ Verificar active en Firestore
        val uid = auth.currentUser?.uid ?: error("No uid")
        val doc = db.collection("users").document(uid).get().await()

        val active = doc.getBoolean("active") ?: true
        if (!active) {
            auth.signOut()
            error("Usuario desactivado. Contacta al administrador.")
        }
    }

    suspend fun registerSeller(email: String, password: String, displayName: String) {
        val res = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = res.user?.uid ?: error("No uid")

        // ✅ Crear perfil en Firestore con active=true
        db.collection("users").document(uid).set(
            mapOf(
                "displayName" to displayName.trim(),
                "role" to "SELLER",
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun getMyRole(): String {
        val uid = auth.currentUser?.uid ?: return "SELLER"
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("role") ?: "SELLER"
    }

    fun logout() = auth.signOut()
}