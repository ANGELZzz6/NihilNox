package com.example.colorblend.ui.gacha.metas

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.colorblend.R
import com.example.colorblend.domain.model.TipoMeta
import com.example.colorblend.ui.gacha.metas.viewmodels.MetaViewModel
import java.util.Calendar

class CrearMetaActivity : AppCompatActivity() {

    private val metaViewModel: MetaViewModel by viewModels()

    private lateinit var cbTodos: CheckBox
    private lateinit var cbLun: CheckBox
    private lateinit var cbMar: CheckBox
    private lateinit var cbMie: CheckBox
    private lateinit var cbJue: CheckBox
    private lateinit var cbVie: CheckBox
    private lateinit var cbSab: CheckBox
    private lateinit var cbDom: CheckBox
    private lateinit var tvHoraSeleccionada: TextView
    private lateinit var btnQuitarHora: Button

    private var horaRecordatorio: String? = null // "HH:mm" o null

    private val diasCheckboxes: List<Pair<CheckBox, Int>> by lazy {
        listOf(cbLun to 1, cbMar to 2, cbMie to 3, cbJue to 4,
            cbVie to 5, cbSab to 6, cbDom to 7)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_meta)

        val inputTitulo      = findViewById<EditText>(R.id.inputTitulo)
        val inputDescripcion = findViewById<EditText>(R.id.inputDescripcion)
        val radioTipo        = findViewById<RadioGroup>(R.id.radioTipo)
        val inputObjetivo    = findViewById<EditText>(R.id.inputObjetivo)
        val btnCrear         = findViewById<Button>(R.id.btnCrearMeta)
        val btnSeleccionarHora = findViewById<Button>(R.id.btnSeleccionarHora)
        tvHoraSeleccionada   = findViewById(R.id.tvHoraSeleccionada)
        btnQuitarHora        = findViewById(R.id.btnQuitarHora)

        cbTodos = findViewById(R.id.cbTodos)
        cbLun   = findViewById(R.id.cbLun)
        cbMar   = findViewById(R.id.cbMar)
        cbMie   = findViewById(R.id.cbMie)
        cbJue   = findViewById(R.id.cbJue)
        cbVie   = findViewById(R.id.cbVie)
        cbSab   = findViewById(R.id.cbSab)
        cbDom   = findViewById(R.id.cbDom)

        cbTodos.setOnCheckedChangeListener { _, checked ->
            if (checked) diasCheckboxes.forEach { (cb, _) -> cb.isChecked = false }
        }
        diasCheckboxes.forEach { (cb, _) ->
            cb.setOnCheckedChangeListener { _, checked ->
                if (checked) cbTodos.isChecked = false
            }
        }
        cbTodos.isChecked = true

        // ── Recordatorio ──────────────────────────────────────────────────────
        btnSeleccionarHora.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hora, minuto ->
                    horaRecordatorio = String.format("%02d:%02d", hora, minuto)
                    tvHoraSeleccionada.text = "⏰ $horaRecordatorio"
                    tvHoraSeleccionada.setTextColor(android.graphics.Color.parseColor("#FFD700"))
                    btnQuitarHora.visibility = View.VISIBLE
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true // formato 24h
            ).show()
        }

        btnQuitarHora.setOnClickListener {
            horaRecordatorio = null
            tvHoraSeleccionada.text = "Sin recordatorio"
            tvHoraSeleccionada.setTextColor(android.graphics.Color.parseColor("#CCCCDD"))
            btnQuitarHora.visibility = View.GONE
        }

        // ── Crear meta ────────────────────────────────────────────────────────
        btnCrear.setOnClickListener {
            val titulo       = inputTitulo.text.toString().trim()
            val descripcion  = inputDescripcion.text.toString().trim().ifEmpty { null }
            val objetivoText = inputObjetivo.text.toString().trim()

            if (titulo.isEmpty()) {
                Toast.makeText(this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (objetivoText.isEmpty()) {
                Toast.makeText(this, "Debes ingresar un objetivo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val objetivo = objetivoText.toIntOrNull()
            if (objetivo == null || objetivo <= 0) {
                Toast.makeText(this, "El objetivo debe ser un número mayor a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tipo = if (radioTipo.checkedRadioButtonId == R.id.radioDiaria)
                TipoMeta.DIARIA else TipoMeta.ACUMULATIVA

            val diasSemana: String? = if (cbTodos.isChecked) {
                null
            } else {
                val seleccionados = diasCheckboxes
                    .filter { (cb, _) -> cb.isChecked }
                    .map { (_, num) -> num.toString() }
                if (seleccionados.isEmpty()) {
                    Toast.makeText(this, "Selecciona al menos un día", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                seleccionados.joinToString(",")
            }

            btnCrear.isEnabled = false

            metaViewModel.crearMeta(
                titulo            = titulo,
                descripcion       = descripcion,
                tipo              = tipo,
                objetivo          = objetivo,
                diasSemana        = diasSemana,
                horaRecordatorio  = horaRecordatorio,
                onError = { mensaje ->
                    runOnUiThread {
                        btnCrear.isEnabled = true
                        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                    }
                },
                onExito = { meta ->
                    runOnUiThread {
                        // Programar alarma si se configuró hora
                        if (horaRecordatorio != null) {
                            MetaRecordatorioScheduler.programar(applicationContext, meta)
                        }
                        Toast.makeText(this, "¡Meta creada! 🎯", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            )
        }
    }
}