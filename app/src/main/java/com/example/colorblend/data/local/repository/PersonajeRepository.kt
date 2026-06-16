package com.example.colorblend.data.local.repository

import com.example.colorblend.data.local.PersonajeDao
import com.example.colorblend.domain.model.AnimeResumen
import com.example.colorblend.domain.model.PersonajeObtenido
import kotlinx.coroutines.flow.Flow

class PersonajeRepository(
    private val dao: PersonajeDao
) {
    fun getPersonajes(): Flow<List<PersonajeObtenido>> = dao.getPersonajes()

    fun getAnimes(): Flow<List<AnimeResumen>> = dao.getAnimes()

    fun getPersonajesPorAnime(animeId: Int): Flow<List<PersonajeObtenido>> =
        dao.getPersonajesPorAnime(animeId)

    fun getPersonajesPorTitulo(titulo: String): Flow<List<PersonajeObtenido>> =
        dao.getPersonajesPorTitulo(titulo)

    suspend fun guardarPersonaje(personaje: PersonajeObtenido) {
        dao.insert(personaje)
    }
}