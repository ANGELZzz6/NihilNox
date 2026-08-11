package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.colorblend.R
import com.example.colorblend.domain.model.GenshinCharacter
import kotlinx.coroutines.launch

class GenshinEditActivity : AppCompatActivity() {

    private val viewModel: GenshinViewModel by viewModels()
    private var characterId: Int = 0

    private lateinit var etNombre: EditText
    private lateinit var etNivel: EditText
    private lateinit var etConstelacion: EditText
    private lateinit var etNotas: EditText
    private lateinit var spElemento: Spinner
    private lateinit var spRareza: Spinner

    private val elementos = arrayOf("Anemo", "Geo", "Electro", "Dendro", "Hydro", "Pyro", "Cryo")
    private val rarezas = arrayOf("5 ★", "4 ★")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_genshin_edit)

        characterId = intent.getIntExtra("character_id", 0)

        etNombre = findViewById(R.id.etGenshinNombre)
        etNivel = findViewById(R.id.etGenshinNivel)
        etConstelacion = findViewById(R.id.etGenshinConstelacion)
        etNotas = findViewById(R.id.etGenshinNotas)
        spElemento = findViewById(R.id.spGenshinElemento)
        spRareza = findViewById(R.id.spGenshinRareza)

        val adapterElem = ArrayAdapter(this, android.R.layout.simple_spinner_item, elementos)
        adapterElem.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spElemento.adapter = adapterElem

        val adapterRare = ArrayAdapter(this, android.R.layout.simple_spinner_item, rarezas)
        adapterRare.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRareza.adapter = adapterRare

        if (characterId != 0) {
            findViewById<Button>(R.id.btnEliminarGenshin).visibility = View.VISIBLE
            cargarDatos()
        }

        findViewById<Button>(R.id.btnGuardarGenshin).setOnClickListener { guardar() }
        
        findViewById<Button>(R.id.btnEliminarGenshin).setOnClickListener { eliminar() }
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            val char = viewModel.getById(characterId) ?: return@launch
            etNombre.setText(char.nombre)
            etNivel.setText(char.nivel.toString())
            etConstelacion.setText(char.constelacion.toString())
            etNotas.setText(char.notas)
            
            val posElem = elementos.indexOf(char.elemento)
            if (posElem >= 0) spElemento.setSelection(posElem)
            
            val posRare = if (char.rareza == 5) 0 else 1
            spRareza.setSelection(posRare)
        }
    }

    private fun guardar() {
        val nombre = etNombre.text.toString().trim()
        val nivelStr = etNivel.text.toString().trim()
        val constStr = etConstelacion.text.toString().trim()
        val elemento = spElemento.selectedItem.toString()
        val rareza = if (spRareza.selectedItemPosition == 0) 5 else 4

        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ponle un nombre al menos", Toast.LENGTH_SHORT).show()
            return
        }

        val nivel = nivelStr.toIntOrNull() ?: 1
        val const = constStr.toIntOrNull() ?: 0

        val char = GenshinCharacter(
            id = if (characterId != 0) characterId else 0,
            nombre = nombre,
            elemento = elemento,
            rareza = rareza,
            nivel = nivel,
            constelacion = const,
            notas = etNotas.text.toString(),
            armaTipo = "Desconocida" // Simplificación por ahora
        )

        if (characterId != 0) viewModel.update(char)
        else viewModel.insert(char)

        Toast.makeText(this, "✅ Guardado", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun eliminar() {
        lifecycleScope.launch {
            val char = viewModel.getById(characterId) ?: return@launch
            viewModel.delete(char)
            Toast.makeText(this@GenshinEditActivity, "🗑️ Eliminado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
