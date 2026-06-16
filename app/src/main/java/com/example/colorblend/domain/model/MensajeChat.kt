package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mensajes_chat")
data class MensajeChat(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val personajeId: Int,
    val contenido: String,
    val esUsuario: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)