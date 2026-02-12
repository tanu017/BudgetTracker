package com.example.budgettracker.viewmodel

import androidx.lifecycle.*
import com.example.budgettracker.data.local.entities.CategoryEntity
import com.example.budgettracker.repository.CategoryRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Categories.
 * Provides lists of Income or Expense categories for selection.
 */
class CategoryViewModel(private val repository: CategoryRepository) : ViewModel() {

    /**
     * Gets categories filtered by type (INCOME/EXPENSE).
     */
    fun getCategoriesByType(type: String): LiveData<List<CategoryEntity>> {
        return repository.getCategoriesByType(type).asLiveData()
    }

    /**
     * Adds a new category (e.g., "Dining Out").
     */
    fun insertCategory(category: CategoryEntity) = viewModelScope.launch {
        repository.insertCategory(category)
    }
}
