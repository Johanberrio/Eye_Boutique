package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val saleId: String = "",
    val status: SaleStatus = SaleStatus.EN_RUTA,
    val createdAt: Long = 0L,
    val messengerName: String = "",
    val customerName: String = "",
    val customerPhone1: String = "",
    val customerPhone2: String? = null,
    val customerAddress: String = "",
    val customerNeighborhood: String = "",
    val notes: String = "",
    val totalSold: Double = 0.0,
    val lines: List<RouteDetailLineUi> = emptyList(),
    val isLoading: Boolean = true
)

sealed class RouteDetailEvent {
    data class Error(val message: String) : RouteDetailEvent()
}

class RouteDetailViewModel(
    private val saleId: String,
    private val repo: SaleRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(RouteDetailUiState(saleId = saleId, isLoading = true))
    val ui = _ui.asStateFlow()

    private val _events = MutableSharedFlow<RouteDetailEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                val sale = repo.getSaleOnce(saleId)
                if (sale == null) {
                    _ui.update { it.copy(isLoading = false) }
                    _events.emit(RouteDetailEvent.Error("No se encontró la ruta (id=$saleId)."))
                    return@launch
                }
                
                val lines = sale.items.map { item ->
                    val sold = item.soldQty ?: 0
                    val returned = item.returnedQty ?: (item.dispatchedQty - sold)
                    RouteDetailLineUi(
                        productName = item.productName,
                        unitPrice = item.unitPrice,
                        dispatched = item.dispatchedQty,
                        sold = sold,
                        returned = returned
                    )
                }

                _ui.value = RouteDetailUiState(
                    saleId = sale.id,
                    status = sale.status,
                    createdAt = sale.createdAtEpochMillis,
                    messengerName = sale.messengerName ?: "—",
                    customerName = sale.customerName,
                    customerPhone1 = sale.customerPhone1,
                    customerPhone2 = sale.customerPhone2,
                    customerAddress = sale.customerAddress ?: "—",
                    customerNeighborhood = sale.customerNeighborhood ?: "—",
                    notes = sale.notes ?: "—",
                    totalSold = if (sale.status == SaleStatus.FINALIZADA) sale.total else lines.sumOf { it.soldTotal },
                    lines = lines,
                    isLoading = false
                )
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false) }
                _events.emit(RouteDetailEvent.Error(e.message ?: "Error cargando historial"))
            }
        }
    }
}

class RouteDetailViewModelFactory(
    private val saleId: String,
    private val repo: SaleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RouteDetailViewModel(saleId, repo) as T
}
