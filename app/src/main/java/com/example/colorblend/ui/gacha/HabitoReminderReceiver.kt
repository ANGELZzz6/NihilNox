package com.example.colorblend.ui.gacha

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class HabitoReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val nombreHabito = intent.getStringExtra("nombre_habito") ?: "Tu hábito"
        val ancla = intent.getStringExtra("ancla_habito") ?: ""
        val habitoId = intent.getIntExtra("habito_id", -1)

        val textoNotif = if (ancla.isNotBlank())
            "Después de $ancla — es tu momento"
        else
            "Es hora de construir tu racha"

        val channelId = "HABITOS_REMINDER"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Hábitos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones diarias para tus hábitos"
                enableLights(true)
                lightColor = android.graphics.Color.parseColor("#FFD700")
            }
            notificationManager.createNotificationChannel(channel)
        }

        val tapIntent = Intent(context, HabitosActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, habitoId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(nombreHabito)
            .setContentText(textoNotif)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(habitoId, notification)
    }
}