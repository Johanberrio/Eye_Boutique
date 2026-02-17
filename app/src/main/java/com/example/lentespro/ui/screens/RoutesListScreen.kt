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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lentespro.data.SaleEntity
import com.example.lentespro.data.SaleStatus
import com.example.lentespro.ui.viewmodel.RoutesListViewModel
import com.example.lentespro.util.Formatters
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesListScreen(
    viewModel: RoutesListViewModel,
    onBack: () -> Unit,
    onNewRoute: () -> Unit,
    onFinalizeRoute: (Long) -> Unit
) {
    val enRuta = viewModel.enRuta.collectAsState()
    val finalizadas = viewModel.finalizadas.collectAsState()

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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader("EN RUTA")
                if (enRuta.value.isEmpty()) {
                    Text("No hay rutas en curso.", style = MaterialTheme.typography.bodySmall)
                }
            }

            items(enRuta.value, key = { it.id }) { sale ->
                RouteCard(
                    sale = sale,
                    onFinalize = { onFinalizeRoute(sale.id) }
                )
            }

            item {
                Spacer(Modifier.height(6.dp))
                SectionHeader("FINALIZADAS")
                if (finalizadas.value.isEmpty()) {
                    Text("Aún no hay rutas finalizadas.", style = MaterialTheme.typography.bodySmall)
                }
            }

            items(finalizadas.value, key = { it.id }) { sale ->
                RouteCard(
                    sale = sale,
                    onFinalize = null
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun RouteCard(
    sale: SaleEntity,
    onFinalize: (() -> Unit)?
) {
    val statusText = if (sale.status == SaleStatus.EN_RUTA) "EN RUTA" else "FINALIZADA"
    val icon = if (sale.status == SaleStatus.EN_RUTA) Icons.Default.TwoWheeler  else Icons.Default.CheckCircle

    val dateText = Instant.ofEpochMilli(sale.createdAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Domicilio #${sale.id}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, contentDescription = null)
                    Text(statusText)
                }
            }

            Text("Mensajero: ${sale.messengerName ?: "—"}")
            Text("Fecha: $dateText", style = MaterialTheme.typography.bodySmall)

            if (sale.status == SaleStatus.FINALIZADA) {
                Text("Total vendido: ${Formatters.money(sale.total)}")
            } else {
                Text("Total vendido: — (pendiente)")
            }

            if (onFinalize != null) {
                Button(
                    onClick = onFinalize,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Finalizar (vendido + devoluciones)")
                }
            }
        }
    }
}








