package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ejercicios")
data class EjercicioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val esEjercicioPrincipal: Boolean,   // true = primero de la tabla, fallo real permitido
    val rangoRepsMin: Int,               // piso del rango
    val rangoRepsMax: Int,               // techo del rango
    val pesoActualKg: Float,
    val orden: Int,
    val activo: Boolean = true,
    val esIsometrico: Boolean = false,
    val descansoSegundos: Int? = null,
    val tempo: String? = null,
    val requiereCalentamientoEspecifico: Boolean = false,
    val protocoloCalentamiento: String? = null,
    val notasTendon: String? = null,
    val seriesPredeterminadas: Int = 3
)

@Entity(tableName = "sesiones")
data class SesionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ejercicioId: Long,
    val fecha: Long,                     // epoch millis
    val esDescarga: Boolean = false
)

@Entity(tableName = "series")
data class SerieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sesionId: Long,
    val numeroSerie: Int,                // 1,2,3,4...
    val pesoKg: Float,
    val reps: Int,
    val rir: Int?                        // solo relevante en la última serie
)

@Entity(tableName = "registro_diario_progresion")
data class RegistroDiarioProgresionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sesionId: Long,
    val molestiaArticular: Int,          // 0-10
    val notas: String = ""
)
