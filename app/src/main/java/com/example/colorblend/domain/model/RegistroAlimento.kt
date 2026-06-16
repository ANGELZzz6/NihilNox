package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registro_alimento")
data class RegistroAlimento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fecha: String,
    val categoria: String,
    val nombre: String,
    val cantidad: Float,
    val unidad: String,
    val calorias: Int,
    val proteina: Float,
    val carbos: Float,
    val grasas: Float,
    val fibra: Float,
    val azucares: Float = 0f,      // ← nuevo
    val fueEscaneado: Boolean = false
)