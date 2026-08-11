package com.example.colorblend.ui.gacha

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.Tarea
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class CalendarioRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CalendarioRemoteViewsFactory(this.applicationContext)
    }
}

class CalendarioRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var tasks = mutableListOf<Tarea>()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            try {
                val db = AppDatabase.getDatabase(context)
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val hoy = cal.timeInMillis
                
                // Obtener todas las tareas y filtrar para hoy
                val allFromDb = db.tareaDao().getTareasDelDia(hoy)
                val filteredToday = allFromDb.filter { esTareaParaElDia(it, cal) }

                // Cruce con registros_tarea para determinar el estado REAL de hoy
                val tasksWithTodayStatus = filteredToday.map { tarea ->
                    val completadaHoy = db.registroTareaDao().esCompletadaEnFecha(tarea.id, hoy) > 0
                    tarea.copy(completada = completadaHoy)
                }

                // Priorizar pendientes, luego completadas. Ordenadas por hora.
                tasks = tasksWithTodayStatus.sortedWith(compareBy<Tarea> { it.completada }.thenBy { it.hora * 60 + it.minuto })
                    .toMutableList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun esTareaParaElDia(tarea: Tarea, cal: Calendar): Boolean {
        val calTarea = Calendar.getInstance().apply { timeInMillis = tarea.fecha }
        
        // Mismo día exacto
        if (esMismoDia(calTarea, cal)) return true
        
        // Si la fecha de la tarea es en el futuro y no es hoy, descartar
        if (cal.timeInMillis < tarea.fecha && !esMismoDia(calTarea, cal)) return false

        return when (tarea.recurrencia) {
            "DIARIO" -> true
            "SEMANAL" -> cal.get(Calendar.DAY_OF_WEEK) == calTarea.get(Calendar.DAY_OF_WEEK)
            "DIAS_SELECCIONADOS" -> {
                val diasValidos = tarea.diasSemana.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
                diasValidos.contains(cal.get(Calendar.DAY_OF_WEEK))
            }
            else -> false
        }
    }

    private fun esMismoDia(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onDestroy() {
        tasks.clear()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= tasks.size) {
            return RemoteViews(context.packageName, R.layout.item_widget_calendario)
        }
        
        val t = tasks[position]
        val views = RemoteViews(context.packageName, R.layout.item_widget_calendario)

        views.setTextViewText(R.id.tvTaskTitle, t.titulo)
        views.setTextViewText(R.id.tvTaskTime, String.format("%02d:%02d", t.hora, t.minuto))
        
        try {
            val color = Color.parseColor(t.color)
            views.setInt(R.id.vTaskColor, "setColorFilter", color)
        } catch (e: Exception) {
            views.setInt(R.id.vTaskColor, "setColorFilter", Color.parseColor("#FFD700"))
        }

        // Checkbox interactivo
        if (t.completada) {
            views.setImageViewResource(R.id.btnCompleteTask, android.R.drawable.checkbox_on_background)
        } else {
            views.setImageViewResource(R.id.btnCompleteTask, android.R.drawable.checkbox_off_background)
        }

        // Fill-in Intent para la acción de completar (se asocia al template del ListView)
        val fillInIntent = Intent().apply {
            putExtra("task_id", t.id)
        }
        views.setOnClickFillInIntent(R.id.btnCompleteTask, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = tasks.getOrNull(position)?.id?.toLong() ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
