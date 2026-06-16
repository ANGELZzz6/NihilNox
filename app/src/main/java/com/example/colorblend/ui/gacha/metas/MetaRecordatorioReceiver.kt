package com.example.colorblend.ui.gacha.metas

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.colorblend.R

class MetaRecordatorioReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID        = "meta_recordatorios"
        const val EXTRA_META_ID     = "meta_id"
        const val EXTRA_TITULO      = "meta_titulo"
        const val EXTRA_DESCRIPCION = "meta_descripcion"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val metaId      = intent.getIntExtra(EXTRA_META_ID, 0)
        val titulo      = intent.getStringExtra(EXTRA_TITULO) ?: "Meta"
        val descripcion = intent.getStringExtra(EXTRA_DESCRIPCION) ?: ""

        crearCanalSiNecesario(context)

        val textoNotif = if (descripcion.isNotEmpty()) descripcion else "¡Es hora de cumplir tu meta!"

        val abrirIntent = Intent(context, MetasActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("metaIdDestacada", metaId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, metaId, abrirIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // ← icono del sistema
            .setContentTitle("⏰ Recordatorio: $titulo")
            .setContentText(textoNotif)
            .setStyle(NotificationCompat.BigTextStyle().bigText(textoNotif))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(metaId, notif)
    }

    private fun crearCanalSiNecesario(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de metas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de recordatorio para tus metas diarias"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}