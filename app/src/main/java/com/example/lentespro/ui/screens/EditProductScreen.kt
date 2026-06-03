package com.example.lentespro.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.lentespro.ui.viewmodel.EditProductEvent
import com.example.lentespro.ui.viewmodel.EditProductViewModel
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    viewModel: EditProductViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // ✅ Launcher para Gemini: Analizar caja de lentes desde foto
    val geminiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.analyzeProductImage(it, context) }
    }

    // Launcher para la foto del producto (Galería normal)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditProductEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is EditProductEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
                EditProductEvent.NavigateBack -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.id.isBlank()) "Nuevo producto" else "Editar producto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // ✨ BOTÓN DE GEMINI (IA Multimodal)
                    IconButton(
                        onClick = { geminiImagePicker.launch("image/*") },
                        enabled = !state.isAnalyzing
                    ) {
                        if (state.isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome, 
                                contentDescription = "Llenar con Gemini AI",
                                tint = Color(0xFF673AB7) // Color violeta IA
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.save() }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

            // 🖼️ SECCIÓN DE IMAGEN
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { imagePickerLauncher.launch("image/*") },
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    val displayImage = state.selectedImageUri ?: state.imageUrl
                    if (displayImage != null) {
                        AsyncImage(
                            model = displayImage,
                            contentDescription = "Foto del producto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        ) {
                            Text(
                                "Toca para cambiar foto",
                                color = Color.White,
                                modifier = Modifier.padding(4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(48.dp))
                            Text("Añadir foto del lente", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            SectionTitle("Datos del producto")
            OutlinedTextField(
                value = state.nombre,
                onValueChange = { v -> viewModel.update { it.copy(nombre = v) } },
                label = { Text("Nombre (ej: Pattaya Blue)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.marca,
                onValueChange = { v -> viewModel.update { it.copy(marca = v) } },
                label = { Text("Marca (ej: EyeShare)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.color,
                onValueChange = { v -> viewModel.update { it.copy(color = v) } },
                label = { Text("Color (ej: Azul)") },
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("Parámetros Ópticos")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.potenciaEsferica,
                    onValueChange = { v -> viewModel.update { it.copy(potenciaEsferica = v) } },
                    label = { Text("Esfera (D)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.diametro,
                    onValueChange = { v -> viewModel.update { it.copy(diametro = v) } },
                    label = { Text("Diámetro (mm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            SectionTitle("Inventario y precios")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.cantidad,
                    onValueChange = { v -> viewModel.update { it.copy(cantidad = v) } },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.stockMinimo,
                    onValueChange = { v -> viewModel.update { it.copy(stockMinimo = v) } },
                    label = { Text("Stock mínimo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = state.precioVenta,
                onValueChange = { v -> viewModel.update { it.copy(precioVenta = v) } },
                label = { Text("Precio venta") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("Caducidad y notas")
            OutlinedTextField(
                value = state.fechaCaducidad,
                onValueChange = { v -> viewModel.update { it.copy(fechaCaducidad = v) } },
                label = { Text("Caducidad (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.lote,
                onValueChange = { v -> viewModel.update { it.copy(lote = v) } },
                label = { Text("Lote") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.notas,
                onValueChange = { v -> viewModel.update { it.copy(notas = v) } },
                label = { Text("Notas - opcional") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}
