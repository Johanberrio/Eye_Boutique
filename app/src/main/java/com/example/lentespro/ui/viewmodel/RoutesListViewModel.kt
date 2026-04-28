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
    private val repo: SaleRepository,
    private val messengerRepo: MessengerRepository
) : ViewModel() {

    // ✅ Filtramos las ventas que finalizaron en 0 antes de numerar
    private val validSalesFlow = repo.observeSales()
        .map { list -> 
            list.filter { sale ->
                // Mantenemos las que están en ruta O las que se vendieron con total > 0
                sale.status == SaleStatus.EN_RUTA || (sale.status == SaleStatus.FINALIZADA && sale.total > 0)
            }.sortedBy { it.createdAtEpochMillis } // Orden cronológico para la numeración
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 🛵 EN RUTA (Usando la lista ya filtrada y numerada)
    val enRuta: StateFlow<List<Pair<Int, SaleEntity>>> =
        validSalesFlow
            .map { list -> 
                list.mapIndexed { index, sale -> (index + 1) to sale }
                    .filter { it.second.status == SaleStatus.EN_RUTA }
                    .reversed() // Más recientes arriba
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 💰 HISTORIAL (Solo las finalizadas exitosas, numeradas correctamente)
    val historyCards: StateFlow<List<SaleHistoryCard>> =
        validSalesFlow
            .map { list -> 
                list.mapIndexed { index, sale -> 
                    val saleNumber = index + 1
                    SaleHistoryCard(
                        saleId = sale.id,
                        saleNumber = saleNumber,
                        soldAtEpochMillis = sale.finalizedAtEpochMillis ?: sale.createdAtEpochMillis,
                        messengerName = sale.messengerName,
                        total = sale.total,
                        firstItemName = sale.items.firstOrNull()?.productName ?: "Sin productos",
                        soldQty = sale.items.sumOf { it.soldQty ?: 0 },
                        customerName = sale.customerName,
                        customerPhone1 = sale.customerPhone1,
                        customerNeighborhood = sale.customerNeighborhood, // ✅ PASADO EL PARÁMETRO FALTANTE
                        sellerUid = sale.sellerUid,
                        sellerName = sale.sellerName
                    )
                }
                .filter { it.soldAtEpochMillis > 0 && list.find { s -> s.id == it.saleId }?.status == SaleStatus.FINALIZADA }
                .reversed() // Más recientes arriba
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ✅ Mensajeros reales desde Firestore
    val messengerOptions: StateFlow<List<String>> = 
        messengerRepo.observeAll()
            .map { list -> listOf("Todos") + list.map { it.name }.sorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf("Todos"))
}

class RoutesListViewModelFactory(
    private val repo: SaleRepository,
    private val messengerRepo: MessengerRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoutesListViewModel(repo, messengerRepo) as T
}
