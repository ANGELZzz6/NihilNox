package com.example.colorblend.ui.gacha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.MetaRepository
import com.example.colorblend.data.local.repository.PersonajeRepository
import com.example.colorblend.data.local.repository.UserStatsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    val stats = UserStatsRepository(db.userStatsDao())
        .getStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val metas = MetaRepository(db.metaDao(), db.userStatsDao())
        .getMetas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personajes = PersonajeRepository(db.personajeDao())
        .getPersonajes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val animes = PersonajeRepository(db.personajeDao())
        .getAnimes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}