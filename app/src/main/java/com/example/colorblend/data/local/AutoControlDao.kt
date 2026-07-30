package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.AutoControlProfile
import com.example.colorblend.domain.model.AutoControlSession
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoControlDao {
    @Query("SELECT * FROM autocontrol_profile WHERE id = 1")
    fun getProfile(): Flow<AutoControlProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AutoControlProfile)

    @Query("SELECT * FROM autocontrol_sessions ORDER BY fecha DESC")
    fun getAllSessions(): Flow<List<AutoControlSession>>

    @Insert
    suspend fun insertSession(session: AutoControlSession)

    @Query("DELETE FROM autocontrol_sessions")
    suspend fun clearHistory()
}
