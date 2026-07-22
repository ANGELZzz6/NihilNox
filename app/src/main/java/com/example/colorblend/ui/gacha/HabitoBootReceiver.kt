package com.example.colorblend.ui.gacha

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.colorblend.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitoBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    val habitos = db.habitoDao().getTodosUnaVez()
                    habitos.filter { it.enabledBurbuja }.forEach { habito ->
                        HabitoAlarmManager.programarBurbuja(context.applicationContext, habito)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
