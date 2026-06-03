package com.example.lentespro.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream

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
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val messengerOptions: List<String> = emptyList(),
    val autoFillText: String = ""
)

sealed class NewRouteEvent {
    data class Error(val message: String) : NewRouteEvent()
    data class Success(val saleId: String) : NewRouteEvent()
    data class Info(val message: String) : NewRouteEvent()
}

class NewRouteViewModel(
    private val productRepo: ProductRepository,
    private val saleRepo: SaleRepository,
    private val messengerRepo: MessengerRepository,
    private val geminiRepo: GeminiRepository
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

    fun analyzeCustomerImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            _ui.update { it.copy(isAnalyzing = true) }
            try {
                // 1. Cargar y optimizar imagen
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                val bitmap = Bitmap.createScaledBitmap(originalBitmap, 1024, (originalBitmap.height * (1024.0 / originalBitmap.width)).toInt(), true)

                val prompt = """
                    Analiza esta imagen y extrae los datos del cliente para una entrega.
                    Devuelve estrictamente este formato:
                    NOMBRE: [valor]
                    TEL: [valor]
                    DIR: [valor]
                    BARRIO: [valor]
                """.trimIndent()

                val response = geminiRepo.analyzeImage(prompt, bitmap)
                
                // ✅ Extractor robusto con Regex
                fun extract(key: String): String {
                    val pattern = Regex("(?i)$key[:\\s\\*]+(.*?)(?:\\r?\\n|$)", RegexOption.MULTILINE)
                    return pattern.find(response)?.groupValues?.get(1)?.replace("*", "")?.trim() ?: ""
                }

                _ui.update { it.copy(
                    customerName = extract("NOMBRE").ifBlank { it.customerName },
                    customerPhone1 = extract("TEL").ifBlank { it.customerPhone1 },
                    customerAddress = extract("DIR").ifBlank { it.customerAddress },
                    customerNeighborhood = extract("BARRIO").ifBlank { it.customerNeighborhood }
                ) }
                _events.emit(NewRouteEvent.Info("Datos del cliente cargados ✨"))
            } catch (e: Exception) {
                _events.emit(NewRouteEvent.Error("No se pudo leer la imagen."))
            } finally {
                _ui.update { it.copy(isAnalyzing = false) }
            }
        }
    }

    fun setMessengerName(v: String) = _ui.update { it.copy(messengerName = v) }
    fun setNotes(v: String) = _ui.update { it.copy(notes = v) }
    fun setSearch(v: String) { _ui.update { it.copy(search = v) }; searchQuery.value = v }
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
        var foundName = ""; var foundPhone = ""; var foundAddr = ""
        val phoneIdx = lines.indexOfFirst { l -> l.filter { it.isDigit() }.length in 7..15 }
        if (phoneIdx != -1) foundPhone = lines[phoneIdx]
        val addrKeywords = listOf("calle", "cl", "cra", "carrera", "diagonal", "dg", "#")
        val addrIdx = lines.indexOfFirst { l -> addrKeywords.any { l.lowercase().contains(it) } }
        if (addrIdx != -1) foundAddr = lines[addrIdx]
        val nameIdx = lines.indices.firstOrNull { it != phoneIdx && it != addrIdx && lines[it].length > 3 }
        if (nameIdx != null) foundName = lines[nameIdx]
        _ui.update { it.copy(customerName = foundName, customerPhone1 = foundPhone, customerAddress = foundAddr, autoFillText = "") }
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
            if (state.cart.isEmpty()) { _events.emit(NewRouteEvent.Error("Agrega al menos un producto.")); return@launch }
            _ui.update { it.copy(isSaving = true) }
            try {
                val items = state.cart.map { CreateRouteItem(it.productId, it.unitPrice, it.quantity) }
                val saleId = saleRepo.createRouteDispatch(state.messengerName, state.notes, items, state.customerName, state.customerPhone1, state.customerPhone2.ifBlank { null }, state.customerAddress, state.customerNeighborhood)
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
    private val messengerRepo: MessengerRepository,
    private val geminiRepo: GeminiRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = NewRouteViewModel(productRepo, saleRepo, messengerRepo, geminiRepo) as T
}
