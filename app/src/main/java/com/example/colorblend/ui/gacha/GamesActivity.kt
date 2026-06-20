package com.example.colorblend.ui.gacha

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.colorblend.R

class GamesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)
        FullScreenHelper.enable(this)

        val btnSolitaire = findViewById<Button>(R.id.btnGamesSolitaire)
        val btnLearn = findViewById<Button>(R.id.btnGamesLearn)
        val btnVolver = findViewById<Button>(R.id.btnVolverGames)

        btnSolitaire.setOnClickListener {
            animarBoton(it) {
                startActivity(Intent(this, DifficultySelectorActivity::class.java))
            }
        }

        btnLearn.setOnClickListener {
            animarBoton(it) {
                startActivity(Intent(this, LearnActivity::class.java))
            }
        }

        btnVolver.setOnClickListener {
            animarBoton(it) {
                finish()
            }
        }
    }

    private fun animarBoton(view: View, action: () -> Unit) {
        SonidoHelper.reproducir(this)
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80)
                    .withEndAction { action() }
                    .start()
            }
            .start()
    }
}
