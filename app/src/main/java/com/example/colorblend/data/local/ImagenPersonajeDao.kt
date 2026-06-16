package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.ImagenPersonaje

@Dao
interface ImagenPersonajeDao {

    @Query("SELECT * FROM imagenes_personaje WHERE personajeId = :personajeId")
    suspend fun getImagenesPorPersonaje(personajeId: Int): List<ImagenPersonaje>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(imagenes: List<ImagenPersonaje>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(imagen: ImagenPersonaje)

    @Query("SELECT COUNT(*) FROM imagenes_personaje WHERE personajeId = :personajeId")
    suspend fun contarImagenes(personajeId: Int): Int
}