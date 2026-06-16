package com.example.colorblend.data.local.repository

import android.content.Context
import com.example.colorblend.data.local.ApiKeysManager
import com.example.colorblend.domain.model.PersonajeObtenido
import com.example.colorblend.domain.model.Rareza
import org.json.JSONArray
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

class IGDBRepository(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://api.igdb.com/v4"
    }

    suspend fun fetchPersonajesEnBloque(
        cantidad: Int,
        idsUsados: MutableSet<Int>,
        generoFiltro: String? = null
    ): List<PersonajeObtenido> {
        return try {
            val clientId    = ApiKeysManager.getIgdbClientId(context)
            val accessToken = ApiKeysManager.getIgdbToken(context)

            val offset = Random.nextInt(0, 4000)
            val limit  = minOf(cantidad * 4, 50)

            val url        = URL("$BASE_URL/characters")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Client-ID", clientId)
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "text/plain")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout    = 15000

            val query = """
                fields name, mug_shot.url, gender, games.name, games.rating;
                limit $limit;
                offset $offset;
            """.trimIndent()

            OutputStreamWriter(connection.outputStream).use {
                it.write(query); it.flush()
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                android.util.Log.e("IGDB", "Error HTTP: ${connection.responseCode}")
                return emptyList()
            }

            val results = JSONArray(connection.inputStream.bufferedReader().readText())

            val personajesProcesados = (0 until results.length()).mapNotNull { i ->
                val character = results.getJSONObject(i)
                val id = character.getInt("id")
                if (idsUsados.contains(id)) return@mapNotNull null

                val nombre    = character.optString("name", "").ifEmpty { return@mapNotNull null }
                val mugShot   = character.optJSONObject("mug_shot") ?: return@mapNotNull null
                val imagenRaw = mugShot.optString("url").ifEmpty { return@mapNotNull null }
                val imagenUrl = if (imagenRaw.startsWith("//")) "https:$imagenRaw" else imagenRaw
                val imagenFinal = imagenUrl.replace("thumb", "cover_big")

                val generoInt = character.optInt("gender", -1)
                val genero = when (generoInt) { 0 -> "Male"; 1 -> "Female"; else -> "Unknown" }

                if (generoFiltro != null && genero != generoFiltro) return@mapNotNull null

                idsUsados.add(id)

                val games       = character.optJSONArray("games")
                val juegoNombre = games?.optJSONObject(0)?.optString("name", "Videojuego") ?: "Videojuego"
                val rating      = games?.optJSONObject(0)?.optDouble("rating", 0.0)?.toInt() ?: 0

                val rareza = when {
                    rating >= 90 -> Rareza.LEGENDARIO
                    rating >= 75 -> Rareza.EPICO
                    rating >= 55 -> Rareza.RARO
                    else         -> Rareza.COMUN
                }

                PersonajeObtenido(
                    id = 3_000_000 + id, nombre = nombre, imagenUrl = imagenFinal,
                    favoritos = rating * 100, rareza = rareza, genero = genero,
                    categoria = "videojuego", animeTitulo = juegoNombre, animeCoverUrl = imagenFinal
                )
            }.take(cantidad)

            android.util.Log.d("IGDB", "Bloque: ${personajesProcesados.size} personajes")
            personajesProcesados

        } catch (e: Exception) {
            android.util.Log.e("IGDB", "Exception: ${e.message}")
            emptyList()
        }
    }
}