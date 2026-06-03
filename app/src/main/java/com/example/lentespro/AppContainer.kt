package com.example.lentespro

import android.content.Context
import com.example.lentespro.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

class AppContainer(context: Context) {

    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val firebaseFunctions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }

    val productRepository: ProductRepository by lazy { ProductRepository(firestore) }
    val saleRepository: SaleRepository by lazy { SaleRepository(firestore) }
    val messengerRepository: MessengerRepository by lazy { MessengerRepository(firestore) }
    val adminNotesRepository: AdminNotesRepository by lazy { AdminNotesRepository(firestore) }
    
    // ✅ Repositorio de Gemini (Obtén la API KEY de BuildConfig o variables de entorno)
    val geminiRepository: GeminiRepository by lazy {
        GeminiRepository(apiKey = "TU_CLAVE_AQUI")
    }

    val biometricPrefs = BiometricPrefs(context)
    val adminUsersRepository: AdminUsersRepository by lazy { AdminUsersRepository(firebaseAuth, firestore, firebaseFunctions) }
    val usersRemoteRepository: UsersRemoteRepository by lazy { UsersRemoteRepository(firestore) }
    val authProfileRepository: AuthProfileRepository by lazy { AuthProfileRepository(firebaseAuth, firestore) }
    val authRepository: AuthRepository by lazy { AuthRepository(firebaseAuth, firestore) }
}
