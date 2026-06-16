package com.example.colorblend.ui.gacha

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.colorblend.ui.gacha.buildGlideUrl
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.colorblend.R
import com.example.colorblend.domain.model.AnimeResumen

class ColeccionAdapter(
    private var animes: List<AnimeResumen> = emptyList()
) : RecyclerView.Adapter<ColeccionAdapter.ViewHolder>() {

    fun update(nuevos: List<AnimeResumen>) {
        animes = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anime, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(animes[position])
    }

    override fun getItemCount() = animes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val portada = itemView.findViewById<ImageView>(R.id.animePortada)
        private val titulo = itemView.findViewById<TextView>(R.id.animeTitulo)

        fun bind(anime: AnimeResumen) {
            titulo.text = anime.animeTitulo
            Glide.with(itemView.context)
                .load(buildGlideUrl(anime.animeCoverUrl))  // ← cambiado
                .into(portada)

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, PersonajesAnimeActivity::class.java)
                intent.putExtra("animeId", anime.animeId)
                intent.putExtra("animeTitulo", anime.animeTitulo)
                itemView.context.startActivity(intent)
            }
        }
    }
}