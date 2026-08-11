package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genshin_characters")
data class GenshinCharacter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val rareza: Int = 5,           // 4 o 5 estrellas
    val elemento: String,          // Anemo, Geo, Electro, Dendro, Hydro, Pyro, Cryo
    val armaTipo: String,          // Espada, Mandoble, Lanza, Arco, Catalizador
    val nivel: Int = 1,
    val nivelAscension: Int = 0,    // 0-6
    val nivelAmistad: Int = 1,
    val constelacion: Int = 0,      // 0-6
    val talentoAtaque: Int = 1,
    val talentoElemental: Int = 1,
    val talentoDefinitiva: Int = 1,
    val notas: String = "",
    val fechaAgregado: Long = System.currentTimeMillis()
)
