package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "autocontrol_sessions")
data class AutoControlSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fecha: Long = System.currentTimeMillis(),
    val horaConsulta: String,
    val duracionSolicitada: Int,
    val respuestaIA: String,
    val aprobado: Boolean,
    val motivoIA: String
)
