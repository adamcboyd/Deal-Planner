package com.dealplanner.data.dao

import androidx.room.*
import com.dealplanner.data.model.ReceiptItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipt_items ORDER BY date DESC")
    fun getAllFlow(): Flow<List<ReceiptItem>>

    @Query("SELECT * FROM receipt_items")
    suspend fun getAll(): List<ReceiptItem>

    @Query("SELECT * FROM receipt_items WHERE id = :id")
    suspend fun getById(id: Long): ReceiptItem?

    @Query("SELECT * FROM receipt_items WHERE needsReview = 1")
    fun getNeedsReviewFlow(): Flow<List<ReceiptItem>>

    @Query("SELECT * FROM receipt_items WHERE date = :date")
    suspend fun getByDate(date: LocalDate): List<ReceiptItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReceiptItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReceiptItem>)

    @Update
    suspend fun update(item: ReceiptItem)

    @Delete
    suspend fun delete(item: ReceiptItem)

    @Query("DELETE FROM receipt_items")
    suspend fun deleteAll()
}
