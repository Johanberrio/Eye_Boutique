package com.example.lentespro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.lentespro.data.ProductEntity
import com.example.lentespro.ui.viewmodel.InventoryViewModel
import com.example.lentespro.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    inventoryViewModel: InventoryViewModel,
    isAdmin: Boolean,
    isSuperAdmin: Boolean,
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onGoToAdminNotes: () -> Unit,
    onEditProduct: (String) -> Unit
) {
    val products by inventoryViewModel.products.collectAsState()
    var query by remember { mutableStateOf("") }
    
    // ✅ Estado para el visor de imagen a pantalla completa
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

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
                    TextButton(onClick = onGoToAdminNotes) {
                        Icon(Icons.Default.Notes, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Lista Nueva")
                    }
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
                        isSuperAdmin = isSuperAdmin,
                        onClick = { if (isAdmin) onEditProduct(product.id) },
                        onDelete = { inventoryViewModel.delete(product) },
                        onImageClick = { fullScreenImageUrl = it } // ✅ Abrir visor
                    )
                }
            }
        }
    }

    // ✅ Visor de imagen a pantalla completa
    fullScreenImageUrl?.let { url ->
        FullScreenImageDialog(url = url, onDismiss = { fullScreenImageUrl = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCard(
    product: ProductEntity,
    isAdmin: Boolean,
    isSuperAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit // ✅ Callback para la imagen
) {
    val lowStock = product.cantidad <= product.stockMinimo
    val stockColor = if (lowStock) Color(0xFFB00020) else MaterialTheme.colorScheme.primary

    Card(onClick = onClick, enabled = isAdmin) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🖼️ Miniatura de la imagen
            AsyncImage(
                model = product.imageUrl, // Puede ser null
                contentDescription = product.nombre,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { product.imageUrl?.let { onImageClick(it) } },
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    }

                    if (isSuperAdmin) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Stock: ${product.cantidad}",
                        color = stockColor,
                        fontWeight = if (lowStock) FontWeight.Bold else FontWeight.Normal
                    )
                    Text("Venta: ${Formatters.money(product.precioVenta)}")
                }

                val extra = buildString {
                    if (product.diametro != null) append("DIA: ${product.diametro}")
                    if (product.potenciaEsferica != 0.0) append(" SPH: ${product.potenciaEsferica}")
                }.trim()
                if (extra.isNotBlank()) {
                    Text(extra, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun FullScreenImageDialog(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
