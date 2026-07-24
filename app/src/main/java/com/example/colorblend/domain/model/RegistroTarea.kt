package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registros_tarea",
    foreignKeys = [ForeignKey(
        entity = Tarea::class,
        parentColumns = ["id"],
        childColumns = ["tareaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tareaId")]
)
data class RegistroTarea(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tareaId: Int,
    val fechaDia: Long  // timestamp del inicio del día (00:00:00)
)
