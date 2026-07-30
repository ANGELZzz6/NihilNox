package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.colorblend.R
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AutoControlActivity : AppCompatActivity() {

    private val viewModel: AutoControlViewModel by viewModels()
    private lateinit var tvContador: TextView
    private lateinit var tvPlanIA: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_control)

        tvContador = findViewById(R.id.tvContador)
        tvPlanIA = findViewById(R.id.tvPlanIA)
        val btnConsultar = findViewById<Button>(R.id.btnConsultarIA)
        val btnReiniciar = findViewById<Button>(R.id.btnRegistrarFalla)

        btnConsultar.setOnClickListener { mostrarDialogoConsulta() }
        
        btnReiniciar.setOnClickListener {
            AlertDialog.Builder(this, R.style.DialogoOscuro)
                .setTitle("Reiniciar Contador")
                .setMessage("¿Estás seguro de que quieres reiniciar tu racha de enfoque?")
                .setPositiveButton("Sí, reiniciar") { _, _ ->
                    viewModel.reiniciarContador()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoaded.collect { loaded ->
                    if (loaded) {
                        val perfil = viewModel.perfil.value
                        if (perfil == null) {
                            startActivity(android.content.Intent(this@AutoControlActivity, AutoControlOnboardingActivity::class.java))
                            finish()
                        } else {
                            tvPlanIA.text = perfil.planIA
                            iniciarContador(perfil.ultimaVez ?: perfil.fechaCreacion)
                        }
                    }
                }
            }
        }

        val etPregunta = findViewById<EditText>(R.id.etPreguntaIA)
        val btnEnviarPregunta = findViewById<android.widget.ImageButton>(R.id.btnEnviarPregunta)

        btnEnviarPregunta.setOnClickListener {
            val pregunta = etPregunta.text.toString().trim()
            if (pregunta.isNotEmpty()) {
                viewModel.preguntarIA(pregunta)
                etPregunta.text.clear()
                // Ocultar teclado
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
        }

        lifecycleScope.launch {
            viewModel.preguntaRespuesta.collect { respuesta ->
                AlertDialog.Builder(this@AutoControlActivity, R.style.DialogoOscuro)
                    .setTitle("💡 Mentor AI")
                    .setMessage(respuesta)
                    .setPositiveButton("Gracias", null)
                    .show()
            }
        }

        lifecycleScope.launch {
            viewModel.consultaResultado.collect { (aprobado, motivo, mensaje) ->
                mostrarResultadoConsulta(aprobado, motivo, mensaje)
            }
        }
    }

    private fun iniciarContador(inicio: Long) {
        runnable?.let { handler.removeCallbacks(it) }
        runnable = object : Runnable {
            override fun run() {
                val diff = System.currentTimeMillis() - inicio
                val dias = TimeUnit.MILLISECONDS.toDays(diff)
                val horas = TimeUnit.MILLISECONDS.toHours(diff) % 24
                val mins = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                
                tvContador.text = String.format("%02dd %02dh %02dm", dias, horas, mins)
                handler.postDelayed(this, 60000) // Actualizar cada minuto
            }
        }
        handler.post(runnable!!)
    }

    private fun mostrarDialogoConsulta() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_autocontrol_consulta, null)
        val etDuracion = view.findViewById<EditText>(R.id.etDuracion)
        
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()
            
        view.findViewById<Button>(R.id.btnCancelar).setOnClickListener { dialog.dismiss() }
        view.findViewById<Button>(R.id.btnConsultar).setOnClickListener {
            val duracion = etDuracion.text.toString().toIntOrNull()
            if (duracion != null && duracion > 0) {
                viewModel.consultarIA(duracion)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Ingresa una duración válida", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun mostrarResultadoConsulta(aprobado: Boolean, motivo: String, mensaje: String) {
        AlertDialog.Builder(this, R.style.DialogoOscuro)
            .setTitle(if (aprobado) "✅ Sesión Aprobada" else "⚠️ Recomendación Alternativa")
            .setMessage("$motivo\n\n$mensaje")
            .setPositiveButton("Entendido", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler.removeCallbacks(it) }
    }
}
