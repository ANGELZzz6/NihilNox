package com.example.colorblend.ui.gacha

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.Habito
import com.example.colorblend.domain.model.Tarea

class TareaResumenAdapter(
    private val items: List<CalendarItem>,
    private val onTareaCheckChanged: (Tarea, Boolean) -> Unit,
    private val onHabitoCheckChanged: (Habito) -> Unit,
    private val onLongClick: (CalendarItem) -> Unit
) : RecyclerView.Adapter<TareaResumenAdapter.TareaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tarea_resumen, parent, false)
        return TareaViewHolder(view)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        val item = items[position]
        
        when (item) {
            is CalendarItem.TareaItem -> {
                val tarea = item.tarea
                holder.tvTitulo.text = tarea.titulo
                holder.viewColor.setBackgroundColor(Color.parseColor(tarea.color))
                
                val horaStr = String.format("%02d:%02d", tarea.hora, tarea.minuto)
                holder.tvInfo.text = "📌 $horaStr · ${tarea.recurrencia.replace("_", " ")}"
                
                holder.cbCompletada.setOnCheckedChangeListener(null)
                holder.cbCompletada.isChecked = tarea.completada
                actualizarEstiloCompletada(holder.tvTitulo, tarea.completada)
                
                holder.cbCompletada.setOnCheckedChangeListener { _, isChecked ->
                    actualizarEstiloCompletada(holder.tvTitulo, isChecked)
                    onTareaCheckChanged(tarea, isChecked)
                }
            }
            is CalendarItem.HabitoItem -> {
                val habito = item.habito
                holder.tvTitulo.text = habito.nombre
                holder.viewColor.setBackgroundColor(Color.parseColor(habito.burbujaColor))
                
                val horaStr = String.format("%02d:%02d", habito.notificacionHora, habito.notificacionMinuto)
                holder.tvInfo.text = "🔥 $horaStr · Hábito diario"
                
                holder.cbCompletada.setOnCheckedChangeListener(null)
                holder.cbCompletada.isChecked = item.completadoEsteDia
                actualizarEstiloCompletada(holder.tvTitulo, item.completadoEsteDia)
                
                holder.cbCompletada.isEnabled = !item.completadoEsteDia
                holder.cbCompletada.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        actualizarEstiloCompletada(holder.tvTitulo, true)
                        onHabitoCheckChanged(habito)
                    }
                }
            }
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    private fun actualizarEstiloCompletada(textView: TextView, completada: Boolean) {
        if (completada) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            textView.alpha = 0.5f
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            textView.alpha = 1.0f
        }
    }

    override fun getItemCount(): Int = items.size

    class TareaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTareaTitulo)
        val tvInfo: TextView = view.findViewById(R.id.tvTareaInfo)
        val viewColor: View = view.findViewById(R.id.viewColorTarea)
        val cbCompletada: CheckBox = view.findViewById(R.id.cbTareaCompletada)
    }
}
