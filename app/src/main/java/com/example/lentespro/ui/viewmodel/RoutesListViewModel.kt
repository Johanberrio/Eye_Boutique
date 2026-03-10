package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RoutesListViewModel(
    repo: SaleRepository
) : ViewModel() {

    // ✅ EN RUTA (Ventas filtradas por estado desde Firestore)
    val enRuta: StateFlow<List<SaleEntity>> =
        repo.observeSales()
            .map { list -> list.filter { it.status == SaleStatus.EN_RUTA } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ✅ HISTORIAL (Ventas finalizadas mapeadas a tarjetas)
    val historyCards: StateFlow<List<SaleHistoryCard>> =
        repo.observeSales()
            .map { list -> 
                list.filter { it.status == SaleStatus.FINALIZADA }
                    .map { sale ->
                        SaleHistoryCard(
                            saleId = sale.id,
                            soldAtEpochMillis = sale.finalizedAtEpochMillis ?: sale.createdAtEpochMillis,
                            messengerName = sale.messengerName,
                            total = sale.total,
                            firstItemName = sale.items.firstOrNull()?.productName ?: "Sin productos",
                            soldQty = sale.items.sumOf { it.soldQty ?: 0 },
                            customerName = sale.customerName,
                            customerPhone1 = sale.customerPhone1,
                            sellerUid = sale.sellerUid,
                            sellerName = sale.sellerName
                        )
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class RoutesListViewModelFactory(
    private val repo: SaleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoutesListViewModel(repo) as T
}
