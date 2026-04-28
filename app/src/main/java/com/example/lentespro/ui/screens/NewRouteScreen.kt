package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lentespro.ui.viewmodel.NewRouteEvent
import com.example.lentespro.ui.viewmodel.NewRouteViewModel
import com.example.lentespro.util.Formatters
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewRouteScreen(
    viewModel: NewRouteViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is NewRouteEvent.Error -> snackbar.showSnackbar(ev.message)
                is NewRouteEvent.Success -> {
                    snackbar.showSnackbar("Salida a ruta creada ✅")
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

        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ---------- CLIENTE ----------
            stickyHeader { SectionHeaderSticky("Cliente") }

            item {
                OutlinedTextField(
                    value = state.customerName,
                    onValueChange = viewModel::setCustomerName,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = state.customerPhone1,
                    onValueChange = viewModel::setCustomerPhone1,
                    label = { Text("Celular 1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone)
                )
            }

            item {
                OutlinedTextField(
                    value = state.customerPhone2,
                    onValueChange = viewModel::setCustomerPhone2,
                    label = { Text("Celular 2 (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone)
                )
            }

            item {
                OutlinedTextField(
                    value = state.customerAddress,
                    onValueChange = viewModel::setCustomerAddress,
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = state.customerNeighborhood,
                    onValueChange = viewModel::setCustomerNeighborhood,
                    label = { Text("Barrio") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item { Divider() }

            // ---------- RUTA ----------
            stickyHeader { SectionHeaderSticky("Ruta") }

            item {
                // ✅ Dropdown de Mensajeros
                MessengerSelector(
                    options = state.messengerOptions,
                    selected = state.messengerName,
                    onSelected = viewModel::setMessengerName
                )
            }

            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::setNotes,
                    label = { Text("Notas / ruta (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Divider() }

            // ---------- CARRITO ----------
            stickyHeader { SectionHeaderSticky("Carrito (sale a ruta)") }

            if (state.cart.isEmpty()) {
                item {
                    Text(
                        "Agrega productos abajo para enviarlos a ruta.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                items(state.cart, key = { "cart_${it.productId}" }) { line ->
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
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = line.unitPrice.toString(),
                                    onValueChange = { v ->
                                        val p = v.replace(',', '.').toDoubleOrNull() ?: line.unitPrice
                                        viewModel.setUnitPrice(line.productId, p)
                                    },
                                    label = { Text("Precio unit.") },
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Text("Subtotal: ${Formatters.money(line.lineTotal)}")
                        }
                    }
                }
            }

            item { Divider() }

            // ---------- BUSCAR + LISTA PRODUCTOS ----------
            stickyHeader { SectionHeaderSticky("Agregar productos") }

            item {
                OutlinedTextField(
                    value = state.search,
                    onValueChange = viewModel::setSearch,
                    label = { Text("Buscar producto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (state.products.isEmpty()) {
                item {
                    Text(
                        "No hay productos para mostrar.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                items(state.products, key = { "prod_${it.id}" }) { p ->
                    Card(onClick = { viewModel.addToCart(p) }) {
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

            // ---------- BOTÓN FINAL ----------
            item {
                Button(
                    onClick = viewModel::dispatchToRoute,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isSaving) "Enviando..." else "Marcar como salió a ruta")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessengerSelector(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = { onSelected(it) }, // Permite escribir también si se desea
            label = { Text("Mensajero (opcional)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(name)
                        expanded = false
                    }
                )
            }
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No hay mensajeros creados") },
                    onClick = { expanded = false },
                    enabled = false
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderSticky(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
    }
}
