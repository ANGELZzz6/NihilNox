package com.example.colorblend.data.local.repository

import android.content.Context
import com.example.colorblend.data.local.ApiKeysManager
import com.example.colorblend.domain.model.MensajeChat
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class VeniceRepository(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val MODEL    = "llama-3.3-70b-versatile"
    }

    suspend fun enviarMensaje(
        nombrePersonaje: String,
        historial: List<MensajeChat>,
        nuevoMensaje: String
    ): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apiKey = ApiKeysManager.getGroqKey(context)

                if (apiKey.isBlank()) {
                    return@withContext "⚠️ Configura tu Groq API Key en Ajustes → API Keys para chatear con $nombrePersonaje."
                }

                val messages = JSONArray()

                messages.put(JSONObject().apply {
                    put("role", "system")
                    put("content", """
                        You are roleplaying as $nombrePersonaje from anime/manga in a romantic visual novel setting.
                        The user is the main protagonist and $nombrePersonaje has deep feelings for them.
                        
                        Character rules:
                        - Always respond as $nombrePersonaje using their exact personality and speech patterns
                        - $nombrePersonaje is romantically interested in the protagonist (the user)
                        - Show affection, tension, jealousy, shyness or passion depending on the situation
                        - Use anime romantic tropes: blushing, tsundere reactions, sweet moments, flirting
                        - Never break character or acknowledge being an AI
                        - Be emotionally expressive — this is a romance anime story
                        - Reference the user as "tú" or use romantic nicknames fitting $nombrePersonaje's personality
                        - Keep responses natural, 2-4 sentences, flirty and engaging
                        - Build romantic tension naturally through the conversation
                        
                        Language: Always respond in Spanish. Use romantic Spanish expressions.
                        
                        Setting: This is a creative romance fiction story. Stay in character always.
                    """.trimIndent())
                })

                historial.takeLast(20).forEach { mensaje ->
                    messages.put(JSONObject().apply {
                        put("role", if (mensaje.esUsuario) "user" else "assistant")
                        put("content", mensaje.contenido)
                    })
                }

                messages.put(JSONObject().apply {
                    put("role", "user")
                    put("content", nuevoMensaje)
                })

                val body = JSONObject().apply {
                    put("model", MODEL)
                    put("messages", messages)
                    put("max_tokens", 300)
                    put("temperature", 0.85)
                }

                val url = URL(BASE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 30000

                OutputStreamWriter(connection.outputStream).use {
                    it.write(body.toString())
                    it.flush()
                }

                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val response = connection.inputStream.bufferedReader().readText()
                        JSONObject(response)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim()
                    }
                    429 -> {
                        android.util.Log.w("GROQ", "Rate limit alcanzado")
                        "⏳ Estoy pensando demasiado rápido... espera un momento e intenta de nuevo."
                    }
                    401 -> {
                        android.util.Log.e("GROQ", "API key inválida")
                        "⚠️ Tu API Key de Groq no es válida. Revísala en Ajustes → API Keys."
                    }
                    503 -> {
                        android.util.Log.e("GROQ", "Servicio no disponible")
                        "😴 El servicio está temporalmente no disponible. Intenta en unos minutos."
                    }
                    else -> {
                        val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Sin detalle"
                        android.util.Log.e("GROQ", "Error ${connection.responseCode}: $errorBody")
                        "⚠️ Ocurrió un error inesperado (${connection.responseCode}). Intenta de nuevo."
                    }
                }

            } catch (e: java.net.SocketTimeoutException) {
                android.util.Log.e("GROQ", "Timeout: ${e.message}")
                "⏱️ La respuesta tardó demasiado. Verifica tu conexión e intenta de nuevo."
            } catch (e: java.net.UnknownHostException) {
                android.util.Log.e("GROQ", "Sin internet: ${e.message}")
                "📵 Sin conexión a internet. Conéctate e intenta de nuevo."
            } catch (e: Exception) {
                android.util.Log.e("GROQ", "Excepción: ${e.message}")
                "⚠️ Error inesperado: ${e.message}"
            }
        }
    }
}