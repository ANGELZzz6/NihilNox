package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.*

@Dao
interface ProgresionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEjercicio(ejercicio: EjercicioEntity): Long

    @Update
    suspend fun actualizarEjercicio(ejercicio: EjercicioEntity)

    @Query("SELECT * FROM ejercicios WHERE activo = 1 ORDER BY orden ASC")
    suspend fun obtenerEjerciciosActivos(): List<EjercicioEntity>

    @Query("SELECT * FROM ejercicios WHERE id = :id")
    suspend fun obtenerEjercicioPorId(id: Long): EjercicioEntity?

    @Insert
    suspend fun insertarSesion(sesion: SesionEntity): Long

    @Insert
    suspend fun insertarSerie(serie: SerieEntity)

    @Insert
    suspend fun insertarRegistroDiario(registro: RegistroDiarioProgresionEntity)

    @Transaction
    suspend fun insertarSesionConDetalles(
        sesion: SesionEntity,
        series: List<SerieEntity>,
        registro: RegistroDiarioProgresionEntity
    ) {
        val sesionId = insertarSesion(sesion)
        series.forEach {
            insertarSerie(it.copy(sesionId = sesionId))
        }
        insertarRegistroDiario(registro.copy(sesionId = sesionId))
    }

    @Query("SELECT * FROM sesiones WHERE ejercicioId = :ejercicioId ORDER BY fecha DESC")
    suspend fun obtenerSesionesPorEjercicio(ejercicioId: Long): List<SesionEntity>

    @Query("SELECT * FROM series WHERE sesionId = :sesionId ORDER BY numeroSerie ASC")
    suspend fun obtenerSeriesPorSesion(sesionId: Long): List<SerieEntity>

    @Query("SELECT * FROM registro_diario_progresion WHERE sesionId = :sesionId")
    suspend fun obtenerRegistroDiarioPorSesion(sesionId: Long): RegistroDiarioProgresionEntity?

    @Query("SELECT * FROM sesiones WHERE ejercicioId = :ejercicioId ORDER BY fecha DESC LIMIT 1")
    suspend fun obtenerUltimaSesion(ejercicioId: Long): SesionEntity?

    @Query("SELECT * FROM sesiones WHERE fecha BETWEEN :inicio AND :fin ORDER BY fecha DESC")
    suspend fun obtenerSesionesEnRango(inicio: Long, fin: Long): List<SesionEntity>
    
    @Query("SELECT * FROM registro_diario_progresion WHERE sesionId IN (SELECT id FROM sesiones WHERE ejercicioId = :ejercicioId) ORDER BY id DESC LIMIT 2")
    suspend fun obtenerUltimosDosRegistrosDiarios(ejercicioId: Long): List<RegistroDiarioProgresionEntity>

    @Query("SELECT SUM(s.pesoKg * s.reps) FROM series s INNER JOIN sesiones se ON s.sesionId = se.id WHERE se.ejercicioId = :ejercicioId AND se.fecha >= :desdeFecha")
    suspend fun obtenerVolumenPorEjercicioDesde(ejercicioId: Long, desdeFecha: Long): Float?

    @Query("DELETE FROM ejercicios")
    suspend fun eliminarTodosLosEjercicios()

    @Query("DELETE FROM sesiones")
    suspend fun eliminarTodasLasSesiones()

    @Query("DELETE FROM series")
    suspend fun eliminarTodasLasSeries()

    @Query("DELETE FROM registro_diario_progresion")
    suspend fun eliminarTodosLosRegistrosDiarios()

    @Transaction
    suspend fun limpiarTodaLaProgresion() {
        eliminarTodasLasSeries()
        eliminarTodosLosRegistrosDiarios()
        eliminarTodasLasSesiones()
        eliminarTodosLosEjercicios()
    }
}
