package com.snapoptimizer.data.dao

import androidx.room.*
import com.snapoptimizer.data.model.CouponModifier
import kotlinx.coroutines.flow.Flow

@Dao
interface CouponModifierDao {
    @Query("SELECT * FROM coupon_modifiers")
    fun getAllFlow(): Flow<List<CouponModifier>>

    @Query("SELECT * FROM coupon_modifiers")
    suspend fun getAll(): List<CouponModifier>

    @Query("SELECT * FROM coupon_modifiers WHERE dealItemId = :dealItemId")
    suspend fun getByDealItem(dealItemId: Long): List<CouponModifier>

    @Query("SELECT * FROM coupon_modifiers WHERE applied = 0")
    suspend fun getUnapplied(): List<CouponModifier>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(modifier: CouponModifier): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modifiers: List<CouponModifier>)

    @Update
    suspend fun update(modifier: CouponModifier)

    @Delete
    suspend fun delete(modifier: CouponModifier)

    @Query("DELETE FROM coupon_modifiers")
    suspend fun deleteAll()
}
