package com.example.budgettracker.data.local.dao

import androidx.room.*
import com.example.budgettracker.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the categories table.
 */
@Dao
interface CategoryDao {

    /**
     * Adds a new category (e.g., Food, Salary).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity)

    /**
     * Retrieves categories filtered by type (INCOME or EXPENSE).
     * Used for populating dropdowns in the UI.
     */
    @Query("SELECT * FROM categories WHERE type = :type")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>
}
