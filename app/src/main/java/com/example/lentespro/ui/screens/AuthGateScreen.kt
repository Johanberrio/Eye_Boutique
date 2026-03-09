package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.lentespro.data.BiometricPrefs
import com.example.lentespro.util.BiometricAuth
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthGateScreen(
    biometricPrefs: BiometricPrefs,
    onGoDashboard: () -> Unit,
    onGoLogin: () -> Unit,
    forceBiometricIfEnabled: Boolean
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

    val auth = remember { FirebaseAuth.getInstance() }
    val loggedIn = auth.currentUser != null

    val snackbar = remember { SnackbarHostState() }

    var biometricEnabled by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        biometricEnabled = biometricPrefs.enabledFlow.first()
        loading = false
    }

    // Redirección automática
    LaunchedEffect(loggedIn, biometricEnabled, forceBiometricIfEnabled, loading) {
        if (loading) return@LaunchedEffect
        
        if (!loggedIn) {
            onGoLogin()
            return@LaunchedEffect
        }

        // Si está logueado, manejar biometría
        if (!biometricEnabled) {
            onGoDashboard()
            return@LaunchedEffect
        }

        if (!forceBiometricIfEnabled) return@LaunchedEffect

        if (activity == null || !BiometricAuth.canAuthenticate(activity)) {
            onGoDashboard()
            return@LaunchedEffect
        }

        BiometricAuth.authenticate(
            activity = activity,
            title = "Entrar a LentesPro",
            subtitle = "Confirma con tu huella",
            onSuccess = onGoDashboard,
            onError = { msg ->
                scope.launch { snackbar.showSnackbar(msg) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
