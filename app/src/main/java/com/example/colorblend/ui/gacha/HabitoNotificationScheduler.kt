package com.example.colorblend.ui.gacha

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.colorblend.domain.model.Habito
import java.util.Calendar

object HabitoNotificationScheduler {

    fun programar(context: Context, habito: Habito) {
        if (!habito.notificacionHabilitada) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, HabitoReminderReceiver::class.java).apply {
            putExtra("habito_id", habito.id)
            putExtra("nombre_habito", habito.nombre)
            putExtra("ancla_habito", habito.ancla)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, habito.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, habito.notificacionHora)
            set(Calendar.MINUTE, habito.notificacionMinuto)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelar(context: Context, habitoId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitoReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, habitoId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}