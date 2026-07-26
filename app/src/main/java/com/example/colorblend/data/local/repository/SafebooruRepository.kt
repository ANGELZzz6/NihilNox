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

    suspend fun getImagenes(
        personajeId: Int, 
        nombrePersonaje: String, 
        serie: String? = null,
        limit: Int = 15
    ): List<String> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Intentar búsqueda combinada: Personaje + Serie (Máxima precisión)
                if (!serie.isNullOrBlank()) {
                    val combinedTag = "${formatTag(nombrePersonaje)} ${formatTag(serie)}"
                    val combinedUrls = fetchByTag(personajeId, combinedTag, limit)
                    if (combinedUrls.isNotEmpty()) return@withContext combinedUrls
                }

                // 2. Intentar búsqueda con nombre completo formateado (ej: saeko_busujima)
                val urls = fetchByTag(personajeId, formatTag(nombrePersonaje), limit)
                if (urls.isNotEmpty()) return@withContext urls

                // 3. Si falla, intentar invertir nombre (muchos boorus usan Apellido_Nombre)
                val invertido = invertirNombre(nombrePersonaje)
                if (invertido != null) {
                    val urlsInv = fetchByTag(personajeId, formatTag(invertido), limit)
                    if (urlsInv.isNotEmpty()) return@withContext urlsInv
                }

                // 4. Si falla, intentar solo con el primer nombre (búsqueda más amplia)
                val primerNombre = nombrePersonaje.split(" ").firstOrNull()
                if (primerNombre != null && primerNombre.length > 2) {
                    val urlsSimple = fetchByTag(personajeId, formatTag(primerNombre), limit)
                    if (urlsSimple.isNotEmpty()) return@withContext urlsSimple
                }

                emptyList()
            } catch (e: Exception) {
                Log.e("SafebooruRepo", "Error general: ${e.message}")
                emptyList()
            }
        }

    private suspend fun fetchByTag(personajeId: Int, tag: String, limit: Int): List<String> {
        val encodedTag = URLEncoder.encode(tag, "UTF-8")
        val urlString = "https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1&tags=$encodedTag&limit=$limit"
        
        Log.d("SafebooruRepo", "Buscando tag: $tag")
        val response = executeGetRequest(urlString)
        
        if (response.isNullOrBlank() || response == "[]") return emptyList()

        return try {
            val results = JSONArray(response)
            val urls = mutableListOf<String>()
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val imageUrl = item.optString("file_url")
                if (imageUrl.isNotEmpty()) {
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
            Log.e("SafebooruRepo", "JSON Error: ${e.message}")
            emptyList()
        }
    }

    private fun formatTag(nombre: String): String {
        return nombre.lowercase()
            .replace(Regex("\\(.*?\\)"), "") 
            .trim()
            .replace(" ", "_")
    }

    private fun invertirNombre(nombre: String): String? {
        val partes = nombre.replace(Regex("\\(.*?\\)"), "").trim().split(" ")
        return if (partes.size >= 2) "${partes.last()} ${partes.first()}" else null
    }

    private fun executeGetRequest(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                body
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
