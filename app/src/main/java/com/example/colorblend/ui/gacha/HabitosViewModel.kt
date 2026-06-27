package com.example.colorblend.ui.gacha

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.example.colorblend.domain.model.EstadoRacha
import com.example.colorblend.domain.model.Habito
import com.example.colorblend.domain.model.HabitoConEstado
import com.example.colorblend.domain.model.Identidad
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HabitosViewModel(
    application: Application,
    private val repository: HabitosRepository
) : AndroidViewModel(application) {

    init {
        resetearSiCambioElDia()
    }

    fun resetearSiCambioElDia() {
        viewModelScope.launch {
            repository.resetearCompletadosDelDia()
        }
    }

    val habitos: StateFlow<List<HabitoConEstado>> = repository.todosLosHabitos
        .map { lista ->
            lista.map { HabitoConEstado(it, repository.estadoRacha(it)) }
                .sortedWith(compareBy {
                    when (it.estado) {
                        EstadoRacha.EN_RIESGO  -> 0
                        EstadoRacha.PENDIENTE  -> 1
                        EstadoRacha.COMPLETADO -> 2
                        EstadoRacha.ROTA       -> 3
                    }
                })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progresoDiario: StateFlow<Pair<Int, Int>> = repository.todosLosHabitos
        .map { lista -> Pair(lista.count { it.completadoHoy }, lista.size) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0, 0))

    val consistenciaSemanal: StateFlow<List<Float>> = flow {
        while (true) {
            val hoy = obtenerInicioDelDia()
            val habitos = repository.todosLosHabitos.first()
            val porcentajes = (0..6).map { diasAtras ->
                val dia = hoy - (6 - diasAtras) * 86_400_000L
                val completados = repository.contarCompletadosEnFecha(dia)
                if (habitos.isEmpty()) 0f
                else completados.toFloat() / habitos.size
            }
            emit(porcentajes)
            kotlinx.coroutines.delay(60_000) // refrescar cada minuto
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { 0f })

    fun agregarHabito(nombre: String, descripcion: String, ancla: String, identidadId: Int? = null) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            repository.insertar(Habito(
                nombre = nombre.trim(), 
                descripcion = descripcion.trim(),
                ancla = ancla.trim(),
                identidadId = identidadId
            ))
        }
    }

    private val _mensajeRefuerzo = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val mensajeRefuerzo: SharedFlow<String> = _mensajeRefuerzo

    fun marcarCompletado(habito: Habito) {
        viewModelScope.launch {
            repository.marcarCompletado(habito)
            
            habito.identidadId?.let { idIdentidad ->
                val identidad = repository.getIdentidadById(idIdentidad)
                identidad?.let {
                    _mensajeRefuerzo.tryEmit("Soy alguien que ${it.declaracion} ✓")
                }
            }

            // actualizar widget de hábitos
            WidgetHabitos.forzarActualizacion(getApplication())
        }
    }

    fun eliminar(habito: Habito) {
        viewModelScope.launch {
            repository.eliminar(habito)
        }
    }

    fun activarNotificacion(habito: Habito, hora: Int, minuto: Int, context: Context) {
        viewModelScope.launch {
            val actualizado = habito.copy(
                notificacionHabilitada = true,
                notificacionHora = hora,
                notificacionMinuto = minuto
            )
            repository.actualizar(actualizado)
            HabitoNotificationScheduler.programar(context, actualizado)
        }
    }

    fun desactivarNotificacion(habito: Habito, context: Context) {
        viewModelScope.launch {
            repository.actualizar(habito.copy(notificacionHabilitada = false))
            HabitoNotificationScheduler.cancelar(context, habito.id)
        }
    }

    suspend fun contarHabitosCompletadosEnFecha(fecha: Long): Int {
        return repository.contarCompletadosEnFecha(fecha)
    }

    fun obtenerInicioDelDia(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // Identidades
    val identidades: StateFlow<List<Identidad>> = repository.todasLasIdentidades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getIdentidadesUnaVez() = repository.getIdentidadesUnaVez()

    fun agregarIdentidad(declaracion: String) {
        if (declaracion.isBlank()) return
        viewModelScope.launch {
            repository.insertarIdentidad(Identidad(declaracion = declaracion.trim()))
        }
    }

    fun eliminarIdentidad(identidad: Identidad) {
        viewModelScope.launch {
            repository.eliminarIdentidad(identidad)
        }
    }
}

class HabitosViewModelFactory(
    private val application: Application,
    private val repository: HabitosRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitosViewModel::class.java))
            @Suppress("UNCHECKED_CAST") return HabitosViewModel(application, repository) as T
        throw IllegalArgumentException("ViewModel desconocido")
    }
}
