package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lentespro.data.ProductRepository
import com.example.lentespro.data.SaleRepository

class InventoryViewModelFactory(
    private val repo: ProductRepository,
    private val saleRepo: SaleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InventoryViewModel(repo, saleRepo) as T
    }
}
