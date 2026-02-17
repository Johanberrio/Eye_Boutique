package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lentespro.data.SaleEntity
import com.example.lentespro.data.SaleRepository
import com.example.lentespro.data.SaleStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RoutesListViewModel(
    repo: SaleRepository
) : ViewModel() {

    val enRuta: StateFlow<List<SaleEntity>> =
        repo.observeSales()
            .map { list -> list.filter { it.status == SaleStatus.EN_RUTA } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val finalizadas: StateFlow<List<SaleEntity>> =
        repo.observeSales()
            .map { list -> list.filter { it.status == SaleStatus.FINALIZADA } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class RoutesListViewModelFactory(
    private val repo: SaleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoutesListViewModel(repo) as T
}
