package com.example.colorblend.ui.gacha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.VeniceRepository
import com.example.colorblend.domain.model.MensajeChat
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class ChatEstado {
    object Idle : ChatEstado()
    object Cargando : ChatEstado()
    data class Error(val mensaje: String) : ChatEstado()
}

class ChatPersonajeViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).mensajeChatDao()
    private val veniceRepo = VeniceRepository(application)

    private val _mensajes = MutableStateFlow<List<MensajeChat>>(emptyList())
    val mensajes: StateFlow<List<MensajeChat>> = _mensajes

    private val _estado = MutableStateFlow<ChatEstado>(ChatEstado.Idle)
    val estado: StateFlow<ChatEstado> = _estado

    private var mensajesJob: Job? = null

    fun cargarMensajes(personajeId: Int) {
        _mensajes.value = emptyList()
        mensajesJob?.cancel()
        mensajesJob = viewModelScope.launch {
            dao.getMensajes(personajeId).collect {
                _mensajes.value = it
            }
        }
    }

    fun enviarMensaje(personajeId: Int, nombrePersonaje: String, texto: String) {
        viewModelScope.launch {
            val historialPrevio = dao.getMensajes(personajeId).first()
                .filter {
                    !it.contenido.startsWith("Error") &&
                            !it.contenido.startsWith("⚠️") &&
                            !it.contenido.startsWith("⏳") &&
                            !it.contenido.startsWith("⏱️") &&
                            !it.contenido.startsWith("📵") &&
                            !it.contenido.startsWith("😴") &&
                            it.contenido != "null" &&
                            it.personajeId == personajeId
                }

            dao.insert(MensajeChat(
                personajeId = personajeId,
                contenido   = texto,
                esUsuario   = true
            ))

            _estado.value = ChatEstado.Cargando

            val respuesta = veniceRepo.enviarMensaje(
                nombrePersonaje = nombrePersonaje,
                historial       = historialPrevio,
                nuevoMensaje    = texto
            )

            // Siempre mostrar la respuesta — sea éxito o mensaje de error amigable
            dao.insert(MensajeChat(
                personajeId = personajeId,
                contenido   = respuesta,
                esUsuario   = false
            ))

            _estado.value = ChatEstado.Idle
        }
    }

    fun borrarChat(personajeId: Int) {
        viewModelScope.launch {
            dao.borrarChat(personajeId)
        }
    }
}