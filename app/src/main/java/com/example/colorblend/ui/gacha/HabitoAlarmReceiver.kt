package com.example.colorblend.ui.gacha

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.colorblend.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val habitoId = intent.getIntExtra("habito_id", -1)
        val habitoNombre = intent.getStringExtra("habito_nombre") ?: "Hábito"
        
        // Iniciar el servicio de la burbuja
        val serviceIntent = Intent(context, BurbujaHabitoService::class.java).apply {
            putExtra("habito_id", habitoId)
            putExtra("habito_nombre", habitoNombre)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // REPROGRAMAR automáticamente para la próxima vez (esto evita que se detenga si no se hace)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val habito = db.habitoDao().getById(habitoId)
                habito?.let {
                    HabitoAlarmManager.programarBurbuja(context.applicationContext, it)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
