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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.colorblend.R
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
                startActivity(Intent(this, FallActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnDashApiKeys).setOnClickListener {
            animarBoton(it) {
                startActivity(Intent(this, ApiKeysActivity::class.java))
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
        try {
            val bitmap = BitmapFactory.decodeFile(path)
            if (bitmap != null) {
                val circularBitmap = recortarCircular(bitmap)
                ivAvatar.setImageBitmap(circularBitmap)
                ivAvatar.visibility = View.VISIBLE
                tvAvatarEmoji.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ivAvatar.visibility = View.GONE
            tvAvatarEmoji.visibility = View.VISIBLE
        }
    }

    private fun recortarCircular(bitmap: Bitmap): Bitmap {
        val size   = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(
                Bitmap.createScaledBitmap(bitmap, size, size, true),
                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return output
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
