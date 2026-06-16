package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "imagenes_personaje")
data class ImagenPersonaje(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val personajeId: Int,
    val imageUrl: String,
    val esLocal: Boolean = false  // ← true si la subió el usuario
)