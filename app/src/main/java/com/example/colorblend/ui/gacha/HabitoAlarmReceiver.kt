package com.example.colorblend.ui.gacha

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.colorblend.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HabitoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val habitoId = intent.getIntExtra("habito_id", -1)
        val habitoNombre = intent.getStringExtra("habito_nombre") ?: "Hábito"
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (habitoId != -99) {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    val cal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val completados = db.registroHabitoDao().getIdsHabitosCompletadosEnFecha(cal.timeInMillis)
                    
                    if (completados.contains(habitoId)) {
                        // Ya se completó, reprogramar para mañana y salir
                        val habito = db.habitoDao().getById(habitoId)
                        habito?.let { HabitoAlarmManager.programarBurbuja(context.applicationContext, it) }
                        return@launch
                    }
                }

                // Si llegamos aquí, o es de prueba o no se ha completado
                withContext(Dispatchers.Main) {
                    val serviceIntent = Intent(context, BurbujaHabitoService::class.java).apply {
                        putExtra("habito_id", habitoId)
                        putExtra("habito_nombre", habitoNombre)
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }

                // Reprogramar para mañana si es un lanzamiento normal
                if (habitoId != -99) {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    val habito = db.habitoDao().getById(habitoId)
                    habito?.let {
                        HabitoAlarmManager.programarBurbuja(context.applicationContext, it)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
