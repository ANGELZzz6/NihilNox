package com.example.colorblend.data.local.repository

import android.content.Context
import com.example.colorblend.domain.model.PersonajeObtenido
import com.example.colorblend.domain.model.Rareza
import org.json.JSONObject
import java.net.URL
import kotlin.random.Random

class SuperheroRepository(private val context: Context) {

    companion object {
        // Sin API key — open source, imágenes en jsDelivr CDN (sin 403)
        private const val BASE_URL = "https://cdn.jsdelivr.net/gh/akabab/superhero-api@0.3.0/api"
        private const val MAX_ID   = 731
    }

    suspend fun fetchPersonajeAleatorio(
        idsUsados: MutableSet<Int>,
        generoFiltro: String? = null
    ): PersonajeObtenido? {
        repeat(10) {
            val id = Random.nextInt(1, MAX_ID)
            if (idsUsados.contains(id)) return@repeat
            idsUsados.add(id)

            return try {
                val response = URL("$BASE_URL/id/$id.json").readText()
                val json     = JSONObject(response)

                val generoApi = json.getJSONObject("appearance")
                    .getString("gender").lowercase()
                val genero = when (generoApi) {
                    "male"   -> "Male"
                    "female" -> "Female"
                    else     -> "Unknown"
                }

                if (generoFiltro != null && genero != generoFiltro) return@repeat

                val nombre     = json.getString("name")
                val stats      = json.getJSONObject("powerstats")
                val poderTotal = listOf(
                    "intelligence", "strength", "speed",
                    "durability", "power", "combat"
                ).sumOf { stat -> stats.optInt(stat, 0) }

                val rareza    = calcularRarezaPorPoder(poderTotal)
                // Imagen md (~320x480) desde jsDelivr — sin 403 garantizado
                val imagenUrl = json.getJSONObject("images").getString("md")
                val publisher = json.getJSONObject("biography")
                    .optString("publisher", "Desconocido")

                PersonajeObtenido(
                    id            = 1_000_000 + id,
                    nombre        = nombre,
                    imagenUrl     = imagenUrl,
                    favoritos     = poderTotal,
                    rareza        = rareza,
                    genero        = genero,
                    categoria     = "superhero",
                    animeTitulo   = publisher,
                    animeCoverUrl = imagenUrl
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        return null
    }

    private fun calcularRarezaPorPoder(poder: Int): Rareza {
        return when {
            poder >= 480 -> Rareza.LEGENDARIO
            poder >= 320 -> Rareza.EPICO
            poder >= 160 -> Rareza.RARO
            else         -> Rareza.COMUN
        }
    }
}