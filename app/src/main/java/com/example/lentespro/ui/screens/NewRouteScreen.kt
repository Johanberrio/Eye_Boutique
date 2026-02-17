package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lentespro.ui.viewmodel.NewRouteEvent
import com.example.lentespro.ui.viewmodel.NewRouteViewModel
import com.example.lentespro.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRouteScreen(
    viewModel: NewRouteViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is NewRouteEvent.Error -> snackbar.showSnackbar(ev.message)
                is NewRouteEvent.Success -> {
                    snackbar.showSnackbar("Salida a ruta creada ✅ (id=${ev.saleId})")
                    onBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva salida a ruta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.messengerName,
                onValueChange = viewModel::setMessengerName,
                label = { Text("Mensajero (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                label = { Text("Notas / ruta (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            Text("Domicilios (sale a ruta)", style = MaterialTheme.typography.titleMedium)

            if (state.cart.isEmpty()) {
                Text("Agrega productos abajo para enviarlos a ruta.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.cart, key = { it.productId }) { line ->
                        Card {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(line.name, style = MaterialTheme.typography.titleSmall)
                                    IconButton(onClick = { viewModel.removeFromCart(line.productId) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Quitar")
                                    }
                                }

                                Text("Stock disponible: ${line.stock}", style = MaterialTheme.typography.bodySmall)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = line.quantity.toString(),
                                        onValueChange = { v ->
                                            val q = v.toIntOrNull() ?: 1
                                            viewModel.setQty(line.productId, q)
                                        },
                                        label = { Text("Cantidad") },
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = line.unitPrice.toString(),
                                        onValueChange = { v ->
                                            val p = v.replace(',', '.').toDoubleOrNull() ?: line.unitPrice
                                            viewModel.setUnitPrice(line.productId, p)
                                        },
                                        label = { Text("Precio unit.") },
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text("Subtotal: ${Formatters.money(line.lineTotal)}")
                            }
                        }
                    }
                }
            }

            Divider()

            Text("Agregar productos", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = state.search,
                onValueChange = viewModel::setSearch,
                label = { Text("Buscar producto") },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.products, key = { it.id }) { p ->
                    Card(
                        onClick = { viewModel.addToCart(p) }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${p.nombre} (${p.marca})", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Stock: ${p.cantidad} | Venta: ${Formatters.money(p.precioVenta)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Button(
                onClick = viewModel::dispatchToRoute,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.TwoWheeler, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.isSaving) "Enviando..." else "Marcar como salió a ruta")
            }
        }
    }
}
