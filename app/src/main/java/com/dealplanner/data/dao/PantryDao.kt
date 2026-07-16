package com.dealplanner.data.dao

import androidx.room.*
import com.dealplanner.data.model.PantryItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY dateAdded DESC")
    fun getAllFlow(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items")
    suspend fun getAll(): List<PantryItem>

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    suspend fun getById(id: Long): PantryItem?

    @Query("SELECT * FROM pantry_items WHERE needsVerify = 1")
    fun getNeedsVerifyFlow(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE location = :location")
    suspend fun getByLocation(location: String): List<PantryItem>

    @Query("SELECT * FROM pantry_items WHERE bestBy IS NOT NULL ORDER BY bestBy ASC")
    suspend fun getByBestByDate(): List<PantryItem>

    @Query("SELECT * FROM pantry_items WHERE item LIKE '%' || :searchTerm || '%'")
    suspend fun search(searchTerm: String): List<PantryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PantryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PantryItem>)

    @Update
    suspend fun update(item: PantryItem)

    @Delete
    suspend fun delete(item: PantryItem)

    @Query("DELETE FROM pantry_items")
    suspend fun deleteAll()

    @Query("UPDATE pantry_items SET qty = qty - :amount WHERE id = :id")
    suspend fun decrementQty(id: Long, amount: Double)
}
