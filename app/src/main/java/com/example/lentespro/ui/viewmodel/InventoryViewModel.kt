package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.ProductEntity
import com.example.lentespro.data.ProductRepository
import com.example.lentespro.data.SaleRepository
import com.example.lentespro.data.SaleStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class InventoryViewModel(
    private val repo: ProductRepository,
    private val saleRepo: SaleRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val products: StateFlow<List<ProductEntity>> =
        searchQuery
            .debounce(200)
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { q ->
                if (q.isBlank()) repo.observeAll() else repo.observeSearch(q)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(q: String) {
        searchQuery.value = q
    }

    fun delete(product: ProductEntity) {
        viewModelScope.launch {
            repo.delete(product)
        }
    }

    private val allProducts: StateFlow<List<ProductEntity>> =
        repo.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalLentes: StateFlow<Int> =
        allProducts
            .map { list -> list.sumOf { it.cantidad } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val alertaTotalBajo: StateFlow<Boolean> =
        totalLentes
            .map { total -> total <= 50 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // ✅ NUEVO: Observar ventas para estadísticas
    private val allSales = saleRepo.observeSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 🛵 Productos en ruta (Ventas con estado EN_RUTA)
    val enRutaCount: StateFlow<Int> = allSales
        .map { list -> list.count { it.status == SaleStatus.EN_RUTA } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // 💰 Ventas diarias (Finalizadas hoy)
    val ventasHoyCount: StateFlow<Int> = allSales
        .map { list ->
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            list.count { 
                it.status == SaleStatus.FINALIZADA && 
                (it.finalizedAtEpochMillis ?: 0L) >= todayStart 
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
