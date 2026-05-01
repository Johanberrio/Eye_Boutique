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
    val isSuperAdmin: Boolean = false, // ✅ Nuevo
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

                val hasAccess = authProfileRepo.isAdmin() // Devuelve true para ADMIN y SUPERADMIN
                val isSuper = authProfileRepo.isSuperAdmin()

                if (!hasAccess) {
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        isAdmin = false,
                        isSuperAdmin = false,
                        users = emptyList(),
                        error = "No autorizado."
                    )
                    return@launch
                }

                val list = repo.listUsers()
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    isAdmin = true,
                    isSuperAdmin = isSuper,
                    users = list,
                    error = null
                )
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(isLoading = false, error = t.message ?: "Error cargando usuarios")
                _events.emit(AdminUsersEvent.Error(_ui.value.error!!))
            }
        }
    }

    fun setActive(user: RemoteUserRow, active: Boolean) {
        viewModelScope.launch {
            try {
                // Solo el SuperAdmin puede desactivar a otros Admins
                val isSuper = _ui.value.isSuperAdmin
                if (!isSuper && user.role.uppercase() == "ADMIN") {
                    _events.emit(AdminUsersEvent.Error("Solo el SuperAdmin puede modificar a otros administradores."))
                    return@launch
                }

                _ui.value = _ui.value.copy(isDeletingUid = user.uid)
                repo.setActive(user.uid, active)
                _ui.value = _ui.value.copy(isDeletingUid = null)
                _events.emit(AdminUsersEvent.Message("Estado actualizado ✅"))
                refresh()
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(isDeletingUid = null)
                _events.emit(AdminUsersEvent.Error(t.message ?: "Error cambiando estado"))
            }
        }
    }
}
