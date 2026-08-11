package com.example.colorblend.ui.gacha

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.GenshinCharacter

class GenshinAdapter(private val onClick: (GenshinCharacter) -> Unit) :
    ListAdapter<GenshinCharacter, GenshinAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvCharNombre)
        val tvDetalle: TextView = view.findViewById(R.id.tvCharDetalle)
        val tvElemento: TextView = view.findViewById(R.id.tvCharElemento)
        val tvRareza: TextView = view.findViewById(R.id.tvCharRareza)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_genshin_character, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val char = getItem(position)
        holder.tvNombre.text = char.nombre
        holder.tvDetalle.text = "Nivel ${char.nivel} • C${char.constelacion}"
        holder.tvRareza.text = if (char.rareza == 5) "⭐⭐⭐⭐⭐" else "⭐⭐⭐⭐"
        
        val emojiElemento = when(char.elemento) {
            "Anemo" -> "🍃"
            "Geo" -> "🪨"
            "Electro" -> "⚡"
            "Dendro" -> "🌿"
            "Hydro" -> "💧"
            "Pyro" -> "🔥"
            "Cryo" -> "❄️"
            else -> "✨"
        }
        holder.tvElemento.text = emojiElemento

        holder.itemView.setOnClickListener { onClick(char) }
    }

    object DiffCallback : DiffUtil.ItemCallback<GenshinCharacter>() {
        override fun areItemsTheSame(oldItem: GenshinCharacter, newItem: GenshinCharacter) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: GenshinCharacter, newItem: GenshinCharacter) = oldItem == newItem
    }
}
