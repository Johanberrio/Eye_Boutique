package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.AdminNotesRepository
import com.example.lentespro.data.AuthProfileRepository
import com.example.lentespro.data.ProductEntity
import com.example.lentespro.data.ProductRepository
import com.example.lentespro.util.Formatters
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

        if (foundDia.isBlank()) {
            Regex("""\b(1[34][.,][0-9])\b""").find(fullText)?.let { foundDia = it.groupValues[1] }
        }
        if (foundSph.isBlank()) {
            Regex("""\b([+-]\d[.,]\d{2})\b""").find(fullText)?.let { foundSph = it.groupValues[1] }
        }

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
                val parts = date.split("/")
                if (parts.size == 2) "${parts[1]}-${parts[0]}-01" else date
            }
            date.contains("-") && date.length == 7 -> {
                val parts = date.split("-")
                "${parts[1]}-${parts[0]}-01"
            }
            else -> date
        }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val nombre = Formatters.normalize(state.nombre)
            val marca = Formatters.normalize(state.marca)

            if (nombre.isBlank() || marca.isBlank()) {
                _events.emit(EditProductEvent.ShowError("Nombre y marca son obligatorios."))
                return@launch
            }

            fun String.toDoubleSafe() = this.replace(',', '.').toDoubleOrNull()

            val entity = ProductEntity(
                id = if (productId == "new") "" else productId,
                nombre = nombre,
                marca = marca,
                color = Formatters.normalize(state.color),
                tipo = state.tipo.trim(),
                potenciaEsferica = state.potenciaEsferica.toDoubleSafe() ?: 0.0,
                diametro = state.diametro.toDoubleSafe(),
                cantidad = state.cantidad.toIntOrNull() ?: 0,
                stockMinimo = state.stockMinimo.toIntOrNull() ?: 1,
                precioVenta = state.precioVenta.toDoubleSafe() ?: 0.0,
                fechaCaducidadEpochMillis = Formatters.dateTextToEpochMillisOrNull(state.fechaCaducidad),
                lote = state.lote.trim().ifBlank { null },
                notas = state.notas.trim().ifBlank { null },
                actualizadoEnEpochMillis = System.currentTimeMillis()
            )

            try {
                repo.upsert(entity)
                // Borrar notas solo si es SuperAdmin
                if (authProfileRepo.isSuperAdmin()) {
                    adminNotesRepo.updateNotes("") 
                }
                _events.emit(EditProductEvent.ShowSuccess("Producto guardado ✅"))
                _events.emit(EditProductEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(EditProductEvent.ShowError("Error al guardar."))
            }
        }
    }

    private fun ProductEntity.toUiState(): EditProductUiState {
        return EditProductUiState(
            id = id, nombre = nombre, marca = marca, color = color, tipo = tipo,
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
}
