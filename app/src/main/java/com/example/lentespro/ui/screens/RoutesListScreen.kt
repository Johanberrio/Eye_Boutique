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
    onFinalizeRoute: (String) -> Unit,
    onOpenDetail: (String) -> Unit
) {
    val enRuta by viewModel.enRuta.collectAsState()
    val historyCards by viewModel.historyCards.collectAsState()

    var saleDateText by rememberSaveable { mutableStateOf("") }
    var selectedMessenger by rememberSaveable { mutableStateOf("Todos") }
    var productQuery by rememberSaveable { mutableStateOf("") }
    var qtySoldText by rememberSaveable { mutableStateOf("") }

    val zone = remember { ZoneId.of("America/Bogota") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    fun epochToLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    val selectedSaleDate: LocalDate? = remember(saleDateText) {
        val t = saleDateText.trim()
        if (t.isBlank()) null else runCatching { LocalDate.parse(t, dateFormatter) }.getOrNull()
    }

    val qtySold: Int? = remember(qtySoldText) { qtySoldText.trim().toIntOrNull() }
    val messengerOptions = remember { listOf("Todos", "Jaime", "Adomibello") }

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
            .filter { card ->
                if (selectedSaleDate == null) true
                else epochToLocalDate(card.soldAtEpochMillis) == selectedSaleDate
            }
            .filter { card ->
                if (selectedMessenger == "Todos") true
                else (card.messengerName ?: "").trim().equals(selectedMessenger, ignoreCase = true)
            }
            .filter { card ->
                if (q.isBlank()) true
                else card.firstItemName.lowercase(Locale.getDefault()).contains(q)
            }
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
            Text("EN RUTA", style = MaterialTheme.typography.titleMedium)

            if (enRuta.isEmpty()) {
                Text("No hay rutas en curso.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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

            Text("FINALIZADAS (historial)", style = MaterialTheme.typography.titleMedium)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = saleDateText,
                    onValueChange = { saleDateText = it },
                    label = { Text("Fecha (AAAA-MM-dd)") },
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
            }

            if (historyFiltrado.isEmpty()) {
                Text("No hay ventas finalizadas.", style = MaterialTheme.typography.bodySmall)
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
            modifier = Modifier.menuAnchor().fillMaxWidth()
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
    val zone = remember { ZoneId.of("America/Bogota") }
    val dateText = Instant.ofEpochMilli(sale.createdAtEpochMillis)
        .atZone(zone)
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    Card(onClick = onOpenDetail) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Venta #${sale.id.takeLast(6)}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TwoWheeler, contentDescription = null)
                    Text("EN RUTA")
                }
            }
            Text("Mensajero: ${sale.messengerName ?: "—"}")
            Text("Fecha salida: $dateText", style = MaterialTheme.typography.bodySmall)

            if (onFinalize != null) {
                Button(onClick = onFinalize, modifier = Modifier.fillMaxWidth()) {
                    Text("Finalizar")
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
    val zone = remember { ZoneId.of("America/Bogota") }
    val soldDateText = Instant.ofEpochMilli(card.soldAtEpochMillis)
        .atZone(zone)
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    Card(onClick = onOpenDetail) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Venta #${card.saleId.takeLast(6)}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Text("FINALIZADA")
                }
            }
            Text("Producto: ${card.firstItemName}")
            Text("Cantidad vendida: ${card.soldQty}")
            Text("Vendedor: ${card.sellerName ?: "—"}")
            Text("Cliente: ${card.customerName ?: "—"}")
            Text("Fecha: $soldDateText", style = MaterialTheme.typography.bodySmall)
            Text("Total: ${Formatters.money(card.total)}")
        }
    }
}
