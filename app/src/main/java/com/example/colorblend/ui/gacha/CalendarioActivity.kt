package com.example.colorblend.ui.gacha

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.Habito
import com.example.colorblend.domain.model.Tarea
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.*

class CalendarioActivity : AppCompatActivity() {

    private val tareaViewModel: TareaViewModel by viewModels()
    private lateinit var rvCalendario: RecyclerView
    private lateinit var rvTareasDia: RecyclerView
    private lateinit var tvMesAnio: TextView
    private lateinit var tvFechaSeleccionada: TextView
    
    private var calendarActual = Calendar.getInstance()
    private var fechaSeleccionada = Calendar.getInstance()
    private var listaTodasTareas: List<Tarea> = emptyList()
    private var listaTodosHabitos: List<Habito> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendario)
        FullScreenHelper.enable(this)

        rvCalendario = findViewById(R.id.rvCalendario)
        rvTareasDia = findViewById(R.id.rvTareasDia)
        tvMesAnio = findViewById(R.id.tvMesAño)
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnMesAnterior).setOnClickListener {
            calendarActual.add(Calendar.MONTH, -1)
            refrescarUI()
        }
        findViewById<ImageButton>(R.id.btnMesSiguiente).setOnClickListener {
            calendarActual.add(Calendar.MONTH, 1)
            refrescarUI()
        }

        findViewById<FloatingActionButton>(R.id.fabNuevaTarea).setOnClickListener {
            val intent = Intent(this, CrearTareaActivity::class.java)
            intent.putExtra("fecha_inicial", fechaSeleccionada.timeInMillis)
            startActivity(intent)
        }

        setupTareasList()
        observeData()
    }

    private fun observeData() {
        lifecycleScope.launch {
            tareaViewModel.todasLasTareas.collect { tareas ->
                listaTodasTareas = tareas
                refrescarUI()
            }
        }
        lifecycleScope.launch {
            tareaViewModel.todosLosHabitos.collect { habitos ->
                listaTodosHabitos = habitos
                refrescarUI()
            }
        }
    }

    private fun refrescarUI() {
        actualizarCalendarioGrid()
        actualizarTareasDelDia()
    }

    private fun actualizarCalendarioGrid() {
        val mes = calendarActual.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("es", "ES"))
        val anio = calendarActual.get(Calendar.YEAR)
        tvMesAnio.text = "${mes?.replaceFirstChar { it.uppercase() }} $anio"

        val dias = generarDiasMes(calendarActual)
        val adapter = CalendarioAdapter(dias, fechaSeleccionada, listaTodasTareas, listaTodosHabitos) { nuevaFecha ->
            fechaSeleccionada = nuevaFecha
            actualizarTareasDelDia()
        }
        rvCalendario.layoutManager = GridLayoutManager(this, 7)
        rvCalendario.adapter = adapter
    }

    private fun generarDiasMes(cal: Calendar): List<Calendar?> {
        val temp = cal.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)
        
        val diaSemana = temp.get(Calendar.DAY_OF_WEEK) 
        val desfase = if (diaSemana == Calendar.SUNDAY) 6 else diaSemana - 2
        
        val dias = mutableListOf<Calendar?>()
        for (i in 0 until desfase) dias.add(null)
        
        val maxDias = temp.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..maxDias) {
            val c = temp.clone() as Calendar
            c.set(Calendar.DAY_OF_MONTH, i)
            dias.add(c)
        }
        return dias
    }

    private fun setupTareasList() {
        rvTareasDia.layoutManager = LinearLayoutManager(this)
    }

    private fun actualizarTareasDelDia() {
        val dia = fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
        val mes = fechaSeleccionada.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("es", "ES"))
        tvFechaSeleccionada.text = "Actividades para el $dia de $mes"

        lifecycleScope.launch {
            // 1. Obtener tareas filtradas
            val todasLasTareas = tareaViewModel.getTareasDelDia(fechaSeleccionada.timeInMillis)
            val tareasFiltradas = todasLasTareas.filter { esTareaParaElDia(it, fechaSeleccionada) }
            
            // 2. Obtener hábitos para este día
            val idsCompletados = tareaViewModel.getIdsHabitosCompletadosEnFecha(fechaSeleccionada.timeInMillis)
            val habitosDelDia = listaTodosHabitos.filter { esHabitoParaElDia(it, fechaSeleccionada) }
            
            // 3. Unificar en lista de CalendarItem
            val items = mutableListOf<CalendarItem>()
            items.addAll(tareasFiltradas.map { CalendarItem.TareaItem(it) })
            items.addAll(habitosDelDia.map { CalendarItem.HabitoItem(it, idsCompletados.contains(it.id)) })

            rvTareasDia.adapter = TareaResumenAdapter(items, 
                onTareaCheckChanged = { tarea, isChecked ->
                    tareaViewModel.marcarCompletada(tarea, isChecked)
                },
                onHabitoCheckChanged = { habito ->
                    tareaViewModel.marcarHabitoCompletado(habito, fechaSeleccionada.timeInMillis)
                },
                onLongClick = { item ->
                    if (item is CalendarItem.TareaItem) mostrarMenuTarea(item.tarea)
                }
            )
        }
    }

    private fun esTareaParaElDia(tarea: Tarea, cal: Calendar): Boolean {
        val calTarea = Calendar.getInstance().apply { timeInMillis = tarea.fecha }
        if (esMismoDia(calTarea, cal)) return true
        if (cal.timeInMillis < tarea.fecha && !esMismoDia(calTarea, cal)) return false

        return when (tarea.recurrencia) {
            "DIARIO" -> true
            "SEMANAL" -> cal.get(Calendar.DAY_OF_WEEK) == calTarea.get(Calendar.DAY_OF_WEEK)
            "DIAS_SELECCIONADOS" -> {
                val diasValidos = tarea.diasSemana.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
                diasValidos.contains(cal.get(Calendar.DAY_OF_WEEK))
            }
            else -> false
        }
    }

    private fun esHabitoParaElDia(habito: Habito, cal: Calendar): Boolean {
        val calHabito = Calendar.getInstance().apply { timeInMillis = habito.fechaCreacion }
        if (cal.timeInMillis < habito.fechaCreacion && !esMismoDia(calHabito, cal)) return false
        
        val diasSemana = habito.diasSemana.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
        // Nota: Habito usa 1=Lunes, 7=Domingo. Calendar usa SUNDAY=1, MONDAY=2...
        val dayOfWeek = when(cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 0
        }
        return diasSemana.contains(dayOfWeek)
    }

    private fun mostrarMenuTarea(tarea: Tarea) {
        androidx.appcompat.app.AlertDialog.Builder(this, R.style.DialogoOscuro)
            .setTitle("Eliminar Tarea")
            .setMessage("¿Estás seguro de que quieres eliminar esta tarea?")
            .setPositiveButton("Eliminar") { _, _ ->
                tareaViewModel.eliminarTarea(tarea)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun esMismoDia(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
