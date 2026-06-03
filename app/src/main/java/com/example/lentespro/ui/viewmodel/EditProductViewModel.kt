package com.example.lentespro.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.*
import com.example.lentespro.util.Formatters
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.util.UUID

data class EditProductUiState(
    val id: String = "",
    val nombre: String = "",
    val marca: String = "EyeShare",
    val color: String = "",
    val tipo: String = "Anual",
    val potenciaEsferica: String = "",
    val diametro: String = "",
    val cantidad: String = "0",
    val stockMinimo: String = "1",
    val precioVenta: String = "27000",
    val fechaCaducidad: String = "",
    val lote: String = "",
    val notas: String = "",
    val imageUrl: String? = null,
    val selectedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false
)

sealed class EditProductEvent {
    data class ShowError(val message: String) : EditProductEvent()
    data class ShowSuccess(val message: String) : EditProductEvent()
    data object NavigateBack : EditProductEvent()
}

class EditProductViewModel(
    private val repo: ProductRepository,
    private val adminNotesRepo: AdminNotesRepository,
    private val authProfileRepo: AuthProfileRepository,
    private val geminiRepo: GeminiRepository,
    private val productId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProductUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditProductEvent>()
    val events = _events.asSharedFlow()

    private val storage = FirebaseStorage.getInstance()

    init {
        viewModelScope.launch {
            if (productId != "new" && productId.isNotBlank()) {
                val entity = repo.getById(productId)
                if (entity != null) {
                    _uiState.value = entity.toUiState()
                } else {
                    _uiState.value = EditProductUiState(isLoading = false)
                    _events.emit(EditProductEvent.ShowError("Producto no encontrado."))
                }
            } else {
                _uiState.value = EditProductUiState(isLoading = false, precioVenta = "27000")
            }
        }
    }

    fun update(block: (EditProductUiState) -> EditProductUiState) {
        _uiState.update(block)
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun analyzeProductImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            try {
                // 1. Cargar y optimizar imagen (Evita colapso de memoria RAM)
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                val ratio = 1024.0 / originalBitmap.width
                val height = (originalBitmap.height * ratio).toInt()
                val bitmap = Bitmap.createScaledBitmap(originalBitmap, 1024, height, true)

                // ✅ PROMPT EXPERTO: Le enseñamos a la IA a leer tu etiqueta específica
                val prompt = """
                    Analiza esta etiqueta de lentes de contacto. Extrae los datos y devuelve ÚNICAMENTE este formato exacto línea por línea (sin markdown). 
                    Reglas estrictas para encontrar los datos en esta etiqueta:
                    - NOMBRE: El texto más grande en la parte superior (ej: ESTONIA GRAY) o el texto debajo del código QR.
                    - COLOR: Si el NOMBRE incluye un color (ej: Gray, Blue, Brown), ponlo aquí traducido al español.
                    - ESFERA: El número que aparece junto a "F'v:" o que termina con la letra "D" (Devuelve SOLO el número, ej: 0.00 o -2.50).
                    - DIAMETRO: El número que aparece junto al símbolo "⌀T:" (Devuelve SOLO el número, ej: 14.50).
                    - LOTE: El código alfanumérico que aparece junto a la palabra "LOT:".
                    - CADUCIDAD: La fecha que aparece junto al icono del reloj de arena (Formato AAAA-MM-DD). Ignora la fecha con el icono de fábrica.

                    Formato de respuesta obligatorio:
                    NOMBRE: [valor]
                    COLOR: [valor]
                    ESFERA: [valor]
                    DIAMETRO: [valor]
                    LOTE: [valor]
                    CADUCIDAD: [valor]
                """.trimIndent()

                val response = geminiRepo.analyzeImage(prompt, bitmap)

                // ✅ Regex robusta para capturar los valores
                fun extract(key: String): String {
                    val pattern = Regex("(?i)$key[:\\s\\*]+(.*?)(?:\\r?\\n|$)", RegexOption.MULTILINE)
                    return pattern.find(response)?.groupValues?.get(1)?.replace("*", "")?.trim() ?: ""
                }

                _uiState.update { it.copy(
                    nombre = extract("NOMBRE").ifBlank { it.nombre },
                    marca = extract("MARCA").ifBlank { it.marca },
                    color = extract("COLOR").ifBlank { it.color },

                    // Limpiamos "D" o "mm" extra por si la IA es terca y los incluye
                    potenciaEsferica = extract("ESFERA").replace("D", "", ignoreCase = true).trim().ifBlank { it.potenciaEsferica },
                    diametro = extract("DIAMETRO").replace("mm", "", ignoreCase = true).trim().ifBlank { it.diametro },

                    lote = extract("LOTE").ifBlank { it.lote },
                    fechaCaducidad = extract("CADUCIDAD").ifBlank { it.fechaCaducidad }
                ) }
                _events.emit(EditProductEvent.ShowSuccess("Datos extraídos correctamente ✨"))

            } catch (e: Exception) {
                _events.emit(EditProductEvent.ShowError("Error IA: ${e.message ?: "Fallo desconocido"}"))
            } finally {
                _uiState.update { it.copy(isAnalyzing = false) }
            }
        }
    }

    private suspend fun uploadImage(uri: Uri): String {
        val fileName = "products/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.nombre.isBlank() || state.marca.isBlank()) {
                _events.emit(EditProductEvent.ShowError("Nombre y marca son obligatorios."))
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            try {
                var finalUrl = state.imageUrl
                state.selectedImageUri?.let { uri -> finalUrl = uploadImage(uri) }

                val entity = ProductEntity(
                    id = if (productId == "new") "" else productId,
                    nombre = Formatters.normalize(state.nombre),
                    marca = Formatters.normalize(state.marca),
                    color = Formatters.normalize(state.color),
                    tipo = state.tipo.trim(),
                    imageUrl = finalUrl,
                    potenciaEsferica = state.potenciaEsferica.replace(',', '.').toDoubleOrNull() ?: 0.0,
                    diametro = state.diametro.replace(',', '.').toDoubleOrNull(),
                    cantidad = state.cantidad.toIntOrNull() ?: 0,
                    stockMinimo = state.stockMinimo.toIntOrNull() ?: 1,
                    precioVenta = state.precioVenta.replace(',', '.').toDoubleOrNull() ?: 0.0,
                    fechaCaducidadEpochMillis = Formatters.dateTextToEpochMillisOrNull(state.fechaCaducidad),
                    lote = state.lote.trim().ifBlank { null },
                    notas = state.notas.trim().ifBlank { null },
                    actualizadoEnEpochMillis = System.currentTimeMillis()
                )
                repo.upsert(entity)
                if (authProfileRepo.isSuperAdmin()) adminNotesRepo.updateNotes("") 
                _events.emit(EditProductEvent.ShowSuccess("Producto guardado ✅"))
                _events.emit(EditProductEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(EditProductEvent.ShowError("Error al guardar."))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun ProductEntity.toUiState() = EditProductUiState(
        id = id, nombre = nombre, marca = marca, color = color, tipo = tipo,
        imageUrl = imageUrl,
        potenciaEsferica = if (potenciaEsferica == 0.0) "" else potenciaEsferica.toString(),
        diametro = diametro?.toString() ?: "",
        cantidad = cantidad.toString(),
        stockMinimo = stockMinimo.toString(),
        precioVenta = precioVenta.toString(),
        fechaCaducidad = if (fechaCaducidadEpochMillis == null) "" else Formatters.epochMillisToDateText(fechaCaducidadEpochMillis),
        lote = lote.orEmpty(),
        notas = notas.orEmpty(),
        isLoading = false
    )
}
