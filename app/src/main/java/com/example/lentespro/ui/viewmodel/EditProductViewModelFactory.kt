package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lentespro.data.ProductRepository

class EditProductViewModelFactory(
    private val repo: ProductRepository,
    private val productId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditProductViewModel(repo, productId) as T
    }
}
