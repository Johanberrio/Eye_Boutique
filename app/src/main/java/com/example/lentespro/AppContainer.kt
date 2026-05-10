package com.example.lentespro

import android.content.Context
import com.example.lentespro.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

class AppContainer(context: Context) {

    // ✅ Firebase singletons
    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val firebaseFunctions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }

    // ✅ Repositorios
    val productRepository: ProductRepository by lazy {
        ProductRepository(firestore)
    }

    val saleRepository: SaleRepository by lazy {
        SaleRepository(firestore)
    }

    val messengerRepository: MessengerRepository by lazy {
        MessengerRepository(firestore)
    }
    
    // ✅ Nuevo: Repositorio para notas informativas
    val adminNotesRepository: AdminNotesRepository by lazy {
        AdminNotesRepository(firestore)
    }

    // Biometría (local)
    val biometricPrefs = BiometricPrefs(context)

    // Gestión de usuarios
    val adminUsersRepository: AdminUsersRepository by lazy {
        AdminUsersRepository(
            auth = firebaseAuth,
            db = firestore,
            functions = firebaseFunctions
        )
    }

    val usersRemoteRepository: UsersRemoteRepository by lazy {
        UsersRemoteRepository(firestore)
    }

    val authProfileRepository: AuthProfileRepository by lazy {
        AuthProfileRepository(firebaseAuth, firestore)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(firebaseAuth, firestore)
    }
}
