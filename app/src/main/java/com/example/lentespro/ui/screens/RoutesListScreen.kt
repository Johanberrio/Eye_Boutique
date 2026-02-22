package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lentespro.data.SaleEntity
import com.example.lentespro.data.SaleHistoryCard
import com.example.lentespro.ui.viewmodel.RoutesListViewModel
import com.example.lentespro.util.Formatters
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesListScreen(
    viewModel: RoutesListViewModel,
    onBack: () -> Unit,
    onNewRoute: () -> Unit,
    onFinalizeRoute: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val enRuta by viewModel.enRuta.collectAsState()
    val historyCards by viewModel.historyCards.collectAsState()

    // ✅ Filtros (FINALIZADAS)
    var saleDateText by rememberSaveable { mutableStateOf("") }      // yyyy-MM-dd (vacío = todas)
    var selectedMessenger by rememberSaveable { mutableStateOf("Todos") }
    var productQuery by rememberSaveable { mutableStateOf("") }
    var qtySoldText by rememberSaveable { mutableStateOf("") }       // cantidad vendida exacta (vacío = todas)

    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    fun epochToLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    val selectedSaleDate: LocalDate? = remember(saleDateText) {
        val t = saleDateText.trim()
        if (t.isBlank()) null else runCatching { LocalDate.parse(t, dateFormatter) }.getOrNull()
    }

    val qtySold: Int? = remember(qtySoldText) { qtySoldText.trim().toIntOrNull() }

    // ✅ Mensajeros fijos como pediste
    val messengerOptions = remember { listOf("Todos", "Jaime", "Adomibello") }

    // ✅ Historial filtrado (FINALIZADAS)
    val historyFiltrado = remember(
        historyCards,
        selectedSaleDate,
        selectedMessenger,
        productQuery,
        qtySold
    ) {
        val q = productQuery.trim().lowercase(Locale.getDefault())

        historyCards
            .asSequence()
            // 1) Fecha de venta (día)
            .filter { card ->
                if (selectedSaleDate == null) true
                else epochToLocalDate(card.soldAtEpochMillis) == selectedSaleDate
            }
            // 2) Mensajero
            .filter { card ->
                if (selectedMessenger == "Todos") true
                else (card.messengerName ?: "").trim().equals(selectedMessenger, ignoreCase = true)
            }
            // 3) Producto
            .filter { card ->
                if (q.isBlank()) true
                else card.productName.lowercase(Locale.getDefault()).contains(q)
            }
            // 4) Cantidad vendida EXACTA
            .filter { card ->
                if (qtySold == null) true else card.soldQty == qtySold
            }
            .toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rutas (salidas y entregas)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewRoute) {
                Icon(Icons.Default.Add, contentDescription = "Nueva salida a ruta")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // -------------------- EN RUTA --------------------
            Text("EN RUTA", style = MaterialTheme.typography.titleMedium)

            if (enRuta.isEmpty()) {
                Text("No hay rutas en curso.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 6.dp)
                ) {
                    items(enRuta, key = { it.id }) { sale ->
                        RouteCardEnRuta(
                            sale = sale,
                            onOpenDetail = { onOpenDetail(sale.id) },
                            onFinalize = { onFinalizeRoute(sale.id) }
                        )
                    }
                }
            }

            Divider()

            // -------------------- FINALIZADAS (HISTORIAL) --------------------
            Text("FINALIZADAS (historial de ventas)", style = MaterialTheme.typography.titleMedium)

            // ✅ Filtros fuera del LazyColumn para que el foco funcione SIEMPRE
            OutlinedTextField(
                value = saleDateText,
                onValueChange = { saleDateText = it },
                label = { Text("Fecha de venta (AAAA-MM-dd)") },
                supportingText = {
                    if (saleDateText.isNotBlank() && selectedSaleDate == null) {
                        Text("Formato inválido. Ejemplo: 2026-02-17")
                    }
                },
                isError = saleDateText.isNotBlank() && selectedSaleDate == null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            MessengerDropdown(
                options = messengerOptions,
                selected = selectedMessenger,
                onSelected = { selectedMessenger = it }
            )

            OutlinedTextField(
                value = productQuery,
                onValueChange = { productQuery = it },
                label = { Text("Producto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = qtySoldText,
                onValueChange = { qtySoldText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Cantidad vendida") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedButton(
                onClick = {
                    saleDateText = ""
                    selectedMessenger = "Todos"
                    productQuery = ""
                    qtySoldText = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Limpiar filtros")
            }

            if (historyFiltrado.isEmpty()) {
                Text("No hay ventas finalizadas con esos filtros.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyFiltrado, key = { it.saleId }) { card ->
                        HistoryCardFinalizada(
                            card = card,
                            onOpenDetail = { onOpenDetail(card.saleId) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessengerDropdown(
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
            onValueChange = {},
            readOnly = true,
            label = { Text("Mensajero") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RouteCardEnRuta(
    sale: SaleEntity,
    onOpenDetail: () -> Unit,
    onFinalize: (() -> Unit)?
) {
    val dateText = Instant.ofEpochMilli(sale.createdAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    Card(onClick = onOpenDetail) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Venta #${sale.id}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TwoWheeler, contentDescription = null)
                    Text("EN RUTA")
                }
            }
            Text("Mensajero: ${sale.messengerName ?: "—"}")
            Text("Fecha salida: $dateText", style = MaterialTheme.typography.bodySmall)
            Text("Total vendido: — (pendiente)")

            if (onFinalize != null) {
                Button(onClick = onFinalize, modifier = Modifier.fillMaxWidth()) {
                    Text("Finalizar (vendido + devoluciones)")
                }
            }
        }
    }
}

@Composable
private fun HistoryCardFinalizada(
    card: SaleHistoryCard,
    onOpenDetail: () -> Unit
) {
    val soldDateText = Instant.ofEpochMilli(card.soldAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    Card(onClick = onOpenDetail) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Venta #${card.saleId}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Text("FINALIZADA")
                }
            }
            Text("Producto: ${card.productName}")
            Text("Cantidad vendida: ${card.soldQty}")
            Text("Cliente: ${card.customerName ?: "—"}")
            Text("Celular: ${card.customerPhone1 ?: "—"}")
            Text("Mensajero: ${card.messengerName ?: "—"}")
            Text("Fecha de venta: $soldDateText", style = MaterialTheme.typography.bodySmall)
            Text("Total vendido: ${Formatters.money(card.total)}")
        }
    }
}
