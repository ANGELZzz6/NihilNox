package com.example.colorblend.ui.gacha

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.Habito
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object HabitoAlarmManager {
    
    private const val ACTION_HABITO_ALARM = "com.example.colorblend.HABITO_ALARM"

    fun programarBurbuja(context: Context, habito: Habito) {
        cancelarBurbuja(context, habito.id)
        if (!habito.enabledBurbuja) return

        val now = Calendar.getInstance()
        val hoyTimestamp = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context.applicationContext)
            val completados = db.registroHabitoDao().getIdsHabitosCompletadosEnFecha(hoyTimestamp)
            
            withContext(Dispatchers.Main) {
                if (completados.contains(habito.id)) {
                    val nextDay = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    realizarProgramacion(context, habito, nextDay)
                } else {
                    realizarProgramacion(context, habito, now)
                }
            }
        }
    }

    private fun realizarProgramacion(context: Context, habito: Habito, startingFrom: Calendar) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitoAlarmReceiver::class.java).apply {
            action = ACTION_HABITO_ALARM
            putExtra("habito_id", habito.id)
            putExtra("habito_nombre", habito.nombre)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            habito.id + 10000,
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val now = Calendar.getInstance()
        val habitTime = Calendar.getInstance().apply {
            timeInMillis = startingFrom.timeInMillis
            set(Calendar.HOUR_OF_DAY, habito.notificacionHora)
            set(Calendar.MINUTE, habito.notificacionMinuto)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val bubbleTime = (habitTime.clone() as Calendar).apply {
            add(Calendar.MINUTE, -habito.tiempoAnticipacion)
        }

        // Obtener días permitidos
        val diasPermitidos = habito.diasSemana.split(",").mapNotNull { it.trim().toIntOrNull() }
        val diasEfectivos = if (diasPermitidos.isEmpty()) listOf(1,2,3,4,5,6,7) else diasPermitidos
        val roomToCalendar = mapOf(1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 1)
        val calendarAllowedDays = diasEfectivos.mapNotNull { roomToCalendar[it] }

        // LÓGICA DE DISPARO INMEDIATO (CATCH-UP)
        if (startingFrom.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
            now.after(bubbleTime) && now.before(habitTime)) {
            
            Log.d("BurbujaScheduler", "Catch-up: Disparando '${habito.nombre}' ahora.")
            
            // Programar primero la siguiente ocurrencia futura
            val nextCycle = (habitTime.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            while (!calendarAllowedDays.contains(nextCycle.get(Calendar.DAY_OF_WEEK))) {
                nextCycle.add(Calendar.DAY_OF_MONTH, 1)
            }
            setAlarm(alarmManager, nextCycle.timeInMillis, pendingIntent)

            // Disparar la actual
            try { pendingIntent.send() } catch (e: Exception) {}
            return
        }

        // Ajustar bubbleTime al próximo día válido
        if (bubbleTime.before(now)) {
            bubbleTime.add(Calendar.DAY_OF_MONTH, 1)
        }
        while (!calendarAllowedDays.contains(bubbleTime.get(Calendar.DAY_OF_WEEK))) {
            bubbleTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        Log.d("BurbujaScheduler", "Programada '${habito.nombre}' para ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(bubbleTime.time)}")
        setAlarm(alarmManager, bubbleTime.timeInMillis, pendingIntent)
    }

    fun reprogramarParaMasTarde(context: Context, habitoId: Int) {
        if (habitoId == -99) return
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context.applicationContext)
            val habito = db.habitoDao().getById(habitoId) ?: return@launch
            val hoyTimestamp = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            if (db.registroHabitoDao().getIdsHabitosCompletadosEnFecha(hoyTimestamp).contains(habitoId)) return@launch

            val proximo = Calendar.getInstance().apply { add(Calendar.MINUTE, 60) }
            if (proximo.get(Calendar.DAY_OF_YEAR) != Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) return@launch

            withContext(Dispatchers.Main) {
                val intent = Intent(context, HabitoAlarmReceiver::class.java).apply {
                    action = ACTION_HABITO_ALARM
                    putExtra("habito_id", habito.id)
                    putExtra("habito_nombre", habito.nombre)
                }
                val pendingIntent = PendingIntent.getBroadcast(context, habito.id + 10000, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                setAlarm(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager, proximo.timeInMillis, pendingIntent)
            }
        }
    }

    private fun setAlarm(alarmManager: AlarmManager, timeInMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        } else alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
    }
    
    fun cancelarBurbuja(context: Context, habitoId: Int) {
        val intent = Intent(context, HabitoAlarmReceiver::class.java).apply { action = ACTION_HABITO_ALARM }
        val pendingIntent = PendingIntent.getBroadcast(context, habitoId + 10000, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
    }
}
