package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.SaleRepository
import com.example.lentespro.data.SaleWithItems
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FinalizeLine(
    val productId: Long,
    val name: String,
    val dispatched: Int,
    val unitPrice: Double,
    val sold: Int
) {
    val returned: Int = dispatched - sold
    val soldTotal: Double = sold * unitPrice
}

data class FinalizeUiState(
    val saleId: Long = -1L,
    val messengerName: String = "",
    val notes: String = "",
    val lines: List<FinalizeLine> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
) {
    val totalSold: Double = lines.sumOf { it.soldTotal }
}

sealed class FinalizeEvent {
    data class Error(val message: String) : FinalizeEvent()
    data object Success : FinalizeEvent()
}

class FinalizeRouteViewModel(
    private val saleId: Long,
    private val repo: SaleRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(FinalizeUiState(saleId = saleId, isLoading = true))
    val ui: StateFlow<FinalizeUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<FinalizeEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                val sw = repo.getSaleWithItemsOnce(saleId)
                if (sw == null) {
                    _ui.value = FinalizeUiState(saleId = saleId, isLoading = false)
                    _events.emit(FinalizeEvent.Error("Salida no encontrada (id=$saleId)."))
                    return@launch
                }
                _ui.value = sw.toUi()
            } catch (e: Exception) {
                _ui.value = FinalizeUiState(saleId = saleId, isLoading = false)
                _events.emit(FinalizeEvent.Error(e.message ?: "Error cargando la salida"))
            }
        }
    }

    fun setSold(productId: Long, sold: Int) {
        _ui.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.productId == productId) {
                        val safe = sold.coerceAtLeast(0).coerceAtMost(line.dispatched)
                        line.copy(sold = safe)
                    } else line
                }
            )
        }
    }

    fun finalize() {
        viewModelScope.launch {
            val state = _ui.value
            if (state.isSaving) return@launch

            _ui.update { it.copy(isSaving = true) }

            try {
                val map = state.lines.associate { it.productId to it.sold }
                repo.finalizeDispatch(saleId = state.saleId, soldByProductId = map)
                _events.emit(FinalizeEvent.Success)
            } catch (e: Exception) {
                _events.emit(FinalizeEvent.Error(e.message ?: "Error finalizando"))
                _ui.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun SaleWithItems.toUi(): FinalizeUiState {
        return FinalizeUiState(
            saleId = sale.id,
            messengerName = sale.messengerName ?: "",
            notes = sale.notes ?: "",
            lines = items.map {
                FinalizeLine(
                    productId = it.productId,
                    name = it.productName,
                    dispatched = it.dispatchedQty,
                    unitPrice = it.unitPrice,
                    sold = it.soldQty ?: 0
                )
            },
            isLoading = false,
            isSaving = false
        )
    }
}

class FinalizeRouteViewModelFactory(
    private val saleId: Long,
    private val repo: SaleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FinalizeRouteViewModel(saleId, repo) as T
}
