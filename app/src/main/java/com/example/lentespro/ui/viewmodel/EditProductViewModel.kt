package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val marca: String = "",
    val color: String = "",
    val tipo: String = "Anual",

    val potenciaEsferica: String = "",
    val cilindro: String = "",
    val eje: String = "",
    val curvaBase: String = "",
    val diametro: String = "",

    val cantidad: String = "0",
    val stockMinimo: String = "1",
    val precioCompra: String = "0",
    val precioVenta: String = "0",

    val fechaCaducidad: String = "", // yyyy-MM-dd
    val lote: String = "",
    val notas: String = "",

    val isLoading: Boolean = false
)

sealed class EditProductEvent {
    data class ShowError(val message: String) : EditProductEvent()
    data class ShowSuccess(val message: String) : EditProductEvent()
    data object NavigateBack : EditProductEvent()
}

class EditProductViewModel(
    private val repo: ProductRepository,
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
                    _events.emit(EditProductEvent.ShowError("No se encontró el producto (id=$productId)."))
                }
            } else {
                _uiState.value = EditProductUiState(isLoading = false)
            }
        }
    }

    fun update(block: (EditProductUiState) -> EditProductUiState) {
        _uiState.update(block)
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value

            // ✅ Normalizamos nombre y marca antes de guardar (quita tildes y pasa a mayúsculas)
            val nombre = Formatters.normalize(state.nombre)
            val marca = Formatters.normalize(state.marca)
            val color = Formatters.normalize(state.color)
            val tipo = state.tipo.trim().ifBlank { "Mensual" }

            if (nombre.isBlank()) {
                _events.emit(EditProductEvent.ShowError("El nombre es obligatorio."))
                return@launch
            }
            if (marca.isBlank()) {
                _events.emit(EditProductEvent.ShowError("La marca es obligatoria."))
                return@launch
            }

            val cantidad = state.cantidad.toIntOrNull() ?: 0
            val pVenta = state.precioVenta.replace(',', '.').toDoubleOrNull() ?: 0.0

            val caducidadEpoch = try {
                Formatters.dateTextToEpochMillisOrNull(state.fechaCaducidad)
            } catch (e: Exception) {
                null
            }

            val entity = ProductEntity(
                id = if (productId == "new") "" else productId,
                nombre = nombre,
                marca = marca,
                color = color,
                tipo = tipo,
                cantidad = cantidad,
                stockMinimo = state.stockMinimo.toIntOrNull() ?: 1,
                precioVenta = pVenta,
                fechaCaducidadEpochMillis = caducidadEpoch,
                lote = state.lote.trim().ifBlank { null },
                notas = state.notas.trim().ifBlank { null },
                actualizadoEnEpochMillis = System.currentTimeMillis()
            )

            repo.upsert(entity)

            _events.emit(EditProductEvent.ShowSuccess("Producto guardado ✅"))
            _events.emit(EditProductEvent.NavigateBack)
        }
    }

    private fun ProductEntity.toUiState(): EditProductUiState {
        return EditProductUiState(
            id = id,
            nombre = nombre,
            marca = marca,
            color = color,
            tipo = tipo,
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
