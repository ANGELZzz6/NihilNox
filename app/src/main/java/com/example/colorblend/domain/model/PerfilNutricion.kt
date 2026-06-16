package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "perfil_nutricion")
data class PerfilNutricion(
    @PrimaryKey val id: Int = 1, // Solo hay un perfil
    val peso: Float,             // kg
    val altura: Int,             // cm
    val edad: Int,
    val sexo: String,            // "Hombre" / "Mujer"
    val objetivo: String,        // "Ganar músculo" / "Bajar peso" / "Mantenimiento" / "Definición"
    val nivelActividad: String,  // "Sedentario" / "Ligero" / "Moderado" / "Activo" / "Muy activo"
    val metaCalorias: Int,       // calculado automáticamente
    val metaProteina: Int,       // gramos
    val metaCarbos: Int,         // gramos
    val metaGrasas: Int,         // gramos
    val metaFibra: Int           // gramos
)