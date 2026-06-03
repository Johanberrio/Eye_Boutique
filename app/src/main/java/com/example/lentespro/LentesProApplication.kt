package com.example.lentespro

import android.app.Application
import androidx.work.*
import com.example.lentespro.util.NotificationHelper
import com.example.lentespro.util.StockMonitor
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import kotlinx.coroutines.MainScope
import java.util.concurrent.TimeUnit


class LentesProApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // 1. Monitoreo en tiempo real (Sigue funcionando de forma óptima con la app abierta)
        val notificationHelper = NotificationHelper(this)
        val stockMonitor = StockMonitor(
            repo = container.productRepository,
            notificationHelper = notificationHelper,
            scope = MainScope()
        )
        stockMonitor.start()

        // 2. ✅ PASO 4: Suscribirse al canal de notificaciones en la nube
        FirebaseMessaging.getInstance().subscribeToTopic("stock_alerts")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Suscrito exitosamente a stock_alerts")
                } else {
                    Log.e("FCM", "Error al suscribirse a stock_alerts")
                }
            }
    }

}
