package com.example.colorblend.data.local

import androidx.room.TypeConverter
import com.example.colorblend.domain.model.Rareza
import com.example.colorblend.domain.model.TipoMeta

class Converters {

    @TypeConverter
    fun fromTipoMeta(value: TipoMeta): String = value.name

    @TypeConverter
    fun toTipoMeta(value: String): TipoMeta = TipoMeta.valueOf(value)

    @TypeConverter
    fun fromRareza(value: Rareza): String = value.name

    @TypeConverter
    fun toRareza(value: String): Rareza = Rareza.valueOf(value)
}