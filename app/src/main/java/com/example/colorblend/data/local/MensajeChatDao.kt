package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.MensajeChat
import kotlinx.coroutines.flow.Flow

@Dao
interface MensajeChatDao {

    @Query("SELECT * FROM mensajes_chat WHERE personajeId = :personajeId ORDER BY timestamp ASC")
    fun getMensajes(personajeId: Int): Flow<List<MensajeChat>>

    @Insert
    suspend fun insert(mensaje: MensajeChat)

    @Query("DELETE FROM mensajes_chat WHERE personajeId = :personajeId")
    suspend fun borrarChat(personajeId: Int)
}