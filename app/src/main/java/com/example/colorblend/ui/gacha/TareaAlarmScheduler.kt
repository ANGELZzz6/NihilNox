package com.example.colorblend.ui.gacha

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.colorblend.domain.model.Tarea
import java.util.*

object TareaAlarmScheduler {

    fun programar(context: Context, tarea: Tarea) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TareaAlarmReceiver::class.java).apply {
            putExtra("tarea_id", tarea.id)
            putExtra("tarea_titulo", tarea.titulo)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            tarea.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = tarea.fecha
            set(Calendar.HOUR_OF_DAY, tarea.hora)
            set(Calendar.MINUTE, tarea.minuto)
            set(Calendar.SECOND, 0)
        }

        // Si la hora ya pasó, y es recurrente, buscar la próxima ocurrencia
        if (calendar.timeInMillis <= System.currentTimeMillis() && tarea.recurrencia != "UNA_VEZ") {
            actualizarAlProximoDiaValido(calendar, tarea)
        }

        if (calendar.timeInMillis > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun actualizarAlProximoDiaValido(cal: Calendar, tarea: Tarea) {
        if (tarea.recurrencia == "DIARIO") {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        } else if (tarea.recurrencia == "DIAS_SELECCIONADOS") {
            val dias = tarea.diasSemana.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
            if (dias.isNotEmpty()) {
                // Avanzar día por día hasta encontrar uno en la lista
                var intentos = 0
                while (intentos < 8) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    if (dias.contains(cal.get(Calendar.DAY_OF_WEEK))) break
                    intentos++
                }
            }
        }
    }
}
