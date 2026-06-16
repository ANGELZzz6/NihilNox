package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val monedas: Int = 0,
    val racha: Int = 0,
    val ultimaFechaCumplida: Long = 0L
)