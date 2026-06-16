package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.ImagenGenerada
import kotlinx.coroutines.flow.Flow

@Dao
interface ImagenGeneradaDao {

    @Query("SELECT * FROM imagenes_generadas WHERE carpetaId = :carpetaId ORDER BY fechaCreada DESC")
    fun getImagenesPorCarpeta(carpetaId: Int): Flow<List<ImagenGenerada>>

    @Insert
    suspend fun insert(imagen: ImagenGenerada)

    @Delete
    suspend fun delete(imagen: ImagenGenerada)

    @Query("SELECT COUNT(*) FROM imagenes_generadas WHERE carpetaId = :carpetaId")
    suspend fun contarImagenes(carpetaId: Int): Int
}