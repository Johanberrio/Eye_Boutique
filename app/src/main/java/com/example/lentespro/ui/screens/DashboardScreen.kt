package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    onGoToRoutes: () -> Unit,
    onGoToMessengers: () -> Unit,
    isAdmin: Boolean,
    onGoToAdminUsers: () -> Unit,
    onLogout: () -> Unit
) {
    val products by inventoryViewModel.products.collectAsState()
    val totalLentes by inventoryViewModel.totalLentes.collectAsState()
    val alertaTotalBajo by inventoryViewModel.alertaTotalBajo.collectAsState()
    
    // ✅ Observamos las nuevas estadísticas de productos
    val enRutaProductCount by inventoryViewModel.enRutaProductCount.collectAsState()
    val ventasHoyProductCount by inventoryViewModel.ventasHoyProductCount.collectAsState()

    val isAlert = alertaTotalBajo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LentesPro") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = onAddProduct) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar producto")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resumen", style = MaterialTheme.typography.titleMedium)
                    
                    Text("Lentes registrados: ${products.size}")
                    Text("Total lentes en inventario: $totalLentes")
                    
                    // ✅ Actualizado: muestra cantidad de productos, no de rutas
                    Text("Productos en ruta 🛵: $enRutaProductCount")
                    Text("Lentes vendidos hoy 💰: $ventasHoyProductCount")

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isAlert) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isAlert) "⚠️ Alerta: inventario total bajo (≤ 50)"
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

            if (isAdmin) {
                Button(
                    onClick = onGoToRoutes,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Route, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Rutas (salidas y entregas)")
                }

                Button(
                    onClick = onGoToMessengers,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.TwoWheeler, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Mensajeros")
                }

                Button(
                    onClick = onGoToAdminUsers,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Group, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Usuarios (Admin)")
                }
            }
        }
    }
}
