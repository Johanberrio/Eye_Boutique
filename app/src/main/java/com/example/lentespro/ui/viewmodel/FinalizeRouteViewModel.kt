package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FinalizeLine(
    val productId: Long,
    val name: String,
    val dispatched: Int,
    val unitPrice: Double,
    val sold: Int
) {
    val returned: Int = dispatched - sold
    val soldTotal: Double = sold * unitPrice
}

data class FinalizeUiState(
    val saleId: Long = -1L,
    val messengerName: String = "",
    val notes: String = "",
    val lines: List<FinalizeLine> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isAdmin: Boolean = false,
    val sellerOptions: List<SellerOption> = emptyList(),
    val selectedSellerUid: String = "",
    val selectedSellerName: String = ""
) {
    val totalSold: Double = lines.sumOf { it.soldTotal }
}

sealed class FinalizeEvent {
    data class Error(val message: String) : FinalizeEvent()
    data object Success : FinalizeEvent()
}

class FinalizeRouteViewModel(
    private val saleId: Long,
    private val repo: SaleRepository,
    private val authProfileRepo: AuthProfileRepository,
    private val usersRemoteRepo: UsersRemoteRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(FinalizeUiState(saleId = saleId, isLoading = true))
    val ui: StateFlow<FinalizeUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<FinalizeEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                // (A) Cargar venta desde Room
                val sw = repo.getSaleWithItemsOnce(saleId)
                if (sw == null) {
                    _ui.value = FinalizeUiState(saleId = saleId, isLoading = false)
                    _events.emit(FinalizeEvent.Error("Salida no encontrada (id=$saleId)."))
                    return@launch
                }
                
                // (B) Cargar perfil del usuario actual desde Firestore
                val me = authProfileRepo.getUserProfile()

                _ui.update { st ->
                    sw.toUi().copy(
                        isAdmin = (me.role == UserRole.ADMIN),
                        selectedSellerUid = me.uid,
                        selectedSellerName = me.displayName
                    )
                }

                // (C) Si es ADMIN, cargar lista de vendedores para el dropdown
                if (me.role == UserRole.ADMIN) {
                    val sellers = usersRemoteRepo.getSellers()

                    // Incluir al admin actual en la lista si no está
                    val withMe = if (sellers.any { it.uid == me.uid }) {
                        sellers
                    } else {
                        sellers + SellerOption(uid = me.uid, displayName = me.displayName)
                    }.sortedBy { it.displayName.lowercase() }

                    _ui.update { st ->
                        st.copy(sellerOptions = withMe)
                    }
                }

            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false) }
                _events.emit(FinalizeEvent.Error(e.message ?: "Error cargando datos"))
            }
        }
    }

    fun setSold(productId: Long, sold: Int) {
        _ui.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.productId == productId) {
                        val safe = sold.coerceAtLeast(0).coerceAtMost(line.dispatched)
                        line.copy(sold = safe)
                    } else line
                }
            )
        }
    }

    fun selectSeller(uid: String, name: String) {
        _ui.update { it.copy(selectedSellerUid = uid, selectedSellerName = name) }
    }

    fun finalize() {
        viewModelScope.launch {
            val state = _ui.value
            if (state.isSaving) return@launch
            
            if (state.selectedSellerUid.isBlank()) {
                _events.emit(FinalizeEvent.Error("Debes seleccionar un vendedor"))
                return@launch
            }

            _ui.update { it.copy(isSaving = true) }

            try {
                val map = state.lines.associate { it.productId to it.sold }
                repo.finalizeDispatch(
                    saleId = state.saleId,
                    soldByProductId = map,
                    sellerUid = state.selectedSellerUid,
                    sellerName = state.selectedSellerName
                )
                _events.emit(FinalizeEvent.Success)
            } catch (e: Exception) {
                _events.emit(FinalizeEvent.Error(e.message ?: "Error finalizando"))
                _ui.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun SaleWithItems.toUi(): FinalizeUiState {
        return FinalizeUiState(
            saleId = sale.id,
            messengerName = sale.messengerName ?: "",
            notes = sale.notes ?: "",
            lines = items.map {
                FinalizeLine(
                    productId = it.productId,
                    name = it.productName,
                    dispatched = it.dispatchedQty,
                    unitPrice = it.unitPrice,
                    sold = 0
                )
            },
            isLoading = false
        )
    }
}

class FinalizeRouteViewModelFactory(
    private val saleId: Long,
    private val repo: SaleRepository,
    private val authProfileRepo: AuthProfileRepository,
    private val usersRemoteRepo: UsersRemoteRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FinalizeRouteViewModel(saleId, repo, authProfileRepo, usersRemoteRepo) as T
}
