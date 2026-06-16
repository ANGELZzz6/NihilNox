package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analisis_dia")
data class AnalisisDia(
    @PrimaryKey val fecha: String,
    val resumenTexto: String,
    val analisisIA: String,
    val calorias: Int,
    val proteina: Float,
    val carbos: Float,
    val grasas: Float,
    val fibra: Float,
    val azucares: Float,
    val timestampGenerado: Long = System.currentTimeMillis(),
    val monedasRecompensa: Int = 0,          // cuántas monedas da este día
    val recompensaReclamada: Boolean = false  // si ya las reclamó
)