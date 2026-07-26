package com.example.colorblend.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.colorblend.domain.model.FraseZen
import kotlinx.coroutines.flow.Flow

@Dao
interface FraseZenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrase(frase: FraseZen)

    @Query("SELECT * FROM frases_zen")
    fun getAllFrases(): Flow<List<FraseZen>>

    @Query("SELECT * FROM frases_zen ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomFrase(): FraseZen?

    @Query("DELETE FROM frases_zen WHERE id = :id")
    suspend fun deleteFrase(id: Int)
}
