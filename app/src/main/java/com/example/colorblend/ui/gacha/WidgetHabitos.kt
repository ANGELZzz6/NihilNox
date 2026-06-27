package com.example.colorblend.ui.gacha

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WidgetHabitos : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            actualizarWidget(context, appWidgetManager, id)
        }
    }

    private fun actualizarWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_habitos)

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val habitos = db.habitoDao().getTodosUnaVez()
                val completadosHoy = habitos.count { it.completadoHoy }
                val total = habitos.size

                val progresoTexto = "$completadosHoy/$total hoy"
                val colorProgreso = when {
                    total == 0 -> "#777777"
                    completadosHoy == total -> "#FFD700"
                    completadosHoy > 0 -> "#4CAF50"
                    else -> "#777777"
                }

                views.setTextViewText(R.id.widgetProgreso, progresoTexto)
                views.setTextColor(R.id.widgetProgreso, Color.parseColor(colorProgreso))
            } catch (e: Exception) {
                views.setTextViewText(R.id.widgetProgreso, "Abre la app")
            }

            views.setOnClickPendingIntent(
                R.id.widgetRaiz,
                WidgetUtils.buildPendingIntent(context, HabitosActivity::class.java, widgetId)
            )

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    companion object {
        fun forzarActualizacion(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WidgetHabitos::class.java))
            val intent = Intent(context, WidgetHabitos::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}