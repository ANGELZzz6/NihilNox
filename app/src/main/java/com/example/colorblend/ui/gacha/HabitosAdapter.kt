package com.example.colorblend.ui.gacha

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.databinding.ItemHabitoBinding
import com.example.colorblend.domain.model.EstadoRacha
import com.example.colorblend.domain.model.Habito
import com.example.colorblend.domain.model.HabitoConEstado

class HabitosAdapter(
    private val onCompletar: (Habito) -> Unit,
    private val onEliminar: (Habito) -> Unit,
    private val onConfigurarNotif: (Habito) -> Unit,
    private val onDesactivarNotif: (Habito) -> Unit
) : ListAdapter<HabitoConEstado, HabitosAdapter.ViewHolder>(DiffCallback()) {

    private var lastPosition = -1

    inner class ViewHolder(val binding: ItemHabitoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHabitoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val habito = item.habito
        val estado = item.estado
        
        with(holder.binding) {
            tvNombreHabito.text = habito.nombre

            // Ancla (habit stacking)
            if (habito.ancla.isNotBlank()) {
                tvAncla.text = "↳ después de ${habito.ancla}"
                tvAncla.visibility = View.VISIBLE
            } else {
                tvAncla.visibility = View.GONE
            }

            // Racha
            tvRacha.text = when {
                habito.rachaActual >= 2 -> "🔥 ${habito.rachaActual} días · mejor: ${habito.rachaMaxima}"
                habito.rachaActual == 1 -> "🔥 1 día"
                else -> "Sin racha aún · ${habito.totalCompletados} completados en total"
            }

            // Estado visual
            when (estado) {
                EstadoRacha.COMPLETADO -> {
                    indicadorEstado.setBackgroundColor(Color.parseColor("#4CAF50"))
                    btnCompletar.text = "✓"
                    btnCompletar.setTextColor(Color.parseColor("#4CAF50"))
                    tvAlerta.visibility = View.GONE
                    cardHabito.alpha = 0.75f
                }
                EstadoRacha.EN_RIESGO -> {
                    indicadorEstado.setBackgroundColor(Color.parseColor("#FF9800"))
                    btnCompletar.text = "○"
                    btnCompletar.setTextColor(Color.WHITE)
                    tvAlerta.text = "⚠ Complétalo hoy para no romper la racha"
                    tvAlerta.setTextColor(Color.parseColor("#FF9800"))
                    tvAlerta.visibility = View.VISIBLE
                    cardHabito.alpha = 1f
                }
                EstadoRacha.ROTA -> {
                    indicadorEstado.setBackgroundColor(Color.parseColor("#F44336"))
                    btnCompletar.text = "○"
                    btnCompletar.setTextColor(Color.WHITE)
                    tvAlerta.text = "Racha rota · empieza de nuevo hoy"
                    tvAlerta.setTextColor(Color.parseColor("#F44336"))
                    tvAlerta.visibility = View.VISIBLE
                    cardHabito.alpha = 1f
                }
                EstadoRacha.PENDIENTE -> {
                    indicadorEstado.setBackgroundColor(Color.parseColor("#555555"))
                    btnCompletar.text = "○"
                    btnCompletar.setTextColor(Color.WHITE)
                    tvAlerta.visibility = View.GONE
                    cardHabito.alpha = 1f
                }
            }

            // Notificación
            holder.itemView.setOnLongClickListener {
                val layout = layoutNotificacion
                if (layout.visibility == View.GONE) {
                    layout.visibility = View.VISIBLE
                    layout.alpha = 0f
                    layout.animate().alpha(1f).setDuration(200).start()
                } else {
                    layout.animate().alpha(0f).setDuration(150)
                        .withEndAction { layout.visibility = View.GONE }.start()
                }
                true
            }

            switchNotif.setOnCheckedChangeListener(null)
            switchNotif.isChecked = habito.notificacionHabilitada
            tvHoraNotif.text = if (habito.notificacionHabilitada)
                "🔔 %02d:%02d".format(habito.notificacionHora, habito.notificacionMinuto)
            else "🔔 Sin recordatorio"

            switchNotif.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) onConfigurarNotif(habito)
                else onDesactivarNotif(habito)
            }

            btnCompletar.setOnClickListener {
                if (!habito.completadoHoy) {
                    // animación de recompensa inmediata
                    btnCompletar.animate()
                        .scaleX(1.4f).scaleY(1.4f).setDuration(150)
                        .withEndAction {
                            btnCompletar.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }.start()
                    onCompletar(habito)
                }
            }
            btnEliminar.setOnClickListener { onEliminar(habito) }
        }

        // animación de entrada solo para items nuevos (scroll down)
        val pos = holder.bindingAdapterPosition
        if (pos > lastPosition) {
            holder.itemView.alpha = 0f
            holder.itemView.translationY = 30f
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(pos * 50L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
            lastPosition = pos
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<HabitoConEstado>() {
        override fun areItemsTheSame(a: HabitoConEstado, b: HabitoConEstado) = a.habito.id == b.habito.id
        override fun areContentsTheSame(a: HabitoConEstado, b: HabitoConEstado) = a == b
    }
}