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

    suspend fun recargarPoolSiEsNecesario() {
        withContext(Dispatchers.IO) {
            try {
                val currentSize = poolDao.getPoolSize()
                val maleCount = poolDao.getMaleCount()
                val femaleCount = poolDao.getFemaleCount()
                
                Log.d("GachaPool", "Estado Pool - Total: $currentSize, M: $maleCount, F: $femaleCount")

                // Umbral por género: si uno baja de 30, priorizamos ese género
                val genderThreshold = 30
                val idealGenderCount = MAX_POOL_SIZE / 2

                if (maleCount < genderThreshold) {
                    Log.d("GachaPool", "Stock de hombres bajo. Recargando masculinos...")
                    val aCargar = idealGenderCount - maleCount
                    val nuevos = fetchNuevosPersonajes(aCargar, "Male")
                    poolDao.insertBatch(nuevos.map { it.toPoolEntity() })
                }
                
                if (femaleCount < genderThreshold) {
                    Log.d("GachaPool", "Stock de mujeres bajo. Recargando femeninos...")
                    val aCargar = idealGenderCount - femaleCount
                    val nuevos = fetchNuevosPersonajes(aCargar, "Female")
                    poolDao.insertBatch(nuevos.map { it.toPoolEntity() })
                }

                // Si después de equilibrar el total sigue bajo, hacemos carga genérica
                val finalSize = poolDao.getPoolSize()
                if (finalSize < MIN_POOL_SIZE) {
                    val aCargar = MAX_POOL_SIZE - finalSize
                    val personajes = fetchNuevosPersonajes(aCargar)
                    if (personajes.isEmpty()) insertarFallbackLocal()
                    else poolDao.insertBatch(personajes.map { it.toPoolEntity() })
                }
                
                return@withContext Unit
            } catch (e: Exception) {
                Log.e("GachaPool", "Error silencioso en recarga balanceada: ${e.message}")
            }
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
            val finalResult = (rawPool.map { it.toObtenido() } + extra).take(cantidad)
            return@withContext finalResult
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
        }

        val finalMezclados = totalObtenidos.shuffled()
        return@withContext finalMezclados
    }

    private suspend fun fetchAnimeBloque(
        cantidad: Int,
        generoFiltro: String?,
        idsUsados: MutableSet<Int>
    ): List<PersonajeObtenido> {
        val lista = mutableListOf<PersonajeObtenido>()
        var paginasProbadas = 0
        
        // Estrategia de muestreo: saltar entre páginas aleatorias
        // Usamos un rango de 200 para evitar errores 400 (Bad Request) por páginas muy profundas
        while (lista.size < cantidad && paginasProbadas < 5) {
            paginasProbadas++
            val page = Random.nextInt(1, 200)
            try {
                val response = ApolloClientProvider.apolloClient
                    .query(GetRandomCharactersQuery(page = page, perPage = 25))
                    .execute()
                
                val characters = response.data?.Page?.characters ?: continue
                
                val chars = characters.filterNotNull()
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
                        genero = normalizeGender(c.gender),
                        categoria = "anime",
                        animeId = animeNode?.id ?: 0,
                        animeTitulo = animeNode?.title?.romaji ?: "Desconocido",
                        animeCoverUrl = animeNode?.coverImage?.large ?: ""
                    ))
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                if (errorMsg.contains("400")) {
                    Log.e("GachaPool", "AniList: Página $page fuera de rango o inválida (400).")
                } else {
                    Log.e("GachaPool", "Error fetching anime page $page: $errorMsg")
                }
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

    private fun normalizeGender(genero: String?): String {
        return when (genero?.lowercase()) {
            "male", "m", "masculino" -> "Male"
            "female", "f", "femenino" -> "Female"
            else -> "Unknown"
        }
    }
}
