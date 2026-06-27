package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registros_habito",
    foreignKeys = [ForeignKey(
        entity = Habito::class,
        parentColumns = ["id"],
        childColumns = ["habitoId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitoId")]
)
data class RegistroHabito(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitoId: Int,
    val fechaDia: Long  // timestamp del inicio del día (00:00:00)
)
