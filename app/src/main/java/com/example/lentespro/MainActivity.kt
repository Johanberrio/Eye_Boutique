package com.example.lentespro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.lentespro.ui.navigation.AppNavGraph
import com.example.lentespro.ui.theme.LentesProTheme
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : FragmentActivity() {

    // Launcher para solicitar el permiso de notificaciones (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Aquí podrías manejar si el usuario denegó el permiso
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Usamos el contenedor ya inicializado en la Application
        val container = (application as LentesProApplication).container

        // Solicitar permiso de notificaciones si es necesario (Android 13+)
        checkNotificationPermission()

        // Suscribirse globalmente a las alertas de stock/devoluciones para que TODOS los celulares las reciban
        FirebaseMessaging.getInstance().subscribeToTopic("stock_alerts")

        setContent {
            val darkModePref by container.biometricPrefs.darkModeFlow.collectAsState(initial = null)
            val useDarkTheme = darkModePref ?: isSystemInDarkTheme()

            LentesProTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        container = container,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != 
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
