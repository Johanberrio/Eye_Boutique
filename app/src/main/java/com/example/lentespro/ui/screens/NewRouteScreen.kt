package com.example.lentespro.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lentespro.ui.viewmodel.NewRouteEvent
import com.example.lentespro.ui.viewmodel.NewRouteViewModel
import com.example.lentespro.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewRouteScreen(
    viewModel: NewRouteViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val geminiCustomerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.analyzeCustomerImage(it, context) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is NewRouteEvent.Error -> snackbar.showSnackbar(ev.message)
                is NewRouteEvent.Success -> {
                    snackbar.showSnackbar("Salida a ruta creada ✅")
                    onBack()
                }
                is NewRouteEvent.Info -> snackbar.showSnackbar(ev.message)
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
            
            // ✅ RESTAURADO: Auto-completado desde TEXTO
            stickyHeader { SectionHeaderSticky("Auto-completar desde texto") }
            item {
                OutlinedTextField(
                    value = state.autoFillText,
                    onValueChange = viewModel::setAutoFillText,
                    label = { Text("Pega aquí los datos (WhatsApp, etc.)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Button(
                    onClick = viewModel::processAutoFill,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = state.autoFillText.isNotBlank()
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Procesar texto")
                }
            }

            // ✅ MANTENIDO: Auto-completado con GEMINI AI ✨
            stickyHeader { SectionHeaderSticky("Auto-completar con Gemini ✨") }
            item {
                Button(
                    onClick = { geminiCustomerPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                    enabled = !state.isAnalyzing
                ) {
                    if (state.isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Analizar foto con Gemini")
                    }
                }
            }

            item { Divider() }

            // ---------- CLIENTE ----------
            stickyHeader { SectionHeaderSticky("Datos del Cliente") }
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
                    label = { Text("Notas de entrega (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Divider() }

            // ---------- CARRITO ----------
            stickyHeader { SectionHeaderSticky("Carrito (sale a ruta)") }
            if (state.cart.isEmpty()) {
                item { Text("Agrega productos abajo.", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
            } else {
                items(state.cart, key = { "cart_${it.productId}" }) { line ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(line.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.removeFromCart(line.productId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = line.quantity.toString(),
                                    onValueChange = { v -> viewModel.setQty(line.productId, v.toIntOrNull() ?: 1) },
                                    label = { Text("Cant.") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = line.unitPrice.toString(),
                                    onValueChange = { v -> viewModel.setUnitPrice(line.productId, v.replace(',','.').toDoubleOrNull() ?: line.unitPrice) },
                                    label = { Text("Precio") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }
                        }
                    }
                }
            }

            item { Divider() }

            // ---------- PRODUCTOS ----------
            stickyHeader { SectionHeaderSticky("Agregar productos") }
            item {
                OutlinedTextField(
                    value = state.search,
                    onValueChange = viewModel::setSearch,
                    label = { Text("Buscar producto") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Search, null) }
                )
            }
            items(state.products, key = { "prod_${it.id}" }) { p ->
                Card(onClick = { viewModel.addToCart(p) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${p.nombre} (${p.marca})", style = MaterialTheme.typography.titleSmall)
                        Text("Stock: ${p.cantidad} | ${Formatters.money(p.precioVenta)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                Button(
                    onClick = viewModel::dispatchToRoute,
                    enabled = !state.isSaving && state.cart.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isSaving) "Creando ruta..." else "Crear salida a ruta")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessengerSelector(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected, onValueChange = { onSelected(it) }, label = { Text("Mensajero") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(), readOnly = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(name); expanded = false }) }
        }
    }
}

@Composable
private fun SectionHeaderSticky(title: String) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
    }
}
