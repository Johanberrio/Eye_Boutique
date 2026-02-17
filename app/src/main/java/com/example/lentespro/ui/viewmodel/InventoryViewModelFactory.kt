package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lentespro.data.ProductRepository

class InventoryViewModelFactory(
    private val repo: ProductRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InventoryViewModel(repo) as T
    }
}
