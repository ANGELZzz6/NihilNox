package com.example.colorblend.ui.gacha

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.Habito
import com.example.colorblend.domain.model.Tarea
import java.util.*

class CalendarioAdapter(
    private val dias: List<Calendar?>,
    private var fechaSeleccionada: Calendar,
    private val tareas: List<Tarea> = emptyList(),
    private val habitos: List<Habito> = emptyList(),
    private val onDiaClick: (Calendar) -> Unit
) : RecyclerView.Adapter<CalendarioAdapter.DiaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendario_dia, parent, false)
        return DiaViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaViewHolder, position: Int) {
        val cal = dias[position]
        if (cal == null) {
            holder.tvDiaNumero.text = ""
            holder.viewSeleccion.visibility = View.GONE
            holder.containerIndicadores.removeAllViews()
            holder.itemView.setOnClickListener(null)
        } else {
            holder.tvDiaNumero.text = cal.get(Calendar.DAY_OF_MONTH).toString()
            
            val esMismoDia = esMismoDia(cal, fechaSeleccionada)
            holder.viewSeleccion.visibility = if (esMismoDia) View.VISIBLE else View.GONE
            
            val esHoy = esMismoDia(cal, Calendar.getInstance())
            if (esHoy && !esMismoDia) {
                holder.tvDiaNumero.setTextColor(Color.parseColor("#FFD700"))
            } else if (esMismoDia) {
                holder.tvDiaNumero.setTextColor(Color.BLACK)
            } else {
                holder.tvDiaNumero.setTextColor(Color.WHITE)
            }

            holder.itemView.setOnClickListener {
                fechaSeleccionada = cal
                onDiaClick(cal)
                notifyDataSetChanged()
            }

            actualizarIndicadores(holder, cal)
        }
    }

    override fun getItemCount(): Int = dias.size

    private fun actualizarIndicadores(holder: DiaViewHolder, cal: Calendar) {
        holder.containerIndicadores.removeAllViews()
        
        // 1. Tareas del día
        val tareasDelDia = tareas.filter { esTareaParaElDia(it, cal) }
        
        // 2. Hábitos del día
        val habitosDelDia = habitos.filter { esHabitoParaElDia(it, cal) }
        
        // Mostrar hasta 4 indicadores en total
        val totalIndicadores = mutableListOf<String>() // Lista de colores hex
        totalIndicadores.addAll(tareasDelDia.take(2).map { it.color })
        totalIndicadores.addAll(habitosDelDia.take(2).map { it.burbujaColor })
        
        totalIndicadores.forEach { colorHex ->
            val dot = View(holder.itemView.context).apply {
                val sizePx = (4 * resources.displayMetrics.density).toInt()
                layoutParams = ViewGroup.MarginLayoutParams(sizePx, sizePx).apply {
                    setMargins(2, 0, 2, 0)
                }
                
                val background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor(colorHex))
                }
                setBackground(background)
            }
            holder.containerIndicadores.addView(dot)
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
                val dias = tarea.diasSemana.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
                dias.contains(cal.get(Calendar.DAY_OF_WEEK))
            }
            else -> false
        }
    }

    private fun esHabitoParaElDia(habito: Habito, cal: Calendar): Boolean {
        val calHabito = Calendar.getInstance().apply { timeInMillis = habito.fechaCreacion }
        if (cal.timeInMillis < habito.fechaCreacion && !esMismoDia(calHabito, cal)) return false

        val diasSemana = habito.diasSemana.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
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

    private fun esMismoDia(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    class DiaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDiaNumero: TextView = view.findViewById(R.id.tvDiaNumero)
        val viewSeleccion: View = view.findViewById(R.id.viewSeleccion)
        val containerIndicadores: ViewGroup = view.findViewById(R.id.containerIndicadores)
    }
}
