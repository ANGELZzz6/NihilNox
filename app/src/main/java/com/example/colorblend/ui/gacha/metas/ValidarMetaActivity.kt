package com.example.colorblend.ui.gacha.metas

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.ui.gacha.metas.viewmodels.MetaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.colorblend.data.local.ApiKeysManager

data class ChatMensaje(val texto: String, val esIA: Boolean)

class ChatAdapter(
    private val mensajes: MutableList<ChatMensaje> = mutableListOf()
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    fun agregar(mensaje: ChatMensaje) {
        mensajes.add(mensaje)
        notifyItemInserted(mensajes.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_mensaje, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(mensajes[position])

    override fun getItemCount() = mensajes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(m: ChatMensaje) {
            val layoutIA      = itemView.findViewById<View>(R.id.layoutIA)
            val layoutUsuario = itemView.findViewById<View>(R.id.layoutUsuario)
            val tvIA          = itemView.findViewById<TextView>(R.id.tvMensajeIA)
            val tvUsuario     = itemView.findViewById<TextView>(R.id.tvMensajeUsuario)
            if (m.esIA) {
                layoutIA.visibility      = View.VISIBLE
                layoutUsuario.visibility = View.GONE
                tvIA.text = m.texto
            } else {
                layoutIA.visibility      = View.GONE
                layoutUsuario.visibility = View.VISIBLE
                tvUsuario.text = m.texto
            }
        }
    }
}

class ValidarMetaActivity : AppCompatActivity() {

    companion object {
        private const val TAG      = "ValidarMetaIA"
        private const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val MODELO   = "llama-3.3-70b-versatile"
    }

    private val metaViewModel: MetaViewModel by viewModels()
    private val historial = mutableListOf<Pair<String, String>>() // role, texto

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var tvTyping: TextView
    private lateinit var btnEnviar: Button
    private lateinit var etMensaje: EditText

    private var preguntasHechas = 0
    private var maxPreguntas    = 5

    private var metaId          = 0
    private var metaTitulo      = ""
    private var metaDescripcion = ""
    private var metaTipo        = ""
    private var metaObjetivo    = 0
    private var metaProgreso    = 0
    private var metaRacha       = 0

    private val sistemaContexto get(): String {
        val tipoExplicado = if (metaTipo == "DIARIA") {
            "Es una meta DIARIA — el usuario debe realizarla cada día. " +
                    "Racha actual: $metaRacha días consecutivos cumplidos. " +
                    "Progreso total: $metaProgreso de $metaObjetivo días completados."
        } else {
            "Es una meta ACUMULATIVA — el usuario va sumando avances poco a poco hasta llegar al objetivo total. " +
                    "No tiene que completarla toda hoy, solo registrar el avance de hoy. " +
                    "Progreso actual: $metaProgreso de $metaObjetivo acumulado hasta ahora."
        }
        return """
            Eres un asistente motivacional que valida si el usuario avanzó en su meta "$metaTitulo".
            ${if (metaDescripcion.isNotEmpty()) "Descripción: $metaDescripcion." else ""}
            $tipoExplicado
            Valida que el usuario hizo algo concreto hoy relacionado con la meta, sin importar cuánto.
        """.trimIndent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_validar_meta)

        metaId          = intent.getIntExtra("metaId", 0)
        metaTitulo      = intent.getStringExtra("metaTitulo") ?: "Meta"
        metaDescripcion = intent.getStringExtra("metaDescripcion") ?: ""
        metaTipo        = intent.getStringExtra("metaTipo") ?: ""
        metaObjetivo    = intent.getIntExtra("metaObjetivo", 0)
        metaProgreso    = intent.getIntExtra("metaProgreso", 0)
        metaRacha       = intent.getIntExtra("metaRacha", 0)
        maxPreguntas = intent.getIntExtra("maxPreguntas", 4)

        findViewById<TextView>(R.id.tvMetaTitulo).text = metaTitulo

        recycler  = findViewById(R.id.chatRecycler)
        etMensaje = findViewById(R.id.etMensaje)
        btnEnviar = findViewById(R.id.btnEnviar)
        tvTyping  = findViewById(R.id.tvTyping)

        chatAdapter = ChatAdapter()
        recycler.adapter = chatAdapter
        recycler.layoutManager = LinearLayoutManager(this).also { it.stackFromEnd = true }

        iniciarConversacion()

        btnEnviar.setOnClickListener {
            val texto = etMensaje.text.toString().trim()
            if (texto.isEmpty()) return@setOnClickListener
            etMensaje.setText("")
            agregarMensaje(ChatMensaje(texto, esIA = false))
            responderConIA(texto)
        }
    }

    private fun iniciarConversacion() {
        setUI(cargando = true)
        lifecycleScope.launch {
            val primerPrompt = if (metaTipo == "DIARIA") {
                "El usuario dice que cumplió su meta diaria hoy. Haz UNA sola pregunta corta y específica para verificar que la realizó hoy. Sé amigable, máximo 2 oraciones."
            } else {
                "El usuario quiere registrar su avance de hoy en su meta acumulativa. " +
                        "Recuerda que NO tiene que completar todo el objetivo hoy, solo avanzar un poco. " +
                        "Pregúntale cuánto avanzó hoy específicamente. Sé amigable, máximo 2 oraciones."
            }
            val respuesta = llamarGroq(primerPrompt)
            setUI(cargando = false)
            agregarMensaje(ChatMensaje(
                respuesta ?: "¡Hola! Cuéntame, ¿cuánto avanzaste hoy con tu meta?",
                esIA = true
            ))
            preguntasHechas++
        }
    }

    private fun responderConIA(textoUsuario: String) {
        if (preguntasHechas >= maxPreguntas) return
        setUI(cargando = true)

        lifecycleScope.launch {
            val esUltima = preguntasHechas >= maxPreguntas - 1

            val minimoMonedas = if (metaTipo == "DIARIA") 50 else 5
            val maximoMonedas = minimoMonedas * 3
            val instruccion = if (!esUltima) {
                if (metaTipo == "DIARIA") {
                    "Haz UNA pregunta corta de seguimiento para verificar que realizó la actividad hoy. Máximo 2 oraciones."
                } else {
                    "Haz UNA pregunta corta de seguimiento sobre el avance de hoy, sin exigir que complete todo el objetivo. Máximo 2 oraciones."
                }
            } else {
                "Esta es la última ronda. Da un veredicto final motivacional en máximo 3 oraciones. " +
                        "IMPORTANTE: incluye exactamente la palabra APROBADO si el usuario cumplió la meta, " +
                        "o RECHAZADO si no hay suficiente evidencia. " +
                        "Si es APROBADO, añade al final en nueva línea: MONEDAS:XX " +
                        "donde XX es un número entre $minimoMonedas y $maximoMonedas según qué tan bien " +
                        "describió su avance (más detalle = más monedas). Progreso: ${metaProgreso}/${metaObjetivo}."
            }

            val promptConInstruccion = "Usuario: \"$textoUsuario\"\n\n$instruccion"
            val respuesta = llamarGroq(promptConInstruccion)

            setUI(cargando = false)

            val textoCompleto = respuesta ?: "Hubo un error de red. Verifica tu conexión e intenta de nuevo."
            val monedasIA     = extraerMonedas(textoCompleto, if (metaTipo == "DIARIA") 50 else 5)
            val textoRespuesta = textoCompleto.replace(Regex("\\nMONEDAS:\\d+"), "").trim()

            agregarMensaje(ChatMensaje(textoRespuesta, esIA = true))
            preguntasHechas++

            if (esUltima) {
                val aprobado = textoCompleto.uppercase().contains("APROBADO")
                manejarVeredicto(aprobado, monedasIA)
            }
        }
    }

    private fun extraerMonedas(texto: String, minimo: Int): Int {
        val match = Regex("MONEDAS:(\\d+)").find(texto.uppercase())
        return match?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(minimo) ?: minimo
    }

    private fun manejarVeredicto(aprobado: Boolean, monedas: Int) {
        etMensaje.isEnabled = false
        btnEnviar.isEnabled = false

        if (aprobado) {
            lifecycleScope.launch {
                val meta = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(applicationContext).metaDao().getMetaById(metaId)
                }
                if (meta != null) {
                    if (metaTipo == "DIARIA") {
                        metaViewModel.cumplirMeta(meta, monedas)
                    } else {
                        metaViewModel.sumarProgreso(meta, 1, monedas)
                    }
                }
                delay(600)
                agregarMensaje(ChatMensaje(
                    "🏆 ¡Meta validada! Has ganado $monedas monedas. ¡Sigue así!",
                    esIA = true
                ))
                configurarBotonFinal(verde = true)
            }
        } else {
            agregarMensaje(ChatMensaje(
                "💪 Parece que aún no completaste la meta. ¡Vuelve cuando hayas avanzado más!",
                esIA = true
            ))
            configurarBotonFinal(verde = false)
        }
    }

    private fun configurarBotonFinal(verde: Boolean) {
        btnEnviar.post {
            btnEnviar.text = "← Volver"
            btnEnviar.isEnabled = true
            btnEnviar.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(if (verde) "#4CAF50" else "#555577")
            )
            btnEnviar.setOnClickListener {
                if (verde) setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private suspend fun llamarGroq(prompt: String): String? =
        withContext(Dispatchers.IO) {
            historial.add("user" to prompt)

            val messages = JSONArray().apply {
                // Contexto del sistema
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", sistemaContexto)
                })
                // Historial de conversación
                historial.forEach { (role, texto) ->
                    put(JSONObject().apply {
                        put("role", role)
                        put("content", texto)
                    })
                }
            }

            val body = JSONObject().apply {
                put("model", MODELO)
                put("messages", messages)
                put("temperature", 0.7)
                put("max_tokens", 1024)
            }.toString()

            try {
                val groqKey = ApiKeysManager.getGroqKey(applicationContext)
                if (groqKey.isBlank()) {
                    Log.e(TAG, "Groq key no configurada")
                    historial.removeLastOrNull()
                    return@withContext "⚠️ Configura tu Groq API Key en Ajustes → API Keys para usar esta función."
                }

                val conn = (java.net.URL(GROQ_URL).openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $groqKey")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout    = 15000
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }

                val code = conn.responseCode
                Log.d(TAG, "Groq response: $code")

                if (code == 429) {
                    historial.removeLastOrNull()
                    return@withContext "⏳ Límite de Groq alcanzado, espera un momento e intenta de nuevo."
                }

                if (code != 200) {
                    val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
                    Log.e(TAG, "Groq error $code: $err")
                    historial.removeLastOrNull()
                    return@withContext null
                }

                val texto = JSONObject(conn.inputStream.bufferedReader().readText())
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()

                Log.d(TAG, "Groq OK: $texto")
                historial.add("assistant" to texto)
                texto

            } catch (e: Exception) {
                Log.e(TAG, "Groq excepción: ${e.message}")
                historial.removeLastOrNull()
                null
            }
        }

    private fun agregarMensaje(mensaje: ChatMensaje) {
        chatAdapter.agregar(mensaje)
        recycler.scrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun setUI(cargando: Boolean) {
        tvTyping.visibility = if (cargando) View.VISIBLE else View.GONE
        btnEnviar.isEnabled = !cargando
    }
}