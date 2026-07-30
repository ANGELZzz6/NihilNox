package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "autocontrol_profile")
data class AutoControlProfile(
    @PrimaryKey val id: Int = 1,
    val frecuenciaActual: String,
    val objetivoPrincipal: String,
    val triggers: String,
    val planIA: String,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val ultimaVez: Long? = null
)
