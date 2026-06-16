package com.example.colorblend.data.local.repository

import com.example.colorblend.domain.model.PersonajeObtenido
import com.example.colorblend.domain.model.Rareza
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

class GiantBombRepository {

    companion object {
        private const val API_KEY = "9034a6e052d0227aa03c53db54c76d23aeceb35b"
        private const val BASE_URL = "https://www.giantbomb.com/api"
        private const val TOTAL_CHARS = 15000
    }

    suspend fun fetchPersonajeAleatorio(idsUsados: MutableSet<Int>): PersonajeObtenido? {
        repeat(5) {
            val offset = Random.nextInt(0, TOTAL_CHARS)

            return try {
                val urlString = "$BASE_URL/characters/" +
                        "?api_key=$API_KEY" +
                        "&format=json" +
                        "&limit=10" +
                        "&offset=$offset" +
                        "&field_list=id,name,image,gender,deck,games"

                android.util.Log.d("GIANTBOMB", "URL: $urlString")

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "GachaQuest/1.0")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    android.util.Log.e("GIANTBOMB", "Error HTTP: $responseCode")
                    return@repeat
                }

                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val results = json.optJSONArray("results") ?: return@repeat

                if (results.length() == 0) return@repeat

                val character = results.getJSONObject(Random.nextInt(results.length()))
                val id = character.getInt("id")

                if (idsUsados.contains(id)) return@repeat
                idsUsados.add(id)

                val nombre = character.optString("name", "").ifEmpty { return@repeat }
                val imagenUrl = character.optJSONObject("image")
                    ?.optString("medium_url") ?: ""

                if (imagenUrl.isEmpty() || imagenUrl.contains("default")) return@repeat

                val generoInt = character.optInt("gender", 0)
                val genero = when (generoInt) {
                    1 -> "Male"
                    2 -> "Female"
                    else -> "Unknown"
                }

                val games = character.optJSONArray("games")
                val juegoNombre = if (games != null && games.length() > 0) {
                    games.getJSONObject(0).optString("name", "Videojuego")
                } else "Videojuego"

                val numJuegos = games?.length() ?: 1
                val rareza = when {
                    numJuegos >= 10 -> Rareza.LEGENDARIO
                    numJuegos >= 5  -> Rareza.EPICO
                    numJuegos >= 2  -> Rareza.RARO
                    else            -> Rareza.COMUN
                }

                android.util.Log.d("GIANTBOMB", "Personaje obtenido: $nombre")

                PersonajeObtenido(
                    id = 2_000_000 + id,
                    nombre = nombre,
                    imagenUrl = imagenUrl,
                    favoritos = numJuegos * 1000,
                    rareza = rareza,
                    genero = genero,
                    categoria = "videojuego",
                    animeTitulo = juegoNombre,
                    animeCoverUrl = imagenUrl
                )
            } catch (e: Exception) {
                android.util.Log.e("GIANTBOMB", "Exception: ${e.message}")
                null
            }
        }
        return null
    }
}