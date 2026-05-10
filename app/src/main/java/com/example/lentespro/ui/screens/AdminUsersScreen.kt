package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lentespro.data.RemoteUserRow
import com.example.lentespro.ui.viewmodel.AdminUsersEvent
import com.example.lentespro.ui.viewmodel.AdminUsersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel: AdminUsersViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.ui.collectAsState()
    val event by viewModel.events.collectAsState(initial = null)
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(event) {
        when (val e = event) {
            is AdminUsersEvent.Error -> snackbar.showSnackbar(e.msg)
            is AdminUsersEvent.Message -> snackbar.showSnackbar(e.msg)
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin - Usuarios") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                !state.isAdmin -> {
                    Text(
                        text = state.error ?: "No autorizado.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                "Usuarios (${state.users.size})",
                                style = MaterialTheme.typography.titleMedium
                            )

                        }

                        items(state.users, key = { it.uid }) { user ->
                            UserRowCard(
                                user = user,
                                currentUserIsSuperAdmin = state.isSuperAdmin, // ✅ Pasamos si quien opera es SuperAdmin
                                isWorking = state.isDeletingUid == user.uid,
                                onToggleActive = { newActive ->
                                    viewModel.setActive(user, newActive)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRowCard(
    user: RemoteUserRow,
    currentUserIsSuperAdmin: Boolean, // ✅ Nuevo parámetro
    isWorking: Boolean,
    onToggleActive: (Boolean) -> Unit
) {
    val targetIsSuperAdmin = user.role.uppercase() == "SUPERADMIN"
    val targetIsAdmin = user.role.uppercase() == "ADMIN"
    
    // ✅ Un SuperAdmin puede editar a cualquier Admin, pero nadie puede editar a un SuperAdmin (solo por consola)
    val canToggle = !isWorking && when {
        targetIsSuperAdmin -> false
        targetIsAdmin -> currentUserIsSuperAdmin
        else -> true // Vendedores siempre editables por Admin/SuperAdmin
    }

    Card {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                Text("Rol: ${user.role}")
                Text(
                    "Estado: ${if (user.active) "Activo" else "Desactivado"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("UID: ${user.uid.take(10)}...", style = MaterialTheme.typography.bodySmall)
            }

            if (isWorking) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    if (!canToggle && (targetIsAdmin || targetIsSuperAdmin)) {
                        AssistChip(onClick = {}, label = { Text(user.role.uppercase()) })
                        Spacer(Modifier.height(6.dp))
                        Text("No editable", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Activo")
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = user.active,
                                onCheckedChange = { onToggleActive(it) },
                                enabled = canToggle
                            )
                        }
                    }
                }
            }
        }
    }
}
