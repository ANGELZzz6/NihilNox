package com.example.colorblend.ui.gacha

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.colorblend.R

class DifficultySelectorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_difficulty_selection)
        FullScreenHelper.enable(this)
        iniciarAnimacionesEntrada()

        val btnReanudar = findViewById<Button>(R.id.btnReanudar)

        // Mostrar botón reanudar solo si hay partida guardada
        val prefs = getSharedPreferences("solitaire_save", MODE_PRIVATE)
        if (prefs.getBoolean("has_save", false)) {
            val dificultadGuardada = prefs.getString("difficulty", "FACIL") ?: "FACIL"
            val nombreDif = Difficulty.valueOf(dificultadGuardada).label
            btnReanudar.text = "▶ Reanudar ($nombreDif)"
            btnReanudar.visibility = View.VISIBLE
        }

        btnReanudar.setOnClickListener { v ->
            animarSeleccion(v) {
                val intent = Intent(this, SolitaireActivity::class.java)
                intent.putExtra("RESUME", true)
                startActivity(intent)
            }
        }

        val cards = mapOf(
            R.id.cardFacil to Difficulty.FACIL,
            R.id.cardNormal to Difficulty.NORMAL,
            R.id.cardDificil to Difficulty.DIFICIL,
            R.id.cardExperto to Difficulty.EXPERTO,
            R.id.cardGranMaestro to Difficulty.GRAN_MAESTRO
        )

        cards.forEach { (id, difficulty) ->
            findViewById<View>(id).setOnClickListener { v ->
                animarSeleccion(v) {
                    val intent = Intent(this, SolitaireActivity::class.java)
                    intent.putExtra("DIFFICULTY", difficulty)
                    startActivity(intent)
                }
            }
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    // Entrada: tarjetas aparecen desde la izquierda igual que en Fall
    private fun iniciarAnimacionesEntrada() {
        val ids = listOf(R.id.cardFacil, R.id.cardNormal, R.id.cardDificil,
            R.id.cardExperto, R.id.cardGranMaestro)
        val vistas = ids.map { findViewById<View>(it) }

        vistas.forEachIndexed { i, v ->
            v.translationX = -900f
            v.alpha = 0f
            v.animate()
                .translationX(0f).alpha(1f)
                .setDuration(420)
                .setStartDelay(i * 80L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    if (i == vistas.size - 1) iniciarLoopIdle(vistas)
                }.start()
        }
    }

    private fun iniciarLoopIdle(vistas: List<View>) {
        vistas.forEachIndexed { i, v ->
            v.postDelayed({ animarRespiracion(v) }, i * 150L)
        }
    }

    private fun animarRespiracion(v: View) {
        v.animate()
            .scaleX(1.02f).scaleY(1.02f)
            .setDuration(2000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                v.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(2000)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction { animarRespiracion(v) }
                    .start()
            }.start()
    }

    // Pulso al seleccionar: crece y lanza la pantalla
    private fun animarSeleccion(v: View, action: () -> Unit) {
        SonidoHelper.reproducir(this)
        v.animate()
            .scaleX(0.94f).scaleY(0.94f)
            .setDuration(90)
            .withEndAction {
                v.animate()
                    .scaleX(1.04f).scaleY(1.04f)
                    .setDuration(100)
                    .withEndAction {
                        v.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(80)
                            .withEndAction { action() }
                            .start()
                    }.start()
            }.start()
    }
}
