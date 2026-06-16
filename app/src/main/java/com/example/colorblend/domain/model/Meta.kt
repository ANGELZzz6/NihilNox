package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metas")
data class Meta(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val titulo: String,
    val descripcion: String?,
    val tipo: TipoMeta,

    val objetivo: Int,
    val progresoActual: Int = 0,

    val rachaActual: Int = 0,
    val mejorRacha: Int = 0,

    val ultimaFecha: Long? = null,

    val activa: Boolean = true,
    val finalizada: Boolean = false,

    // Días de la semana habilitados: null = todos los días
    // 1=Lun, 2=Mar, 3=Mié, 4=Jue, 5=Vie, 6=Sáb, 7=Dom
    val diasSemana: String? = null,

    // Hora del recordatorio diario: "HH:mm" ej "08:30", null = sin recordatorio
    val horaRecordatorio: String? = null
) {
    fun hoyEsDiaHabilitado(): Boolean {
        if (diasSemana.isNullOrEmpty()) return true
        val diasLista = diasSemana.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (diasLista.isEmpty()) return true
        val cal = java.util.Calendar.getInstance()
        val diaCal = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val diaFormato = when (diaCal) {
            java.util.Calendar.MONDAY    -> 1
            java.util.Calendar.TUESDAY   -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY  -> 4
            java.util.Calendar.FRIDAY    -> 5
            java.util.Calendar.SATURDAY  -> 6
            java.util.Calendar.SUNDAY    -> 7
            else -> 1
        }
        return diaFormato in diasLista
    }

    fun diasSemanaTexto(): String {
        if (diasSemana.isNullOrEmpty()) return "Todos los días"
        val diasLista = diasSemana.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (diasLista.size == 7) return "Todos los días"
        val nombres = mapOf(1 to "Lun", 2 to "Mar", 3 to "Mié",
            4 to "Jue", 5 to "Vie", 6 to "Sáb", 7 to "Dom")
        return diasLista.sorted().joinToString(", ") { nombres[it] ?: "" }
    }
}