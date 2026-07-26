package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.PersonajePool

@Dao
interface PersonajePoolDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBatch(personajes: List<PersonajePool>)

    @Query("SELECT COUNT(*) FROM personaje_pool")
    suspend fun getPoolSize(): Int

    @Query("SELECT * FROM personaje_pool ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomCharacters(limit: Int): List<PersonajePool>

    @Query("SELECT * FROM personaje_pool WHERE genero = :genero COLLATE NOCASE ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomCharactersByGender(genero: String, limit: Int): List<PersonajePool>

    @Delete
    suspend fun deleteBatch(personajes: List<PersonajePool>)

    @Query("DELETE FROM personaje_pool WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM personaje_pool WHERE genero = 'Male' COLLATE NOCASE")
    suspend fun getMaleCount(): Int

    @Query("SELECT COUNT(*) FROM personaje_pool WHERE genero = 'Female' COLLATE NOCASE")
    suspend fun getFemaleCount(): Int
}
