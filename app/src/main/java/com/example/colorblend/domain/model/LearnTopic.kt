package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learn_topics")
data class LearnTopic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val descripcion: String,        // Resumen generado por IA
    val categoria: String,          // "libre", "idiomas", "ciencia", etc.
    val materialUsuario: String?,   // Texto que el usuario aportó (opcional)
    val fechaCreacion: Long = System.currentTimeMillis(),
    val ultimaRepaso: Long = 0L,
    val rachaEstudio: Int = 0,
    val dominioTotal: Float = 0f,   // 0.0 a 1.0
    val totalSesiones: Int = 0,
    val activo: Boolean = true
)
