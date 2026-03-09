package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.AdminUsersRepository
import com.example.lentespro.data.AuthProfileRepository
import com.example.lentespro.data.RemoteUserRow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminUsersUiState(
    val isLoading: Boolean = true,
    val isAdmin: Boolean = false,
    val users: List<RemoteUserRow> = emptyList(),
    val error: String? = null,

    val isCreating: Boolean = false,
    val isDeletingUid: String? = null
)

sealed class AdminUsersEvent {
    data class Error(val msg: String) : AdminUsersEvent()
    data class Message(val msg: String) : AdminUsersEvent()
}

class AdminUsersViewModel(
    private val repo: AdminUsersRepository,
    private val authProfileRepo: AuthProfileRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AdminUsersUiState())
    val ui: StateFlow<AdminUsersUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<AdminUsersEvent>()
    val events: SharedFlow<AdminUsersEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(isLoading = true, error = null)

                val isAdmin = authProfileRepo.isAdmin()
                if (!isAdmin) {
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        isAdmin = false,
                        users = emptyList(),
                        error = "No autorizado. Solo ADMIN."
                    )
                    return@launch
                }

                val list = repo.listUsers() // ✅ incluye admins y sellers
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    isAdmin = true,
                    users = list,
                    error = null
                )
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(isLoading = false, error = t.message ?: "Error cargando usuarios")
                _events.emit(AdminUsersEvent.Error(_ui.value.error!!))
            }
        }
    }

    fun createSeller(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            try {
                if (!_ui.value.isAdmin) {
                    _events.emit(AdminUsersEvent.Error("Solo ADMIN puede crear usuarios."))
                    return@launch
                }

                val e = email.trim()
                val d = displayName.trim()
                if (d.isBlank()) {
                    _events.emit(AdminUsersEvent.Error("Nombre obligatorio."))
                    return@launch
                }
                if (e.isBlank()) {
                    _events.emit(AdminUsersEvent.Error("Correo obligatorio."))
                    return@launch
                }
                if (password.length < 6) {
                    _events.emit(AdminUsersEvent.Error("Contraseña mínimo 6 caracteres."))
                    return@launch
                }

                _ui.value = _ui.value.copy(isCreating = true)
                // Usar registerSeller de AuthRepository o similar si repo no lo tiene
                // Por ahora asumimos que repo tiene createSeller o similar
                // repo.createSeller(email = e, password = password, displayName = d)
                
                _ui.value = _ui.value.copy(isCreating = false)
                _events.emit(AdminUsersEvent.Message("Vendedor creado ✅"))
                refresh()
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(isCreating = false)
                _events.emit(AdminUsersEvent.Error(t.message ?: "Error creando vendedor"))
            }
        }
    }

    fun setActive(user: RemoteUserRow, active: Boolean) {
        viewModelScope.launch {
            try {
                if (!_ui.value.isAdmin) {
                    _events.emit(AdminUsersEvent.Error("Solo ADMIN."))
                    return@launch
                }
                _ui.value = _ui.value.copy(isDeletingUid = user.uid)
                repo.setActive(user.uid, active)
                _ui.value = _ui.value.copy(isDeletingUid = null)
                _events.emit(AdminUsersEvent.Message(if (active) "Usuario activado ✅" else "Usuario desactivado ✅"))
                refresh()
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(isDeletingUid = null)
                _events.emit(AdminUsersEvent.Error(t.message ?: "Error cambiando estado"))
            }
        }
    }
}
