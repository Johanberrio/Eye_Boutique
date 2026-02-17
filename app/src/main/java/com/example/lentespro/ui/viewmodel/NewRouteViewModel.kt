package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.CreateRouteItem
import com.example.lentespro.data.ProductEntity
import com.example.lentespro.data.ProductRepository
import com.example.lentespro.data.SaleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RouteCartLine(
    val productId: Long,
    val name: String,
    val stock: Int,
    val unitPrice: Double,
    val quantity: Int
) {
    val lineTotal: Double get() = unitPrice * quantity
}

data class NewRouteUiState(
    val messengerName: String = "",
    val notes: String = "",
    val search: String = "",
    val products: List<ProductEntity> = emptyList(),
    val cart: List<RouteCartLine> = emptyList(),
    val isSaving: Boolean = false
)

sealed class NewRouteEvent {
    data class Error(val message: String) : NewRouteEvent()
    data class Success(val saleId: Long) : NewRouteEvent()
}

class NewRouteViewModel(
    private val productRepo: ProductRepository,
    private val saleRepo: SaleRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val _ui = MutableStateFlow(NewRouteUiState())
    val ui: StateFlow<NewRouteUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<NewRouteEvent>()
    val events = _events.asSharedFlow()

    private val productsFlow: Flow<List<ProductEntity>> =
        searchQuery
            .debounce(200)
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { q -> if (q.isBlank()) productRepo.observeAll() else productRepo.observeSearch(q) }

    init {
        viewModelScope.launch {
            productsFlow.collect { list ->
                _ui.update { it.copy(products = list) }
            }
        }
    }

    fun setMessengerName(v: String) = _ui.update { it.copy(messengerName = v) }
    fun setNotes(v: String) = _ui.update { it.copy(notes = v) }
    fun setSearch(v: String) {
        _ui.update { it.copy(search = v) }
        searchQuery.value = v
    }

    fun addToCart(p: ProductEntity) {
        _ui.update { state ->
            val existing = state.cart.find { it.productId == p.id }
            val newCart = if (existing == null) {
                state.cart + RouteCartLine(
                    productId = p.id,
                    name = "${p.nombre} (${p.marca})",
                    stock = p.cantidad,
                    unitPrice = p.precioVenta,
                    quantity = 1
                )
            } else {
                state.cart.map {
                    if (it.productId == p.id) it.copy(quantity = (it.quantity + 1).coerceAtMost(p.cantidad), stock = p.cantidad)
                    else it
                }
            }
            state.copy(cart = newCart)
        }
    }

    fun removeFromCart(productId: Long) {
        _ui.update { it.copy(cart = it.cart.filterNot { l -> l.productId == productId }) }
    }

    fun setQty(productId: Long, qty: Int) {
        _ui.update { state ->
            state.copy(cart = state.cart.map { line ->
                if (line.productId == productId) {
                    val safe = qty.coerceAtLeast(1).coerceAtMost(line.stock)
                    line.copy(quantity = safe)
                } else line
            })
        }
    }

    fun setUnitPrice(productId: Long, price: Double) {
        _ui.update { state ->
            state.copy(cart = state.cart.map { line ->
                if (line.productId == productId) line.copy(unitPrice = price) else line
            })
        }
    }

    fun dispatchToRoute() {
        viewModelScope.launch {
            val state = _ui.value
            if (state.cart.isEmpty()) {
                _events.emit(NewRouteEvent.Error("Agrega al menos un producto para salir a ruta."))
                return@launch
            }

            _ui.update { it.copy(isSaving = true) }

            try {
                val items = state.cart.map {
                    CreateRouteItem(
                        productId = it.productId,
                        unitPrice = it.unitPrice,
                        quantity = it.quantity
                    )
                }

                val saleId = saleRepo.createRouteDispatch(
                    messengerName = state.messengerName,
                    notes = state.notes,
                    items = items
                )

                _events.emit(NewRouteEvent.Success(saleId))
                _ui.value = NewRouteUiState()
            } catch (e: Exception) {
                _events.emit(NewRouteEvent.Error(e.message ?: "Error creando salida a ruta"))
                _ui.update { it.copy(isSaving = false) }
            }
        }
    }
}

class NewRouteViewModelFactory(
    private val productRepo: ProductRepository,
    private val saleRepo: SaleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NewRouteViewModel(productRepo, saleRepo) as T
}
