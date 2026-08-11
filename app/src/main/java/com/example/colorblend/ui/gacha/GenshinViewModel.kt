package com.example.colorblend.ui.gacha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.GenshinCharacter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GenshinViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).genshinDao()

    val characters: StateFlow<List<GenshinCharacter>> = dao.getAllCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insert(character: GenshinCharacter) = viewModelScope.launch {
        dao.insert(character)
    }

    fun update(character: GenshinCharacter) = viewModelScope.launch {
        dao.update(character)
    }

    fun delete(character: GenshinCharacter) = viewModelScope.launch {
        dao.delete(character)
    }
    
    suspend fun getById(id: Int) = dao.getById(id)
}
