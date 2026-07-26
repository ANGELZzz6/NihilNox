package com.example.colorblend.ui.gacha

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.RegistroTarea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetCalendario : AppWidgetProvider() {

    companion object {
        const val ACTION_COMPLETE_CALENDAR_TASK = "com.example.colorblend.ui.gacha.ACTION_COMPLETE_CALENDAR_TASK"

        fun forzarActualizacion(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WidgetCalendario::class.java))
            manager.notifyAppWidgetViewDataChanged(ids, R.id.lvCalendario)
            
            val intent = Intent(context, WidgetCalendario::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            actualizarWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_COMPLETE_CALENDAR_TASK) {
            val taskId = intent.getIntExtra("task_id", -1)
            if (taskId != -1) {
                marcarCompletada(context, taskId)
            }
        }
        super.onReceive(context, intent)
    }

    private fun marcarCompletada(context: Context, taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val tarea = db.tareaDao().getTareaById(taskId)
            tarea?.let {
                val nuevoEstado = !it.completada
                db.tareaDao().updateTarea(it.copy(completada = nuevoEstado))
                
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                }
                
                if (nuevoEstado) {
                    db.registroTareaDao().insertar(RegistroTarea(tareaId = it.id, fechaDia = cal.timeInMillis))
                } else {
                    db.registroTareaDao().eliminarRegistro(it.id, cal.timeInMillis)
                }
                
                forzarActualizacion(context)
                WidgetLifeStream.forzarActualizacion(context)
            }
        }
    }

    private fun actualizarWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_calendario)

        // Configurar ListView
        val serviceIntent = Intent(context, CalendarioRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
        views.setRemoteAdapter(R.id.lvCalendario, serviceIntent)
        views.setEmptyView(R.id.lvCalendario, R.id.tvEmptyCalendario)

        // Template para clics en items de la lista
        val completeIntent = Intent(context, WidgetCalendario::class.java).apply {
            action = ACTION_COMPLETE_CALENDAR_TASK
        }
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getBroadcast(context, 0, completeIntent, flags)
        views.setPendingIntentTemplate(R.id.lvCalendario, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
