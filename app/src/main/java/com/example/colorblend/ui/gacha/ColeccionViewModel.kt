package com.example.colorblend.ui.gacha

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.PersonajeRepository
import com.example.colorblend.domain.model.AnimeResumen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ColeccionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PersonajeRepository(
        AppDatabase.getDatabase(application).personajeDao()
    )

    private val prefs = application.getSharedPreferences("coleccion_prefs", Context.MODE_PRIVATE)

    private val _busqueda = MutableStateFlow("")

    // ✅ Carga el estado guardado al iniciar
    private val _mostrarMasculinos = MutableStateFlow(
        prefs.getBoolean("mostrar_masculinos", true)
    )
    val mostrarMasculinos: StateFlow<Boolean> = _mostrarMasculinos

    private val _todosLosAnimes = repository.getAnimes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val animes: StateFlow<List<AnimeResumen>> = combine(
        _todosLosAnimes,
        _busqueda,
        _mostrarMasculinos
    ) { lista, query, mostrarMasc ->
        var resultado = lista
        if (query.isNotBlank()) {
            resultado = resultado.filter {
                it.animeTitulo.contains(query, ignoreCase = true)
            }
        }
        resultado
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun buscar(query: String) {
        _busqueda.value = query
    }

    fun toggleMasculinos() {
        val nuevoValor = !_mostrarMasculinos.value
        _mostrarMasculinos.value = nuevoValor
        // ✅ Persiste el estado
        prefs.edit().putBoolean("mostrar_masculinos", nuevoValor).apply()
    }
}