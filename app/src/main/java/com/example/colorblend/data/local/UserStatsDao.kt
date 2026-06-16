package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {

    @Query("SELECT * FROM user_stats LIMIT 1")
    fun getStats(): Flow<UserStats>

    @Query("SELECT * FROM user_stats LIMIT 1")
    suspend fun getStatsOnce(): UserStats?

    @Insert
    suspend fun insert(stats: UserStats)

    @Update
    suspend fun update(stats: UserStats)
}