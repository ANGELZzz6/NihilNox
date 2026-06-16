package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personajes_obtenidos")
data class PersonajeObtenido(
    @PrimaryKey
    val id: Int,
    val nombre: String,
    val imagenUrl: String,
    val favoritos: Int,
    val rareza: Rareza = Rareza.COMUN,
    val genero: String = "Unknown",
    val categoria: String = "anime", // ← "anime", "superhero", "videojuego"
    val animeId: Int = 0,
    val animeTitulo: String = "Desconocido",
    val animeCoverUrl: String = "",
    val fechaObtenido: Long = System.currentTimeMillis()
)