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

    // ✅ Observar ventas para estadísticas
    private val allSales = saleRepo.observeSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 🛵 Cantidad total de PRODUCTOS en ruta (Suma de dispatchedQty de ventas EN_RUTA)
    val enRutaProductCount: StateFlow<Int> = allSales
        .map { list -> 
            list.filter { it.status == SaleStatus.EN_RUTA }
                .sumOf { sale -> sale.items.sumOf { it.dispatchedQty } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // 💰 Cantidad total de PRODUCTOS vendidos hoy (Suma de soldQty de ventas FINALIZADAS hoy)
    val ventasHoyProductCount: StateFlow<Int> = allSales
        .map { list ->
            val todayStart = Calendar.getInstance(TimeZone.getTimeZone("America/Bogota")).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            list.filter { 
                it.status == SaleStatus.FINALIZADA && 
                (it.finalizedAtEpochMillis ?: 0L) >= todayStart 
            }.sumOf { sale -> sale.items.sumOf { it.soldQty ?: 0 } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // 🗓️ Lógica de periodos de 30 días desde el 14 de Abril de 2026
    val ventasPeriodoActualCount: StateFlow<Int> = allSales
        .map { list ->
            val zone = TimeZone.getTimeZone("America/Bogota")
            // Fecha base: 14 de Abril de 2026
            val baseDate = Calendar.getInstance(zone).apply {
                set(2026, Calendar.APRIL, 14, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val now = System.currentTimeMillis()
            if (now < baseDate) return@map 0 

            val msPerDay = 24L * 60 * 60 * 1000
            val msPerPeriod = 30 * msPerDay
            
            val diff = now - baseDate
            val periodsCount = diff / msPerPeriod
            val currentPeriodStart = baseDate + (periodsCount * msPerPeriod)
            
            list.filter { 
                it.status == SaleStatus.FINALIZADA && 
                (it.finalizedAtEpochMillis ?: 0L) >= currentPeriodStart 
            }.sumOf { sale -> sale.items.sumOf { it.soldQty ?: 0 } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ✅ NUEVO: Total histórico de lentes vendidos en toda la base de datos
    val totalHistoricoVendido: StateFlow<Int> = allSales
        .map { list -> 
            list.filter { it.status == SaleStatus.FINALIZADA }
                .sumOf { sale -> sale.items.sumOf { it.soldQty ?: 0 } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
