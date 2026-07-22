package com.example.colorblend.ui.gacha

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.colorblend.domain.model.Habito
import java.text.SimpleDateFormat
import java.util.*

object HabitoAlarmManager {
    fun programarBurbuja(context: Context, habito: Habito) {
        cancelarBurbuja(context, habito.id)
        if (!habito.enabledBurbuja) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitoAlarmReceiver::class.java).apply {
            putExtra("habito_id", habito.id)
            putExtra("habito_nombre", habito.nombre)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            habito.id + 10000,
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val now = Calendar.getInstance()
        var calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, habito.notificacionHora)
            set(Calendar.MINUTE, habito.notificacionMinuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -habito.tiempoAnticipacion)
        }

        // Si la hora de hoy ya pasó, empezar a buscar desde mañana
        if (calendar.timeInMillis <= now.timeInMillis) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Buscar el próximo día válido
        var daysAdded = 0
        val diasPermitidos = habito.diasSemana
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }

        // Fallback: si diasPermitidos está vacío, usar todos los días
        val diasEfectivos = if (diasPermitidos.isEmpty()) listOf(1,2,3,4,5,6,7) else diasPermitidos

        val roomToCalendar = mapOf(1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 1)
        val calendarAllowedDays = diasEfectivos.mapNotNull { roomToCalendar[it] }

        while (daysAdded < 8) {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (calendarAllowedDays.contains(dayOfWeek)) break
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            daysAdded++
        }

        // Fallback final: si no encontró día válido en 8 días, programar para mañana
        if (daysAdded >= 8) {
            calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, habito.notificacionHora)
                set(Calendar.MINUTE, habito.notificacionMinuto)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        Log.d("BurbujaScheduler", 
            "Burbuja programada — habitoId:${habito.id} " +
            "nombre:${habito.nombre} " +
            "para:${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(calendar.time)}"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }
    
    fun cancelarBurbuja(context: Context, habitoId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitoAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitoId + 10000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
