package com.example.budgettracker.repository

import com.example.budgettracker.data.local.dao.CategoryDao
import com.example.budgettracker.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing categories.
 * Separates the data source logic from the rest of the application.
 */
class CategoryRepository(private val categoryDao: CategoryDao) {

    // Retrieves categories filtered by type (Income/Expense)
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = 
        categoryDao.getCategoriesByType(type)

    // Inserts a new category into the local database
    suspend fun insertCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }
}
