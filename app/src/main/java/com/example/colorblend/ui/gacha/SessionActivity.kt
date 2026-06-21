package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.UserStatsRepository
import com.example.colorblend.domain.model.LearnCard
import com.example.colorblend.domain.model.LearnQuizQuestion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SessionActivity : AppCompatActivity() {

    private lateinit var viewModel: LearnViewModel
    private var topicId = 0
    private var topicTitulo = ""
    private var modoQuiz = false

    // Estado de sesión
    private var cards = listOf<LearnCard>()
    private var quizQuestions = listOf<LearnQuizQuestion>()
    private var indexActual = 0
    private var cartaVolteada = false
    private var xpSesion = 0
    private var monedasSesion = 0
    private var correctasQuiz = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session)
        FullScreenHelper.enable(this)

        topicId = intent.getIntExtra("TOPIC_ID", 0)
        topicTitulo = intent.getStringExtra("TOPIC_TITULO") ?: ""
        modoQuiz = intent.getBooleanExtra("MODO_QUIZ", false)

        viewModel = ViewModelProvider(this)[LearnViewModel::class.java]

        findViewById<TextView>(R.id.tvSessionTitulo).text = topicTitulo
        observeState()

        if (modoQuiz) viewModel.iniciarQuiz(topicId)
        else viewModel.iniciarSesion(topicId)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LearnUiState.SesionLista -> {
                            viewModel.sessionCards.collect { c ->
                                if (c.isNotEmpty()) { cards = c; mostrarTarjeta() }
                            }
                        }
                        is LearnUiState.QuizListo -> {
                            viewModel.quizQuestions.collect { q ->
                                if (q.isNotEmpty()) { quizQuestions = q; mostrarPreguntaQuiz() }
                            }
                        }
                        is LearnUiState.SesionCompletada -> mostrarResumen(state)
                        is LearnUiState.Error -> {
                            Toast.makeText(this@SessionActivity,
                                state.mensaje, Toast.LENGTH_LONG).show()
                            finish()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    // ── Modo Tarjetas ──────────────────────────────────────────────────
    private fun mostrarTarjeta() {
        if (indexActual >= cards.size) { terminarSesion(); return }
        cartaVolteada = false
        val card = cards[indexActual]

        val layoutTarjeta = findViewById<View>(R.id.layoutTarjeta)
        val layoutQuiz = findViewById<View>(R.id.layoutQuiz)
        val layoutCalificacion = findViewById<View>(R.id.layoutCalificacion)
        layoutTarjeta.visibility = View.VISIBLE
        layoutQuiz.visibility = View.GONE
        layoutCalificacion.visibility = View.GONE

        // Animación de entrada
        val container = findViewById<View>(R.id.cardFlipContainer)
        container.alpha = 0f
        container.translationY = 30f
        container.animate().alpha(1f).translationY(0f)
            .setDuration(300).setInterpolator(android.view.animation.DecelerateInterpolator()).start()

        findViewById<TextView>(R.id.tvSessionProgreso).text =
            "${indexActual + 1} / ${cards.size}"
        findViewById<ProgressBar>(R.id.progressSession).apply {
            max = cards.size; progress = indexActual + 1
        }
        findViewById<TextView>(R.id.tvCardFrente).text = card.frente
        findViewById<TextView>(R.id.tvCardReverso).text = card.reverso
        findViewById<TextView>(R.id.tvCardEjemplo).text =
            if (!card.ejemplo.isNullOrBlank()) "💡 ${card.ejemplo}" else ""
        findViewById<TextView>(R.id.tvCardReverso).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.tvCardEjemplo).visibility = View.GONE
        findViewById<TextView>(R.id.tvCardHint).visibility = View.VISIBLE

        // Tap para voltear
        findViewById<View>(R.id.cardFlipContainer).setOnClickListener {
            if (!cartaVolteada) voltearTarjeta()
        }
    }

    private fun voltearTarjeta() {
        cartaVolteada = true
        val container = findViewById<View>(R.id.cardFlipContainer)
        container.animate().rotationY(90f).setDuration(150).withEndAction {
            findViewById<TextView>(R.id.tvCardReverso).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvCardEjemplo).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvCardHint).visibility = View.GONE
            findViewById<View>(R.id.layoutCalificacion).visibility = View.VISIBLE
            container.animate().rotationY(0f).setDuration(150).start()
        }.start()
    }

    private fun calificarYSiguiente(calificacion: Int) {
        val card = cards[indexActual]
        viewModel.calificarCard(card, calificacion)
        xpSesion += when (calificacion) { 3 -> 15; 2 -> 8; else -> 2 }
        monedasSesion += when (calificacion) { 3 -> 3; 2 -> 1; else -> 0 }
        indexActual++
        animarTransicion { mostrarTarjeta() }
    }

    fun onCalMal(v: View) { calificarYSiguiente(1) }
    fun onCalBien(v: View) { calificarYSiguiente(2) }
    fun onCalPerfecto(v: View) { calificarYSiguiente(3) }

    // ── Modo Quiz ──────────────────────────────────────────────────────
    private fun mostrarPreguntaQuiz() {
        if (indexActual >= quizQuestions.size) { terminarSesion(); return }
        val pregunta = quizQuestions[indexActual]

        findViewById<View>(R.id.layoutTarjeta).visibility = View.GONE
        findViewById<View>(R.id.layoutCalificacion).visibility = View.GONE
        val layoutQuiz = findViewById<View>(R.id.layoutQuiz)
        layoutQuiz.visibility = View.VISIBLE

        // Animación de entrada
        layoutQuiz.alpha = 0f
        layoutQuiz.animate().alpha(1f).setDuration(250).start()

        findViewById<TextView>(R.id.tvSessionProgreso).text =
            "${indexActual + 1} / ${quizQuestions.size}"
        findViewById<ProgressBar>(R.id.progressSession).apply {
            max = quizQuestions.size; progress = indexActual + 1
        }

        findViewById<TextView>(R.id.tvQuizPregunta).text = pregunta.pregunta
        val opciones = mapOf(
            R.id.btnQuizA to ("A" to pregunta.opcionA),
            R.id.btnQuizB to ("B" to pregunta.opcionB),
            R.id.btnQuizC to ("C" to pregunta.opcionC),
            R.id.btnQuizD to ("D" to pregunta.opcionD)
        )
        opciones.forEach { (btnId, opcion) ->
            val btn = findViewById<Button>(btnId)
            btn.text = "${opcion.first}) ${opcion.second}"
            btn.setBackgroundResource(R.drawable.btn_games_gradient)
            btn.isEnabled = true
            btn.setOnClickListener {
                evaluarRespuesta(opcion.first, pregunta, opciones)
            }
        }
        findViewById<TextView>(R.id.tvQuizExplicacion).visibility = View.GONE
    }

    private fun evaluarRespuesta(
        respuesta: String,
        pregunta: LearnQuizQuestion,
        opciones: Map<Int, Pair<String, String>>
    ) {
        val correcta = respuesta == pregunta.respuestaCorrecta
        if (correcta) { correctasQuiz++; xpSesion += 20; monedasSesion += 5 }

        // Colorear respuestas
        opciones.forEach { (btnId, opcion) ->
            val btn = findViewById<Button>(btnId)
            btn.isEnabled = false
            btn.setBackgroundResource(when {
                opcion.first == pregunta.respuestaCorrecta -> R.drawable.btn_gold_action
                opcion.first == respuesta && !correcta -> R.drawable.difficulty_card_bg_orange
                else -> R.drawable.btn_games_gradient
            })
        }

        // Mostrar explicación
        val tvExp = findViewById<TextView>(R.id.tvQuizExplicacion)
        tvExp.text = if (correcta) "✅ ${pregunta.explicacion}"
                     else "❌ Correcto: ${pregunta.respuestaCorrecta}\n${pregunta.explicacion}"
        tvExp.setTextColor(if (correcta) 0xFF88FFAA.toInt() else 0xFFFF6666.toInt())
        tvExp.visibility = View.VISIBLE

        android.os.Handler(mainLooper).postDelayed({
            indexActual++
            animarTransicion { mostrarPreguntaQuiz() }
        }, 2000)
    }

    // ── Fin de sesión ──────────────────────────────────────────────────
    private fun terminarSesion() {
        viewModel.finalizarSesion(topicId, xpSesion, monedasSesion)
    }

    private fun mostrarResumen(state: LearnUiState.SesionCompletada) {
        val db = AppDatabase.getDatabase(this)
        val repo = UserStatsRepository(db.userStatsDao())
        CoroutineScope(Dispatchers.IO).launch {
            repo.addMonedas(monedasSesion)
            repo.addXP(xpSesion)
        }

        val layoutResumen = findViewById<View>(R.id.layoutResumen)
        val layoutTarjeta = findViewById<View>(R.id.layoutTarjeta)
        val layoutQuiz = findViewById<View>(R.id.layoutQuiz)
        layoutTarjeta.visibility = View.GONE
        layoutQuiz.visibility = View.GONE
        layoutResumen.visibility = View.VISIBLE

        // Animación de entrada del resumen
        layoutResumen.alpha = 0f
        layoutResumen.animate().alpha(1f).setDuration(400).start()

        val resumenTexto = if (modoQuiz)
            "Quiz completado\n$correctasQuiz / ${quizQuestions.size} correctas"
        else "Sesión completada\n${cards.size} tarjetas repasadas"

        findViewById<TextView>(R.id.tvResumenTitulo).text = resumenTexto
        findViewById<TextView>(R.id.tvResumenXP).text = "+$xpSesion XP"
        findViewById<TextView>(R.id.tvResumenMonedas).text = "+$monedasSesion 🪙"

        findViewById<Button>(R.id.btnResumenVolver).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnResumenRepetir).setOnClickListener {
            indexActual = 0; xpSesion = 0; monedasSesion = 0; correctasQuiz = 0
            viewModel.resetState()
            if (modoQuiz) viewModel.iniciarQuiz(topicId)
            else viewModel.iniciarSesion(topicId)
            layoutResumen.visibility = View.GONE
        }

        // Si es quiz, regresar automáticamente después de 3 segundos
        if (modoQuiz) {
            android.os.Handler(mainLooper).postDelayed({
                finish()
            }, 3000)
        }
    }

    private fun animarTransicion(onDone: () -> Unit) {
        val container = findViewById<View>(R.id.sessionContainer)
        container.animate().alpha(0f).setDuration(150).withEndAction {
            onDone()
            container.animate().alpha(1f).setDuration(150).start()
        }.start()
    }
}
