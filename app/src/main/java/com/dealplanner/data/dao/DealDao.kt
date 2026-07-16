package com.dealplanner.data.dao

import androidx.room.*
import com.dealplanner.data.model.DealItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DealDao {
    @Query("SELECT * FROM deal_items ORDER BY dealScore DESC")
    fun getAllFlow(): Flow<List<DealItem>>

    @Query("SELECT * FROM deal_items")
    suspend fun getAll(): List<DealItem>

    @Query("SELECT * FROM deal_items WHERE id = :id")
    suspend fun getById(id: Long): DealItem?

    @Query("SELECT * FROM deal_items WHERE confidence < :threshold")
    fun getLowConfidenceFlow(threshold: Double = 0.7): Flow<List<DealItem>>

    @Query("SELECT * FROM deal_items WHERE store = :store ORDER BY dealScore DESC")
    suspend fun getByStore(store: String): List<DealItem>

    @Query("SELECT * FROM deal_items WHERE dealScore >= :minScore ORDER BY dealScore DESC")
    suspend fun getTopDeals(minScore: Double = 0.5): List<DealItem>

    @Query("SELECT * FROM deal_items WHERE name LIKE '%' || :searchTerm || '%'")
    suspend fun search(searchTerm: String): List<DealItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DealItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DealItem>)

    @Update
    suspend fun update(item: DealItem)

    @Delete
    suspend fun delete(item: DealItem)

    @Query("DELETE FROM deal_items")
    suspend fun deleteAll()
}
