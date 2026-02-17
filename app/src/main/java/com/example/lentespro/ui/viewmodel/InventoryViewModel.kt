package com.example.lentespro.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.ProductEntity
import com.example.lentespro.data.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val repo: ProductRepository
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

    // ✅ NUEVO: siempre observa TODO el inventario (sin filtro)
    private val allProducts: StateFlow<List<ProductEntity>> =
        repo.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ✅ NUEVO: total de lentes en inventario (suma de cantidades)
    val totalLentes: StateFlow<Int> =
        allProducts
            .map { list -> list.sumOf { it.cantidad } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ✅ NUEVO: alerta cuando el total es 50 o menos
    val alertaTotalBajo: StateFlow<Boolean> =
        totalLentes
            .map { total -> total <= 50 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
