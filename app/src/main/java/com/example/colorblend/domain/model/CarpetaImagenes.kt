package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "carpetas_imagenes")
data class CarpetaImagenes(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val fechaCreada: Long = System.currentTimeMillis()
)