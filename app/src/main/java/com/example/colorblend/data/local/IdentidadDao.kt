package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.Identidad
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentidadDao {
    @Query("SELECT * FROM identidades ORDER BY votosTotal DESC")
    fun getTodas(): Flow<List<Identidad>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(identidad: Identidad)

    @Update
    suspend fun actualizar(identidad: Identidad)

    @Delete
    suspend fun eliminar(identidad: Identidad)

    @Query("UPDATE identidades SET votosTotal = votosTotal + 1 WHERE id = :id")
    suspend fun incrementarVotos(id: Int)

    @Query("SELECT * FROM identidades WHERE id = :id")
    suspend fun getById(id: Int): Identidad?

    @Query("SELECT * FROM identidades")
    suspend fun getTodasUnaVez(): List<Identidad>
}
