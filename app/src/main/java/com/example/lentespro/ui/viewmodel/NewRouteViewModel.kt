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
    val messengerOptions: List<String> = emptyList(),
    val autoFillText: String = ""
)

sealed class NewRouteEvent {
    data class Error(val message: String) : NewRouteEvent()
    data class Success(val saleId: String) : NewRouteEvent()
}

class NewRouteViewModel(
    private val productRepo: ProductRepository,
    private val saleRepo: SaleRepository,
    private val messengerRepo: MessengerRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val _ui = MutableStateFlow(NewRouteUiState())
    val ui: StateFlow<NewRouteUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<NewRouteEvent>()
    val events = _events.asSharedFlow()

    init {
        searchQuery
            .debounce(200)
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { q -> 
                if (q.isBlank()) productRepo.observeAll() 
                else productRepo.observeSearch(q) 
            }
            .onEach { list -> _ui.update { it.copy(products = list) } }
            .launchIn(viewModelScope)

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

    fun setCustomerName(v: String) = _ui.update { it.copy(customerName = v) }
    fun setCustomerPhone1(v: String) = _ui.update { it.copy(customerPhone1 = v) }
    fun setCustomerPhone2(v: String) = _ui.update { it.copy(customerPhone2 = v) }
    fun setCustomerAddress(v: String) = _ui.update { it.copy(customerAddress = v) }
    fun setCustomerNeighborhood(v: String) = _ui.update { it.copy(customerNeighborhood = v) }
    fun setAutoFillText(v: String) = _ui.update { it.copy(autoFillText = v) }

    fun processAutoFill() {
        val raw = _ui.value.autoFillText
        if (raw.isBlank()) return

        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        var foundName = ""
        var foundPhone = ""
        var foundAddr = ""
        var foundNeigh = ""

        val addrKeywords = listOf("carrera", "cra", "calle", "cl", "diagonal", "dg", "transversal", "tv", "avenida", "av", "#")
        val extraAddrKeywords = listOf("interior", "int", "apto", "apartamento", "piso", "casa", "torre", "bloque", "local", "conjunto", "unidad")

        val used = mutableSetOf<Int>()

        // 1. Teléfono
        val phoneIdx = lines.indexOfFirst { line ->
            val digits = line.filter { it.isDigit() }
            digits.length in 7..15 && line.count { it.isLetter() } < 5
        }
        if (phoneIdx != -1) {
            foundPhone = lines[phoneIdx]
            used.add(phoneIdx)
        }

        // 2. Dirección y Barrio
        val addrIdx = lines.indexOfFirst { line ->
            if (used.contains(lines.indexOf(line))) return@indexOfFirst false
            val lower = line.lowercase()
            addrKeywords.any { kw -> lower.startsWith("$kw ") || lower.contains(" $kw ") || lower.contains("$kw#") } || 
            lower.contains(Regex("""\d+[a-zA-Z]?\s*#\s*\d+"""))
        }

        if (addrIdx != -1) {
            val rawAddrLine = lines[addrIdx]
            used.add(addrIdx)
            
            // Regex para detectar el final de la numeración (ej: #39-64 o a 21)
            val houseNumPattern = Regex("""(\d+[a-zA-Z]?\s*(?:#|-|a|bis|#|–|—|a)\s*\d+[a-zA-Z]?)""", RegexOption.IGNORE_CASE)
            val matches = houseNumPattern.findAll(rawAddrLine).toList()
            val lastMatch = matches.lastOrNull()
            
            if (lastMatch != null) {
                val endOfHouseNum = lastMatch.range.last + 1
                foundAddr = rawAddrLine.substring(0, endOfHouseNum).trim()
                val tail = rawAddrLine.substring(endOfHouseNum).trim()
                
                if (tail.length > 2) {
                    val tailLower = tail.lowercase()
                    if (extraAddrKeywords.any { tailLower.contains(it) || tailLower.startsWith("int") }) {
                        foundAddr = rawAddrLine
                    } else {
                        foundNeigh = tail
                    }
                }
            } else {
                foundAddr = rawAddrLine
            }

            // Buscar en líneas siguientes (Extras o Barrio)
            var current = addrIdx + 1
            while (current < lines.size) {
                if (used.contains(current)) { current++; continue }
                val line = lines[current]
                val lower = line.lowercase()
                if (extraAddrKeywords.any { lower.contains(it) } || lower.contains(Regex("""\b(int|apto|piso|local|torre|bloque|casa)\b"""))) {
                    foundAddr += " - $line"
                    used.add(current); current++
                } else if (foundNeigh.isBlank() && line.length in 3..50 && line.count { it.isDigit() } < 6) {
                    foundNeigh = line
                    used.add(current); break
                } else break
            }
        }

        // 3. Nombre
        val nameIdx = lines.indices.firstOrNull { idx -> !used.contains(idx) && lines[idx].count { it.isLetter() } > 3 }
        if (nameIdx != null) foundName = lines[nameIdx]

        _ui.update { it.copy(
            customerName = foundName.ifBlank { it.customerName },
            customerPhone1 = foundPhone.ifBlank { it.customerPhone1 },
            customerAddress = foundAddr.ifBlank { it.customerAddress },
            customerNeighborhood = foundNeigh.ifBlank { it.customerNeighborhood },
            autoFillText = "" 
        ) }
    }

    fun addToCart(p: ProductEntity) {
        _ui.update { state ->
            val existing = state.cart.find { it.productId == p.id }
            val newCart = if (existing == null) {
                state.cart + RouteCartLine(p.id, "${p.nombre} (${p.marca})", p.cantidad, p.precioVenta, 1)
            } else {
                state.cart.map { if (it.productId == p.id) it.copy(quantity = (it.quantity + 1).coerceAtMost(p.cantidad)) else it }
            }
            state.copy(cart = newCart)
        }
    }

    fun removeFromCart(productId: String) = _ui.update { it.copy(cart = it.cart.filterNot { l -> l.productId == productId }) }
    fun setQty(productId: String, qty: Int) = _ui.update { state -> state.copy(cart = state.cart.map { if (it.productId == productId) it.copy(quantity = qty.coerceAtLeast(1).coerceAtMost(it.stock)) else it }) }
    fun setUnitPrice(productId: String, price: Double) = _ui.update { state -> state.copy(cart = state.cart.map { if (it.productId == productId) it.copy(unitPrice = price) else it }) }

    fun dispatchToRoute() {
        viewModelScope.launch {
            val state = _ui.value
            if (state.cart.isEmpty()) {
                _events.emit(NewRouteEvent.Error("Agrega al menos un producto."))
                return@launch
            }
            _ui.update { it.copy(isSaving = true) }
            try {
                val items = state.cart.map { CreateRouteItem(it.productId, it.unitPrice, it.quantity) }
                val saleId = saleRepo.createRouteDispatch(
                    state.messengerName, state.notes, items, state.customerName, 
                    state.customerPhone1, state.customerPhone2.ifBlank { null }, 
                    state.customerAddress, state.customerNeighborhood
                )
                _events.emit(NewRouteEvent.Success(saleId))
                _ui.value = NewRouteUiState(messengerOptions = state.messengerOptions)
            } catch (e: Exception) {
                _events.emit(NewRouteEvent.Error(e.message ?: "Error creando ruta"))
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
