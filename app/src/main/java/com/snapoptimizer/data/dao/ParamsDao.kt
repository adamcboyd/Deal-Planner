package com.snapoptimizer.data.dao

import androidx.room.*
import com.snapoptimizer.data.model.Params
import kotlinx.coroutines.flow.Flow

@Dao
interface ParamsDao {
    @Query("SELECT * FROM params WHERE id = 1")
    fun getFlow(): Flow<Params?>

    @Query("SELECT * FROM params WHERE id = 1")
    suspend fun get(): Params?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(params: Params)

    @Update
    suspend fun update(params: Params)

    @Query("DELETE FROM params")
    suspend fun deleteAll()
}
