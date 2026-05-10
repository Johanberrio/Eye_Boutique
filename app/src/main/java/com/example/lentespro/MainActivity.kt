package com.example.lentespro

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.lentespro.ui.navigation.AppNavGraph
import com.example.lentespro.ui.theme.LentesProTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = AppContainer(applicationContext)

        setContent {
            // ✅ Observamos la preferencia de modo oscuro
            val darkModePref by container.biometricPrefs.darkModeFlow.collectAsState(initial = null)
            
            // Si la preferencia es null, seguimos el sistema. Si no, usamos el valor guardado.
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
}
