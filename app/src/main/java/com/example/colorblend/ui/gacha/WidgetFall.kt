package com.example.colorblend.ui.gacha

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.colorblend.R

class WidgetFall : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_fall)
            // Note: DashboardActivity uses a logic to decide between VideoCategorySelectionActivity and FallActivity.
            // For the widget, we will point to DashboardActivity to maintain that logic or simply FallActivity as a direct entry.
            // Based on instructions, we point to the main activity of the section.
            views.setOnClickPendingIntent(R.id.widgetRaiz, WidgetUtils.buildPendingIntent(context, FallActivity::class.java, id))
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}