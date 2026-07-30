package com.example.colorblend.ui.gacha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.AutoControlRepository
import com.example.colorblend.domain.model.AutoControlProfile
import com.example.colorblend.domain.model.AutoControlSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class AutoControlState {
    object Idle : AutoControlState()
    object Cargando : AutoControlState()
    data class Exito(val mensaje: String) : AutoControlState()
    data class Error(val mensaje: String) : AutoControlState()
}

class AutoControlViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AutoControlRepository(
        application,
        AppDatabase.getDatabase(application).autoControlDao()
    )

    private val _perfil = MutableStateFlow<AutoControlProfile?>(null)
    val perfil: StateFlow<AutoControlProfile?> = _perfil

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    private val _estado = MutableStateFlow<AutoControlState>(AutoControlState.Idle)
    val estado: StateFlow<AutoControlState> = _estado

    private val _consultaResultado = MutableSharedFlow<Triple<Boolean, String, String>>(replay = 1)
    val consultaResultado = _consultaResultado

    private val _sesiones = MutableStateFlow<List<AutoControlSession>>(emptyList())
    val sesiones: StateFlow<List<AutoControlSession>> = _sesiones

    init {
        observarPerfil()
        observarSesiones()
    }

    private fun observarPerfil() = viewModelScope.launch {
        repo.perfil.collectLatest { 
            _perfil.value = it 
            _isLoaded.value = true
        }
    }

    private fun observarSesiones() = viewModelScope.launch {
        repo.sesiones.collectLatest { _sesiones.value = it }
    }

    fun generarPlan(frecuencia: String, objetivo: String, triggers: String) = viewModelScope.launch {
        _estado.value = AutoControlState.Cargando
        repo.generarPlanIA(frecuencia, objetivo, triggers)
            .onSuccess { plan ->
                val nuevoPerfil = AutoControlProfile(
                    frecuenciaActual = frecuencia,
                    objetivoPrincipal = objetivo,
                    triggers = triggers,
                    planIA = plan
                )
                repo.guardarPerfil(nuevoPerfil)
                _estado.value = AutoControlState.Exito("Plan generado correctamente")
            }
            .onFailure {
                _estado.value = AutoControlState.Error(it.message ?: "Error desconocido")
            }
    }

    fun consultarIA(duracion: Int) = viewModelScope.launch {
        val p = _perfil.value ?: return@launch
        _estado.value = AutoControlState.Cargando
        
        val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val historial = _sesiones.value
        
        repo.consultarIA(p, horaActual, duracion, historial)
            .onSuccess { (aprobado, motivo, mensaje) ->
                val sesion = AutoControlSession(
                    horaConsulta = horaActual,
                    duracionSolicitada = duracion,
                    respuestaIA = mensaje,
                    aprobado = aprobado,
                    motivoIA = motivo
                )
                repo.guardarSesion(sesion)
                _consultaResultado.emit(Triple(aprobado, motivo, mensaje))
                _estado.value = AutoControlState.Exito("Consulta finalizada")
            }
            .onFailure {
                _estado.value = AutoControlState.Error(it.message ?: "Error en la consulta")
            }
    }

    private val _preguntaRespuesta = MutableSharedFlow<String>(replay = 1)
    val preguntaRespuesta = _preguntaRespuesta

    fun preguntarIA(pregunta: String) = viewModelScope.launch {
        val p = _perfil.value ?: return@launch
        _estado.value = AutoControlState.Cargando
        
        repo.preguntarIA(p, pregunta, _sesiones.value)
            .onSuccess { respuesta ->
                _preguntaRespuesta.emit(respuesta)
                _estado.value = AutoControlState.Exito("Respuesta recibida")
            }
            .onFailure {
                _estado.value = AutoControlState.Error(it.message ?: "Error al preguntar")
            }
    }

    fun reiniciarContador() = viewModelScope.launch {
        val p = _perfil.value ?: return@launch
        val actualizado = p.copy(ultimaVez = System.currentTimeMillis())
        repo.guardarPerfil(actualizado)
    }
}
