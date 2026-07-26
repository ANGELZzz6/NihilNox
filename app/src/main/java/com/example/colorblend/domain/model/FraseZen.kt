package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "frases_zen")
data class FraseZen(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val texto: String
)
