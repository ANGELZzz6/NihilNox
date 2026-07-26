package com.example.colorblend.data.local.repository

import android.content.Context
import android.util.Log
import com.example.colorblend.data.local.PersonajePoolDao
import com.example.colorblend.domain.model.PersonajeObtenido
import com.example.colorblend.domain.model.PersonajePool
import com.example.colorblend.domain.model.Rareza
import com.example.colorblend.graphql.GetRandomCharactersQuery
import com.example.colorblend.network.ApolloClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.Collections
import kotlin.random.Random

class GachaPoolRepository(
    private val context: Context,
    private val poolDao: PersonajePoolDao
) {
    private val superheroRepo = SuperheroRepository(context)
    private val igdbRepo = IGDBRepository(context)

    companion object {
        private const val MIN_POOL_SIZE = 60
        private const val MAX_POOL_SIZE = 150
        private const val BATCH_SIZE = 50
        
        // Prioridad: 60% Anime, 20% Superhéroes, 20% Videojuegos
        private const val RATIO_ANIME = 0.6
        private const val RATIO_SUPER = 0.2
        private const val RATIO_GAMES = 0.2
    }

    suspend fun getPoolSize(): Int = poolDao.getPoolSize()

    suspend fun recargarPoolSiEsNecesario() = withContext(Dispatchers.IO) {
        try {
            val currentSize = poolDao.getPoolSize()
            if (currentSize < MIN_POOL_SIZE) {
                Log.d("GachaPool", "Pool bajo ($currentSize). Recargando...")
                val aCargar = MAX_POOL_SIZE - currentSize
                val personajes = fetchNuevosPersonajes(aCargar)
                
                if (personajes.isEmpty()) {
                    Log.w("GachaPool", "No se pudieron obtener nuevos personajes. Usando fallback local.")
                    insertarFallbackLocal()
                } else {
                    poolDao.insertBatch(personajes.map { it.toPoolEntity() })
                    Log.d("GachaPool", "Pool recargado con ${personajes.size} personajes.")
                }
            }
        } catch (e: Exception) {
            Log.e("GachaPool", "Error silencioso en recarga: ${e.message}")
        }
    }

    private suspend fun insertarFallbackLocal() {
        // Personajes de emergencia si todo falla
        val fallback = listOf(
            PersonajeObtenido(999990, "Goku", "https://s4.anilist.co/file/anilistcdn/character/large/b13-7J17Jv0R9v9v.png", 10000, Rareza.LEGENDARIO, "Male", "anime", 0, "Dragon Ball Z"),
            PersonajeObtenido(999991, "Spider-Man", "https://cdn.jsdelivr.net/gh/akabab/superhero-api@0.3.0/api/images/md/620-spider-man.jpg", 8000, Rareza.EPICO, "Male", "superhero", 0, "Marvel Comics"),
            PersonajeObtenido(999992, "Lara Croft", "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r7h.jpg", 5000, Rareza.RARO, "Female", "videojuego", 0, "Tomb Raider"),
            PersonajeObtenido(999993, "Saitama", "https://s4.anilist.co/file/anilistcdn/character/large/b80327-0F5xW9M0H7H7.png", 9000, Rareza.LEGENDARIO, "Male", "anime", 0, "One Punch Man"),
            PersonajeObtenido(999994, "Wonder Woman", "https://cdn.jsdelivr.net/gh/akabab/superhero-api@0.3.0/api/images/md/720-wonder-woman.jpg", 7500, Rareza.EPICO, "Female", "superhero", 0, "DC Comics")
        )
        poolDao.insertBatch(fallback.map { it.toPoolEntity() })
    }

    suspend fun obtenerTiradaDivera(cantidad: Int, genero: String? = null): List<PersonajeObtenido> = withContext(Dispatchers.IO) {
        // Pedimos más personajes de los necesarios para poder filtrar por diversidad
        val bufferSize = cantidad * 3
        val rawPool = if (genero == null) {
            poolDao.getRandomCharacters(bufferSize)
        } else {
            poolDao.getRandomCharactersByGender(genero, bufferSize)
        }

        if (rawPool.size < cantidad) {
            // Carga de emergencia si no hay suficientes
            val extra = fetchNuevosPersonajes(cantidad - rawPool.size, genero)
            return@withContext (rawPool.map { it.toObtenido() } + extra).take(cantidad)
        }

        // Algoritmo de diversidad: elegir títulos distintos
        val seleccionados = mutableListOf<PersonajePool>()
        val titulosUsados = mutableSetOf<String>()

        // Primero intentamos llenar con títulos únicos
        for (p in rawPool.shuffled()) {
            if (!titulosUsados.contains(p.animeTitulo)) {
                seleccionados.add(p)
                titulosUsados.add(p.animeTitulo)
            }
            if (seleccionados.size >= cantidad) break
        }

        // Si faltan, rellenamos con el resto permitiendo algunos duplicados
        if (seleccionados.size < cantidad) {
            val restantes = rawPool.filter { !seleccionados.contains(it) }.shuffled()
            for (p in restantes) {
                seleccionados.add(p)
                if (seleccionados.size >= cantidad) break
            }
        }

        // Eliminar del pool los que vamos a usar
        poolDao.deleteBatch(seleccionados)

        return@withContext seleccionados.map { it.toObtenido() }
    }

    private suspend fun fetchNuevosPersonajes(
        cantidad: Int, 
        generoFiltro: String? = null
    ): List<PersonajeObtenido> = withContext(Dispatchers.IO) {
        val idsUsados = Collections.synchronizedSet(mutableSetOf<Int>())
        
        // Priorizando AniList (60%)
        val animeCant = (cantidad * RATIO_ANIME).toInt() + 1
        val superCant = (cantidad * RATIO_SUPER).toInt() + 1
        val gameCant = (cantidad * RATIO_GAMES).toInt() + 1

        val jobAnime = async { 
            try { fetchAnimeBloque(animeCant, generoFiltro, idsUsados) } 
            catch (e: Exception) { Log.e("GachaPool", "Fallo AniList"); emptyList() }
        }
        val jobSuper = async { 
            try { superheroRepo.fetchBloque(superCant, idsUsados, generoFiltro) }
            catch (e: Exception) { Log.e("GachaPool", "Fallo Superhero"); emptyList() }
        }
        val jobGames = async { 
            try { igdbRepo.fetchPersonajesEnBloque(gameCant, idsUsados, generoFiltro) }
            catch (e: Exception) { Log.e("GachaPool", "Fallo IGDB"); emptyList() }
        }

        val resAnime = jobAnime.await()
        val resSuper = jobSuper.await()
        val resGames = jobGames.await()

        val totalObtenidos = resAnime + resSuper + resGames

        // Si alguna API falló y no llegamos al total, intentamos compensar con las que funcionaron
        if (totalObtenidos.size < cantidad && totalObtenidos.isNotEmpty()) {
            val faltante = cantidad - totalObtenidos.size
            Log.d("GachaPool", "Compensando faltante de $faltante personajes...")
            // Lógica de compensación simple: pedir más a lo que esté disponible
            // En una versión más compleja, aquí llamaríamos de nuevo a las APIs que no fallaron
        }

        return@withContext totalObtenidos.shuffled()
    }

    private suspend fun fetchAnimeBloque(
        cantidad: Int,
        generoFiltro: String?,
        idsUsados: MutableSet<Int>
    ): List<PersonajeObtenido> {
        val lista = mutableListOf<PersonajeObtenido>()
        var paginasProbadas = 0
        
        // Estrategia de muestreo: saltar entre páginas aleatorias
        while (lista.size < cantidad && paginasProbadas < 5) {
            paginasProbadas++
            val page = Random.nextInt(1, 400)
            try {
                val response = ApolloClientProvider.apolloClient
                    .query(GetRandomCharactersQuery(page = page, perPage = 25))
                    .execute()
                
                val chars = response.data?.Page?.characters?.filterNotNull() ?: continue
                val filtrados = if (generoFiltro != null) {
                    chars.filter { it.gender?.equals(generoFiltro, ignoreCase = true) == true }
                } else chars

                for (c in filtrados.shuffled()) {
                    if (lista.size >= cantidad) break
                    if (idsUsados.contains(c.id)) continue
                    
                    idsUsados.add(c.id)
                    val favorites = c.favourites as? Int ?: 0
                    val animeNode = c.media?.nodes?.firstOrNull()
                    
                    lista.add(PersonajeObtenido(
                        id = c.id,
                        nombre = c.name?.full ?: "???",
                        imagenUrl = c.image?.large ?: "",
                        favoritos = favorites,
                        rareza = Rareza.desde(favorites),
                        genero = c.gender ?: "Unknown",
                        categoria = "anime",
                        animeId = animeNode?.id ?: 0,
                        animeTitulo = animeNode?.title?.romaji ?: "Desconocido",
                        animeCoverUrl = animeNode?.coverImage?.large ?: ""
                    ))
                }
            } catch (e: Exception) {
                Log.e("GachaPool", "Error fetching anime page $page: ${e.message}")
            }
        }
        return lista
    }

    private fun PersonajeObtenido.toPoolEntity() = PersonajePool(
        id = id, nombre = nombre, imagenUrl = imagenUrl, favoritos = favoritos,
        rareza = rareza, genero = genero, categoria = categoria,
        animeId = animeId, animeTitulo = animeTitulo, animeCoverUrl = animeCoverUrl
    )

    private fun PersonajePool.toObtenido() = PersonajeObtenido(
        id = id, nombre = nombre, imagenUrl = imagenUrl, favoritos = favoritos,
        rareza = rareza, genero = genero, categoria = categoria,
        animeId = animeId, animeTitulo = animeTitulo, animeCoverUrl = animeCoverUrl
    )
}
