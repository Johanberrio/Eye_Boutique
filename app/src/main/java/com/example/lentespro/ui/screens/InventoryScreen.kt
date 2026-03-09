package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lentespro.data.ProductEntity
import com.example.lentespro.ui.viewmodel.InventoryViewModel
import com.example.lentespro.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    inventoryViewModel: InventoryViewModel,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (Long) -> Unit
) {
    val products by inventoryViewModel.products.collectAsState()
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = onAddProduct) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    inventoryViewModel.setSearchQuery(it)
                },
                label = { Text("Buscar (nombre/marca/tipo)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        isAdmin = isAdmin,
                        onClick = { if (isAdmin) onEditProduct(product.id) },
                        onDelete = { inventoryViewModel.delete(product) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCard(
    product: ProductEntity,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val lowStock = product.cantidad <= product.stockMinimo
    val stockColor = if (lowStock) Color(0xFFB00020) else MaterialTheme.colorScheme.primary

    Card(onClick = onClick, enabled = isAdmin) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${product.nombre} (${product.marca})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tipo: ${product.tipo}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val extra = buildString {
                        if (product.cilindro != null) append(" Cil: ${product.cilindro}")
                        if (product.eje != null) append(" Eje: ${product.eje}")
                        if (product.curvaBase != null) append(" BC: ${product.curvaBase}")
                        if (product.diametro != null) append(" DIA: ${product.diametro}")
                    }.trim()
                    if (extra.isNotBlank()) {
                        Text(extra, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (isAdmin) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Stock: ${product.cantidad} (mín: ${product.stockMinimo})",
                    color = stockColor,
                    fontWeight = if (lowStock) FontWeight.Bold else FontWeight.Normal
                )
                Text("Venta: ${Formatters.money(product.precioVenta)}")
            }

            Text(
                text = "Caducidad: ${Formatters.epochMillisToDateText(product.fechaCaducidadEpochMillis)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
