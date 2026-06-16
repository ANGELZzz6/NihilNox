package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.Meta
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {

    @Query("SELECT * FROM metas")
    fun getMetas(): Flow<List<Meta>>

    @Query("SELECT * FROM metas WHERE id = :id LIMIT 1")
    suspend fun getMetaById(id: Int): Meta?

    @Query("SELECT COUNT(*) FROM metas WHERE LOWER(titulo) = LOWER(:titulo)")
    suspend fun contarMetasConTitulo(titulo: String): Int

    @Insert
    suspend fun insert(meta: Meta): Long

    @Update
    suspend fun update(meta: Meta)

    @Delete
    suspend fun delete(meta: Meta)
}