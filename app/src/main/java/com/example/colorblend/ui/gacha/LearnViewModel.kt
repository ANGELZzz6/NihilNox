package com.example.colorblend.ui.gacha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.LearnRepository
import com.example.colorblend.domain.model.LearnCard
import com.example.colorblend.domain.model.LearnQuizQuestion
import com.example.colorblend.domain.model.LearnTopic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LearnViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LearnRepository(
        AppDatabase.getDatabase(application).learnDao()
    )

    val topics: StateFlow<List<LearnTopic>> = repository.getAllTopics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<LearnUiState>(LearnUiState.Idle)
    val uiState: StateFlow<LearnUiState> = _uiState

    private val _sessionCards = MutableStateFlow<List<LearnCard>>(emptyList())
    val sessionCards: StateFlow<List<LearnCard>> = _sessionCards

    private val _quizQuestions = MutableStateFlow<List<LearnQuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<LearnQuizQuestion>> = _quizQuestions

    // ── Crear tema nuevo ───────────────────────────────────────────────
    fun crearTema(
        titulo: String,
        categoria: String,
        materialUsuario: String?,
        groqKey: String
    ) {
        viewModelScope.launch {
            _uiState.value = LearnUiState.Cargando("Generando tu lección con IA...")
            val result = repository.generarContenidoTema(
                titulo, categoria, materialUsuario, groqKey, getApplication()
            )
            _uiState.value = result.fold(
                onSuccess = { topicId -> LearnUiState.TemaCreado(topicId) },
                onFailure = { LearnUiState.Error(it.message ?: "Error desconocido") }
            )
        }
    }

    // ── Iniciar sesión de estudio ──────────────────────────────────────
    fun iniciarSesion(topicId: Int) {
        viewModelScope.launch {
            _uiState.value = LearnUiState.Cargando("Preparando sesión...")
            val cards = repository.getCardsParaSesion(topicId)
            _sessionCards.value = cards
            _uiState.value = if (cards.isEmpty())
                LearnUiState.Error("No hay tarjetas para repasar hoy")
            else LearnUiState.SesionLista
        }
    }

    // ── Iniciar quiz ───────────────────────────────────────────────────
    fun iniciarQuiz(topicId: Int) {
        viewModelScope.launch {
            val preguntas = repository.getQuizAleatorio(topicId)
            _quizQuestions.value = preguntas
            _uiState.value = LearnUiState.QuizListo
        }
    }

    // ── Calificar tarjeta (SM-2) ───────────────────────────────────────
    fun calificarCard(card: LearnCard, calificacion: Int) {
        viewModelScope.launch {
            repository.actualizarCard(card, calificacion)
        }
    }

    // ── Finalizar sesión ───────────────────────────────────────────────
    fun finalizarSesion(topicId: Int, xpGanado: Int, monedasGanadas: Int) {
        viewModelScope.launch {
            repository.actualizarDominio(topicId)
            _uiState.value = LearnUiState.SesionCompletada(xpGanado, monedasGanadas)
        }
    }

    fun eliminarTema(topicId: Int) {
        viewModelScope.launch {
            repository.eliminarTema(topicId)
        }
    }

    fun resetState() { _uiState.value = LearnUiState.Idle }
}

sealed class LearnUiState {
    object Idle : LearnUiState()
    object SesionLista : LearnUiState()
    object QuizListo : LearnUiState()
    data class Cargando(val mensaje: String) : LearnUiState()
    data class TemaCreado(val topicId: Int) : LearnUiState()
    data class SesionCompletada(val xpGanado: Int, val monedasGanadas: Int) : LearnUiState()
    data class Error(val mensaje: String) : LearnUiState()
}
