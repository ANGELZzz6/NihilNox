package com.example.colorblend.ui.gacha

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import android.content.Intent
import android.app.PendingIntent
import com.example.colorblend.R

class WidgetNotas : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_notas)
            
            val intent = Intent(context, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("abrir_notas_con_seguridad", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            views.setOnClickPendingIntent(R.id.widgetRaiz, pendingIntent)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}