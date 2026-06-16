package com.example.colorblend.data.local.repository

import com.example.colorblend.data.local.NutricionDao
import com.example.colorblend.data.local.ResumenDia
import com.example.colorblend.domain.model.AlimentoGuardado
import com.example.colorblend.domain.model.AnalisisDia
import com.example.colorblend.domain.model.PerfilNutricion
import com.example.colorblend.domain.model.RegistroAlimento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class RecompensaDia(val monedas: Int, val mensaje: String)

data class MacrosEstimados(
    val calorias: Int,
    val proteina: Float,
    val carbos: Float,
    val grasas: Float,
    val fibra: Float,
    val azucares: Float
)

class NutricionRepository(private val dao: NutricionDao) {

    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    fun hoy(): String = fmt.format(Date())
    fun fechaHaceNDias(n: Long): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -n.toInt())
        return fmt.format(cal.time)
    }

    // ── Perfil ────────────────────────────────────────────────────────────────
    fun observarPerfil(): Flow<PerfilNutricion?> = dao.observarPerfil()
    suspend fun getPerfil(): PerfilNutricion? = dao.getPerfil()
    suspend fun guardarPerfil(perfil: PerfilNutricion) = dao.guardarPerfil(perfil)

    fun calcularMetas(
        peso: Float, altura: Int, edad: Int,
        sexo: String, nivelActividad: String, objetivo: String
    ): PerfilNutricion {
        val tmb = if (sexo == "Hombre") {
            88.36f + (13.4f * peso) + (4.8f * altura) - (5.7f * edad)
        } else {
            447.6f + (9.2f * peso) + (3.1f * altura) - (4.3f * edad)
        }
        val factorActividad = when (nivelActividad) {
            "Sedentario" -> 1.2f
            "Ligero"     -> 1.375f
            "Moderado"   -> 1.55f
            "Activo"     -> 1.725f
            "Muy activo" -> 1.9f
            else         -> 1.55f
        }
        val tdee = tmb * factorActividad
        val calorias = when (objetivo) {
            "Ganar músculo" -> (tdee + 300).toInt()
            "Bajar peso"    -> (tdee - 500).toInt()
            "Definición"    -> (tdee - 250).toInt()
            else            -> tdee.toInt()
        }
        val proteina = when (objetivo) {
            "Ganar músculo" -> (peso * 2.2f).toInt()
            "Bajar peso"    -> (peso * 2.0f).toInt()
            "Definición"    -> (peso * 2.4f).toInt()
            else            -> (peso * 1.6f).toInt()
        }
        val caloriasProteina  = proteina * 4
        val caloriasRestantes = calorias - caloriasProteina
        val grasas = ((caloriasRestantes * 0.30f) / 9f).toInt()
        val carbos = ((caloriasRestantes * 0.70f) / 4f).toInt()
        val fibra  = if (sexo == "Hombre") 38 else 25
        return PerfilNutricion(
            peso = peso, altura = altura, edad = edad, sexo = sexo,
            objetivo = objetivo, nivelActividad = nivelActividad,
            metaCalorias = calorias, metaProteina = proteina,
            metaCarbos = carbos, metaGrasas = grasas, metaFibra = fibra
        )
    }

    // ── Registros ─────────────────────────────────────────────────────────────
    fun observarAlimentosDia(fecha: String): Flow<List<RegistroAlimento>> =
        dao.observarAlimentosDia(fecha)

    suspend fun agregarAlimento(alimento: RegistroAlimento) {
        dao.insertarAlimento(alimento)
        val yaGuardado = dao.buscarAlimentosGuardados(alimento.nombre)
            .any { it.nombre.equals(alimento.nombre, ignoreCase = true) }
        if (yaGuardado) {
            dao.incrementarUso(alimento.nombre)
        } else {
            dao.insertarAlimentoGuardado(
                AlimentoGuardado(
                    nombre           = alimento.nombre,
                    calorias         = alimento.calorias,
                    proteina         = alimento.proteina,
                    carbos           = alimento.carbos,
                    grasas           = alimento.grasas,
                    fibra            = alimento.fibra,
                    azucares         = alimento.azucares,
                    unidadPorDefecto = alimento.unidad
                )
            )
        }
    }

    suspend fun eliminarAlimento(alimento: RegistroAlimento) = dao.eliminarAlimento(alimento)
    suspend fun getAlimentosDia(fecha: String): List<RegistroAlimento> = dao.getAlimentosDia(fecha)
    suspend fun getResumenSemana(): List<ResumenDia> = dao.getResumenSemana(fechaHaceNDias(6))

    // ── Búsqueda local ────────────────────────────────────────────────────────
    suspend fun buscarAlimentosGuardados(query: String): List<AlimentoGuardado> =
        dao.buscarAlimentosGuardados(query)

    fun observarFrecuentes(): Flow<List<AlimentoGuardado>> = dao.observarFrecuentes()

    // ── Búsqueda remota combinada (OFF + USDA + Nutritionix) ──────────────────
    suspend fun buscarEnFuentesRemotas(query: String): List<AlimentoGuardado> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val offJob  = async { buscarEnOpenFoodFacts(query) }
                val usdaJob = async { buscarEnUSDA(query) }
                val nixJob  = async { buscarEnNutritionix(query) }
                val off  = runCatching { offJob.await() }.getOrDefault(emptyList())
                val usda = runCatching { usdaJob.await() }.getOrDefault(emptyList())
                val nix  = runCatching { nixJob.await() }.getOrDefault(emptyList())
                (off + usda + nix)
                    .distinctBy { it.nombre.lowercase().trim() }
                    .filter { it.calorias > 0 }
                    .take(20)
            }
        }

    // ── Open Food Facts ───────────────────────────────────────────────────────
    private suspend fun buscarEnOpenFoodFacts(query: String): List<AlimentoGuardado> {
        return try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://world.openfoodfacts.org/cgi/search.pl" +
                    "?search_terms=$encoded&search_simple=1&action=process" +
                    "&json=1&page_size=6&fields=product_name,nutriments"
            val response   = java.net.URL(url).readText()
            val json       = org.json.JSONObject(response)
            val products   = json.getJSONArray("products")
            val resultados = mutableListOf<AlimentoGuardado>()
            for (i in 0 until products.length()) {
                val p          = products.getJSONObject(i)
                val nombre     = p.optString("product_name", "").trim()
                if (nombre.isBlank()) continue
                val nutriments = p.optJSONObject("nutriments") ?: continue
                val calorias   = nutriments.optDouble("energy-kcal_100g", 0.0).toInt()
                if (calorias == 0) continue
                resultados.add(AlimentoGuardado(
                    nombre           = nombre,
                    calorias         = calorias,
                    proteina         = nutriments.optDouble("proteins_100g", 0.0).toFloat(),
                    carbos           = nutriments.optDouble("carbohydrates_100g", 0.0).toFloat(),
                    grasas           = nutriments.optDouble("fat_100g", 0.0).toFloat(),
                    fibra            = nutriments.optDouble("fiber_100g", 0.0).toFloat(),
                    azucares         = nutriments.optDouble("sugars_100g", 0.0).toFloat(),
                    unidadPorDefecto = "g"
                ))
            }
            resultados
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    // ── USDA FoodData Central ─────────────────────────────────────────────────
    private suspend fun buscarEnUSDA(query: String): List<AlimentoGuardado> {
        return try {
            val encoded  = java.net.URLEncoder.encode(query, "UTF-8")
            val url      = "https://api.nal.usda.gov/fdc/v1/foods/search" +
                    "?query=$encoded&pageSize=6&api_key=DEMO_KEY"
            val response = java.net.URL(url).readText()
            val json     = org.json.JSONObject(response)
            val foods    = json.optJSONArray("foods") ?: return emptyList()
            val resultados = mutableListOf<AlimentoGuardado>()
            for (i in 0 until foods.length()) {
                val food      = foods.getJSONObject(i)
                val nombre    = food.optString("description", "").trim()
                    .replaceFirstChar { it.uppercase() }
                if (nombre.isBlank()) continue
                val nutrients = food.optJSONArray("foodNutrients") ?: continue
                var calorias = 0; var proteina = 0f; var carbos = 0f
                var grasas   = 0f; var fibra   = 0f; var azucares = 0f
                for (j in 0 until nutrients.length()) {
                    val n  = nutrients.getJSONObject(j)
                    val id = n.optInt("nutrientId", 0)
                    val v  = n.optDouble("value", 0.0).toFloat()
                    when (id) {
                        1008 -> calorias = v.toInt()
                        1003 -> proteina = v
                        1005 -> carbos   = v
                        1004 -> grasas   = v
                        1079 -> fibra    = v
                        2000 -> azucares = v
                    }
                }
                if (calorias == 0) continue
                resultados.add(AlimentoGuardado(
                    nombre = nombre, calorias = calorias, proteina = proteina,
                    carbos = carbos, grasas = grasas, fibra = fibra,
                    azucares = azucares, unidadPorDefecto = "g"
                ))
            }
            resultados
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    // ── Nutritionix ───────────────────────────────────────────────────────────
    private suspend fun buscarEnNutritionix(query: String): List<AlimentoGuardado> {
        return try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url     = "https://trackapi.nutritionix.com/v2/search/instant?query=$encoded"
            val conn    = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("x-app-id",         "43a2bb0a")
            conn.setRequestProperty("x-app-key",        "1fa225d5bb356c381d1f71f3e8a94c29")
            conn.setRequestProperty("x-remote-user-id", "0")
            if (conn.responseCode != 200) return emptyList()
            val json    = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
            val branded = json.optJSONArray("branded") ?: org.json.JSONArray()
            val common  = json.optJSONArray("common")  ?: org.json.JSONArray()
            val resultados = mutableListOf<AlimentoGuardado>()
            for (i in 0 until minOf(common.length(), 5)) {
                val item   = common.getJSONObject(i)
                val nombre = item.optString("food_name", "").trim().replaceFirstChar { it.uppercase() }
                if (nombre.isBlank()) continue
                val cal = item.optDouble("nf_calories", 0.0).toInt()
                if (cal == 0) continue
                resultados.add(AlimentoGuardado(
                    nombre = nombre, calorias = cal,
                    proteina = item.optDouble("nf_protein", 0.0).toFloat(),
                    carbos   = item.optDouble("nf_total_carbohydrate", 0.0).toFloat(),
                    grasas   = item.optDouble("nf_total_fat", 0.0).toFloat(),
                    fibra    = item.optDouble("nf_dietary_fiber", 0.0).toFloat(),
                    azucares = item.optDouble("nf_sugars", 0.0).toFloat(),
                    unidadPorDefecto = "porción"
                ))
            }
            for (i in 0 until minOf(branded.length(), 4)) {
                val item   = branded.getJSONObject(i)
                val nombre = item.optString("food_name", "").trim().replaceFirstChar { it.uppercase() }
                if (nombre.isBlank()) continue
                val cal = item.optDouble("nf_calories", 0.0).toInt()
                if (cal == 0) continue
                resultados.add(AlimentoGuardado(
                    nombre = nombre, calorias = cal,
                    proteina = item.optDouble("nf_protein", 0.0).toFloat(),
                    carbos   = item.optDouble("nf_total_carbohydrate", 0.0).toFloat(),
                    grasas   = item.optDouble("nf_total_fat", 0.0).toFloat(),
                    fibra    = item.optDouble("nf_dietary_fiber", 0.0).toFloat(),
                    azucares = item.optDouble("nf_sugars", 0.0).toFloat(),
                    unidadPorDefecto = "porción"
                ))
            }
            resultados
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    // ── Estimación de macros con IA ───────────────────────────────────────────
    suspend fun estimarMacrosConIA(
        nombrePlato: String,
        pesoGramos: Float,
        groqKey: String
    ): Result<MacrosEstimados> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
Eres un nutricionista experto. Estima las macros nutricionales de este alimento/plato.

Alimento: "$nombrePlato"
Cantidad: ${pesoGramos.toInt()}g

INSTRUCCIONES:
- Si es un plato casero típico (bandeja paisa, sancocho, arroz con pollo, etc.), usa valores promedio realistas
- Los valores deben ser PARA LA CANTIDAD INDICADA, no por 100g
- Sé preciso pero realista, no subestimes ni sobreestimes
- Si el nombre es ambiguo, asume la preparación más común

Responde ÚNICAMENTE con este JSON exacto, sin texto adicional, sin markdown:
{"calorias":350,"proteina":25.5,"carbos":40.0,"grasas":12.0,"fibra":3.5,"azucares":2.0}
            """.trimIndent()

            val requestBody = org.json.JSONObject().apply {
                put("model", "llama-3.3-70b-versatile")
                put("max_tokens", 100)
                put("messages", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            val url  = java.net.URL("https://api.groq.com/openai/v1/chat/completions")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $groqKey")
            conn.doOutput = true
            conn.outputStream.write(requestBody.toString().toByteArray())

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                return@withContext Result.failure(Exception("Error $responseCode"))
            }

            val responseText = conn.inputStream.bufferedReader().readText()
            val content = org.json.JSONObject(responseText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val json = org.json.JSONObject(content)
            Result.success(MacrosEstimados(
                calorias = json.optInt("calorias", 0),
                proteina = json.optDouble("proteina", 0.0).toFloat(),
                carbos   = json.optDouble("carbos", 0.0).toFloat(),
                grasas   = json.optDouble("grasas", 0.0).toFloat(),
                fibra    = json.optDouble("fibra", 0.0).toFloat(),
                azucares = json.optDouble("azucares", 0.0).toFloat()
            ))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ── Recompensa IA ─────────────────────────────────────────────────────────
    suspend fun calcularRecompensaConIA(
        resumenTexto: String,
        perfil: PerfilNutricion,
        groqKey: String
    ): RecompensaDia = withContext(Dispatchers.IO) {
        try {
            val prompt = """
            Eres un sistema de recompensas de una app de nutrición gamificada.
            Analiza el resumen del día COMPLETO y asigna monedas virtuales al usuario.
            
            REGLAS ESTRICTAS:
            - Mínimo siempre: 100 monedas
            - Máximo posible: 270 monedas
            - Escala justa basada en cumplimiento de metas
            
            ESCALA BASE:
            - 100-120: Día muy incompleto o muy alejado de las metas
            - 121-160: Día parcial, cumplió algunas metas
            - 161-200: Buen día, cumplió la mayoría de metas
            - 201-250: Día excelente, cumplió todas o casi todas las metas
            
            BONUS (suman a la escala base):
            - Si superó la proteína entre 10% y 30% de su meta: +10 monedas
            - Si superó la proteína más del 30% de su meta: +20 monedas
            - Si cumplió la fibra (≥100% de su meta): +10 monedas extra
            
            PENALIZACIONES (restan de la escala base):
            - Si superó las calorías en más de 15%: -20 monedas
            - Si superó los azúcares (meta 50g): -15 monedas
            - Si superó las grasas en más de 20%: -15 monedas
            - Si superó los carbos en más de 20%: -10 monedas
            - El mínimo final sigue siendo 100 aunque haya penalizaciones
            
            DATOS DEL DÍA:
            $resumenTexto
            
            Responde ÚNICAMENTE con este JSON exacto, sin texto adicional, sin markdown:
            {"monedas": 150, "mensaje": "¡Cumpliste tus calorías y proteína!"}
            
            El mensaje debe ser corto (máximo 8 palabras), motivador o constructivo.
            """.trimIndent()

            val requestBody = org.json.JSONObject().apply {
                put("model", "llama-3.3-70b-versatile")
                put("max_tokens", 80)
                put("messages", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            val url  = java.net.URL("https://api.groq.com/openai/v1/chat/completions")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $groqKey")
            conn.doOutput = true
            conn.outputStream.write(requestBody.toString().toByteArray())

            if (conn.responseCode != 200) return@withContext RecompensaDia(100, "¡Día registrado!")

            val responseText = conn.inputStream.bufferedReader().readText()
            val content = org.json.JSONObject(responseText)
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
                .trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val jsonResp = org.json.JSONObject(content)
            RecompensaDia(
                monedas = jsonResp.optInt("monedas", 100).coerceIn(100, 250),
                mensaje = jsonResp.optString("mensaje", "¡Buen trabajo!")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            RecompensaDia(100, "¡Día registrado!")
        }
    }

    // ── Análisis IA ───────────────────────────────────────────────────────────
    suspend fun getAnalisisPorFecha(fecha: String): AnalisisDia? = dao.getAnalisisPorFecha(fecha)
    suspend fun getHistorialAnalisis(): List<AnalisisDia> = dao.getHistorialAnalisis()
    fun observarHistorialAnalisis(): Flow<List<AnalisisDia>> = dao.observarHistorialAnalisis()

    suspend fun analizarDiaConIA(
        fecha: String,
        alimentos: List<RegistroAlimento>,
        perfil: PerfilNutricion,
        groqKey: String
    ): Result<AnalisisDia> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            sb.appendLine("Fecha: $fecha")
            sb.appendLine("Perfil: ${perfil.sexo}, ${perfil.edad} años, ${perfil.peso}kg, ${perfil.altura}cm")
            sb.appendLine("Objetivo: ${perfil.objetivo} | Actividad: ${perfil.nivelActividad}")
            sb.appendLine("Metas: ${perfil.metaCalorias}kcal | P:${perfil.metaProteina}g C:${perfil.metaCarbos}g G:${perfil.metaGrasas}g F:${perfil.metaFibra}g")
            sb.appendLine()
            sb.appendLine("Alimentos consumidos:")
            listOf("Desayuno", "Almuerzo", "Cena", "Snack").forEach { cat ->
                val items = alimentos.filter { it.categoria == cat }
                if (items.isNotEmpty()) {
                    sb.appendLine("  $cat:")
                    items.forEach { a ->
                        sb.appendLine("    - ${a.nombre} (${a.cantidad}${a.unidad}): ${a.calorias}kcal | P:${String.format("%.1f", a.proteina)}g C:${String.format("%.1f", a.carbos)}g G:${String.format("%.1f", a.grasas)}g")
                    }
                }
            }
            val totalCal  = alimentos.sumOf { it.calorias }
            val totalProt = alimentos.sumOf { it.proteina.toDouble() }.toFloat()
            val totalCarb = alimentos.sumOf { it.carbos.toDouble() }.toFloat()
            val totalGras = alimentos.sumOf { it.grasas.toDouble() }.toFloat()
            val totalFib  = alimentos.sumOf { it.fibra.toDouble() }.toFloat()
            val totalAzuc = alimentos.sumOf { it.azucares.toDouble() }.toFloat()
            sb.appendLine()
            sb.appendLine("TOTALES DEL DÍA:")
            sb.appendLine("  Calorías: $totalCal / ${perfil.metaCalorias} kcal")
            sb.appendLine("  Proteína: ${String.format("%.1f", totalProt)} / ${perfil.metaProteina}g")
            sb.appendLine("  Carbos:   ${String.format("%.1f", totalCarb)} / ${perfil.metaCarbos}g")
            sb.appendLine("  Grasas:   ${String.format("%.1f", totalGras)} / ${perfil.metaGrasas}g")
            sb.appendLine("  Fibra:    ${String.format("%.1f", totalFib)} / ${perfil.metaFibra}g")
            sb.appendLine("  Azúcares: ${String.format("%.1f", totalAzuc)}g")
            val resumenTexto = sb.toString()

            val prompt = """
Eres un nutricionista experto analizando la alimentación del día actual, que AÚN NO HA TERMINADO.

$resumenTexto

Tu objetivo es ayudar a la persona a COMPLETAR BIEN el resto del día. Responde en español con:

1. 📊 Estado actual del día (calorías y macros vs metas, 2 líneas)
2. 🍽️ Qué le falta comer para completar el día correctamente (sé específico: "Te faltan Xg de proteína, come Y")
3. ✅ Lo que va bien hasta ahora
4. 💡 Una sugerencia concreta de qué comer en la próxima comida

Sé directo, práctico y motivador. Máximo 220 palabras.
            """.trimIndent()

            val requestBody = org.json.JSONObject().apply {
                put("model", "llama-3.3-70b-versatile")
                put("max_tokens", 400)
                put("messages", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }
            val url  = java.net.URL("https://api.groq.com/openai/v1/chat/completions")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $groqKey")
            conn.doOutput = true
            conn.outputStream.write(requestBody.toString().toByteArray())
            val responseCode = conn.responseCode
            val responseText = if (responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: "Error desconocido"
            }
            if (responseCode != 200) {
                return@withContext Result.failure(Exception("Groq error $responseCode: $responseText"))
            }
            val analisisTexto = org.json.JSONObject(responseText)
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim()
            val analisis = AnalisisDia(
                fecha = fecha, resumenTexto = resumenTexto, analisisIA = analisisTexto,
                calorias = totalCal, proteina = totalProt, carbos = totalCarb,
                grasas = totalGras, fibra = totalFib, azucares = totalAzuc
            )
            dao.guardarAnalisis(analisis)
            Result.success(analisis)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    suspend fun analizarDiaCompletadoConIA(
        fecha: String,
        alimentos: List<RegistroAlimento>,
        perfil: PerfilNutricion,
        groqKey: String
    ): Result<AnalisisDia> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            sb.appendLine("Fecha: $fecha")
            sb.appendLine("Perfil: ${perfil.sexo}, ${perfil.edad} años, ${perfil.peso}kg, ${perfil.altura}cm")
            sb.appendLine("Objetivo: ${perfil.objetivo} | Actividad: ${perfil.nivelActividad}")
            sb.appendLine("Metas: ${perfil.metaCalorias}kcal | P:${perfil.metaProteina}g C:${perfil.metaCarbos}g G:${perfil.metaGrasas}g F:${perfil.metaFibra}g")
            sb.appendLine()
            sb.appendLine("Alimentos consumidos:")
            listOf("Desayuno", "Almuerzo", "Cena", "Snack").forEach { cat ->
                val items = alimentos.filter { it.categoria == cat }
                if (items.isNotEmpty()) {
                    sb.appendLine("  $cat:")
                    items.forEach { a ->
                        sb.appendLine("    - ${a.nombre} (${a.cantidad}${a.unidad}): ${a.calorias}kcal | P:${String.format("%.1f", a.proteina)}g C:${String.format("%.1f", a.carbos)}g G:${String.format("%.1f", a.grasas)}g")
                    }
                }
            }
            val totalCal  = alimentos.sumOf { it.calorias }
            val totalProt = alimentos.sumOf { it.proteina.toDouble() }.toFloat()
            val totalCarb = alimentos.sumOf { it.carbos.toDouble() }.toFloat()
            val totalGras = alimentos.sumOf { it.grasas.toDouble() }.toFloat()
            val totalFib  = alimentos.sumOf { it.fibra.toDouble() }.toFloat()
            val totalAzuc = alimentos.sumOf { it.azucares.toDouble() }.toFloat()
            sb.appendLine()
            sb.appendLine("TOTALES DEL DÍA:")
            sb.appendLine("  Calorías: $totalCal / ${perfil.metaCalorias} kcal")
            sb.appendLine("  Proteína: ${String.format("%.1f", totalProt)} / ${perfil.metaProteina}g")
            sb.appendLine("  Carbos:   ${String.format("%.1f", totalCarb)} / ${perfil.metaCarbos}g")
            sb.appendLine("  Grasas:   ${String.format("%.1f", totalGras)} / ${perfil.metaGrasas}g")
            sb.appendLine("  Fibra:    ${String.format("%.1f", totalFib)} / ${perfil.metaFibra}g")
            sb.appendLine("  Azúcares: ${String.format("%.1f", totalAzuc)}g")
            val resumenTexto = sb.toString()

            // Prompt orientado a día COMPLETO — resumen final
            val prompt = """
Eres un nutricionista experto haciendo el resumen FINAL del día de ayer de tu paciente.
El día ya terminó completamente.

$resumenTexto

Responde en español con un resumen final estructurado:

1. 📊 Resumen general del día (cómo fue en calorías y macros vs metas)
2. ✅ Lo que hizo bien
3. ⚠️ Lo que puede mejorar mañana (máximo 2 puntos concretos)
4. 💪 Mensaje motivador de cierre (1 línea)

Sé directo y constructivo. Máximo 200 palabras.
        """.trimIndent()

            val requestBody = org.json.JSONObject().apply {
                put("model", "llama-3.3-70b-versatile")
                put("max_tokens", 400)
                put("messages", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }
            val url  = java.net.URL("https://api.groq.com/openai/v1/chat/completions")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $groqKey")
            conn.doOutput = true
            conn.outputStream.write(requestBody.toString().toByteArray())
            val responseCode = conn.responseCode
            val responseText = if (responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: "Error desconocido"
            }
            if (responseCode != 200) {
                return@withContext Result.failure(Exception("Groq error $responseCode: $responseText"))
            }
            val analisisTexto = org.json.JSONObject(responseText)
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim()

            val analisis = AnalisisDia(
                fecha = fecha, resumenTexto = resumenTexto, analisisIA = analisisTexto,
                calorias = totalCal, proteina = totalProt, carbos = totalCarb,
                grasas = totalGras, fibra = totalFib, azucares = totalAzuc
            )
            dao.guardarAnalisis(analisis)
            Result.success(analisis)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    suspend fun guardarMonedasRecompensa(fecha: String, monedas: Int) =
        dao.guardarMonedasRecompensa(fecha, monedas)

    suspend fun marcarRecompensaReclamada(fecha: String) =
        dao.marcarRecompensaReclamada(fecha)
}