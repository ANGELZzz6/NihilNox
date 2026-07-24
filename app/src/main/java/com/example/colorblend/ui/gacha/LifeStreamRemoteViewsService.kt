package com.example.colorblend.ui.gacha

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class LifeStreamRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return LifeStreamRemoteViewsFactory(this.applicationContext)
    }
}

class LifeStreamRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var items = mutableListOf<StreamItem>()

    data class StreamItem(val id: Int, val name: String, val history: List<Boolean>, val color: String, val isTarea: Boolean)

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // runBlocking es necesario aquí porque el widget requiere retorno inmediato de datos
        runBlocking {
            try {
                val db = AppDatabase.getDatabase(context)
                val habitos = db.habitoDao().getTodosUnaVez()
                val tareas = db.tareaDao().getTareasDelDia(0)

                val newItems = mutableListOf<StreamItem>()
                
                habitos.forEach { h ->
                    newItems.add(StreamItem(h.id, h.nombre, getHistoryHabito(db, h.id), h.burbujaColor, false))
                }
                
                tareas.filter { it.recurrencia != "UNA_VEZ" }.forEach { t ->
                    newItems.add(StreamItem(t.id, t.titulo, getHistoryTarea(db, t.id), t.color, true))
                }
                
                items = newItems
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun getHistoryHabito(db: AppDatabase, id: Int): List<Boolean> {
        val history = mutableListOf<Boolean>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val hoy = cal.timeInMillis
        val desde = hoy - (9 * 86400000L) // 10 días para el widget
        
        val registros = db.registroHabitoDao().getRegistrosDesdeFecha(id, desde)
        for (i in 0..9) {
            history.add(registros.contains(desde + (i * 86400000L)))
        }
        return history
    }

    private suspend fun getHistoryTarea(db: AppDatabase, id: Int): List<Boolean> {
        val history = mutableListOf<Boolean>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val hoy = cal.timeInMillis
        val desde = hoy - (9 * 86400000L)
        
        val registros = db.registroTareaDao().getRegistrosDesde(id, desde)
        for (i in 0..9) {
            history.add(registros.contains(desde + (i * 86400000L)))
        }
        return history
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.item_widget_lifestream)
        val item = items[position]

        val icon = if (item.isTarea) "📌" else "🔥"
        views.setTextViewText(R.id.tvStreamName, "$icon ${item.name}")
        
        // Generar la onda
        val bitmap = WaveGenerator.generateWaveBitmap(400, 100, item.history, item.color)
        views.setImageViewBitmap(R.id.ivStreamWave, bitmap)

        // Configurar el click para abrir la app
        val fillInIntent = Intent().apply {
            putExtra("item_id", item.id)
            putExtra("is_tarea", item.isTarea)
        }
        views.setOnClickFillInIntent(R.id.itemRaiz, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
