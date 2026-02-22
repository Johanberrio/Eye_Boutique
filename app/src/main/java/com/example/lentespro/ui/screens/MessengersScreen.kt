package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lentespro.data.MessengerEntity
import com.example.lentespro.ui.viewmodel.MessengersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengersScreen(
    viewModel: MessengersViewModel,
    onBack: () -> Unit
) {
    val list by viewModel.messengers.collectAsState()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mensajeros") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Crear mensajero", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Celular") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Dirección (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    val canSave = name.trim().isNotBlank() && phone.trim().isNotBlank()
                    Button(
                        onClick = {
                            viewModel.create(name, phone, address)
                            name = ""; phone = ""; address = ""
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Guardar") }
                }
            }

            Text("Lista", style = MaterialTheme.typography.titleMedium)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { m ->
                    MessengerRow(m, onDelete = { viewModel.delete(m) })
                }
            }
        }
    }
}

@Composable
private fun MessengerRow(m: MessengerEntity, onDelete: () -> Unit) {
    Card {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(m.name, style = MaterialTheme.typography.titleSmall)
                Text("Cel: ${m.phone}")
                if (!m.address.isNullOrBlank()) Text("Dir: ${m.address}")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}
