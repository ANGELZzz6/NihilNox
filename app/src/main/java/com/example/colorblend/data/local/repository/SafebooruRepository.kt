package com.example.colorblend.data.local.repository

import android.util.Log
import com.example.colorblend.data.local.ImagenPersonajeDao
import com.example.colorblend.domain.model.ImagenPersonaje
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SafebooruRepository(
    private val dao: ImagenPersonajeDao
) {

    suspend fun getImagenes(personajeId: Int, nombrePersonaje: String): List<String> =
        withContext(Dispatchers.IO) {
            try {
                // Formatear nombre para etiquetas de Safebooru (ej: "Saeko Busujima" -> "saeko_busujima")
                val tag = nombrePersonaje.lowercase()
                    .replace(Regex("\\(.*?\\)"), "") // Quitar paréntesis
                    .trim()
                    .replace(" ", "_")
                
                val encodedTag = URLEncoder.encode(tag, "UTF-8")
                val urlString = "https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1&tags=$encodedTag&limit=15"
                
                val response = executeGetRequest(urlString) ?: return@withContext emptyList()
                val results = JSONArray(response)

                val urls = mutableListOf<String>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val imageUrl = item.optString("file_url")
                    if (imageUrl.isNotEmpty()) {
                        // Asegurar protocolo https
                        val finalUrl = if (imageUrl.startsWith("//")) "https:$imageUrl" else imageUrl
                        urls.add(finalUrl)
                    }
                }

                if (urls.isNotEmpty()) {
                    val entidades = urls.map { ImagenPersonaje(personajeId = personajeId, imageUrl = it) }
                    dao.insertAll(entidades)
                }

                urls
            } catch (e: Exception) {
                Log.e("SafebooruRepo", "Error fetching: ${e.message}")
                emptyList()
            }
        }

    private fun executeGetRequest(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "ColorBlendApp/1.0 (Android)")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.e("SafebooruRepo", "HTTP Error $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e("SafebooruRepo", "Request failed: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }
}
