package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identidades")
data class Identidad(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val declaracion: String,           // "Soy alguien que se mueve cada día"
    val fechaCreacion: Long = System.currentTimeMillis(),
    val votosTotal: Int = 0            // número de hábitos completados vinculados
)
