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

class WidgetLifeStream : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            actualizarWidget(context, appWidgetManager, id)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun actualizarWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_lifestream)

        // Configurar el servicio de la lista
        val intent = Intent(context, LifeStreamRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.lvLifeStream, intent)
        views.setEmptyView(R.id.lvLifeStream, R.id.tvWidgetEmpty)

        // Configurar el click genérico (Template)
        val clickIntent = Intent(context, DashboardActivity::class.java)
        val clickPendingIntent = PendingIntent.getActivity(
            context, 0, clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setPendingIntentTemplate(R.id.lvLifeStream, clickPendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.lvLifeStream)
    }

    companion object {
        fun forzarActualizacion(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WidgetLifeStream::class.java))
            manager.notifyAppWidgetViewDataChanged(ids, R.id.lvLifeStream)
            
            val intent = Intent(context, WidgetLifeStream::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
