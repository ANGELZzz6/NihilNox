package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tareas")
data class Tarea(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val descripcion: String = "",
    val fecha: Long, // Timestamp del día (medianoche)
    val hora: Int = 0,
    val minuto: Int = 0,
    val notificacionHabilitada: Boolean = false,
    val recurrencia: String = "UNA_VEZ", // UNA_VEZ, DIARIO, SEMANAL, DIAS_SELECCIONADOS
    val diasSemana: String = "", // Ej: "1,3,5"
    val color: String = "#FFD700", // Gold por defecto
    val completada: Boolean = false
)
