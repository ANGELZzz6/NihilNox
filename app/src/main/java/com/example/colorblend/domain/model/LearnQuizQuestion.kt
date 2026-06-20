package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learn_quiz_questions")
data class LearnQuizQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topicId: Int,
    val pregunta: String,
    val opcionA: String,
    val opcionB: String,
    val opcionC: String,
    val opcionD: String,
    val respuestaCorrecta: String,  // "A", "B", "C" o "D"
    val explicacion: String         // Por qué es correcta esa respuesta
)
