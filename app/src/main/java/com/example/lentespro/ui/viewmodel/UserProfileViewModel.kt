package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.AuthProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val isLoading: Boolean = true,
    val isAdmin: Boolean = false,
    val displayName: String = "Usuario"
)

class UserProfileViewModel(
    private val repo: AuthProfileRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(UserProfileUiState())
    val ui: StateFlow<UserProfileUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(isLoading = true)
                val admin = repo.isAdmin()
                val name = repo.displayName()
                _ui.value = UserProfileUiState(
                    isLoading = false,
                    isAdmin = admin,
                    displayName = name
                )
            } catch (_: Exception) {
                // Si falla internet/Firestore, por seguridad: no admin
                _ui.value = UserProfileUiState(
                    isLoading = false,
                    isAdmin = false,
                    displayName = "Usuario"
                )
            }
        }
    }
}

class UserProfileViewModelFactory(
    private val repo: AuthProfileRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UserProfileViewModel(repo) as T
    }
}