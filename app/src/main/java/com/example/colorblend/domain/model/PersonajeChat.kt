package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personajes_chat")
data class PersonajeChat(
    @PrimaryKey
    val personajeId: Int,
    val fechaAgregado: Long = System.currentTimeMillis()
)