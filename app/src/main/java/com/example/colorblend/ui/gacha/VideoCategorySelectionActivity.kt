package com.example.colorblend.ui.gacha

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.colorblend.R

class VideoCategorySelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_category_selection)
        FullScreenHelper.enable(this)

        val cardReflexion = findViewById<View>(R.id.cardReflexion)
        val cardFuego = findViewById<View>(R.id.cardFuego)
        val btnNews = findViewById<View>(R.id.btnNews)

        // Configurar clics
        cardReflexion.setOnClickListener { lanzarFall("REFLEXION") }
        cardFuego.setOnClickListener { lanzarFall("FUEGO") }
        btnNews.setOnClickListener { lanzarFall("NEWS") }

        // Animaciones de entrada
        iniciarAnimaciones(cardReflexion, cardFuego, btnNews)
    }

    private fun iniciarAnimaciones(reflexion: View, fuego: View, news: View) {
        // Reflexión entra desde la izquierda
        reflexion.translationX = -800f
        reflexion.alpha = 0f
        reflexion.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Fuego entra desde la derecha
        fuego.translationX = 800f
        fuego.alpha = 0f
        fuego.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // News entra con fade in después
        news.alpha = 0f
        news.animate()
            .alpha(1f)
            .setStartDelay(300)
            .setDuration(400)
            .start()
    }

    private fun lanzarFall(categoria: String) {
        SonidoHelper.reproducir(this)
        val intent = Intent(this, FallActivity::class.java).apply {
            putExtra("video_category", categoria)
        }
        startActivity(intent)
        finish() // Opcional: cerrar esta pantalla al elegir
    }
}
