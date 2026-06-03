package com.example.lentespro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lentespro.data.AdminNotesRepository
import com.example.lentespro.data.AuthProfileRepository
import com.example.lentespro.data.GeminiRepository
import com.example.lentespro.data.ProductRepository

class EditProductViewModelFactory(
    private val repo: ProductRepository,
    private val adminNotesRepo: AdminNotesRepository,
    private val authProfileRepo: AuthProfileRepository,
    private val geminiRepo: GeminiRepository,
    private val productId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditProductViewModel(
            repo = repo,
            adminNotesRepo = adminNotesRepo,
            authProfileRepo = authProfileRepo,
            geminiRepo = geminiRepo,
            productId = productId
        ) as T
    }
}
