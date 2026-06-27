package com.example.colorblend.ui.gacha

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.colorblend.R

class WidgetPerfil : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_perfil)
            views.setOnClickPendingIntent(R.id.widgetRaiz, WidgetUtils.buildPendingIntent(context, PerfilActivity::class.java, id))
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}