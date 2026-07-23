package com.example.colorblend.ui.gacha

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.colorblend.R
import com.example.colorblend.domain.model.Tarea
import kotlinx.coroutines.launch
import java.util.*

class CrearTareaActivity : AppCompatActivity() {

    private val tareaViewModel: TareaViewModel by viewModels()
    
    private lateinit var etTitulo: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var btnFecha: Button
    private lateinit var btnHora: Button
    private lateinit var cbNotificacion: CheckBox
    private lateinit var containerColores: LinearLayout
    private val selectedDays = mutableSetOf<Int>() // 1=D, 2=L... 7=S (Java Calendar)
    
    private var fechaSeleccionada = Calendar.getInstance()
    private var hora = 10
    private var minuto = 30
    private var colorSeleccionado = "#FFD700"
    private var editingTareaId: Int = -1

    private val colores = listOf("#FFD700", "#FF5555", "#55FF55", "#5555FF", "#FF55FF", "#55FFFF")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_tarea)
        FullScreenHelper.enable(this)

        etTitulo = findViewById(R.id.etTitulo)
        etDescripcion = findViewById(R.id.etDescripcion)
        btnFecha = findViewById(R.id.btnFecha)
        btnHora = findViewById(R.id.btnHora)
        cbNotificacion = findViewById(R.id.cbNotificacion)
        containerColores = findViewById(R.id.containerColores)

        editingTareaId = intent.getIntExtra("tarea_id", -1)
        
        setupUI()
        setupDaySelectors()

        if (editingTareaId != -1) {
            cargarDatosTarea(editingTareaId)
            findViewById<TextView>(R.id.btnGuardar).text = "ACTUALIZAR TAREA"
        } else {
            val fechaInicial = intent.getLongExtra("fecha_inicial", System.currentTimeMillis())
            fechaSeleccionada.timeInMillis = fechaInicial
            actualizarFechaBoton()
            actualizarHoraBoton()
        }
    }

    private fun cargarDatosTarea(id: Int) {
        lifecycleScope.launch {
            val tarea = tareaViewModel.getTareaById(id)
            tarea?.let { t ->
                etTitulo.setText(t.titulo)
                etDescripcion.setText(t.descripcion)
                fechaSeleccionada.timeInMillis = t.fecha
                hora = t.hora
                minuto = t.minuto
                cbNotificacion.isChecked = t.notificacionHabilitada
                colorSeleccionado = t.color
                
                if (t.recurrencia == "DIAS_SELECCIONADOS") {
                    val dias = t.diasSemana.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
                    selectedDays.addAll(dias)
                    marcarDiasSeleccionados()
                }
                
                actualizarFechaBoton()
                actualizarHoraBoton()
                resaltarColorSeleccionado()
            }
        }
    }

    private fun setupUI() {
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
            R.id.tvDiaL to Calendar.MONDAY,
            R.id.tvDiaM to Calendar.TUESDAY,
            R.id.tvDiaX to Calendar.WEDNESDAY,
            R.id.tvDiaJ to Calendar.THURSDAY,
            R.id.tvDiaV to Calendar.FRIDAY,
            R.id.tvDiaS to Calendar.SATURDAY,
            R.id.tvDiaD to Calendar.SUNDAY
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

    private fun marcarDiasSeleccionados() {
        val daysMap = mapOf(
            Calendar.MONDAY to R.id.tvDiaL,
            Calendar.TUESDAY to R.id.tvDiaM,
            Calendar.WEDNESDAY to R.id.tvDiaX,
            Calendar.THURSDAY to R.id.tvDiaJ,
            Calendar.FRIDAY to R.id.tvDiaV,
            Calendar.SATURDAY to R.id.tvDiaS,
            Calendar.SUNDAY to R.id.tvDiaD
        )
        for ((value, id) in daysMap) {
            val view = findViewById<TextView>(id)
            if (selectedDays.contains(value)) {
                view.alpha = 1.0f
                view.setTextColor(android.graphics.Color.parseColor("#FFD700"))
            } else {
                view.alpha = 0.6f
                view.setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
            }
        }
    }

    private fun setupSelectorColores() {
        containerColores.removeAllViews()
        for (colorHex in colores) {
            val view = View(this).apply {
                val size = (32 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(12, 0, 12, 0)
                }
                
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.parseColor(colorHex))
                    if (colorHex == colorSeleccionado) {
                        setStroke(4, android.graphics.Color.WHITE)
                    }
                }
                background = bg
                
                setOnClickListener {
                    colorSeleccionado = colorHex
                    setupSelectorColores() // Refrescar para mostrar el borde en el seleccionado
                }
            }
            containerColores.addView(view)
        }
    }

    private fun resaltarColorSeleccionado() {
        setupSelectorColores()
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
            id = if (editingTareaId != -1) editingTareaId else 0,
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

        lifecycleScope.launch {
            if (editingTareaId != -1) {
                tareaViewModel.actualizarTarea(tarea)
                if (tarea.notificacionHabilitada) {
                    TareaAlarmScheduler.programar(this@CrearTareaActivity, tarea)
                }
            } else {
                val newId = tareaViewModel.insertarTarea(tarea)
                if (tarea.notificacionHabilitada) {
                    TareaAlarmScheduler.programar(this@CrearTareaActivity, tarea.copy(id = newId.toInt()))
                }
            }
            finish()
        }
    }
}
