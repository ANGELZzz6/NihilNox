package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.RegistroHabito
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroHabitoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(registro: RegistroHabito)

    @Query("""
        SELECT fechaDia FROM registros_habito 
        WHERE habitoId = :habitoId 
        AND fechaDia >= :desde 
        ORDER BY fechaDia ASC
    """)
    suspend fun getRegistrosDesdeFecha(habitoId: Int, desde: Long): List<Long>

    @Query("""
        SELECT fechaDia FROM registros_habito
        WHERE fechaDia >= :desde
        ORDER BY fechaDia ASC
    """)
    fun getRegistrosGlobalesDesdeFecha(desde: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM registros_habito WHERE habitoId = :habitoId")
    suspend fun getTotalRegistros(habitoId: Int): Int

    @Query("SELECT COUNT(DISTINCT habitoId) FROM registros_habito WHERE fechaDia = :fecha")
    suspend fun contarHabitosCompletadosEnFecha(fecha: Long): Int

    @Query("""
        SELECT r.habitoId, h.nombre, r.fechaDia 
        FROM registros_habito r
        JOIN habitos h ON r.habitoId = h.id
        WHERE r.fechaDia >= :desde
    """)
    fun getRegistrosConNombreDesde(desde: Long): Flow<List<RegistroConNombre>>
}

data class RegistroConNombre(
    val habitoId: Int,
    val nombre: String,
    val fechaDia: Long
)
