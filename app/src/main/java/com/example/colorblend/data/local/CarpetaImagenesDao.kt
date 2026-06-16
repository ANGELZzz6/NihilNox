package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.CarpetaImagenes
import kotlinx.coroutines.flow.Flow

@Dao
interface CarpetaImagenesDao {

    @Query("SELECT * FROM carpetas_imagenes ORDER BY fechaCreada DESC")
    fun getCarpetas(): Flow<List<CarpetaImagenes>>

    @Insert
    suspend fun insert(carpeta: CarpetaImagenes): Long

    @Delete
    suspend fun delete(carpeta: CarpetaImagenes)
}