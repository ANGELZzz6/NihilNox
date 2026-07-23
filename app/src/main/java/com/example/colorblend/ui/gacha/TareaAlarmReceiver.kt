package com.example.colorblend.ui.gacha

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TareaAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tareaId = intent.getIntExtra("tarea_id", 0)
        val titulo = intent.getStringExtra("tarea_titulo") ?: "Tarea pendiente"

        mostrarNotificacion(context, tareaId, titulo)
        
        // Reprogramar si es recurrente
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val tarea = db.tareaDao().getTareaById(tareaId)
            if (tarea != null && tarea.recurrencia != "UNA_VEZ" && tarea.notificacionHabilitada) {
                TareaAlarmScheduler.programar(context, tarea)
            }
        }
    }

    private fun mostrarNotificacion(context: Context, id: Int, titulo: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "tareas_channel_v2" // Cambio de ID para forzar nueva config de canal

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Recordatorios de Tareas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificaciones importantes de tus tareas y recordatorios"
                enableLights(true)
                lightColor = android.graphics.Color.YELLOW
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, CalendarioActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_sparkles)
            .setContentTitle("⏰ Tarea Pendiente")
            .setContentText(titulo)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Máxima prioridad para heads-up
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(android.app.Notification.DEFAULT_ALL)
            .setFullScreenIntent(pendingIntent, true) // Ayuda a que aparezca sobre otras apps
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(id, notification)
    }
}
