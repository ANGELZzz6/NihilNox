package com.example.colorblend.ui.gacha

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.UserStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DifficultySelectorActivity : AppCompatActivity() {

    private lateinit var repository: UserStatsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_difficulty_selection)
        FullScreenHelper.enable(this)

        val db = AppDatabase.getDatabase(this)
        repository = UserStatsRepository(db.userStatsDao())

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

    override fun onResume() {
        super.onResume()
        cargarPerfilYXP()
    }

    private fun cargarPerfilYXP() {
        val ivAvatar = findViewById<ImageView>(R.id.ivDiffAvatar)
        val tvEmoji = findViewById<TextView>(R.id.tvDiffEmoji)
        val tvNick = findViewById<TextView>(R.id.tvDiffNick)
        val progressXP = findViewById<ProgressBar>(R.id.progressDiffXP)
        val tvNivel = findViewById<TextView>(R.id.tvDiffNivel)

        val prefs = getSharedPreferences("perfil_prefs", MODE_PRIVATE)
        val nick = prefs.getString("nick", "Jugador") ?: "Jugador"
        val avatarPath = prefs.getString("avatar_path", null)
        tvNick.text = nick

        CoroutineScope(Dispatchers.IO).launch {
            val stats = repository.getStatsOnce()
            val nivel = stats?.nivel ?: 1
            val xp = stats?.xp ?: 0
            val xpEnNivel = xp % 2000
            val config = AvatarHelper.getMarcoConfig(nivel)

            var avatarBitmap: android.graphics.Bitmap? = null
            if (!avatarPath.isNullOrEmpty() && File(avatarPath).exists()) {
                val bmp = android.graphics.BitmapFactory.decodeFile(avatarPath)
                avatarBitmap = AvatarHelper.dibujarAvatarConMarco(
                    this@DifficultySelectorActivity, bmp, nivel, 56)
            }

            withContext(Dispatchers.Main) {
                val ivAlas = findViewById<ImageView>(R.id.ivDiffAlas)

                if (avatarBitmap != null) {
                    ivAvatar.setImageBitmap(avatarBitmap)
                    ivAvatar.visibility = View.VISIBLE
                    tvEmoji.visibility = View.GONE

                    // Alas separadas
                    val alasBitmap = AvatarHelper.dibujarAlas(
                        this@DifficultySelectorActivity, nivel, 52)
                    if (alasBitmap != null) {
                        ivAlas.setImageBitmap(alasBitmap)
                        ivAlas.visibility = View.VISIBLE
                    }
                } else {
                    ivAvatar.visibility = View.GONE
                    ivAlas.visibility = View.GONE
                    tvEmoji.visibility = View.VISIBLE
                }
                tvNivel.text = "Nv.$nivel ${config.nombre}"
                progressXP.max = 2000
                ObjectAnimator.ofInt(progressXP, "progress", 0, xpEnNivel)
                    .apply { duration = 800; interpolator = DecelerateInterpolator(); start() }
            }
        }
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
