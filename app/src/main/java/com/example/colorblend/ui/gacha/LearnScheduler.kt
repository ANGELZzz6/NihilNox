package com.example.colorblend.ui.gacha

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object LearnScheduler {

    fun programarRecordatorioDiario(context: Context, horaHH: Int = 19, minutoMM: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, LearnReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 2001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendario = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, horaHH)
            set(java.util.Calendar.MINUTE, minutoMM)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendario.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelarRecordatorio(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, LearnReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 2001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
