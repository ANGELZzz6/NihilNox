package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.MetaImagenDia
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaImagenDao {

    @Query("SELECT * FROM meta_imagenes_dia WHERE metaId = :metaId ORDER BY fecha DESC")
    fun getImagenesPorMeta(metaId: Int): Flow<List<MetaImagenDia>>

    @Query("SELECT * FROM meta_imagenes_dia WHERE metaId = :metaId AND fecha = :fecha")
    suspend fun getImagenesDeDia(metaId: Int, fecha: Long): List<MetaImagenDia>

    @Insert
    suspend fun insert(imagen: MetaImagenDia)

    @Delete
    suspend fun delete(imagen: MetaImagenDia)

    @Query("DELETE FROM meta_imagenes_dia WHERE metaId = :metaId")
    suspend fun deleteByMeta(metaId: Int)
}