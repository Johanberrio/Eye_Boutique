package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.SaleRepository
import com.example.lentespro.data.SaleStatus
import com.example.lentespro.data.SaleWithItems
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RouteDetailLineUi(
    val productName: String,
    val unitPrice: Double,
    val dispatched: Int,
    val sold: Int,
    val returned: Int
) {
    val soldTotal: Double = sold * unitPrice
}

data class RouteDetailUiState(
    val saleId: Long = -1L,
    val status: SaleStatus = SaleStatus.EN_RUTA,
    val createdAt: Long = 0L,
    val messengerName: String = "",
    val customerName: String = "",
    val customerPhone1: String = "",
    val notes: String = "",
    val totalSold: Double = 0.0,
    val lines: List<RouteDetailLineUi> = emptyList(),
    val isLoading: Boolean = true
)

sealed class RouteDetailEvent {
    data class Error(val message: String) : RouteDetailEvent()
}

class RouteDetailViewModel(
    private val saleId: Long,
    private val repo: SaleRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(RouteDetailUiState(saleId = saleId, isLoading = true))
    val ui = _ui.asStateFlow()

    private val _events = MutableSharedFlow<RouteDetailEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                val sw = repo.getSaleWithItemsOnce(saleId)
                if (sw == null) {
                    _ui.value = RouteDetailUiState(saleId = saleId, isLoading = false)
                    _events.emit(RouteDetailEvent.Error("No se encontró la ruta (id=$saleId)."))
                    return@launch
                }
                _ui.value = sw.toUi()
            } catch (e: Exception) {
                _ui.value = RouteDetailUiState(saleId = saleId, isLoading = false)
                _events.emit(RouteDetailEvent.Error(e.message ?: "Error cargando historial"))
            }
        }
    }

    private fun SaleWithItems.toUi(): RouteDetailUiState {
        val lines = items.map { it ->
            val sold = it.soldQty ?: 0
            val returned = it.returnedQty ?: (it.dispatchedQty - sold)
            RouteDetailLineUi(
                productName = it.productName,
                unitPrice = it.unitPrice,
                dispatched = it.dispatchedQty,
                sold = sold,
                returned = returned
            )
        }

        val totalSold = lines.sumOf { it.soldTotal }

        return RouteDetailUiState(
            saleId = sale.id,
            status = sale.status,
            createdAt = sale.createdAtEpochMillis,
            messengerName = sale.messengerName ?: "",
            notes = sale.notes ?: "",
            totalSold = if (sale.status == SaleStatus.FINALIZADA) sale.total else totalSold,
            lines = lines,
            isLoading = false
        )
    }
}

class RouteDetailViewModelFactory(
    private val saleId: Long,
    private val repo: SaleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RouteDetailViewModel(saleId, repo) as T
}


