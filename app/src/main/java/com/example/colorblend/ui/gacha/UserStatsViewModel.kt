package com.example.colorblend.ui.gacha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.UserStatsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class UserStatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserStatsRepository(
        AppDatabase.getDatabase(application).userStatsDao()
    )

    val stats = repository.getStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}