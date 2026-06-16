package com.example.colorblend.ui.gacha

import android.content.Context
import com.example.colorblend.data.local.ApiKeysManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class YoutubeCancion(
    val videoId: String,
    val titulo: String,
    val duracion: String,
    val rutaColab: String = ""
)

object YoutubeExtractor {

    private fun getUrl(context: Context): String =
        ApiKeysManager.getServidorUrl(context).trimEnd('/')

    private fun getKey(context: Context): String =
        ApiKeysManager.getServidorKey(context)

    fun servidorConfigurado(context: Context): Boolean {
        val url = getUrl(context)
        val key = getKey(context)
        return url.isNotBlank() && key.isNotBlank()
    }

    fun extraerPlaylistId(url: String): String? {
        val regex = Regex("[?&]list=([a-zA-Z0-9_-]+)")
        return regex.find(url)?.groupValues?.get(1)
    }

    suspend fun obtenerInfoPlaylist(
        context: Context,
        playlistUrl: String,
        onProgreso: (String) -> Unit
    ): Pair<List<YoutubeCancion>, List<String>> = withContext(Dispatchers.IO) {
        val canciones = mutableListOf<YoutubeCancion>()
        val fallidos  = mutableListOf<String>()

        val servidorUrl = getUrl(context)
        val apiKey      = getKey(context)

        if (servidorUrl.isBlank() || apiKey.isBlank()) {
            return@withContext Pair(canciones, fallidos)
        }

        try {
            onProgreso("Conectando con el servidor...")

            val body = """{"url":"$playlistUrl"}"""
            val conn = URL("$servidorUrl/descargar").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.connectTimeout = 300000
            conn.readTimeout = 300000
            conn.doOutput = true
            conn.outputStream.write(body.toByteArray())

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "Error $responseCode"
                android.util.Log.e("YOUTUBE", "Error servidor: $error")
                return@withContext Pair(canciones, fallidos)
            }

            val response = conn.inputStream.bufferedReader().readText()
            val json     = JSONObject(response)
            val items    = json.optJSONArray("canciones") ?: return@withContext Pair(canciones, fallidos)

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                canciones.add(
                    YoutubeCancion(
                        videoId   = i.toString(),
                        titulo    = item.optString("titulo", "Sin titulo"),
                        duracion  = "",
                        rutaColab = item.optString("ruta", "")
                    )
                )
                onProgreso("${i + 1}/${items.length()} — ${item.optString("titulo")}")
            }

            val itemsFallidos = json.optJSONArray("fallidos")
            if (itemsFallidos != null) {
                for (i in 0 until itemsFallidos.length()) {
                    fallidos.add(itemsFallidos.optString(i))
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("YOUTUBE", "Error: ${e.message}")
        }
        Pair(canciones, fallidos)
    }

    suspend fun descargarAudio(
        context: Context,
        rutaColab: String,
        nombreArchivo: String,
        carpeta: File,
        onProgreso: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val servidorUrl = getUrl(context)
        val apiKey      = getKey(context)

        try {
            val archivo = File(carpeta, "${limpiarNombre(nombreArchivo)}.m4a")
            if (archivo.exists()) return@withContext archivo

            val encodedRuta = java.net.URLEncoder.encode(rutaColab, "UTF-8")
            val conn = URL("$servidorUrl/archivo?ruta=$encodedRuta")
                .openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.connectTimeout = 60000
            conn.readTimeout = 60000
            conn.connect()

            if (conn.responseCode != 200) {
                android.util.Log.e("YOUTUBE", "Error descarga: ${conn.responseCode}")
                return@withContext null
            }

            val total      = conn.contentLength
            var descargado = 0

            archivo.outputStream().use { output ->
                conn.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        descargado += bytes
                        if (total > 0) onProgreso((descargado * 100 / total))
                    }
                }
            }
            archivo

        } catch (e: Exception) {
            android.util.Log.e("YOUTUBE", "Error descarga: ${e.message}")
            null
        }
    }

    fun limpiarNombre(nombre: String): String =
        nombre.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(100)

    fun crearCarpetaPlaylist(context: Context, nombrePlaylist: String): File {
        val musica  = File(context.getExternalFilesDir(null), "Playlists")
        val carpeta = File(musica, limpiarNombre(nombrePlaylist))
        carpeta.mkdirs()
        return carpeta
    }
}