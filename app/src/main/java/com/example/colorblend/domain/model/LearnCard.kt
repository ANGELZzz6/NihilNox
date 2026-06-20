package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learn_cards")
data class LearnCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topicId: Int,               // FK a LearnTopic
    val frente: String,             // Pregunta o concepto
    val reverso: String,            // Respuesta o explicación
    val ejemplo: String?,           // Ejemplo opcional
    // Spaced repetition (algoritmo SM-2)
    val intervalo: Int = 1,         // Días hasta próximo repaso
    val facilidad: Float = 2.5f,    // Factor de facilidad (2.5 = normal)
    val repeticiones: Int = 0,      // Veces repasada correctamente seguidas
    val proximoRepaso: Long = System.currentTimeMillis(),
    val ultimaCalificacion: Int = 0 // 0=sin repasar, 1=mal, 2=bien, 3=perfecto
)
