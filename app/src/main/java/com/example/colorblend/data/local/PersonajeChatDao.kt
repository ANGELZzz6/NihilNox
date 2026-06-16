package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.PersonajeChat
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonajeChatDao {

    @Query("SELECT * FROM personajes_chat ORDER BY fechaAgregado DESC")
    fun getPersonajesChat(): Flow<List<PersonajeChat>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(personajeChat: PersonajeChat)

    @Delete
    suspend fun delete(personajeChat: PersonajeChat)

    @Query("SELECT EXISTS(SELECT 1 FROM personajes_chat WHERE personajeId = :personajeId)")
    suspend fun estaEnChat(personajeId: Int): Boolean
}