package com.example.colorblend.ui.gacha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.Habito
import com.example.colorblend.domain.model.Tarea
import com.example.colorblend.domain.model.RegistroHabito
import com.example.colorblend.domain.model.RegistroTarea
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TareaViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.tareaDao()
    private val habitoDao = db.habitoDao()
    private val registroDao = db.registroHabitoDao()
    private val registroTareaDao = db.registroTareaDao()

    val todasLasTareas: Flow<List<Tarea>> = dao.getAllTareas()
    val todosLosHabitos: Flow<List<Habito>> = habitoDao.getTodos()

    suspend fun getTareasDelDia(timestamp: Long): List<Tarea> {
        val inicioDia = normalizeToStartOfDay(timestamp)
        return dao.getTareasDelDia(inicioDia)
    }

    suspend fun getIdsHabitosCompletadosEnFecha(fecha: Long): List<Int> {
        return registroDao.getIdsHabitosCompletadosEnFecha(normalizeToStartOfDay(fecha))
    }

    suspend fun insertarTarea(tarea: Tarea): Long {
        return dao.insertTarea(tarea)
    }

    fun actualizarTarea(tarea: Tarea) {
        viewModelScope.launch {
            dao.updateTarea(tarea)
        }
    }

    suspend fun getTareaById(id: Int): Tarea? {
        return dao.getTareaById(id)
    }

    fun marcarCompletada(tarea: Tarea, completada: Boolean) {
        viewModelScope.launch {
            dao.updateTarea(tarea.copy(completada = completada))
            
            // Guardar registro histórico
            val hoy = normalizeToStartOfDay(System.currentTimeMillis())
            if (completada) {
                registroTareaDao.insertar(RegistroTarea(tareaId = tarea.id, fechaDia = hoy))
            }
            
            // Notificar al widget
            WidgetLifeStream.forzarActualizacion(getApplication())
        }
    }

    fun eliminarTarea(tarea: Tarea) {
        viewModelScope.launch {
            dao.deleteTarea(tarea)
            WidgetLifeStream.forzarActualizacion(getApplication())
        }
    }

    fun marcarHabitoCompletado(habito: Habito, timestamp: Long) {
        viewModelScope.launch {
            val inicioDia = normalizeToStartOfDay(timestamp)
            registroDao.insertar(RegistroHabito(habitoId = habito.id, fechaDia = inicioDia))
            
            val hoy = normalizeToStartOfDay(System.currentTimeMillis())
            if (inicioDia == hoy) {
                habitoDao.actualizar(habito.copy(completadoHoy = true, ultimaFechaCompletado = System.currentTimeMillis()))
            }
            
            WidgetLifeStream.forzarActualizacion(getApplication())
        }
    }

    private fun normalizeToStartOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
