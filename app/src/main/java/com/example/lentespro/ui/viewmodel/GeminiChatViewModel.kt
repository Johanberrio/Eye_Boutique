package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.GeminiRepository
import com.example.lentespro.data.ProductRepository
import com.example.lentespro.data.SaleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeminiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val navigationCommand: String? = null // Para que la UI reaccione y navegue
)

class GeminiChatViewModel(
    private val geminiRepo: GeminiRepository,
    private val productRepo: ProductRepository,
    private val saleRepo: SaleRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(GeminiChatUiState())
    val ui = _ui.asStateFlow()

    fun setInputText(text: String) {
        _ui.update { it.copy(inputText = text) }
    }

    fun clearNavigation() {
        _ui.update { it.copy(navigationCommand = null) }
    }

    fun sendMessage() {
        val text = _ui.value.inputText.trim()
        if (text.isBlank()) return

        val userMsg = ChatMessage(text, true)
        _ui.update { it.copy(
            messages = it.messages + userMsg,
            inputText = "",
            isTyping = true
        ) }

        viewModelScope.launch {
            // Recopilamos contexto de la app para que Gemini sepa de qué habla
            val contextInfo = getAppContextSummary()
            val prompt = "CONTEXTO DE LA APP:\n$contextInfo\n\nUSUARIO PREGUNTA: $text"
            
            val responseText = geminiRepo.sendMessage(prompt)
            
            // Detectar comandos de navegación [NAV:XXX]
            val navMatch = Regex("""\[NAV:(.+?)\]""").find(responseText)
            val cleanResponse = responseText.replace(Regex("""\[NAV:.+?\]"""), "").trim()
            
            val botMsg = ChatMessage(cleanResponse, false)
            
            _ui.update { it.copy(
                messages = it.messages + botMsg,
                isTyping = false,
                navigationCommand = navMatch?.groupValues?.get(1)
            ) }
        }
    }

    private suspend fun getAppContextSummary(): String {
        // Obtenemos datos reales para que Gemini analice
        val products = productRepo.getAllOnce()
        val sales = saleRepo.getAllSalesOnce() // Necesitamos este método en el repo
        
        val totalStock = products.sumOf { it.cantidad }
        val lowStockItems = products.filter { it.cantidad <= it.stockMinimo }.joinToString { it.nombre }
        
        // Un resumen simple para no saturar el prompt (tokens)
        return """
            - Lentes en inventario: ${products.size} modelos.
            - Stock total: $totalStock lentes.
            - Productos con stock bajo: $lowStockItems.
            - Ventas totales registradas: ${sales.size}.
            - Los nombres de lentes más comunes son: ${products.take(5).joinToString { it.nombre }}.
        """.trimIndent()
    }
}

class GeminiChatViewModelFactory(
    private val geminiRepo: GeminiRepository,
    private val productRepo: ProductRepository,
    private val saleRepo: SaleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        GeminiChatViewModel(geminiRepo, productRepo, saleRepo) as T
}
