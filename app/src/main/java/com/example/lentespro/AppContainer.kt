package com.example.lentespro

import android.content.Context
import com.example.lentespro.data.AdminUsersRepository
import com.example.lentespro.data.AppDatabase
import com.example.lentespro.data.AuthProfileRepository
import com.example.lentespro.data.AuthRepository
import com.example.lentespro.data.BiometricPrefs
import com.example.lentespro.data.MessengerRepository
import com.example.lentespro.data.ProductRepository
import com.example.lentespro.data.SaleRepository
import com.example.lentespro.data.UsersRemoteRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

class AppContainer(context: Context) {

    private val db = AppDatabase.get(context)

    // Room repos
    val productRepository: ProductRepository = ProductRepository(db.productDao())

    val saleRepository: SaleRepository = SaleRepository(
        db = db,
        saleDao = db.saleDao()
    )

    val messengerRepository: MessengerRepository by lazy {
        MessengerRepository(db.messengerDao())
    }

    // Biometría (local)
    val biometricPrefs = BiometricPrefs(context)

    // ✅ Firebase singletons
    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val firebaseFunctions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }

    // ✅ Admin (crear/borrar usuarios con Cloud Functions + Firestore)
    val adminUsersRepository: AdminUsersRepository by lazy {
        AdminUsersRepository(
            auth = firebaseAuth,
            db = firestore,
            functions = firebaseFunctions
        )
    }

    // ✅ (Opcional) para dropdown de vendedores (Finalizar ruta)
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
