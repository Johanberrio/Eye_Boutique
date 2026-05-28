package com.example.lentespro.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.AdminNotesRepository
import com.example.lentespro.data.AuthProfileRepository
import com.example.lentespro.data.ProductEntity
import com.example.lentespro.data.ProductRepository
import com.example.lentespro.util.Formatters
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    val isScanning: Boolean = false
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

    fun setScanning(enabled: Boolean) {
        _uiState.update { it.copy(isScanning = enabled) }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    private suspend fun uploadImage(uri: Uri): String {
        // Esta función ahora lanza excepciones para que save() pueda capturarlas y mostrarlas
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
                // 1. Subir imagen si hay una nueva seleccionada
                var finalImageUrl = state.imageUrl
                state.selectedImageUri?.let { uri ->
                    try {
                        finalImageUrl = uploadImage(uri)
                    } catch (e: Exception) {
                        Log.e("LentesPro", "Error subiendo imagen", e)
                        _events.emit(EditProductEvent.ShowError("Fallo al subir imagen: ${e.localizedMessage}"))
                        _uiState.update { it.copy(isLoading = false) }
                        return@launch // Detenemos el guardado si falla la imagen
                    }
                }

                // 2. Crear y guardar la entidad
                val entity = ProductEntity(
                    id = if (productId == "new") "" else productId,
                    nombre = Formatters.normalize(state.nombre),
                    marca = Formatters.normalize(state.marca),
                    color = Formatters.normalize(state.color),
                    tipo = state.tipo.trim(),
                    imageUrl = finalImageUrl, // ✅ Se guarda la URL final
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

                // Borrar lista informativa si es SuperAdmin
                if (authProfileRepo.isSuperAdmin()) {
                    adminNotesRepo.updateNotes("") 
                }

                _events.emit(EditProductEvent.ShowSuccess("Producto guardado ✅"))
                _events.emit(EditProductEvent.NavigateBack)
            } catch (e: Exception) {
                Log.e("LentesPro", "Error al guardar producto", e)
                _events.emit(EditProductEvent.ShowError("Error al guardar: ${e.localizedMessage}"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun ProductEntity.toUiState(): EditProductUiState {
        return EditProductUiState(
            id = id,
            nombre = nombre,
            marca = marca,
            color = color,
            tipo = tipo,
            imageUrl = imageUrl, // ✅ Recuperamos la imagen guardada
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

    fun onTextScanned(rawText: String) {
        val fullText = rawText.replace("\n", " ").replace("\r", " ")
        var foundLote = ""; var foundExp = ""; var foundDia = ""; var foundSph = ""
        val loteRegex = Regex("""(?i)(?:LOT|LOTE|L)[:\s]*([A-Z0-9-]+)""")
        val expRegex = Regex("""(?i)(?:EXP|VENCE|CAD)[:\s]*(\d{4}-\d{2}-\d{2}|\d{2}/\d{4}|\d{2}-\d{4})""")
        val diaRegex = Regex("""(?i)(?:DIA|DIAM)[:\s]*(1[34][.,]\d)""")
        val sphRegex = Regex("""(?i)(?:PWR|SPH|ESF|D|P)[:\s]*([+-]\d{1,2}[.,]\d{2})""")
        loteRegex.find(fullText)?.let { foundLote = it.groupValues[1] }
        expRegex.find(fullText)?.let { foundExp = it.groupValues[1] }
        diaRegex.find(fullText)?.let { foundDia = it.groupValues[1] }
        sphRegex.find(fullText)?.let { foundSph = it.groupValues[1] }
        if (foundDia.isBlank()) Regex("""\b(1[34][.,][0-9])\b""").find(fullText)?.let { foundDia = it.groupValues[1] }
        if (foundSph.isBlank()) Regex("""\b([+-]\d[.,]\d{2})\b""").find(fullText)?.let { foundSph = it.groupValues[1] }
        _uiState.update { state ->
            state.copy(
                lote = foundLote.ifBlank { state.lote },
                fechaCaducidad = if (foundExp.isNotBlank()) normalizeDate(foundExp) else state.fechaCaducidad,
                diametro = foundDia.replace(",", ".").ifBlank { state.diametro },
                potenciaEsferica = foundSph.replace(",", ".").ifBlank { state.potenciaEsferica }
            )
        }
    }

    private fun normalizeDate(date: String): String {
        return when {
            date.contains("/") -> {
                val parts = date.split("/"); if (parts.size == 2) "${parts[1]}-${parts[0]}-01" else date
            }
            date.contains("-") && date.length == 7 -> {
                val parts = date.split("-"); "${parts[1]}-${parts[0]}-01"
            }
            else -> date
        }
    }
}
