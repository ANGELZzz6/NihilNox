package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.colorblend.R
import com.example.colorblend.data.local.ApiKeysManager
import kotlinx.coroutines.launch

class NewTopicActivity : AppCompatActivity() {

    private lateinit var viewModel: LearnViewModel

    private val categorias = listOf(
        "🌍 General", "🔬 Ciencia", "📖 Historia", "💻 Tecnología",
        "🎨 Arte", "🗣️ Idiomas", "🧮 Matemáticas", "💼 Negocios", "🎯 Personal"
    )
    private var categoriaSeleccionada = "🌍 General"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_topic)
        FullScreenHelper.enable(this)

        viewModel = ViewModelProvider(this)[LearnViewModel::class.java]

        setupCategorias()
        setupBotones()
        observeState()
        iniciarAnimaciones()
    }

    private fun setupCategorias() {
        val chipGroup = findViewById<LinearLayout>(R.id.chipGroupCategorias)
        categorias.forEach { cat ->
            val chip = TextView(this).apply {
                text = cat
                textSize = 12f
                setTextColor(0xFFAAAAAA.toInt())
                background = getDrawable(R.drawable.chip_categoria_bg)
                setPadding(24, 8, 24, 8)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = 8
                layoutParams = lp
                setOnClickListener { seleccionarCategoria(cat, this, chipGroup) }
            }
            chipGroup.addView(chip)
        }
    }

    private fun seleccionarCategoria(cat: String, chip: TextView, group: LinearLayout) {
        categoriaSeleccionada = cat
        for (i in 0 until group.childCount) {
            val v = group.getChildAt(i) as? TextView ?: continue
            v.setTextColor(0xFFAAAAAA.toInt())
            v.setBackgroundResource(R.drawable.chip_categoria_bg)
        }
        chip.setTextColor(0xFF1A1200.toInt())
        chip.setBackgroundResource(R.drawable.btn_gold_action)
    }

    private fun setupBotones() {
        findViewById<Button>(R.id.btnNewTopicCrear).setOnClickListener {
            val titulo = findViewById<EditText>(R.id.etNewTopicTitulo).text.toString().trim()
            val material = findViewById<EditText>(R.id.etNewTopicMaterial).text.toString().trim()

            if (titulo.isBlank()) {
                findViewById<EditText>(R.id.etNewTopicTitulo).error = "Escribe un tema"
                return@setOnClickListener
            }

            val groqKey = ApiKeysManager.getGroqKey(applicationContext)
            if (groqKey.isBlank()) {
                Toast.makeText(this, "Configura tu clave de Groq primero", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Animación de carga pulsante mientras la IA trabaja
            val btn = findViewById<Button>(R.id.btnNewTopicCrear)
            btn.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).withEndAction {
                btn.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()

            viewModel.crearTema(
                titulo = titulo,
                categoria = categoriaSeleccionada,
                materialUsuario = material.ifBlank { null },
                groqKey = groqKey
            )
        }

        findViewById<Button>(R.id.btnNewTopicVolver).setOnClickListener { finish() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val btnCrear = findViewById<Button>(R.id.btnNewTopicCrear)
                    val progressBar = findViewById<ProgressBar>(R.id.progressNewTopic)
                    val tvEstado = findViewById<TextView>(R.id.tvNewTopicEstado)

                    when (state) {
                        is LearnUiState.Cargando -> {
                            btnCrear.isEnabled = false
                            progressBar.visibility = View.VISIBLE
                            tvEstado.visibility = View.VISIBLE
                            tvEstado.text = state.mensaje
                        }
                        is LearnUiState.TemaCreado -> {
                            Toast.makeText(this@NewTopicActivity,
                                "¡Lección creada!", Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                            finish()
                        }
                        is LearnUiState.Error -> {
                            btnCrear.isEnabled = true
                            progressBar.visibility = View.GONE
                            tvEstado.visibility = View.VISIBLE
                            tvEstado.text = "❌ ${state.mensaje}"
                            tvEstado.setTextColor(0xFFFF5555.toInt())
                        }
                        else -> {
                            btnCrear.isEnabled = true
                            progressBar.visibility = View.GONE
                            tvEstado.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun iniciarAnimaciones() {
        val vistas = listOf(
            R.id.cardNewTopicTitulo, R.id.cardNewTopicCategoria,
            R.id.cardNewTopicMaterial, R.id.btnNewTopicCrear
        ).map { findViewById<View>(it) }

        vistas.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 60f
            v.animate().alpha(1f).translationY(0f)
                .setDuration(400).setStartDelay(i * 80L)
                .setInterpolator(DecelerateInterpolator()).start()
        }
    }
}
