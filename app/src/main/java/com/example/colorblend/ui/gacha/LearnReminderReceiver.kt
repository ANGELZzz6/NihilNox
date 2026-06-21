package com.example.colorblend.ui.gacha

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class LearnReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "LEARN_REMINDER"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Recordatorios de Academia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Te recuerda repasar tus temas diarios"
                enableLights(true)
                lightColor = android.graphics.Color.parseColor("#FFD700")
            }
            manager.createNotificationChannel(channel)
        }

        val intentAbrir = Intent(context, LearnActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intentAbrir,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mensajes = listOf(
            "🧠 ¿Listo para repasar? Tus tarjetas te esperan",
            "📚 5 minutos de estudio hoy valen más que 1 hora mañana",
            "🔥 No rompas tu racha, es hora de estudiar",
            "⚡ Tu cerebro está listo, ¿y tú?"
        )

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Academia — Repaso diario")
            .setContentText(mensajes.random())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(2001, notif)
    }
}
