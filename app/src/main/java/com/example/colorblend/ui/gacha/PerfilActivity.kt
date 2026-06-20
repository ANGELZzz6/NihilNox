package com.example.colorblend.ui.gacha

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class PerfilActivity : AppCompatActivity() {

    private val viewModel: PerfilViewModel by viewModels()

    private val PREFS_NAME       = "block_notas_prefs"
    private val KEY_PASSWORD     = "password"
    private val DEFAULT_PASSWORD = "1234"
    private val PREFS_PERFIL     = "perfil_prefs"
    private val KEY_AVATAR       = "avatar_path"
    private val KEY_NICK         = "nick"

    private lateinit var ivAvatar: ImageView
    private lateinit var tvAvatarEmoji: TextView
    private lateinit var tvNick: TextView

    private val seleccionarImagen = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { guardarYMostrarAvatar(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        ivAvatar      = findViewById(R.id.ivAvatar)
        tvAvatarEmoji = findViewById(R.id.tvAvatarEmoji)
        tvNick        = findViewById(R.id.tvNick)

        cargarAvatarGuardado()

        // ── Nick editable ─────────────────────────────────────────────────────
        val nickGuardado = getSharedPreferences(PREFS_PERFIL, Context.MODE_PRIVATE)
            .getString(KEY_NICK, "Jugador") ?: "Jugador"
        tvNick.text = nickGuardado

        findViewById<View>(R.id.layoutNick).setOnClickListener {
            SonidoHelper.reproducir(this)
            mostrarDialogEditarNick()
        }

        // ── Avatar ────────────────────────────────────────────────────────────
        findViewById<View>(R.id.frameAvatar).setOnClickListener {
            SonidoHelper.reproducir(this)
            seleccionarImagen.launch("image/*")
        }

        val tvMonedas          = findViewById<TextView>(R.id.tvPerfilMonedas)
        val tvPersonajes       = findViewById<TextView>(R.id.tvPerfilPersonajes)
        val tvMetasActivas     = findViewById<TextView>(R.id.tvPerfilMetasActivas)
        val tvMetasCompletadas = findViewById<TextView>(R.id.tvPerfilMetasCompletadas)
        val tvMejorRacha       = findViewById<TextView>(R.id.tvPerfilMejorRacha)
        val tvAnimes           = findViewById<TextView>(R.id.tvPerfilAnimes)

        findViewById<Button>(R.id.btnChatPersonaje).setOnClickListener {
            animarBoton(it) { SonidoHelper.reproducir(this); startActivity(Intent(this, MisPersonajesChatActivity::class.java)) }
        }
        findViewById<Button>(R.id.btnGaleria).setOnClickListener {
            animarBoton(it) { SonidoHelper.reproducir(this); startActivity(Intent(this, GaleriaActivity::class.java)) }
        }
        findViewById<Button>(R.id.btnReproductor).setOnClickListener {
            animarBoton(it) { SonidoHelper.reproducir(this); startActivity(Intent(this, ReproductorActivity::class.java)) }
        }
        findViewById<Button>(R.id.btnBlockNotas).setOnClickListener {
            animarBoton(it) { SonidoHelper.reproducir(this); abrirBlockNotas() }
        }

        // ── Nutrición ─────────────────────────────────────────────────────────
        findViewById<Button>(R.id.btnNutricion).setOnClickListener {
            animarBoton(it) {
                SonidoHelper.reproducir(this)
                lifecycleScope.launch {
                    val tienePerfil = AppDatabase.getDatabase(applicationContext)
                        .nutricionDao().getPerfil() != null
                    val destino = if (tienePerfil) NutricionActivity::class.java
                    else NutricionOnboardingActivity::class.java
                    startActivity(Intent(this@PerfilActivity, destino))
                }
            }
        }

        findViewById<Button>(R.id.btnApiKeys).setOnClickListener {
            animarBoton(it) {
                SonidoHelper.reproducir(this)
                startActivity(Intent(this, ApiKeysActivity::class.java))
            }
        }

        lifecycleScope.launch {
            launch { viewModel.stats.collect      { tvMonedas.text          = "🪙 ${it?.monedas ?: 0}" } }
            launch { viewModel.personajes.collect { tvPersonajes.text       = "🎴  ${it.size}" } }
            launch { viewModel.animes.collect     { tvAnimes.text           = "📺  ${it.size}" } }
            launch {
                viewModel.metas.collect { lista ->
                    tvMetasActivas.text     = "🎯  ${lista.count { !it.finalizada }}"
                    tvMetasCompletadas.text = "${lista.count { it.finalizada }}"
                    tvMejorRacha.text       = "🔥  ${lista.maxOfOrNull { it.mejorRacha } ?: 0} días"
                }
            }
        }
    }

    // ── Nick ──────────────────────────────────────────────────────────────────

    private fun mostrarDialogEditarNick() {
        val input = EditText(this).apply {
            setText(tvNick.text)
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.parseColor("#444466"))
            hint = "Tu nombre de jugador"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            background = ContextCompat.getDrawable(this@PerfilActivity, R.drawable.chat_input_bg)
            setPadding(40, 28, 40, 28)
            selectAll()
        }

        AlertDialog.Builder(this)
            .setTitle("Editar nombre")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNick = input.text.toString().trim()
                if (nuevoNick.isNotEmpty()) {
                    tvNick.text = nuevoNick
                    getSharedPreferences(PREFS_PERFIL, Context.MODE_PRIVATE)
                        .edit().putString(KEY_NICK, nuevoNick).apply()
                    Toast.makeText(this, "✅ Nombre actualizado", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
            .also {
                input.requestFocus()
                input.postDelayed({
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
                }, 100)
            }
    }

    // ── Animación ─────────────────────────────────────────────────────────────

    private fun animarBoton(btn: View, accion: () -> Unit) {
        btn.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80)
            .withEndAction {
                btn.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                accion()
            }.start()
    }

    // ── Avatar ────────────────────────────────────────────────────────────────

    private fun guardarYMostrarAvatar(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val archivo = File(filesDir, "avatar.jpg")
            FileOutputStream(archivo).use { output -> inputStream.copyTo(output) }
            inputStream.close()
            getSharedPreferences(PREFS_PERFIL, Context.MODE_PRIVATE)
                .edit().putString(KEY_AVATAR, archivo.absolutePath).apply()
            mostrarAvatarCircular(archivo.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error al cargar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarAvatarGuardado() {
        val path = getSharedPreferences(PREFS_PERFIL, Context.MODE_PRIVATE)
            .getString(KEY_AVATAR, null)
        if (!path.isNullOrEmpty() && File(path).exists()) mostrarAvatarCircular(path)
    }

    private fun mostrarAvatarCircular(path: String) {
        val bitmap = BitmapFactory.decodeFile(path) ?: return
        ivAvatar.setImageBitmap(AvatarHelper.recortarCircular(bitmap))
        ivAvatar.visibility      = View.VISIBLE
        tvAvatarEmoji.visibility = View.GONE
    }

    // ── Block de Notas ────────────────────────────────────────────────────────

    private fun abrirBlockNotas() {
        val puedeUsarHuella = BiometricManager.from(this)
            .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        if (puedeUsarHuella) mostrarHuella() else mostrarPopupContrasena()
    }

    private fun mostrarHuella() {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    startActivity(Intent(this@PerfilActivity, BlockNotasActivity::class.java))
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED -> mostrarPopupContrasena()
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            Toast.makeText(this@PerfilActivity, "Huella bloqueada. Usa la contraseña.", Toast.LENGTH_SHORT).show()
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
}