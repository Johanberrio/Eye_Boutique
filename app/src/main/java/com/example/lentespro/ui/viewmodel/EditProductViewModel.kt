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
import kotlinx.coroutines.launch

data class EditProductUiState(
    val id: Long = 0L,

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
    private val productId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProductUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditProductEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            if (productId >= 0L) {
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
        _uiState.value = block(_uiState.value)
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value

            val nombre = state.nombre.trim()
            val marca = state.marca.trim()
            val color = state.color.trim()
            val tipo = state.tipo.trim().ifBlank { "Mensual" }

            if (nombre.isBlank()) {
                _events.emit(EditProductEvent.ShowError("El nombre es obligatorio."))
                return@launch
            }
            if (marca.isBlank()) {
                _events.emit(EditProductEvent.ShowError("La marca es obligatoria."))
                return@launch
            }

            if (color.isBlank()) {
                _events.emit(EditProductEvent.ShowError("El color es obligatorio."))
                return@launch
            }

            //val potencia = parseDoubleRequired(state.potenciaEsferica, "Potencia esférica") ?: return@launch
            val cantidad = parseIntRequired(state.cantidad, "Cantidad") ?: return@launch
            val stockMin = parseIntRequired(state.stockMinimo, "Stock mínimo") ?: return@launch
            //val pCompra = parseDoubleRequired(state.precioCompra, "Precio compra") ?: return@launch
            val pVenta = parseDoubleRequired(state.precioVenta, "Precio venta") ?: return@launch

            val cilindro = parseDoubleOptional(state.cilindro)
            val eje = parseIntOptional(state.eje)
            val bc = parseDoubleOptional(state.curvaBase)
            val dia = parseDoubleOptional(state.diametro)

            if (eje != null && (eje < 0 || eje > 180)) {
                _events.emit(EditProductEvent.ShowError("El eje debe estar entre 0 y 180."))
                return@launch
            }

            val caducidadEpoch = try {
                Formatters.dateTextToEpochMillisOrNull(state.fechaCaducidad)
            } catch (e: Exception) {
                _events.emit(EditProductEvent.ShowError("Fecha inválida. Usa formato yyyy-MM-dd (ej: 2026-12-31)."))
                return@launch
            }

            val entity = ProductEntity(
                id = state.id,
                nombre = nombre,
                marca = marca,
                color = color,
                tipo = tipo,
                potenciaEsferica = 0.0,
                cilindro = cilindro,
                eje = eje,
                curvaBase = bc,
                diametro = dia,
                cantidad = cantidad,
                stockMinimo = 1,
                precioCompra = 0.0,
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

    private suspend fun parseDoubleRequired(raw: String, label: String): Double? {
        val d = parseDoubleOptional(raw)
        if (d == null) {
            _events.emit(EditProductEvent.ShowError("$label es obligatorio y debe ser numérico (ej: -1.25)."))
        }
        return d
    }

    private fun parseDoubleOptional(raw: String): Double? {
        val t = raw.trim()
        if (t.isBlank()) return null
        // Soporta coma decimal
        return t.replace(',', '.').toDoubleOrNull()
    }

    private suspend fun parseIntRequired(raw: String, label: String): Int? {
        val v = parseIntOptional(raw)
        if (v == null) {
            _events.emit(EditProductEvent.ShowError("$label es obligatorio y debe ser entero."))
        }
        return v
    }

    private fun parseIntOptional(raw: String): Int? {
        val t = raw.trim()
        if (t.isBlank()) return null
        return t.toIntOrNull()
    }

    private fun ProductEntity.toUiState(): EditProductUiState {
        return EditProductUiState(
            id = id,
            nombre = nombre,
            marca = marca,
            color = color,
            tipo = tipo,
            potenciaEsferica = potenciaEsferica.toString(),
            //cilindro = cilindro?.toString().orEmpty(),
            eje = eje?.toString().orEmpty(),
            curvaBase = curvaBase?.toString().orEmpty(),
            diametro = diametro?.toString().orEmpty(),
            cantidad = cantidad.toString(),
            stockMinimo = stockMinimo.toString(),
            precioCompra = precioCompra.toString(),
            precioVenta = precioVenta.toString(),
            fechaCaducidad = if (fechaCaducidadEpochMillis == null) "" else Formatters.epochMillisToDateText(fechaCaducidadEpochMillis),
            lote = lote.orEmpty(),
            notas = notas.orEmpty(),
            isLoading = false
        )
    }
}
