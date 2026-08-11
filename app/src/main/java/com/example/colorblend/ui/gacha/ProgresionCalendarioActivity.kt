package com.example.colorblend.ui.gacha

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.ProgresionRepository
import com.example.colorblend.domain.usecase.ProgresionUseCase
import java.text.SimpleDateFormat
import java.util.*

class ProgresionCalendarioActivity : AppCompatActivity() {

    private lateinit var viewModel: ProgresionViewModel
    private lateinit var tvMesAnio: TextView
    private lateinit var tvFechaSeleccionada: TextView
    private lateinit var rvCalendario: RecyclerView
    private lateinit var rvSesiones: RecyclerView
    
    private var calendarActual = Calendar.getInstance()
    private var fechaSeleccionada = Calendar.getInstance()
    private lateinit var sesionesAdapter: CalendarioEntrenamientoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progresion_calendario)
        FullScreenHelper.enable(this)

        val repository = ProgresionRepository(AppDatabase.getDatabase(this).progresionDao())
        val useCase = ProgresionUseCase()
        val factory = ProgresionViewModelFactory(repository, useCase)
        viewModel = ViewModelProvider(this, factory)[ProgresionViewModel::class.java]

        initViews()
        setupObservers()
        refrescarDatos()
    }

    private fun initViews() {
        tvMesAnio = findViewById(R.id.tvMesAnioProgresion)
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionadaCal)
        rvCalendario = findViewById(R.id.rvCalendarioProgresion)
        rvSesiones = findViewById(R.id.rvSesionesDelDia)

        findViewById<ImageButton>(R.id.btnBackCalProgresion).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnMesAnteriorCal).setOnClickListener {
            calendarActual.add(Calendar.MONTH, -1)
            refrescarDatos()
        }
        findViewById<ImageButton>(R.id.btnMesSiguienteCal).setOnClickListener {
            calendarActual.add(Calendar.MONTH, 1)
            refrescarDatos()
        }

        rvCalendario.layoutManager = GridLayoutManager(this, 7)
        rvSesiones.layoutManager = LinearLayoutManager(this)
        sesionesAdapter = CalendarioEntrenamientoAdapter()
        rvSesiones.adapter = sesionesAdapter
    }

    private fun setupObservers() {
        viewModel.sesionesDelMes.observe(this) { sesiones ->
            actualizarCalendario(sesiones)
            actualizarListaSesiones(sesiones)
        }
    }

    private fun refrescarDatos() {
        val mes = calendarActual.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("es", "ES"))
        val anio = calendarActual.get(Calendar.YEAR)
        tvMesAnio.text = "${mes?.uppercase()} $anio"
        
        viewModel.cargarSesionesMes(calendarActual.get(Calendar.MONTH), calendarActual.get(Calendar.YEAR))
    }

    private fun actualizarCalendario(sesiones: List<HistoryItem>) {
        val dias = generarDiasMes(calendarActual)
        val adapter = ProgresionCalendarioGridAdapter(dias, fechaSeleccionada, sesiones) { nuevaFecha ->
            fechaSeleccionada = nuevaFecha
            actualizarCalendario(sesiones) // Refrescar para iluminar el nuevo dia
            actualizarListaSesiones(viewModel.sesionesDelMes.value ?: emptyList())
        }
        rvCalendario.adapter = adapter
    }

    private fun actualizarListaSesiones(todas: List<HistoryItem>) {
        val dia = fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
        val mes = fechaSeleccionada.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("es", "ES"))
        tvFechaSeleccionada.text = "Sesiones del $dia de $mes"

        val filtradas = todas.filter { item ->
            val cal = Calendar.getInstance().apply { timeInMillis = item.sesion.fecha }
            cal.get(Calendar.YEAR) == fechaSeleccionada.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == fechaSeleccionada.get(Calendar.DAY_OF_YEAR)
        }
        sesionesAdapter.submitList(filtradas)
    }

    private fun generarDiasMes(cal: Calendar): List<Calendar?> {
        val temp = cal.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)
        val desfase = when(temp.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        val dias = mutableListOf<Calendar?>()
        for (i in 0 until desfase) dias.add(null)
        val max = temp.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..max) {
            val c = temp.clone() as Calendar
            c.set(Calendar.DAY_OF_MONTH, i)
            dias.add(c)
        }
        return dias
    }
}

class ProgresionCalendarioGridAdapter(
    private val dias: List<Calendar?>,
    private val seleccionada: Calendar,
    private val sesiones: List<HistoryItem>,
    private val onDiaClick: (Calendar) -> Unit
) : RecyclerView.Adapter<ProgresionCalendarioGridAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_calendario_dia, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dia = dias[position]
        if (dia == null) {
            holder.tvDia.text = ""
            holder.viewPunto.visibility = View.GONE
            holder.viewSeleccion.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
        } else {
            holder.tvDia.text = dia.get(Calendar.DAY_OF_MONTH).toString()
            
            val esSeleccionado = isSameDay(dia, seleccionada)
            val tieneEntrenamiento = sesiones.any { isSameDay(dia, Calendar.getInstance().apply { timeInMillis = it.sesion.fecha }) }

            holder.tvDia.setTextColor(if (esSeleccionado) Color.BLACK else Color.WHITE)
            holder.viewSeleccion.visibility = if (esSeleccionado) View.VISIBLE else View.GONE

            // Limpiamos indicadores previos
            holder.containerIndicadores.removeAllViews()
            if (tieneEntrenamiento) {
                val dot = View(holder.itemView.context).apply {
                    val size = (4 * holder.itemView.context.resources.displayMetrics.density).toInt()
                    layoutParams = ViewGroup.LayoutParams(size, size)
                    background = ContextCompat.getDrawable(context, R.drawable.blurred_point)
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.dash_secondary_gold))
                }
                holder.containerIndicadores.addView(dot)
            }

            holder.itemView.setOnClickListener { onDiaClick(dia) }
        }
    }

    override fun getItemCount() = dias.size

    private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvDia: TextView = v.findViewById(R.id.tvDiaNumero)
        val viewSeleccion: View = v.findViewById(R.id.viewSeleccion)
        val containerIndicadores: ViewGroup = v.findViewById(R.id.containerIndicadores)
        val viewPunto: View = View(v.context) // Dummy para no romper si se usa fuera
    }
}

class CalendarioEntrenamientoAdapter : RecyclerView.Adapter<CalendarioEntrenamientoAdapter.ViewHolder>() {
    private var items = listOf<HistoryItem>()
    private val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun submitList(newItems: List<HistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_calendario_entrenamiento, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNombre.text = item.nombreEjercicio
        val seriesResumen = item.series.joinToString(", ") { "${it.pesoKg}kg x ${it.reps}" }
        holder.tvSeries.text = "${item.series.size} series: $seriesResumen"
        holder.tvMolestia.text = "Molestia: ${item.registro?.molestiaArticular ?: 0}/10"
        holder.tvHora.text = timeFmt.format(Date(item.sesion.fecha))
    }

    override fun getItemCount() = items.size
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvNombre: TextView = v.findViewById(R.id.tvNombreEjCal)
        val tvSeries: TextView = v.findViewById(R.id.tvResumenSeriesCal)
        val tvMolestia: TextView = v.findViewById(R.id.tvMolestiaCal)
        val tvHora: TextView = v.findViewById(R.id.tvHoraCal)
    }
}
