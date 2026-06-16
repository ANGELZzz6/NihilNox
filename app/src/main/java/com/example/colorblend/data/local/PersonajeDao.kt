package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.PersonajeObtenido
import kotlinx.coroutines.flow.Flow
import com.example.colorblend.domain.model.AnimeResumen

@Dao
interface PersonajeDao {

    @Query("SELECT * FROM personajes_obtenidos ORDER BY fechaObtenido DESC")
    fun getPersonajes(): Flow<List<PersonajeObtenido>>

    @Query("SELECT * FROM personajes_obtenidos WHERE animeId = :animeId AND animeId != 0")
    fun getPersonajesPorAnime(animeId: Int): Flow<List<PersonajeObtenido>>

    @Query("SELECT * FROM personajes_obtenidos WHERE animeTitulo = :titulo")
    fun getPersonajesPorTitulo(titulo: String): Flow<List<PersonajeObtenido>>

    @Query("SELECT DISTINCT animeId, animeTitulo, animeCoverUrl FROM personajes_obtenidos GROUP BY animeTitulo ORDER BY animeTitulo ASC")
    fun getAnimes(): Flow<List<AnimeResumen>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(personaje: PersonajeObtenido)

    @Query("SELECT COUNT(*) FROM personajes_obtenidos")
    suspend fun contarPersonajes(): Int
}