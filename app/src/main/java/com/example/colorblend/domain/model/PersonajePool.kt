package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personaje_pool")
data class PersonajePool(
    @PrimaryKey
    val id: Int,
    val nombre: String,
    val imagenUrl: String,
    val favoritos: Int,
    val rareza: Rareza = Rareza.COMUN,
    val genero: String = "Unknown",
    val categoria: String = "anime", 
    val animeId: Int = 0,
    val animeTitulo: String = "Desconocido",
    val animeCoverUrl: String = "",
    val fechaAgregado: Long = System.currentTimeMillis()
)
