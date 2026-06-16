package com.example.colorblend.ui.gacha.metas.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.IGDBRepository
import com.example.colorblend.data.local.repository.PersonajeRepository
import com.example.colorblend.data.local.repository.SuperheroRepository
import com.example.colorblend.data.local.repository.UserStatsRepository
import com.example.colorblend.domain.model.PersonajeObtenido
import com.example.colorblend.network.ApolloClientProvider
import com.example.colorblend.graphql.GetRandomCharactersQuery
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.util.Collections
import com.example.colorblend.domain.model.Rareza
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class CharacterUI(
    val title: String?,
    val imageUrl: String?,
    val score: Int?,
    val origen: String? = null,
    val categoria: String? = "anime"
)

sealed class GachaEstado {
    object Idle : GachaEstado()
    object Cargando : GachaEstado()
    object SinMonedas : GachaEstado()
    data class Exito(val personajes: List<CharacterUI>) : GachaEstado()
    data class Error(val mensaje: String) : GachaEstado()
}

enum class TipoGacha {
    NORMAL,
    FEMENINO,
    MASCULINO
}

class AnimeViewModel(application: Application) : AndroidViewModel(application) {

    private val personajeRepository = PersonajeRepository(
        AppDatabase.getDatabase(application).personajeDao()
    )
    private val userStatsRepository = UserStatsRepository(
        AppDatabase.getDatabase(application).userStatsDao()
    )
    private val superheroRepo = SuperheroRepository(application)
    private val igdbRepo = IGDBRepository(application)

    private val _gachaEstado = MutableLiveData<GachaEstado>(GachaEstado.Idle)
    val gachaEstado: LiveData<GachaEstado> = _gachaEstado

    companion object {
        const val COSTO_1X = 5
        const val COSTO_10X = 45
    }

    fun tirarGacha(cantidad: Int, tipo: TipoGacha = TipoGacha.NORMAL) {
        viewModelScope.launch {
            try {
                val costo = if (cantidad == 1) COSTO_1X else COSTO_10X

                val puedeTirar = userStatsRepository.restarMonedas(costo)
                if (!puedeTirar) {
                    _gachaEstado.postValue(GachaEstado.SinMonedas)
                    return@launch
                }

                _gachaEstado.postValue(GachaEstado.Cargando)

                val lista = fetchPersonajesMezclados(cantidad, tipo)

                withContext(Dispatchers.IO) {
                    lista.map { (_, personaje) ->
                        async { personajeRepository.guardarPersonaje(personaje) }
                    }.awaitAll()
                }

                _gachaEstado.postValue(GachaEstado.Exito(lista.map { it.first }))

            } catch (e: Exception) {
                _gachaEstado.postValue(GachaEstado.Error(e.message ?: "Error desconocido"))
            }
        }
    }

    private suspend fun fetchPersonajesMezclados(
        cantidad: Int,
        tipo: TipoGacha
    ): List<Pair<CharacterUI, PersonajeObtenido>> = withContext(Dispatchers.IO) {

        val idsUsados = Collections.synchronizedSet(mutableSetOf<Int>())
        val generoFiltro = when (tipo) {
            TipoGacha.FEMENINO -> "Female"
            TipoGacha.MASCULINO -> "Male"
            else -> null
        }

        // ✅ Solo 3 requests totales en paralelo — una por API
        val jobAnime = async {
            fetchAnimeEnBloque(cantidad, tipo, idsUsados)
        }
        val jobSuperhero = async {
            val cantidad2 = (cantidad / 4) + 1
            (0 until cantidad2).mapNotNull {
                superheroRepo.fetchPersonajeAleatorio(idsUsados, generoFiltro)
            }
        }
        val jobIGDB = async {
            igdbRepo.fetchPersonajesEnBloque(
                (cantidad / 4) + 1,
                idsUsados,
                generoFiltro
            )
        }

        val (animes, superheroes, juegos) = Triple(
            jobAnime.await(),
            jobSuperhero.await(),
            jobIGDB.await()
        )

        // Mezclar todo y tomar los que necesitamos
        val todos = (animes + superheroes + juegos).shuffled()

        todos.take(cantidad).map { p ->
            Pair(
                CharacterUI(
                    title = p.nombre,
                    imageUrl = p.imagenUrl,
                    score = p.favoritos,
                    origen = p.animeTitulo,
                    categoria = p.categoria
                ),
                p
            )
        }
    }

    private suspend fun fetchAnimeEnBloque(
        cantidad: Int,
        tipo: TipoGacha,
        idsUsados: MutableSet<Int>
    ): List<PersonajeObtenido> {
        val lista = mutableListOf<PersonajeObtenido>()
        var intentos = 0

        loop@ while (lista.size < cantidad && intentos < 3) {
            intentos++
            val randomPage = Random.nextInt(1, 300)

            val response = ApolloClientProvider.apolloClient
                .query(GetRandomCharactersQuery(page = randomPage, perPage = 25))
                .execute()

            val characters = response.data?.Page?.characters ?: continue

            val filtrados = when (tipo) {
                TipoGacha.FEMENINO  -> characters.filter { it?.gender?.lowercase() == "female" }
                TipoGacha.MASCULINO -> characters.filter { it?.gender?.lowercase() == "male" }
                TipoGacha.NORMAL    -> characters.filterNotNull()
            }.shuffled()

            for (character in filtrados) {
                if (lista.size >= cantidad) break@loop  // ✅ break con label
                if (character == null) continue
                val id = character.id
                if (idsUsados.contains(id)) continue
                idsUsados.add(id)

                val favoritos = character.favourites as? Int ?: 0
                val animeNode = character.media?.nodes?.firstOrNull()

                lista.add(PersonajeObtenido(
                    id = id,
                    nombre = character.name?.full ?: "Desconocido",
                    imagenUrl = character.image?.large ?: "",
                    favoritos = favoritos,
                    rareza = Rareza.desde(favoritos),
                    genero = character.gender ?: "Unknown",
                    categoria = "anime",
                    animeId = animeNode?.id as? Int ?: 0,
                    animeTitulo = animeNode?.title?.romaji ?: "Desconocido",
                    animeCoverUrl = animeNode?.coverImage?.large ?: ""
                ))
            }

            if (lista.size < cantidad) kotlinx.coroutines.delay(300L)
        }

        return lista
    }
}