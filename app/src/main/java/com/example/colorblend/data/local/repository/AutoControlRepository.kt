package com.example.colorblend.data.local.repository

import android.content.Context
import com.example.colorblend.data.local.ApiKeysManager
import com.example.colorblend.data.local.AutoControlDao
import com.example.colorblend.domain.model.AutoControlProfile
import com.example.colorblend.domain.model.AutoControlSession
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class AutoControlRepository(
    private val context: Context,
    private val dao: AutoControlDao
) {

    companion object {
        private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val MODEL    = "llama-3.3-70b-versatile"
    }

    val perfil: Flow<AutoControlProfile?> = dao.getProfile()
    val sesiones: Flow<List<AutoControlSession>> = dao.getAllSessions()

    suspend fun guardarPerfil(profile: AutoControlProfile) = dao.insertProfile(profile)
    suspend fun guardarSesion(session: AutoControlSession) = dao.insertSession(session)

    suspend fun generarPlanIA(
        frecuencia: String,
        objetivo: String,
        triggers: String
    ): Result<String> {
        val prompt = """
            Actúa como un coach experto en optimización de dopamina y disciplina personal. 
            El usuario desea regular un hábito impulsivo que afecta su energía y enfoque.
            
            Contexto del usuario:
            - Frecuencia actual: $frecuencia
            - Objetivo: $objetivo
            - Disparadores identificados: $triggers
            
            Instrucciones:
            1. Genera un plan de acción breve (máximo 150 palabras) enfocado en la salud mental y el rendimiento.
            2. Identifica estrategias para manejar los disparadores.
            3. Usa un lenguaje profesional, motivador y centrado en el bienestar.
            4. Responde en Español.
            
            Nota: Enfócate en la regulación de impulsos y mejora de la productividad.
        """.trimIndent()

        val respuesta = llamarAGroq(prompt)
        return if (respuesta.startsWith("Error")) {
            Result.failure(Exception(respuesta))
        } else {
            Result.success(respuesta)
        }
    }

    suspend fun consultarIA(
        perfil: AutoControlProfile,
        horaActual: String,
        duracion: Int,
        historial: List<AutoControlSession>
    ): Result<Triple<Boolean, String, String>> {
        val historialTexto = historial.take(5).joinToString("\n") {
            "- ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(it.fecha)}: ${if(it.aprobado) "Aprobado" else "Denegado"} (${it.duracionSolicitada} min)"
        }

        val prompt = """
            Actúa como un asistente de auto-control y salud conductual. 
            El usuario está considerando realizar una actividad que busca regular según su plan de bienestar.
            
            Datos del usuario:
            - Objetivo: ${perfil.objetivoPrincipal}
            - Plan actual: ${perfil.planIA}
            
            Historial reciente:
            $historialTexto
            
            Consulta actual:
            - Hora: $horaActual
            - Duración estimada de la actividad: $duracion minutos
            
            Tu tarea:
            1. Determina si esta actividad en este momento y con esa duración es coherente con su plan de mejora y su historial reciente.
            2. Si es aceptable, da una recomendación breve.
            3. Si no es recomendable, sugiere una actividad alternativa (ej: ejercicio, lectura, meditación) y explica por qué en términos de energía y enfoque.
            
            Formato de respuesta:
            [DECISION: SI/NO]
            [MOTIVO: Breve explicación técnica/conductual]
            [MENSAJE: Tu consejo empático en español]
        """.trimIndent()

        val respuesta = llamarAGroq(prompt)
        if (respuesta.startsWith("Error")) return Result.failure(Exception(respuesta))
        
        val aprobado = respuesta.contains("[DECISION: SI]", ignoreCase = true)
        val motivo = respuesta.substringAfter("[MOTIVO:").substringBefore("[").trim()
        val mensaje = respuesta.substringAfter("[MENSAJE:").trim()
        
        return Result.success(Triple(aprobado, motivo, mensaje))
    }

    suspend fun preguntarIA(
        perfil: AutoControlProfile,
        pregunta: String,
        historial: List<AutoControlSession>
    ): Result<String> {
        val historialTexto = historial.take(5).joinToString("\n") {
            "- ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(it.fecha)}: ${if(it.aprobado) "Aprobado" else "Denegado"} (${it.duracionSolicitada} min)"
        }

        val prompt = """
            Actúa como un mentor de disciplina y salud mental. 
            El usuario te hace una pregunta contextual sobre su plan de auto-control.
            
            Datos del usuario:
            - Objetivo: ${perfil.objetivoPrincipal}
            - Plan: ${perfil.planIA}
            
            Historial reciente:
            $historialTexto
            
            Pregunta del usuario: $pregunta
            
            Instrucciones:
            1. Responde de forma breve y directa.
            2. Ten en cuenta sus planes futuros o compromisos si los menciona (ej. gym, trabajo, sueño).
            3. Sé motivador pero firme en los principios de salud conductual.
            4. Responde en Español.
        """.trimIndent()

        val respuesta = llamarAGroq(prompt)
        return if (respuesta.startsWith("Error")) {
            Result.failure(Exception(respuesta))
        } else {
            Result.success(respuesta)
        }
    }

    private suspend fun llamarAGroq(prompt: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apiKey = ApiKeysManager.getGroqKey(context)
                if (apiKey.isBlank()) return@withContext "Error: API Key no configurada"

                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                }

                val body = JSONObject().apply {
                    put("model", MODEL)
                    put("messages", messages)
                    put("max_tokens", 500)
                    put("temperature", 0.7)
                }

                val url = URL(BASE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.doOutput = true

                OutputStreamWriter(connection.outputStream).use {
                    it.write(body.toString())
                    it.flush()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    JSONObject(response)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                } else {
                    "Error al conectar con la IA (${connection.responseCode})"
                }
            } catch (e: Exception) {
                "Error de red: ${e.message}"
            }
        }
    }
}
