package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.FraseZen
import android.view.LayoutInflater
import android.widget.SeekBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ZenRecordarActivity : AppCompatActivity() {

    private lateinit var tvFrase: TextView
    private lateinit var db: AppDatabase
    private var fallingJob: Job? = null
    
    private val PREFS_ZEN = "zen_prefs"
    private val KEY_FALL_DELAY = "fall_delay"
    private var fallDelayMillis: Long = 3000L

    // Lógica para modo Lluvia (Combo)
    private var clickCount = 0
    private var lastClickTime = 0L
    private val COMBO_THRESHOLD = 30
    private val COMBO_INTERVAL = 600L // Tiempo máximo entre clics para contar el combo
    private val RESET_INTERVAL = 5000L // Tiempo de calma para volver a modo normal
    private var resetJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zen_recordar)
        FullScreenHelper.enable(this)

        db = AppDatabase.getDatabase(this)
        tvFrase = findViewById(R.id.tvZenFrase)

        val prefs = getSharedPreferences(PREFS_ZEN, MODE_PRIVATE)
        fallDelayMillis = prefs.getLong(KEY_FALL_DELAY, 3000L)

        findViewById<FrameLayout>(R.id.rootZen).setOnClickListener {
            manejarClickPantalla()
        }

        findViewById<ImageButton>(R.id.btnZenAdd).setOnClickListener {
            mostrarDialogoAgregar()
        }
    }

    private fun manejarClickPantalla() {
        val currentTime = System.currentTimeMillis()
        
        // Lógica de combo
        if (currentTime - lastClickTime < COMBO_INTERVAL) {
            clickCount++
        } else {
            clickCount = 1
        }
        lastClickTime = currentTime

        // Reiniciar el combo tras 5 segundos de calma
        resetJob?.cancel()
        resetJob = lifecycleScope.launch {
            delay(RESET_INTERVAL)
            clickCount = 0
        }

        lifecycleScope.launch {
            val frase = db.fraseZenDao().getRandomFrase()
            if (frase == null) {
                prepararYMostrarNuevaFrase(null)
                return@launch
            }

            if (clickCount >= COMBO_THRESHOLD) {
                // MODO LLUVIA: Aparecen múltiples frases en lugares aleatorios
                spawnFraseLluvia(frase.texto)
                // Ocultar la frase central si estaba visible
                if (tvFrase.alpha > 0f) {
                    tvFrase.animate().alpha(0f).setDuration(200).start()
                }
            } else {
                // MODO NORMAL: Una sola frase en el centro
                prepararCambioFraseNormal(frase)
            }
        }
    }

    private fun prepararCambioFraseNormal(frase: FraseZen) {
        fallingJob?.cancel() 
        if (tvFrase.alpha > 0f) {
            tvFrase.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .translationY(0f)
                .setDuration(150)
                .withEndAction {
                    prepararYMostrarNuevaFrase(frase)
                }
                .start()
        } else {
            prepararYMostrarNuevaFrase(frase)
        }
    }

    private fun spawnFraseLluvia(texto: String) {
        val root = findViewById<FrameLayout>(R.id.rootZen)
        val newTv = TextView(this).apply {
            text = texto
            setTextColor(android.graphics.Color.WHITE)
            textSize = 22f
            setTypeface(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.ITALIC)
            alpha = 0f
            gravity = android.view.Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        root.addView(newTv)
        
        newTv.post {
            // Posicionamiento aleatorio dentro de los límites
            val maxX = root.width - newTv.width - 100
            val maxY = root.height - newTv.height - 200
            
            if (maxX > 100 && maxY > 100) {
                newTv.x = (100..maxX).random().toFloat()
                newTv.y = (100..maxY).random().toFloat()
            }

            // Animación Pop
            newTv.scaleX = 0.5f
            newTv.scaleY = 0.5f
            newTv.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator(1.5f))
                .withEndAction {
                    // Caída independiente después del delay
                    lifecycleScope.launch {
                        delay(fallDelayMillis)
                        if (newTv.parent != null) {
                            newTv.animate()
                                .translationYBy(400f)
                                .alpha(0f)
                                .setDuration(1000)
                                .setInterpolator(AccelerateInterpolator())
                                .withEndAction { root.removeView(newTv) }
                                .start()
                        }
                    }
                }
                .start()
        }
    }

    private fun prepararYMostrarNuevaFrase(frase: FraseZen?) {
        if (frase != null) {
            animarPop(frase.texto)
        } else {
            tvFrase.text = "Toca el icono para añadir frases"
            tvFrase.translationY = 0f
            tvFrase.alpha = 0f
            tvFrase.animate().alpha(0.3f).setDuration(500).start()
        }
    }

    private fun animarPop(texto: String) {
        tvFrase.animate().cancel()
        tvFrase.text = texto
        tvFrase.alpha = 0f
        tvFrase.scaleX = 0.6f
        tvFrase.scaleY = 0.6f
        tvFrase.translationY = 0f // Asegurar que empieza en el centro
        
        tvFrase.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator(1.8f))
            .withEndAction {
                programarCaida()
            }
            .start()
    }

    private fun programarCaida() {
        fallingJob = lifecycleScope.launch {
            delay(fallDelayMillis) // Usar el delay configurado
            
            tvFrase.animate()
                .translationY(300f) // Caer hacia abajo
                .alpha(0f)          // Desvanecerse mientras cae
                .setDuration(800)
                .setInterpolator(AccelerateInterpolator()) // Aceleración de caída
                .start()
        }
    }

    private fun mostrarDialogoAgregar() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_zen_settings, null)
        val etInput = dialogView.findViewById<EditText>(R.id.etZenFraseInput)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.sbZenSpeed)
        val tvSpeed = dialogView.findViewById<TextView>(R.id.tvZenSpeedLabel)

        // Configurar estado inicial del SeekBar
        val currentSeconds = (fallDelayMillis / 1000).toInt().coerceIn(1, 10)
        seekBar.progress = currentSeconds
        tvSpeed.text = "$currentSeconds segundos"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val p = if (progress < 1) 1 else progress
                tvSpeed.text = "$p segundos"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                // Guardar frase si hay texto
                val texto = etInput.text.toString().trim()
                if (texto.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.fraseZenDao().insertFrase(FraseZen(texto = texto))
                        // Resetear para mostrar la nueva
                        clickCount = 0
                        prepararCambioFraseNormal(FraseZen(texto = texto))
                    }
                }
                
                // Guardar nueva velocidad
                val newSeconds = if (seekBar.progress < 1) 1 else seekBar.progress
                fallDelayMillis = newSeconds.toLong() * 1000
                getSharedPreferences(PREFS_ZEN, MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_FALL_DELAY, fallDelayMillis)
                    .apply()
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}
