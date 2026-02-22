package com.example.lentespro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessengerDao {

    @Query("SELECT * FROM messengers ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<MessengerEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(m: MessengerEntity): Long

    @Delete
    suspend fun delete(m: MessengerEntity)
}
