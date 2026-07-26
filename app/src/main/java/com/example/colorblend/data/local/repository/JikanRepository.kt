package com.example.colorblend.data.local.repository

import com.example.colorblend.data.local.ImagenPersonajeDao
import com.example.colorblend.domain.model.ImagenPersonaje
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class JikanRepository(
    private val dao: ImagenPersonajeDao
) {

    suspend fun getImagenes(personajeId: Int, nombrePersonaje: String): List<String> =
        withContext(Dispatchers.IO) {

            // Si ya están guardadas, retornarlas directo
            val guardadas = dao.getImagenesPorPersonaje(personajeId)
            if (guardadas.isNotEmpty()) {
                return@withContext guardadas.map { it.imageUrl }
            }

            return@withContext try {
                val nombreLimpio = limpiarNombre(nombrePersonaje)
                val nombreEncoded = java.net.URLEncoder.encode(nombreLimpio, "UTF-8")
                
                // 1. Buscar el personaje para obtener su MAL ID
                val searchUrl = "https://api.jikan.moe/v4/characters?q=$nombreEncoded&limit=1"
                val searchResponse = executeGetRequest(searchUrl) ?: return@withContext emptyList()
                val searchJson = JSONObject(searchResponse)
                val results = searchJson.optJSONArray("data")

                if (results == null || results.length() == 0) return@withContext emptyList()

                val malId = results.getJSONObject(0).getInt("mal_id")

                // 2. Obtener la galería de imágenes del personaje
                val picturesUrl = "https://api.jikan.moe/v4/characters/$malId/pictures"
                val picturesResponse = executeGetRequest(picturesUrl) ?: return@withContext emptyList()
                val picturesJson = JSONObject(picturesResponse)
                val picturesArray = picturesJson.optJSONArray("data") ?: return@withContext emptyList()

                val urls = mutableListOf<String>()
                for (i in 0 until picturesArray.length()) {
                    val jpg = picturesArray.getJSONObject(i).optJSONObject("jpg") ?: continue
                    val url = jpg.optString("large_image_url").ifEmpty {
                        jpg.optString("image_url")
                    }
                    if (url.isNotEmpty()) urls.add(url)
                }

                if (urls.isNotEmpty()) {
                    val entidades = urls.map { ImagenPersonaje(personajeId = personajeId, imageUrl = it) }
                    dao.insertAll(entidades)
                }

                urls
            } catch (e: Exception) {
                android.util.Log.e("JikanRepo", "Error general: ${e.message}")
                emptyList()
            }
        }

    private fun executeGetRequest(urlString: String): String? {
        var connection: java.net.HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "ColorBlendApp/1.0 (Android)")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                if (responseCode == 429) {
                    android.util.Log.e("JikanRepo", "Rate limit exceeded (429)")
                } else {
                    android.util.Log.e("JikanRepo", "HTTP Error $responseCode: ${connection.errorStream?.bufferedReader()?.use { it.readText() }}")
                }
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("JikanRepo", "Request failed: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun limpiarNombre(nombre: String): String {
        // Quitar paréntesis y contenido (ej: "Saeko (Highschool of the Dead)" -> "Saeko")
        return nombre.replace(Regex("\\(.*?\\)"), "").trim()
    }
}