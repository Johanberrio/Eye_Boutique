package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.AdminNotesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminNotesViewModel(
    private val repo: AdminNotesRepository
) : ViewModel() {

    val notes: StateFlow<String> = repo.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun updateNotes(text: String) {
        viewModelScope.launch {
            repo.updateNotes(text)
        }
    }
}

class AdminNotesViewModelFactory(
    private val repo: AdminNotesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AdminNotesViewModel(repo) as T
}
