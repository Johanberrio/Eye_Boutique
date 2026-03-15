package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lentespro.data.SaleStatus
import com.example.lentespro.ui.viewmodel.RouteDetailEvent
import com.example.lentespro.ui.viewmodel.RouteDetailViewModel
import com.example.lentespro.util.Formatters
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    viewModel: RouteDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is RouteDetailEvent.Error -> snackbar.showSnackbar(ev.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // ✅ Ahora muestra el número secuencial calculado en el ViewModel
                title = { Text("Historial - Ruta #${state.saleNumber}") },
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

        val dateText = if (state.createdAt == 0L) "—" else {
            Instant.ofEpochMilli(state.createdAt)
                .atZone(ZoneId.systemDefault()) // ✅ Consistente con GMT-5
                .toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        }

        val isFinal = state.status == SaleStatus.FINALIZADA
        val statusText = if (isFinal) "FINALIZADA" else "EN RUTA"
        val statusIcon = if (isFinal) Icons.Default.CheckCircle else Icons.Default.TwoWheeler

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estado: $statusText", style = MaterialTheme.typography.titleMedium)
                        Icon(statusIcon, contentDescription = null)
                    }
                    Text("Fecha: $dateText", style = MaterialTheme.typography.bodySmall)
                    Text("Mensajero: ${state.messengerName.ifBlank { "—" }}")
                    if (state.notes.isNotBlank()) Text("Notas: ${state.notes}")
                    Spacer(Modifier.height(4.dp))
                    Text("Total vendido: ${Formatters.money(state.totalSold)}", style = MaterialTheme.typography.titleMedium)
                }
            }

            Text("Productos", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.lines, key = { it.productName }) { line ->
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(line.productName, style = MaterialTheme.typography.titleSmall)
                            Text("Precio: ${Formatters.money(line.unitPrice)}")
                            Text("Despachado: ${line.dispatched} | Vendido: ${line.sold} | Devuelto: ${line.returned}")
                            Text("Subtotal vendido: ${Formatters.money(line.soldTotal)}")
                        }
                    }
                }
            }
        }
    }
}
