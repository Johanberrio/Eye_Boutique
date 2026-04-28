package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RouteCartLine(
    val productId: String,
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

    // ✅ Cliente
    val customerName: String = "",
    val customerPhone1: String = "",
    val customerPhone2: String = "",
    val customerAddress: String = "",
    val customerNeighborhood: String = "",

    val search: String = "",
    val products: List<ProductEntity> = emptyList(),
    val cart: List<RouteCartLine> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
    
    // ✅ Nuevo: Lista de mensajeros para el dropdown
    val messengerOptions: List<String> = emptyList()
)

sealed class NewRouteEvent {
    data class Error(val message: String) : NewRouteEvent()
    data class Success(val saleId: String) : NewRouteEvent()
}

class NewRouteViewModel(
    private val productRepo: ProductRepository,
    private val saleRepo: SaleRepository,
    private val messengerRepo: MessengerRepository // ✅ Añadido repositorio de mensajeros
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val _ui = MutableStateFlow(NewRouteUiState())
    val ui: StateFlow<NewRouteUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<NewRouteEvent>()
    val events = _events.asSharedFlow()

    init {
        // Observar búsqueda y productos en tiempo real
        searchQuery
            .debounce(200)
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { q -> 
                if (q.isBlank()) productRepo.observeAll() 
                else productRepo.observeSearch(q) 
            }
            .onEach { list -> 
                _ui.update { it.copy(products = list) } 
            }
            .launchIn(viewModelScope)

        // ✅ Observar mensajeros reales desde Firestore
        messengerRepo.observeAll()
            .onEach { list ->
                val names = list.map { it.name }.sorted()
                _ui.update { it.copy(messengerOptions = names) }
            }
            .launchIn(viewModelScope)
    }

    fun setMessengerName(v: String) = _ui.update { it.copy(messengerName = v) }
    fun setNotes(v: String) = _ui.update { it.copy(notes = v) }
    fun setSearch(v: String) {
        _ui.update { it.copy(search = v) }
        searchQuery.value = v
    }

    // Cliente setters
    fun setCustomerName(v: String) = _ui.update { it.copy(customerName = v) }
    fun setCustomerPhone1(v: String) = _ui.update { it.copy(customerPhone1 = v) }
    fun setCustomerPhone2(v: String) = _ui.update { it.copy(customerPhone2 = v) }
    fun setCustomerAddress(v: String) = _ui.update { it.copy(customerAddress = v) }
    fun setCustomerNeighborhood(v: String) = _ui.update { it.copy(customerNeighborhood = v) }

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
                    if (it.productId == p.id)
                        it.copy(
                            quantity = (it.quantity + 1).coerceAtMost(p.cantidad),
                            stock = p.cantidad
                        )
                    else it
                }
            }
            state.copy(cart = newCart)
        }
    }

    fun removeFromCart(productId: String) {
        _ui.update { it.copy(cart = it.cart.filterNot { l -> l.productId == productId }) }
    }

    fun setQty(productId: String, qty: Int) {
        _ui.update { state ->
            state.copy(
                cart = state.cart.map { line ->
                    if (line.productId == productId) {
                        val safe = qty.coerceAtLeast(1).coerceAtMost(line.stock)
                        line.copy(quantity = safe)
                    } else line
                }
            )
        }
    }

    fun setUnitPrice(productId: String, price: Double) {
        _ui.update { state ->
            state.copy(
                cart = state.cart.map { line ->
                    if (line.productId == productId) line.copy(unitPrice = price) else line
                }
            )
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
                    messengerName = state.messengerName.trim(),
                    notes = state.notes.trim(),
                    items = items,
                    customerName = state.customerName.trim(),
                    customerPhone1 = state.customerPhone1.trim(),
                    customerPhone2 = state.customerPhone2.trim().ifBlank { null },
                    customerAddress = state.customerAddress.trim(),
                    customerNeighborhood = state.customerNeighborhood.trim()
                )

                _events.emit(NewRouteEvent.Success(saleId))
                _ui.value = NewRouteUiState(messengerOptions = state.messengerOptions) // resetea pero mantiene opciones
            } catch (e: Exception) {
                _events.emit(NewRouteEvent.Error(e.message ?: "Error creando salida a ruta"))
                _ui.update { it.copy(isSaving = false) }
            }
        }
    }
}

class NewRouteViewModelFactory(
    private val productRepo: ProductRepository,
    private val saleRepo: SaleRepository,
    private val messengerRepo: MessengerRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NewRouteViewModel(productRepo, saleRepo, messengerRepo) as T
}
