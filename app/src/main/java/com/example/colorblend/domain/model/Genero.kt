package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generos")
data class Genero(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String
)
