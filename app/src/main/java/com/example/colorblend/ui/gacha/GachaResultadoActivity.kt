package com.example.colorblend.ui.gacha

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.colorblend.R
import com.example.colorblend.ui.gacha.metas.viewmodels.CharacterUI
import java.io.File

class GachaResultadoActivity : AppCompatActivity() {

    private var personajes: List<CharacterUI> = emptyList()
    private var indiceActual = 0
    private lateinit var miniAdapter: MiniPersonajesAdapter
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gacha_resultado)

        @Suppress("DEPRECATION")
        personajes = (intent.getSerializableExtra("personajes") as? ArrayList<*>)
            ?.filterIsInstance<CharacterUISerializable>()
            ?.map { CharacterUI(it.title, it.imageUrl, it.score, it.origen, it.categoria) }
            ?: emptyList()

        if (personajes.isEmpty()) { finish(); return }

        val recyclerMini = findViewById<RecyclerView>(R.id.recyclerMiniPersonajes)
        miniAdapter = MiniPersonajesAdapter(personajes) { indice ->
            mostrarPersonaje(indice, animado = true)
        }
        recyclerMini.adapter = miniAdapter
        recyclerMini.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        handler.postDelayed({ mostrarPersonaje(0, animado = true) }, 300)

        findViewById<Button>(R.id.btnSiguientePersonaje).setOnClickListener {
            if (indiceActual < personajes.size - 1) mostrarPersonaje(indiceActual + 1, animado = true)
        }
        findViewById<Button>(R.id.btnAnteriorPersonaje).setOnClickListener {
            if (indiceActual > 0) mostrarPersonaje(indiceActual - 1, animado = true)
        }
        findViewById<Button>(R.id.btnCerrarResultado).setOnClickListener { finish() }
    }

    private fun mostrarPersonaje(indice: Int, animado: Boolean) {
        if (indice < 0 || indice >= personajes.size) return
        indiceActual = indice
        val personaje = personajes[indice]

        val ivCentral   = findViewById<ImageView>(R.id.ivPersonajeCentral)
        val bordeRareza = findViewById<View>(R.id.bordeRarezaCentral)
        val layoutInfo  = findViewById<View>(R.id.layoutInfoCentral)
        val viewGlow    = findViewById<View>(R.id.viewGlowFondo)
        val tvNombre    = findViewById<TextView>(R.id.tvNombreCentral)
        val tvRareza    = findViewById<TextView>(R.id.tvRarezaCentral)
        val tvOrigen    = findViewById<TextView>(R.id.tvOrigenCentral)
        val tvScore     = findViewById<TextView>(R.id.tvScoreCentral)
        val tvContador  = findViewById<TextView>(R.id.tvContador)

        tvContador.text = "${indice + 1} / ${personajes.size}"
        val favoritos = personaje.score ?: 0
        val (rarezaTexto, colorHex) = getRarezaYColor(personaje)

        tvNombre.text = personaje.title ?: "Desconocido"
        tvRareza.text = rarezaTexto
        tvOrigen.text = personaje.origen ?: ""
        tvScore.text  = "⭐ $favoritos"
        tvRareza.setTextColor(Color.parseColor(colorHex))

        val bordeDraw = GradientDrawable()
        bordeDraw.setStroke(6, Color.parseColor(colorHex))
        bordeDraw.cornerRadius = 8f
        bordeRareza.background = bordeDraw

        val glowDraw = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor(colorHex), Color.TRANSPARENT)
        )
        glowDraw.gradientType = GradientDrawable.RADIAL_GRADIENT
        glowDraw.gradientRadius = 400f
        viewGlow.background = glowDraw

        // ── Cargar imagen — soporta ruta local y URL ──────────────────────────
        val url = personaje.imageUrl ?: ""
        if (url.startsWith("/")) {
            Glide.with(this).load(File(url)).into(ivCentral)
        } else {
            Glide.with(this).load(glideUrl(url)).into(ivCentral)
        }

        miniAdapter.setActivo(indice)

        findViewById<Button>(R.id.btnAnteriorPersonaje).isEnabled = indice > 0
        findViewById<Button>(R.id.btnAnteriorPersonaje).alpha = if (indice > 0) 1f else 0.3f
        val btnSig = findViewById<Button>(R.id.btnSiguientePersonaje)
        if (indice == personajes.size - 1) {
            btnSig.text = "VER COLECCIÓN ✦"
            btnSig.setOnClickListener { finish() }
        } else {
            btnSig.text = "SIGUIENTE ▶"
            btnSig.setOnClickListener { mostrarPersonaje(indiceActual + 1, animado = true) }
        }

        if (!animado) {
            ivCentral.alpha   = 1f
            bordeRareza.alpha = 1f
            layoutInfo.alpha  = 1f
            viewGlow.alpha    = 0.15f
            return
        }

        ivCentral.alpha   = 0f
        ivCentral.scaleX  = 0.7f
        ivCentral.scaleY  = 0.7f
        bordeRareza.alpha = 0f
        layoutInfo.alpha  = 0f
        viewGlow.alpha    = 0f

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(ivCentral, "alpha", 0f, 1f).apply { duration = 350 },
                ObjectAnimator.ofFloat(ivCentral, "scaleX", 0.7f, 1f).apply { duration = 400; interpolator = OvershootInterpolator(1.5f) },
                ObjectAnimator.ofFloat(ivCentral, "scaleY", 0.7f, 1f).apply { duration = 400; interpolator = OvershootInterpolator(1.5f) },
                ObjectAnimator.ofFloat(viewGlow, "alpha", 0f, 0.15f).apply { duration = 500 }
            )
            start()
        }

        handler.postDelayed({
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(bordeRareza, "alpha", 0f, 1f).apply { duration = 300 },
                    ObjectAnimator.ofFloat(layoutInfo, "alpha", 0f, 1f).apply { duration = 300; interpolator = AccelerateDecelerateInterpolator() }
                )
                start()
            }
        }, 250)
    }

    private fun glideUrl(url: String?): Any {
        if (url.isNullOrEmpty()) return ""
        return if (url.contains("superherodb") || url.contains("superheroapi")) {
            GlideUrl(url, LazyHeaders.Builder()
                .addHeader("Referer", "https://www.superherodb.com/")
                .addHeader("User-Agent", "Mozilla/5.0")
                .build())
        } else url
    }

    private fun getRarezaYColor(personaje: CharacterUI): Pair<String, String> {
        val favoritos = personaje.score ?: 0
        return when (personaje.categoria) {
            "superhero" -> when {
                favoritos >= 480 -> "🟡 LEGENDARIO" to "#FFD700"
                favoritos >= 320 -> "🟣 ÉPICO"      to "#9B59B6"
                favoritos >= 160 -> "🔵 RARO"       to "#3498DB"
                else             -> "⚪ COMÚN"      to "#95A5A6"
            }
            "videojuego" -> when {
                favoritos >= 9000 -> "🟡 LEGENDARIO" to "#FFD700"
                favoritos >= 7500 -> "🟣 ÉPICO"      to "#9B59B6"
                favoritos >= 5500 -> "🔵 RARO"       to "#3498DB"
                else              -> "⚪ COMÚN"      to "#95A5A6"
            }
            else -> when {
                favoritos >= 50000 -> "🟡 LEGENDARIO" to "#FFD700"
                favoritos >= 13000 -> "🟣 ÉPICO"      to "#9B59B6"
                favoritos >= 1000  -> "🔵 RARO"       to "#3498DB"
                else               -> "⚪ COMÚN"      to "#95A5A6"
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}

fun buildGlideUrl(url: String?): Any {
    if (url.isNullOrEmpty()) return ""
    return if (url.contains("superherodb") || url.contains("superheroapi")) {
        GlideUrl(url, LazyHeaders.Builder()
            .addHeader("Referer", "https://www.superherodb.com/")
            .addHeader("User-Agent", "Mozilla/5.0")
            .build())
    } else url
}

@Suppress("DEPRECATION")
class CharacterUISerializable(
    val title: String?,
    val imageUrl: String?,
    val score: Int?,
    val origen: String?,
    val categoria: String?
) : java.io.Serializable

fun CharacterUI.toSerializable() =
    CharacterUISerializable(title, imageUrl, score, origen, categoria)

class MiniPersonajesAdapter(
    private val personajes: List<CharacterUI>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<MiniPersonajesAdapter.ViewHolder>() {

    private var indiceActivo = 0

    fun setActivo(indice: Int) {
        val anterior = indiceActivo
        indiceActivo = indice
        notifyItemChanged(anterior)
        notifyItemChanged(indice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mini_personaje, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(personajes[position], position)

    override fun getItemCount() = personajes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(personaje: CharacterUI, pos: Int) {
            val miniImage = itemView.findViewById<ImageView>(R.id.miniImage)
            val miniBorde = itemView.findViewById<View>(R.id.miniBorde)
            val indicador = itemView.findViewById<View>(R.id.miniIndicadorActivo)

            // ── Cargar imagen — soporta ruta local y URL ──────────────────────
            val url = personaje.imageUrl ?: ""
            if (url.startsWith("/")) {
                Glide.with(itemView.context).load(File(url)).into(miniImage)
            } else {
                Glide.with(itemView.context).load(buildGlideUrl(url)).into(miniImage)
            }

            val activo = pos == indiceActivo
            miniImage.alpha = if (activo) 1f else 0.45f
            indicador.visibility = if (activo) View.VISIBLE else View.GONE

            val favoritos = personaje.score ?: 0
            val colorHex = when (personaje.categoria) {
                "superhero" -> when {
                    favoritos >= 480 -> "#FFD700"
                    favoritos >= 320 -> "#9B59B6"
                    favoritos >= 160 -> "#3498DB"
                    else -> "#95A5A6"
                }
                "videojuego" -> when {
                    favoritos >= 9000 -> "#FFD700"
                    favoritos >= 7500 -> "#9B59B6"
                    favoritos >= 5500 -> "#3498DB"
                    else -> "#95A5A6"
                }
                else -> when {
                    favoritos >= 50000 -> "#FFD700"
                    favoritos >= 13000 -> "#9B59B6"
                    favoritos >= 1000  -> "#3498DB"
                    else -> "#95A5A6"
                }
            }
            val borde = GradientDrawable()
            borde.setStroke(if (activo) 4 else 2, Color.parseColor(colorHex))
            borde.cornerRadius = 6f
            miniBorde.background = borde

            itemView.setOnClickListener { onClick(pos) }
        }
    }
}