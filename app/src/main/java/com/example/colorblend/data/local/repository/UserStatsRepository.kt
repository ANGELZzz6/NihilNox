package com.example.colorblend.data.local.repository

import com.example.colorblend.data.local.UserStatsDao
import com.example.colorblend.domain.model.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UserStatsRepository(
    private val dao: UserStatsDao
) {

    // ✅ Mutex para evitar condición de carrera
    private val mutex = Mutex()

    fun getStats(): Flow<UserStats> = dao.getStats()

    suspend fun addMonedas(cantidad: Int) {
        mutex.withLock {
            val stats = dao.getStatsOnce()
            if (stats == null) {
                dao.insert(UserStats(monedas = cantidad))
            } else {
                dao.update(stats.copy(monedas = stats.monedas + cantidad))
            }
        }
    }

    // ✅ Verificación y resta en una sola operación protegida
    suspend fun restarMonedas(cantidad: Int): Boolean {
        mutex.withLock {
            val stats = dao.getStatsOnce() ?: return false
            if (stats.monedas < cantidad) return false
            dao.update(stats.copy(monedas = stats.monedas - cantidad))
            return true
        }
    }

    suspend fun getMonedas(): Int {
        mutex.withLock {
            return dao.getStatsOnce()?.monedas ?: 0
        }
    }
}