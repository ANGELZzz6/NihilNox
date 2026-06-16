package com.example.colorblend.ui.gacha

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.colorblend.R

class NutricionOnboardingActivity : AppCompatActivity() {

    private val viewModel: NutricionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nutricion_onboarding)

        val btnSiguiente    = findViewById<Button>(R.id.btnOnboardingSiguiente)
        val etPeso          = findViewById<EditText>(R.id.etPeso)
        val etAltura        = findViewById<EditText>(R.id.etAltura)
        val etEdad          = findViewById<EditText>(R.id.etEdad)
        val rgSexo          = findViewById<RadioGroup>(R.id.rgSexo)
        val spinnerObjetivo = findViewById<Spinner>(R.id.spinnerObjetivo)
        val spinnerActividad = findViewById<Spinner>(R.id.spinnerActividad)

        // Spinner objetivo
        ArrayAdapter.createFromResource(
            this, R.array.objetivos_array, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerObjetivo.adapter = adapter
        }

        // Spinner actividad
        ArrayAdapter.createFromResource(
            this, R.array.actividad_array, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerActividad.adapter = adapter
        }

        btnSiguiente.setOnClickListener {
            val peso    = etPeso.text.toString().toFloatOrNull()
            val altura  = etAltura.text.toString().toIntOrNull()
            val edad    = etEdad.text.toString().toIntOrNull()
            val sexo    = if (rgSexo.checkedRadioButtonId == R.id.rbHombre) "Hombre" else "Mujer"
            val objetivo    = spinnerObjetivo.selectedItem.toString()
            val actividad   = spinnerActividad.selectedItem.toString()

            if (peso == null || altura == null || edad == null) {
                Toast.makeText(this, "⚠️ Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (peso < 30 || peso > 300) {
                Toast.makeText(this, "⚠️ Peso inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (altura < 100 || altura > 250) {
                Toast.makeText(this, "⚠️ Altura inválida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.guardarPerfil(peso, altura, edad, sexo, actividad, objetivo)

            startActivity(Intent(this, NutricionActivity::class.java))
            finish()
        }
    }
}