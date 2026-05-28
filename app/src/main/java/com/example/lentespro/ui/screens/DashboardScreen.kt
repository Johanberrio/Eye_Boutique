package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lentespro.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    inventoryViewModel: InventoryViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onGoToInventory: () -> Unit,
    onAddProduct: () -> Unit,
    onGoToRoutes: () -> Unit,
    onGoToMessengers: () -> Unit,
    onGoToHistory: () -> Unit,
    onGoToGemini: () -> Unit,
    isAdmin: Boolean,
    onGoToAdminUsers: () -> Unit,
    onLogout: () -> Unit
) {
    val products by inventoryViewModel.products.collectAsState()
    val totalLentes by inventoryViewModel.totalLentes.collectAsState()
    val alertaTotalBajo by inventoryViewModel.alertaTotalBajo.collectAsState()
    
    val enRutaProductCount by inventoryViewModel.enRutaProductCount.collectAsState()
    val ventasHoyProductCount by inventoryViewModel.ventasHoyProductCount.collectAsState()
    
    val ventasPeriodoActualCount by inventoryViewModel.ventasPeriodoActualCount.collectAsState()
    val totalHistoricoVendido by inventoryViewModel.totalHistoricoVendido.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LentesPro") },
                actions = {
                    IconButton(onClick = { onToggleDarkMode(!isDarkMode) }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambiar tema"
                        )
                    }
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
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ✅ Botón de Gemini (Arriba del +)
                FloatingActionButton(
                    onClick = onGoToGemini,
                    containerColor = Color(0xFF673AB7),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Asistente Gemini")
                }

                // Botón de Agregar (Abajo)
                if (isAdmin) {
                    FloatingActionButton(onClick = onAddProduct) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar producto")
                    }
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
            // ✅ RESTAURADO: Tarjeta de Resumen con todos los contadores
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resumen", style = MaterialTheme.typography.titleMedium)
                    
                    Text("Lentes registrados: ${products.size}")
                    Text("Total lentes en inventario: $totalLentes")
                    
                    Text("Productos en ruta 🛵: $enRutaProductCount")
                    Text("Lentes vendidos hoy 💰: $ventasHoyProductCount")
                    
                    Text("Lentes vendidos (mes actual) 🗓️: $ventasPeriodoActualCount")
                    Text("Total histórico vendido 🏆: $totalHistoricoVendido")

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (alertaTotalBajo) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (alertaTotalBajo) "⚠️ Alerta: inventario total bajo (≤ 50)"
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
                Icon(Icons.Default.Route, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Rutas (salidas y entregas)")
            }

            Button(
                onClick = onGoToHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Historial")
            }

            if (isAdmin) {
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
