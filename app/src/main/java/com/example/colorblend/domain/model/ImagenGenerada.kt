package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "imagenes_generadas")
data class ImagenGenerada(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val carpetaId: Int,
    val rutaLocal: String,
    val prompt: String = "",
    val fechaCreada: Long = System.currentTimeMillis()
)