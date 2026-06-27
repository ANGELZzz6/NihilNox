package com.example.colorblend.ui.gacha

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.UserStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DashboardActivity : AppCompatActivity() {

    private val PREFS_PERFIL = "perfil_prefs"
    private val KEY_AVATAR = "avatar_path"
    private val KEY_NICK = "nick"

    private val PREFS_NAME       = "block_notas_prefs"
    private val KEY_PASSWORD     = "password"
    private val DEFAULT_PASSWORD = "1234"

    private lateinit var ivAvatar: ImageView
    private lateinit var tvAvatarEmoji: TextView
    private lateinit var tvNick: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        FullScreenHelper.enable(this)

        ivAvatar = findViewById(R.id.ivDashAvatar)
        tvAvatarEmoji = findViewById(R.id.tvDashAvatarEmoji)
        tvNick = findViewById(R.id.tvDashNick)

        // ── Botones ───────────────────────────────────────────────────────
        findViewById<Button>(R.id.btnDashGacha).setOnClickListener {
            animarBoton(it) {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnDashReproductor).setOnClickListener {
            animarBoton(it) {
                startActivity(Intent(this, ReproductorActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnDashBlockNotas).setOnClickListener {
            animarBoton(it) {
                abrirBlockNotas()
            }
        }

        findViewById<Button>(R.id.btnDashNutricion).setOnClickListener {
            animarBoton(it) {
                startActivity(Intent(this, NutricionActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnDashPerfil).setOnClickListener {
            animarBoton(it) {
                startActivity(Intent(this, PerfilActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnDashFall).setOnClickListener {
            animarBoton(it) {
                lifecycleScope.launch {
                    val dao = AppDatabase.getDatabase(this@DashboardActivity).fallVideoDao()
                    val count = dao.getCategorizedCount()
                    if (count > 0) {
                        startActivity(Intent(this@DashboardActivity, VideoCategorySelectionActivity::class.java))
                    } else {
                        startActivity(Intent(this@DashboardActivity, FallActivity::class.java).apply {
                            putExtra("video_category", "RANDOM")
                        })
                    }
                }
            }
        }

        findViewById<Button>(R.id.btnDashGames).setOnClickListener {
            animarBoton(it) {
                startActivity(Intent(this, GamesActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnDashLearn).setOnClickListener {
            animarBoton(it) {
                startActivity(Intent(this, LearnActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnHabitos).setOnClickListener {
            animarBoton(it) {
                startActivity(Intent(this, HabitosActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        findViewById<Button>(R.id.btnDashApiKeys).setOnClickListener {
            animarBoton(it) {
                startActivity(Intent(this, ApiKeysActivity::class.java))
            }
        }

        // ── Interceptar Intent de Archivo Excel ───────────────────────────
        intent?.let { manejarIntent(it) }

        // Interceptar apertura desde Widget de Notas
        if (intent?.getBooleanExtra("abrir_notas_con_seguridad", false) == true) {
            abrirBlockNotas()
        }

        iniciarAnimacionesEntrada()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        manejarIntent(intent)
        
        if (intent.getBooleanExtra("abrir_notas_con_seguridad", false)) {
            abrirBlockNotas()
        }
    }

    private fun manejarIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            val mimeType = contentResolver.getType(uri)
            val path = uri.path?.lowercase() ?: ""
            
            val esExcel = mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
                          mimeType == "application/vnd.ms-excel" ||
                          mimeType == "application/octet-stream" ||
                          path.endsWith(".xlsx") || 
                          path.endsWith(".xls")

            if (esExcel) {
                val nextIntent = Intent(this, DescargarPlaylistActivity::class.java).apply {
                    data = uri
                    action = Intent.ACTION_VIEW
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(nextIntent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatosPerfil()
    }

    private fun cargarDatosPerfil() {
        val prefs = getSharedPreferences(PREFS_PERFIL, Context.MODE_PRIVATE)
        val nick = prefs.getString(KEY_NICK, "Jugador")
        val avatarPath = prefs.getString(KEY_AVATAR, null)

        tvNick.text = "¡Hola, $nick!"

        if (!avatarPath.isNullOrEmpty() && File(avatarPath).exists()) {
            mostrarAvatarCircular(avatarPath)
        } else {
            ivAvatar.visibility = View.GONE
            tvAvatarEmoji.visibility = View.VISIBLE
        }
    }

    private fun mostrarAvatarCircular(path: String) {
        val bitmap = BitmapFactory.decodeFile(path)
        if (bitmap != null) {
            val circularBitmap = AvatarHelper.recortarCircular(bitmap)
            ivAvatar.setImageBitmap(circularBitmap)
            ivAvatar.visibility = View.VISIBLE
            tvAvatarEmoji.visibility = View.GONE
        }
    }

    // ── Block de Notas (Seguridad) ────────────────────────────────────────────

    private fun abrirBlockNotas() {
        val puedeUsarHuella = BiometricManager.from(this)
            .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        if (puedeUsarHuella) mostrarHuella() else mostrarPopupContrasena()
    }

    private fun mostrarHuella() {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    startActivity(Intent(this@DashboardActivity, BlockNotasActivity::class.java))
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED -> mostrarPopupContrasena()
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            Toast.makeText(this@DashboardActivity, "Huella bloqueada. Usa la contraseña.", Toast.LENGTH_SHORT).show()
                            mostrarPopupContrasena()
                        }
                        else -> {}
                    }
                }
                override fun onAuthenticationFailed() {}
            })

        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("🔒 Block de Notas")
            .setSubtitle("Usa tu huella para acceder")
            .setNegativeButtonText("Usar contraseña")
            .build())
    }

    private fun mostrarPopupContrasena() {
        val input = EditText(this).apply {
            hint      = "Introduce la contraseña"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.GRAY)
            setBackgroundColor(android.graphics.Color.parseColor("#2A2A2A"))
            setPadding(32, 24, 32, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("🔒 Block de Notas")
            .setMessage("Introduce la contraseña para acceder")
            .setView(input)
            .setPositiveButton("Entrar") { _, _ ->
                val guardada = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_PASSWORD, DEFAULT_PASSWORD)
                if (input.text.toString() == guardada) {
                    startActivity(Intent(this, BlockNotasActivity::class.java))
                } else {
                    Toast.makeText(this, "❌ Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun animarBoton(view: View, action: () -> Unit) {
        SonidoHelper.reproducir(this)
        action()
    }

    // ── Entrada suave ──────────────────────────────────────────────────────
    private fun iniciarAnimacionesEntrada() {
        val avatar = findViewById<FrameLayout>(R.id.flDashAvatar)
        val nombre = findViewById<TextView>(R.id.tvDashNick)
        val botones = listOf(
            R.id.btnDashGacha, R.id.btnDashReproductor, R.id.btnDashBlockNotas,
            R.id.btnDashNutricion, R.id.btnDashPerfil, R.id.btnDashFall,
            R.id.btnDashLearn, R.id.btnHabitos, R.id.btnDashGames
        ).map { findViewById<View>(it) }

        listOf(avatar, nombre).forEachIndexed { i, v ->
            v.translationY = -60f
            v.alpha = 0f
            v.animate()
                .translationY(0f).alpha(1f)
                .setDuration(500).setStartDelay(i * 80L)
                .setInterpolator(DecelerateInterpolator()).start()
        }

        botones.forEachIndexed { i, v ->
            v.translationY = 80f
            v.alpha = 0f
            v.animate()
                .translationY(0f).alpha(1f)
                .setDuration(450).setStartDelay(150L + i * 70L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    if (i == botones.size - 1) {
                        iniciarLoopIdle(botones)
                        iniciarLoopAvatar(avatar)
                    }
                }.start()
        }
    }

    // ── Loop idle botones (escala mínima, sin recorte visual) ─────────────
    private fun animarRespiracion(v: View) {
        v.animate()
            .scaleX(1.015f).scaleY(1.015f)
            .setDuration(2200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                v.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(2200)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction { animarRespiracion(v) }
                    .start()
            }.start()
    }

    // ── Loop avatar: flota suavemente arriba y abajo ──────────────────────
    private fun iniciarLoopAvatar(avatar: View) {
        animarFlotacion(avatar)
    }

    private fun animarFlotacion(v: View) {
        v.animate()
            .translationY(-8f)
            .setDuration(2000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                v.animate()
                    .translationY(0f)
                    .setDuration(2000)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction { animarFlotacion(v) }
                    .start()
            }.start()
    }

    // ── Loop idle suave (respiración) en los botones ─────────
    private fun iniciarLoopIdle(botones: List<View>) {
        botones.forEachIndexed { i, v ->
            val delay = i * 120L // desfase para que no pulsen todos igual
            v.postDelayed({ animarRespiracion(v) }, delay)
        }
    }
}
