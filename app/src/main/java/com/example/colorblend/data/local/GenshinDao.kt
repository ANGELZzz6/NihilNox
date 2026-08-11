package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.GenshinCharacter
import kotlinx.coroutines.flow.Flow

@Dao
interface GenshinDao {
    @Query("SELECT * FROM genshin_characters ORDER BY rareza DESC, nivel DESC, nombre ASC")
    fun getAllCharacters(): Flow<List<GenshinCharacter>>

    @Query("SELECT * FROM genshin_characters WHERE id = :id")
    suspend fun getById(id: Int): GenshinCharacter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(character: GenshinCharacter)

    @Update
    suspend fun update(character: GenshinCharacter)

    @Delete
    suspend fun delete(character: GenshinCharacter)
}
