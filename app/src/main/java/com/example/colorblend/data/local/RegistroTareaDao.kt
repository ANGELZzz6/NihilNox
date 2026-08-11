package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.RegistroTarea
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroTareaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(registro: RegistroTarea)

    @Query("SELECT fechaDia FROM registros_tarea WHERE tareaId = :tareaId AND fechaDia >= :desde ORDER BY fechaDia ASC")
    suspend fun getRegistrosDesde(tareaId: Int, desde: Long): List<Long>

    @Query("SELECT COUNT(*) FROM registros_tarea WHERE tareaId = :tareaId AND fechaDia = :fecha")
    suspend fun esCompletadaEnFecha(tareaId: Int, fecha: Long): Int

    @Query("SELECT tareaId FROM registros_tarea WHERE fechaDia = :fecha")
    suspend fun getIdsCompletadosEnFecha(fecha: Long): List<Int>

    @Query("DELETE FROM registros_tarea WHERE tareaId = :tareaId AND fechaDia = :fecha")
    suspend fun eliminarRegistro(tareaId: Int, fecha: Long)
}
