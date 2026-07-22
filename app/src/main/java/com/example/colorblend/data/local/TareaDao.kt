package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.Tarea
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {
    @Query("SELECT * FROM tareas ORDER BY fecha ASC, hora ASC, minuto ASC")
    fun getAllTareas(): Flow<List<Tarea>>

    @Query("SELECT * FROM tareas WHERE fecha = :fecha OR recurrencia != 'UNA_VEZ' ORDER BY hora ASC, minuto ASC")
    suspend fun getTareasDelDia(fecha: Long): List<Tarea>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarea(tarea: Tarea): Long

    @Update
    suspend fun updateTarea(tarea: Tarea)

    @Delete
    suspend fun deleteTarea(tarea: Tarea)

    @Query("SELECT * FROM tareas WHERE id = :id")
    suspend fun getTareaById(id: Int): Tarea?
}
