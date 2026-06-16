package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meta_imagenes_dia")
data class MetaImagenDia(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val metaId: Int,
    val fecha: Long,        // inicio del día en millis
    val rutaImagen: String  // path absoluto del archivo guardado
)