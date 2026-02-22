package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.MessengerEntity
import com.example.lentespro.data.MessengerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MessengersViewModel(private val repo: MessengerRepository) : ViewModel() {
    val messengers: StateFlow<List<MessengerEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, phone: String, address: String?) {
        viewModelScope.launch { repo.create(name, phone, address) }
    }

    fun delete(m: MessengerEntity) {
        viewModelScope.launch { repo.delete(m) }
    }
}

class MessengersViewModelFactory(private val repo: MessengerRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MessengersViewModel(repo) as T
}
