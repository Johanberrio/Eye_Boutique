package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lentespro.ui.viewmodel.FinalizeEvent
import com.example.lentespro.ui.viewmodel.FinalizeRouteViewModel
import com.example.lentespro.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalizeRouteScreen(
    viewModel: FinalizeRouteViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is FinalizeEvent.Error -> snackbar.showSnackbar(ev.message)
                FinalizeEvent.Success -> {
                    snackbar.showSnackbar("Ruta finalizada ✅ Inventario actualizado")
                    onBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finalizar ruta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Salida #${state.saleId}", style = MaterialTheme.typography.titleMedium)
                    Text("Mensajero: ${state.messengerName.ifBlank { "—" }}")
                    if (state.notes.isNotBlank()) Text("Notas: ${state.notes}")
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        // vendido todo = sold = dispatched
                        state.lines.forEach { line ->
                            viewModel.setSold(line.productId, line.dispatched)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Vendido todo")
                }

                OutlinedButton(
                    onClick = {
                        // nada vendido = sold = 0
                        state.lines.forEach { line ->
                            viewModel.setSold(line.productId, 0)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DoNotDisturbOn, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Nada se vendió")
                }
            }

            Text("Productos despachados", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.lines, key = { it.productId }) { line ->
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(line.name, style = MaterialTheme.typography.titleSmall)
                            Text("Despachado: ${line.dispatched}  |  Precio: ${Formatters.money(line.unitPrice)}")

                            OutlinedTextField(
                                value = line.sold.toString(),
                                onValueChange = { v ->
                                    val sold = v.toIntOrNull() ?: 0
                                    viewModel.setSold(line.productId, sold)
                                },
                                label = { Text("Vendido") },
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Devuelto: ${line.returned}")
                                }
                                Text("Subtotal vendido: ${Formatters.money(line.soldTotal)}")
                            }
                        }
                    }
                }
            }

            Card {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total vendido")
                    Text(Formatters.money(state.totalSold))
                }
            }

            Button(
                onClick = viewModel::finalize,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSaving) "Finalizando..." else "Confirmar vendido y devoluciones")
            }
        }
    }
}
