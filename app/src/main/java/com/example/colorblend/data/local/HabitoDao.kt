package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.Habito
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitoDao {
    @Query("SELECT * FROM habitos ORDER BY nombre ASC")
    fun getTodos(): Flow<List<Habito>>

    @Query("SELECT * FROM habitos")
    suspend fun getTodosUnaVez(): List<Habito>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(habito: Habito)

    @Update
    suspend fun actualizar(habito: Habito)

    @Delete
    suspend fun eliminar(habito: Habito)
}