package com.example.colorblend.ui.gacha

import com.example.colorblend.domain.model.Habito
import com.example.colorblend.domain.model.Tarea

sealed class CalendarItem {
    data class TareaItem(val tarea: Tarea) : CalendarItem()
    data class HabitoItem(val habito: Habito, val completadoEsteDia: Boolean) : CalendarItem()
}
