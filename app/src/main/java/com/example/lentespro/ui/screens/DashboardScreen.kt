package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lentespro.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    inventoryViewModel: InventoryViewModel,
    onGoToInventory: () -> Unit,
    onAddProduct: () -> Unit,
    onGoToRoutes: () -> Unit
) {
    val products = inventoryViewModel.products.collectAsState()
    val totalLentes = inventoryViewModel.totalLentes.collectAsState()
    val alertaTotalBajo = inventoryViewModel.alertaTotalBajo.collectAsState()

    val isAlert = alertaTotalBajo.value

    val cardColors = if (isAlert) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LentesPro") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resumen", style = MaterialTheme.typography.titleMedium)
                    Text("Lentes registrados: ${products.value.size}")
                    Text("Total lentes en inventario: ${totalLentes.value}")

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null,
                            tint = if (isAlert) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isAlert) "⚠️ Alerta: inventario total bajo (≤ 50)"
                            else "Inventario total OK"
                        )
                    }
                }
            }

            Button(
                onClick = onGoToInventory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ir a Inventario")
            }

            Button(
                onClick = onGoToRoutes,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rutas (salidas y entregas)")
            }


            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Siguiente paso 🤔", style = MaterialTheme.typography.titleMedium)
                    Text("En la Parte 2 agregamos: Ventas, descuento automático de inventario e historial.")
                }
            }
        }
    }
}
