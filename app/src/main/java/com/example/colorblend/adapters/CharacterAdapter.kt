package com.example.colorblend.adapters

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.example.colorblend.ui.gacha.buildGlideUrl
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.colorblend.R
import com.example.colorblend.ui.gacha.metas.viewmodels.CharacterUI

class CharacterAdapter(
    private var characters: List<CharacterUI>
) : RecyclerView.Adapter<CharacterAdapter.ViewHolder>() {

    fun update(newList: List<CharacterUI>) {
        characters = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_character, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(characters[position])
    }

    override fun getItemCount() = characters.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image = itemView.findViewById<ImageView>(R.id.charImage)
        private val name = itemView.findViewById<TextView>(R.id.charName)
        private val score = itemView.findViewById<TextView>(R.id.charScore)
        private val rarezaLabel = itemView.findViewById<TextView>(R.id.charRareza)
        private val borde = itemView.findViewById<View>(R.id.rarezaBorde)
        private val glow = itemView.findViewById<View>(R.id.rarezaGlow)
        private val origen = itemView.findViewById<TextView>(R.id.charOrigen)

        fun bind(character: CharacterUI) {
            name.text = character.title
            score.text = "⭐ ${character.score}"
            origen.text = character.origen ?: ""

            Glide.with(itemView.context)
                .load(buildGlideUrl(character.imageUrl))
                .placeholder(android.R.color.darker_gray)
                .into(image)

            val favoritos = character.score ?: 0
            val (emoji, colorHex) = getRarezaYColor(character)

            rarezaLabel.text = emoji

            // Borde de rareza
            val drawable = GradientDrawable()
            drawable.setStroke(6, Color.parseColor(colorHex))
            drawable.cornerRadius = 8f
            borde.background = drawable

            // ✅ Glow exterior — solo para legendarios y épicos
            val esRaroOSuperior = when (character.categoria) {
                "superhero"  -> favoritos >= 160
                "videojuego" -> favoritos >= 5500
                else         -> favoritos >= 1000
            }

            if (esRaroOSuperior) {
                val glowDraw = GradientDrawable()
                glowDraw.setStroke(10, Color.parseColor(colorHex))
                glowDraw.cornerRadius = 12f
                glow.background = glowDraw
                glow.visibility = View.VISIBLE

                // ✅ Animación de pulso en el glow para legendarios
                val esLegendario = when (character.categoria) {
                    "superhero"  -> favoritos >= 480
                    "videojuego" -> favoritos >= 9000
                    else         -> favoritos >= 50000
                }
                if (esLegendario) {
                    val pulso = ObjectAnimator.ofFloat(glow, "alpha", 0.15f, 0.55f).apply {
                        duration = 900
                        repeatMode = ValueAnimator.REVERSE
                        repeatCount = ValueAnimator.INFINITE
                        interpolator = AccelerateDecelerateInterpolator()
                    }
                    pulso.start()
                } else {
                    glow.alpha = 0.25f
                }
            } else {
                glow.visibility = View.INVISIBLE
            }

            // Animación de entrada con fade + scale
            itemView.alpha = 0f
            itemView.scaleX = 0.85f
            itemView.scaleY = 0.85f
            itemView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(280)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        private fun getRarezaYColor(character: CharacterUI): Pair<String, String> {
            val favoritos = character.score ?: 0
            return when (character.categoria) {
                "superhero" -> when {
                    favoritos >= 480 -> "🟡 Legendario" to "#FFD700"
                    favoritos >= 320 -> "🟣 Épico"      to "#9B59B6"
                    favoritos >= 160 -> "🔵 Raro"       to "#3498DB"
                    else             -> "⚪ Común"      to "#95A5A6"
                }
                "videojuego" -> when {
                    favoritos >= 9000 -> "🟡 Legendario" to "#FFD700"
                    favoritos >= 7500 -> "🟣 Épico"      to "#9B59B6"
                    favoritos >= 5500 -> "🔵 Raro"       to "#3498DB"
                    else              -> "⚪ Común"      to "#95A5A6"
                }
                else -> when {
                    favoritos >= 50000 -> "🟡 Legendario" to "#FFD700"
                    favoritos >= 13000 -> "🟣 Épico"      to "#9B59B6"
                    favoritos >= 1000  -> "🔵 Raro"       to "#3498DB"
                    else               -> "⚪ Común"      to "#95A5A6"
                }
            }
        }
    }
}