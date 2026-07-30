package com.example.colorblend.ui.gacha

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.colorblend.R
import kotlinx.coroutines.launch

class AutoControlOnboardingActivity : AppCompatActivity() {

    private val viewModel: AutoControlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_control_onboarding)

        val spinnerFrecuencia = findViewById<Spinner>(R.id.spinnerFrecuencia)
        val spinnerObjetivo = findViewById<Spinner>(R.id.spinnerObjetivo)
        val btnGenerar = findViewById<Button>(R.id.btnGenerarPlan)
        val progress = findViewById<ProgressBar>(R.id.progressLoading)
        
        val cbAburrimiento = findViewById<CheckBox>(R.id.cbAburrimiento)
        val cbEstres = findViewById<CheckBox>(R.id.cbEstres)
        val cbSoledad = findViewById<CheckBox>(R.id.cbSoledad)
        val cbInsomnio = findViewById<CheckBox>(R.id.cbInsomnio)
        val cbSocial = findViewById<CheckBox>(R.id.cbSocial)

        // Configurar Spinners
        ArrayAdapter.createFromResource(
            this, R.array.frecuencia_autocontrol, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerFrecuencia.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this, R.array.objetivos_autocontrol, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerObjetivo.adapter = adapter
        }

        btnGenerar.setOnClickListener {
            val frecuencia = spinnerFrecuencia.selectedItem.toString()
            val objetivo = spinnerObjetivo.selectedItem.toString()
            
            val triggers = mutableListOf<String>()
            if (cbAburrimiento.isChecked) triggers.add("Aburrimiento")
            if (cbEstres.isChecked) triggers.add("Estrés/Ansiedad")
            if (cbSoledad.isChecked) triggers.add("Soledad")
            if (cbInsomnio.isChecked) triggers.add("Insomnio")
            if (cbSocial.isChecked) triggers.add("Redes Sociales")

            if (triggers.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos un disparador", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.generarPlan(frecuencia, objetivo, triggers.joinToString(", "))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect { estado ->
                    when (estado) {
                        is AutoControlState.Cargando -> {
                            btnGenerar.isEnabled = false
                            btnGenerar.text = "Consultando a la IA..."
                            progress.visibility = View.VISIBLE
                        }
                        is AutoControlState.Exito -> {
                            Toast.makeText(this@AutoControlOnboardingActivity, estado.mensaje, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@AutoControlOnboardingActivity, AutoControlActivity::class.java))
                            finish()
                        }
                        is AutoControlState.Error -> {
                            btnGenerar.isEnabled = true
                            btnGenerar.text = "Generar mi Plan ✦"
                            progress.visibility = View.GONE
                            Toast.makeText(this@AutoControlOnboardingActivity, "Error: ${estado.mensaje}", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            btnGenerar.isEnabled = true
                            btnGenerar.text = "Generar mi Plan ✦"
                            progress.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
}
