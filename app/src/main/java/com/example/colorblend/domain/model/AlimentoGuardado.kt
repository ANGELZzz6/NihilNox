package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alimento_guardado")
data class AlimentoGuardado(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val calorias: Int,
    val proteina: Float,
    val carbos: Float,
    val grasas: Float,
    val fibra: Float,
    val azucares: Float = 0f,       // ← nuevo
    val unidadPorDefecto: String = "g",
    val vecesUsado: Int = 1
)