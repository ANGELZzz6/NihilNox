package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habitos")
data class Habito(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val descripcion: String = "",
    val ancla: String = "",           // habit stacking: "después de..."
    val fechaCreacion: Long = System.currentTimeMillis(),
    val rachaActual: Int = 0,
    val rachaMaxima: Int = 0,
    val ultimaFechaCompletado: Long? = null,
    val penultimaFechaCompletado: Long? = null,  // para detectar "dos veces seguidas"
    val completadoHoy: Boolean = false,
    val totalCompletados: Int = 0,
    val notificacionHabilitada: Boolean = false,
    val notificacionHora: Int = 8,    // hora en formato 24h (0-23)
    val notificacionMinuto: Int = 0,
    val identidadId: Int? = null
)
