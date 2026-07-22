package com.example.colorblend.ui.gacha

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.colorblend.R
import com.example.colorblend.domain.model.Tarea
import java.util.*

class CrearTareaActivity : AppCompatActivity() {

    private val tareaViewModel: TareaViewModel by viewModels()
    
    private lateinit var etTitulo: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var btnFecha: Button
    private lateinit var btnHora: Button
    private lateinit var cbNotificacion: CheckBox
    private lateinit var containerColores: LinearLayout
    private val selectedDays = mutableSetOf<Int>() // 1=L, 2=M... 7=D
    
    private var fechaSeleccionada = Calendar.getInstance()
    private var hora = 10
    private var minuto = 30
    private var colorSeleccionado = "#FFD700"

    private val colores = listOf("#FFD700", "#FF5555", "#55FF55", "#5555FF", "#FF55FF", "#55FFFF")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_tarea)
        FullScreenHelper.enable(this)

        val fechaInicial = intent.getLongExtra("fecha_inicial", System.currentTimeMillis())
        fechaSeleccionada.timeInMillis = fechaInicial

        etTitulo = findViewById(R.id.etTitulo)
        etDescripcion = findViewById(R.id.etDescripcion)
        btnFecha = findViewById(R.id.btnFecha)
        btnHora = findViewById(R.id.btnHora)
        cbNotificacion = findViewById(R.id.cbNotificacion)
        containerColores = findViewById(R.id.containerColores)

        setupUI()
        setupDaySelectors()
    }

    private fun setupUI() {
        actualizarFechaBoton()
        actualizarHoraBoton()

        btnFecha.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                fechaSeleccionada.set(y, m, d)
                actualizarFechaBoton()
            }, fechaSeleccionada.get(Calendar.YEAR), fechaSeleccionada.get(Calendar.MONTH), fechaSeleccionada.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnHora.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                hora = h
                minuto = m
                actualizarHoraBoton()
            }, hora, minuto, true).show()
        }

        setupSelectorColores()

        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            guardarTarea()
        }
    }

    private fun setupDaySelectors() {
        val days = listOf(
            R.id.tvDiaL to 2, // Calendar.MONDAY
            R.id.tvDiaM to 3, // Calendar.TUESDAY
            R.id.tvDiaX to 4,
            R.id.tvDiaJ to 5,
            R.id.tvDiaV to 6,
            R.id.tvDiaS to 7,
            R.id.tvDiaD to 1  // Calendar.SUNDAY
        )

        for ((id, dayValue) in days) {
            findViewById<TextView>(id).setOnClickListener { view ->
                if (selectedDays.contains(dayValue)) {
                    selectedDays.remove(dayValue)
                    view.alpha = 0.6f
                    (view as TextView).setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
                } else {
                    selectedDays.add(dayValue)
                    view.alpha = 1.0f
                    (view as TextView).setTextColor(android.graphics.Color.parseColor("#FFD700"))
                }
            }
        }
    }

    private fun setupSelectorColores() {
        for (colorHex in colores) {
            val view = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                    setMargins(8, 0, 8, 0)
                }
                setBackgroundColor(android.graphics.Color.parseColor(colorHex))
                setOnClickListener {
                    colorSeleccionado = colorHex
                    resaltarColorSeleccionado()
                }
            }
            containerColores.addView(view)
        }
    }

    private fun resaltarColorSeleccionado() {
        // Implementar un borde o algo para indicar seleccion
    }

    private fun actualizarFechaBoton() {
        val d = fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
        val m = fechaSeleccionada.get(Calendar.MONTH) + 1
        val y = fechaSeleccionada.get(Calendar.YEAR)
        btnFecha.text = "Fecha: $d/$m/$y"
    }

    private fun actualizarHoraBoton() {
        btnHora.text = String.format("Hora: %02d:%02d", hora, minuto)
    }

    private fun guardarTarea() {
        val titulo = etTitulo.text.toString()
        if (titulo.isEmpty()) {
            Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        val recurrencia = if (selectedDays.isEmpty()) "UNA_VEZ" else "DIAS_SELECCIONADOS"
        val diasSemana = selectedDays.joinToString(",")
        
        // Normalizar fecha a medianoche
        fechaSeleccionada.set(Calendar.HOUR_OF_DAY, 0)
        fechaSeleccionada.set(Calendar.MINUTE, 0)
        fechaSeleccionada.set(Calendar.SECOND, 0)
        fechaSeleccionada.set(Calendar.MILLISECOND, 0)

        val tarea = Tarea(
            titulo = titulo,
            descripcion = etDescripcion.text.toString(),
            fecha = fechaSeleccionada.timeInMillis,
            hora = hora,
            minuto = minuto,
            notificacionHabilitada = cbNotificacion.isChecked,
            recurrencia = recurrencia,
            diasSemana = diasSemana,
            color = colorSeleccionado
        )

        tareaViewModel.insertarTarea(tarea)
        
        if (tarea.notificacionHabilitada) {
            TareaAlarmScheduler.programar(this, tarea)
        }

        finish()
    }
}
