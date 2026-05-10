package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lentespro.ui.viewmodel.EditProductEvent
import com.example.lentespro.ui.viewmodel.EditProductViewModel
import androidx.compose.foundation.text.KeyboardOptions


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    viewModel: EditProductViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditProductEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is EditProductEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
                EditProductEvent.NavigateBack -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.id.isBlank()) "Nuevo producto" else "Editar producto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save() }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            SectionTitle("Datos del producto")
            OutlinedTextField(
                value = state.nombre,
                onValueChange = { v -> viewModel.update { it.copy(nombre = v) } },
                label = { Text("Nombre (ej: Pattaya Blue)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.marca,
                onValueChange = { v -> viewModel.update { it.copy(marca = v) } },
                label = { Text("Marca (ej: EyeShare)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.color,
                onValueChange = { v -> viewModel.update { it.copy(color = v) } },
                label = { Text("Color (ej: Azul)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.tipo,
                onValueChange = { v -> viewModel.update { it.copy(tipo = v) } },
                label = { Text("Tipo (Anual/Diaria/etc.)") },
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("Parámetros ópticos")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.potenciaEsferica,
                    onValueChange = { v -> viewModel.update { it.copy(potenciaEsferica = v) } },
                    label = { Text("Esfera (D)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = state.diametro,
                onValueChange = { v -> viewModel.update { it.copy(diametro = v) } },
                label = { Text("Diámetro (mm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("Inventario y precios")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.cantidad,
                    onValueChange = { v -> viewModel.update { it.copy(cantidad = v) } },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.stockMinimo,
                    onValueChange = { v -> viewModel.update { it.copy(stockMinimo = v) } },
                    label = { Text("Stock mínimo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.precioVenta,
                    onValueChange = { v -> viewModel.update { it.copy(precioVenta = v) } },
                    label = { Text("Precio venta") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            SectionTitle("Caducidad y notas")
            OutlinedTextField(
                value = state.fechaCaducidad,
                onValueChange = { v -> viewModel.update { it.copy(fechaCaducidad = v) } },
                label = { Text("Caducidad (yyyy-MM-dd) - opcional") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.lote,
                onValueChange = { v -> viewModel.update { it.copy(lote = v) } },
                label = { Text("Lote - opcional") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.notas,
                onValueChange = { v -> viewModel.update { it.copy(notas = v) } },
                label = { Text("Notas - opcional") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}
