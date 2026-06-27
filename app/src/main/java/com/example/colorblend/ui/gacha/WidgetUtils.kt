package com.example.colorblend.ui.gacha

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object WidgetUtils {
    fun buildPendingIntent(context: Context, activityClass: Class<*>, requestCode: Int): PendingIntent {
        val intent = Intent(context, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}