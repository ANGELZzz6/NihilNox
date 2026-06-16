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
                val nombreEncoded = java.net.URLEncoder.encode(nombrePersonaje, "UTF-8")
                val searchUrl = "https://api.jikan.moe/v4/characters?q=$nombreEncoded&limit=1"
                val searchResponse = URL(searchUrl).readText()
                val searchJson = JSONObject(searchResponse)
                val results = searchJson.getJSONArray("data")

                if (results.length() == 0) return@withContext emptyList()

                val malId = results.getJSONObject(0).getInt("mal_id")

                val picturesUrl = "https://api.jikan.moe/v4/characters/$malId/pictures"
                val picturesResponse = URL(picturesUrl).readText()
                val picturesJson = JSONObject(picturesResponse)
                val picturesArray = picturesJson.getJSONArray("data")

                val urls = mutableListOf<String>()
                for (i in 0 until picturesArray.length()) {
                    val jpg = picturesArray.getJSONObject(i).getJSONObject("jpg")
                    val url = jpg.optString("large_image_url").ifEmpty {
                        jpg.optString("image_url")
                    }
                    if (url.isNotEmpty()) urls.add(url)
                }

                val entidades = urls.map { ImagenPersonaje(personajeId = personajeId, imageUrl = it) }
                dao.insertAll(entidades)

                urls
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
}