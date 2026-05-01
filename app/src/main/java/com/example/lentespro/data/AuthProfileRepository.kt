package com.example.lentespro.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class UserRole { SUPERADMIN, ADMIN, SELLER }

data class UserProfile(
    val uid: String,
    val displayName: String,
    val role: UserRole
)

class AuthProfileRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    suspend fun isAdmin(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val doc = db.collection("users").document(uid).get().await()
        val role = doc.getString("role")
        return role == "ADMIN" || role == "SUPERADMIN"
    }

    suspend fun isSuperAdmin(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("role") == "SUPERADMIN"
    }

    suspend fun displayName(): String {
        val uid = auth.currentUser?.uid ?: return "Usuario"
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("displayName") ?: "Usuario"
    }

    suspend fun getUserProfile(): UserProfile {
        val user = auth.currentUser ?: error("No hay sesión activa")
        val doc = db.collection("users").document(user.uid).get().await()
        
        val roleStr = doc.getString("role") ?: "SELLER"
        val role = when (roleStr) {
            "SUPERADMIN" -> UserRole.SUPERADMIN
            "ADMIN" -> UserRole.ADMIN
            else -> UserRole.SELLER
        }
        
        return UserProfile(
            uid = user.uid,
            displayName = doc.getString("displayName") ?: "Usuario",
            role = role
        )
    }
}
