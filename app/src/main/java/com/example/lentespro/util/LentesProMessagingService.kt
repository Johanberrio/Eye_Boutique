package com.example.lentespro.util

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class LentesProMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Cuando la app está en segundo plano/cerrada, procesamos los datos ("data payload")
        if (remoteMessage.data.isNotEmpty()) {
            val productName = remoteMessage.data["productName"] ?: "Lente"

            // Reutilizamos tu lógica de NotificationHelper existente
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.notifyStockOut(productName)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Si en el futuro necesitas enviar notificaciones a usuarios específicos en lugar de un canal global,
        // aquí es donde asocias este token al UID del usuario en Firestore.
    }
}