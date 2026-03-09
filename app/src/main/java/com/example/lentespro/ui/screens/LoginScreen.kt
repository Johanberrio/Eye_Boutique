package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.lentespro.data.BiometricPrefs
import com.example.lentespro.data.AuthRepository
import com.example.lentespro.util.BiometricAuth
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    biometricPrefs: BiometricPrefs,
    authRepo: AuthRepository,
    onLoggedIn: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

    val auth = remember { FirebaseAuth.getInstance() }
    val snackbar = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }

    fun afterAuthSuccess() {
        scope.launch {
            val uid = auth.currentUser?.uid
            val canUseBio = enableBiometric &&
                    activity != null &&
                    BiometricAuth.canAuthenticate(activity)

            biometricPrefs.setEnabled(canUseBio)
            biometricPrefs.setLastUid(uid)
            onLoggedIn()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Login") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Habilitar huella")
                Switch(checked = enableBiometric, onCheckedChange = { enableBiometric = it })
            }

            Button(
                enabled = !loading,
                onClick = {
                    scope.launch {
                        try {
                            loading = true
                            authRepo.login(email, password)
                            afterAuthSuccess()
                        } catch (t: Throwable) {
                            snackbar.showSnackbar(t.message ?: "Error al iniciar sesión")
                        } finally {
                            loading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (loading) "Entrando..." else "Entrar")
            }
        }
    }
}
