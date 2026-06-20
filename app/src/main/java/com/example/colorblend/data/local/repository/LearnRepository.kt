package com.example.colorblend.data.local.repository

import android.content.Context
import com.example.colorblend.data.local.LearnDao
import com.example.colorblend.domain.model.LearnCard
import com.example.colorblend.domain.model.LearnQuizQuestion
import com.example.colorblend.domain.model.LearnTopic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LearnRepository(
    private val dao: LearnDao
) {

    // ── Algoritmo SM-2 (Spaced Repetition) ────────────────────────────
    fun calcularProximoRepaso(card: LearnCard, calificacion: Int): LearnCard {
        // calificacion: 1=mal, 2=bien, 3=perfecto
        val nuevaFacilidad = when (calificacion) {
            1 -> maxOf(1.3f, card.facilidad - 0.2f)
            2 -> card.facilidad
            3 -> card.facilidad + 0.1f
            else -> card.facilidad
        }.coerceIn(1.3f, 4.0f)

        val nuevasRepeticiones = if (calificacion == 1) 0 else card.repeticiones + 1

        val nuevoIntervalo = when {
            calificacion == 1 -> 1
            nuevasRepeticiones == 1 -> 1
            nuevasRepeticiones == 2 -> 3
            else -> (card.intervalo * nuevaFacilidad).toInt().coerceAtLeast(1)
        }

        val proximoRepaso = System.currentTimeMillis() +
                (nuevoIntervalo * 24 * 60 * 60 * 1000L)

        return card.copy(
            facilidad = nuevaFacilidad,
            repeticiones = nuevasRepeticiones,
            intervalo = nuevoIntervalo,
            proximoRepaso = proximoRepaso,
            ultimaCalificacion = calificacion
        )
    }

    // ── Generar contenido completo con Groq (1 sola llamada) ──────────
    suspend fun generarContenidoTema(
        titulo: String,
        categoria: String,
        materialUsuario: String?,
        groqKey: String,
        context: Context
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val contextoExtra = if (!materialUsuario.isNullOrBlank())
                "\nMaterial adicional del usuario:\n$materialUsuario" else ""

            val prompt = """
Eres un experto en pedagogía. Genera contenido de aprendizaje sobre: "$titulo" (categoría: $categoria).$contextoExtra

Responde ÚNICAMENTE con este JSON, sin texto extra, sin markdown:
{
  "descripcion": "resumen del tema en 2-3 oraciones claras",
  "tarjetas": [
    {"frente": "concepto o pregunta", "reverso": "explicación clara", "ejemplo": "ejemplo concreto"},
    {"frente": "...", "reverso": "...", "ejemplo": "..."}
  ],
  "quiz": [
    {
      "pregunta": "pregunta de opción múltiple",
      "opcionA": "...", "opcionB": "...", "opcionC": "...", "opcionD": "...",
      "respuestaCorrecta": "A",
      "explicacion": "por qué es correcta"
    }
  ]
}

Reglas:
- Exactamente 8 tarjetas, conceptos progresivos de básico a avanzado
- Exactamente 5 preguntas de quiz con 4 opciones cada una
- Lenguaje claro, directo, sin tecnicismos innecesarios
- Las tarjetas deben poder estudiarse en 5-10 minutos en total
""".trimIndent()

            val requestBody = org.json.JSONObject().apply {
                put("model", "llama3-8b-8192")
                put("max_tokens", 2000)
                put("temperature", 0.7)
                put("messages", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            val url = java.net.URL("https://api.groq.com/openai/v1/chat/completions")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $groqKey")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.outputStream.write(requestBody.toString().toByteArray())

            val responseText = if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText()
                return@withContext Result.failure(Exception("Groq error ${conn.responseCode}: $error"))
            }

            val content = org.json.JSONObject(responseText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = org.json.JSONObject(content)

            // Guardar topic
            val topic = LearnTopic(
                titulo = titulo,
                descripcion = json.getString("descripcion"),
                categoria = categoria,
                materialUsuario = materialUsuario,
                fechaCreacion = System.currentTimeMillis()
            )
            val topicId = dao.insertTopic(topic).toInt()

            // Guardar tarjetas
            val tarjetasJson = json.getJSONArray("tarjetas")
            val tarjetas = (0 until tarjetasJson.length()).map { i ->
                val t = tarjetasJson.getJSONObject(i)
                LearnCard(
                    topicId = topicId,
                    frente = t.getString("frente"),
                    reverso = t.getString("reverso"),
                    ejemplo = t.optString("ejemplo").ifBlank { null },
                    proximoRepaso = System.currentTimeMillis()
                )
            }
            dao.insertCards(tarjetas)

            // Guardar quiz
            val quizJson = json.getJSONArray("quiz")
            val preguntas = (0 until quizJson.length()).map { i ->
                val q = quizJson.getJSONObject(i)
                LearnQuizQuestion(
                    topicId = topicId,
                    pregunta = q.getString("pregunta"),
                    opcionA = q.getString("opcionA"),
                    opcionB = q.getString("opcionB"),
                    opcionC = q.getString("opcionC"),
                    opcionD = q.getString("opcionD"),
                    respuestaCorrecta = q.getString("respuestaCorrecta"),
                    explicacion = q.getString("explicacion")
                )
            }
            dao.insertQuizQuestions(preguntas)

            Result.success(topicId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Operaciones del DAO ────────────────────────────────────────────
    fun getAllTopics() = dao.getAllTopics()

    suspend fun getTopicById(id: Int) = dao.getTopicById(id)

    suspend fun getCardsParaRepasar(topicId: Int): List<LearnCard> =
        dao.getCardsParaRepasar(topicId, System.currentTimeMillis())

    suspend fun getCardsParaSesion(topicId: Int): List<LearnCard> {
        val paraRepasar = dao.getCardsParaRepasar(topicId, System.currentTimeMillis())
        // Si hay menos de 5 para repasar, completar con nuevas
        return if (paraRepasar.size >= 5) paraRepasar.take(10)
        else {
            val todasLasCards = dao.getCardsByTopic(topicId)
            val nuevas = todasLasCards
                .filter { it.repeticiones == 0 }
                .take(10 - paraRepasar.size)
            (paraRepasar + nuevas).distinctBy { it.id }
        }
    }

    suspend fun actualizarCard(card: LearnCard, calificacion: Int) {
        val cardActualizada = calcularProximoRepaso(card, calificacion)
        dao.updateCard(cardActualizada)
    }

    suspend fun getQuizAleatorio(topicId: Int) = dao.getQuizAleatorio(topicId)

    suspend fun actualizarDominio(topicId: Int) {
        val topic = dao.getTopicById(topicId) ?: return
        val total = dao.contarCards(topicId)
        val dominadas = dao.contarCardsDominadas(topicId)
        val dominio = if (total > 0) dominadas.toFloat() / total else 0f

        val hoy = System.currentTimeMillis()
        val ayer = hoy - 24 * 60 * 60 * 1000L
        val nuevaRacha = if (topic.ultimaRepaso >= ayer) topic.rachaEstudio + 1
                         else 1

        dao.updateTopic(topic.copy(
            dominioTotal = dominio,
            ultimaRepaso = hoy,
            totalSesiones = topic.totalSesiones + 1,
            rachaEstudio = nuevaRacha
        ))
    }

    suspend fun archivarTopic(id: Int) = dao.archivarTopic(id)
}
