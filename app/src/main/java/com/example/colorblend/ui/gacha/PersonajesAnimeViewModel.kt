package com.example.colorblend.ui.gacha

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.PersonajeRepository
import com.example.colorblend.domain.model.PersonajeObtenido
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PersonajesAnimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PersonajeRepository(
        AppDatabase.getDatabase(application).personajeDao()
    )

    private val prefs = application.getSharedPreferences("coleccion_prefs", Context.MODE_PRIVATE)

    private val _personajesFiltrados = MutableStateFlow<List<PersonajeObtenido>>(emptyList())
    val personajes: StateFlow<List<PersonajeObtenido>> = _personajesFiltrados

    private var collectJob: Job? = null

    private fun aplicarFiltro(lista: List<PersonajeObtenido>) {
        val mostrarMasculinos = prefs.getBoolean("mostrar_masculinos", true)
        _personajesFiltrados.value = if (mostrarMasculinos) lista
        else lista.filter { it.genero.lowercase() != "male" }
    }

    // Para anime — busca por animeId
    fun cargarPersonajes(animeId: Int) {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repository.getPersonajesPorAnime(animeId).collect { lista ->
                aplicarFiltro(lista)
            }
        }
    }

    // Para superhéroes y videojuegos — busca por título
    fun cargarPersonajesPorTitulo(titulo: String) {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repository.getPersonajesPorTitulo(titulo).collect { lista ->
                aplicarFiltro(lista)
            }
        }
    }
}