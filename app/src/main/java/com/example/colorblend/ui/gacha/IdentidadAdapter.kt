package com.example.colorblend.ui.gacha

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.databinding.ItemIdentidadBinding
import com.example.colorblend.domain.model.Identidad

class IdentidadAdapter(
    private val onEliminar: (Identidad) -> Unit
) : ListAdapter<Identidad, IdentidadAdapter.ViewHolder>(DiffCallback()) {

    private var lastPosition = -1

    inner class ViewHolder(val binding: ItemIdentidadBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemIdentidadBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val identidad = getItem(position)
        with(holder.binding) {
            tvDeclaracion.text = "Soy alguien que ${identidad.declaracion}"
            tvVotos.text = "🗳 ${identidad.votosTotal} votos acumulados"
            btnEliminarIdentidad.setOnClickListener { onEliminar(identidad) }
        }

        // animación de entrada
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

    class DiffCallback : DiffUtil.ItemCallback<Identidad>() {
        override fun areItemsTheSame(a: Identidad, b: Identidad) = a.id == b.id
        override fun areContentsTheSame(a: Identidad, b: Identidad) = a == b
    }
}
