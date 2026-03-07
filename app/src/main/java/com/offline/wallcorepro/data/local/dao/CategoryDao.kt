package com.offline.wallcorepro.data.local.dao

import androidx.room.*
import com.offline.wallcorepro.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE niche = :niche ORDER BY count DESC")
    fun getCategoriesFlow(niche: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE niche = :niche ORDER BY count DESC")
    suspend fun getCategories(niche: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE niche = :niche")
    suspend fun clearCategories(niche: String)

    @Query("SELECT COUNT(*) FROM categories WHERE niche = :niche")
    suspend fun getCategoryCount(niche: String): Int
}
