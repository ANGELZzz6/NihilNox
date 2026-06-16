package com.example.colorblend.ui.gacha.metas

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.Meta
import com.example.colorblend.domain.model.TipoMeta
import com.example.colorblend.ui.gacha.metas.viewmodels.MetaViewModel
import java.util.Calendar

class MetaAdapter(
    private val onCumplirClick: (Meta) -> Unit,
    private val onSumarClick: (Meta) -> Unit,
    private val metaViewModel: MetaViewModel
) : RecyclerView.Adapter<MetaAdapter.ViewHolder>() {

    private var metas: List<Meta> = emptyList()

    fun update(nuevas: List<Meta>) { metas = nuevas; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meta, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(metas[position])
    override fun getItemCount() = metas.size

    private fun obtenerInicioDelDia(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val titulo       = itemView.findViewById<TextView>(R.id.metaTitulo)
        private val progreso     = itemView.findViewById<TextView>(R.id.metaProgreso)
        private val progressBar  = itemView.findViewById<ProgressBar>(R.id.metaProgressBar)
        private val layoutRacha  = itemView.findViewById<View>(R.id.layoutRacha)
        private val tvRacha      = itemView.findViewById<TextView>(R.id.metaRacha)
        private val tvMejorRacha = itemView.findViewById<TextView>(R.id.metaMejorRacha)
        private val boton        = itemView.findViewById<Button>(R.id.metaBoton)
        private val btnBasura    = itemView.findViewById<ImageButton>(R.id.btnEliminarMeta)
        private val tipoBadge    = itemView.findViewById<TextView>(R.id.metaTipoBadge)
        private val stripe       = itemView.findViewById<View>(R.id.metaStripe)

        fun bind(meta: Meta) {
            titulo.text = meta.titulo

            itemView.setOnClickListener { abrirDetalle(meta) }

            if (meta.tipo == TipoMeta.DIARIA) {
                tipoBadge.text = "DIARIA"
                stripe.setBackgroundColor(Color.parseColor("#4FC3F7"))
                layoutRacha.visibility = View.VISIBLE
                tvRacha.text      = "🔥 Racha: ${meta.rachaActual} días"
                tvMejorRacha.text = "🏆 Mejor: ${meta.mejorRacha}"
            } else {
                tipoBadge.text = "ACUMULATIVA"
                stripe.setBackgroundColor(Color.parseColor("#FFD700"))
                layoutRacha.visibility = View.GONE
            }

            val pct = if (meta.objetivo > 0)
                (meta.progresoActual * 100 / meta.objetivo).coerceIn(0, 100) else 0
            progreso.text = "${meta.progresoActual} / ${meta.objetivo}  ($pct%)"
            progressBar.progress = pct
            progressBar.max = 100

            btnBasura.setOnClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Eliminar meta")
                    .setMessage("¿Eliminar \"${meta.titulo}\"? No se puede deshacer.")
                    .setPositiveButton("Eliminar") { _, _ -> metaViewModel.eliminarMeta(meta) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            configurarBoton(meta)
        }

        private fun configurarBoton(meta: Meta) {
            when {
                meta.finalizada -> {
                    boton.text = "✅ Completada"
                    boton.isEnabled = false
                    boton.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#444444"))
                }

                meta.tipo == TipoMeta.DIARIA -> {
                    val hoy     = obtenerInicioDelDia()
                    val yaHecha = meta.ultimaFecha == hoy
                    val diaHabilitado = meta.hoyEsDiaHabilitado()

                    when {
                        yaHecha -> {
                            boton.text = "✅ Cumplida hoy"
                            boton.isEnabled = false
                            boton.backgroundTintList =
                                android.content.res.ColorStateList.valueOf(Color.parseColor("#2A5C2A"))
                        }
                        !diaHabilitado -> {
                            // Hoy no es un día habilitado para esta meta
                            val diasTexto = meta.diasSemanaTexto()
                            boton.text = "⏸ No toca hoy"
                            boton.isEnabled = false
                            boton.backgroundTintList =
                                android.content.res.ColorStateList.valueOf(Color.parseColor("#2A2A3A"))
                            // Mostrar qué días toca al hacer long press
                            boton.setOnLongClickListener {
                                AlertDialog.Builder(itemView.context)
                                    .setTitle("Días habilitados")
                                    .setMessage("Esta meta está programada para: $diasTexto")
                                    .setPositiveButton("Ok", null)
                                    .show()
                                true
                            }
                        }
                        else -> {
                            boton.text = "✓ Cumplí hoy"
                            boton.isEnabled = true
                            boton.backgroundTintList =
                                android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD700"))
                            boton.setOnClickListener { mostrarPopupAntesDeCumplir(meta) }
                        }
                    }
                }

                else -> {
                    // ACUMULATIVA — no depende del día
                    boton.text = "+1 Progreso"
                    boton.isEnabled = true
                    boton.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD700"))
                    boton.setOnClickListener { mostrarPopupAntesDeCumplir(meta) }
                }
            }
        }

        private fun mostrarPopupAntesDeCumplir(meta: Meta) {
            val context = itemView.context
            AlertDialog.Builder(context)
                .setTitle("📸 Registro del día")
                .setMessage("¿Quieres agregar una foto de hoy para registrar tu avance?")
                .setPositiveButton("📷 Sí, agregar foto") { _, _ ->
                    val intent = Intent(context, MetaDetalleActivity::class.java).apply {
                        putExtra("metaId",          meta.id)
                        putExtra("metaTitulo",      meta.titulo)
                        putExtra("metaTipo",        meta.tipo.name)
                        putExtra("metaDescripcion", meta.descripcion ?: "")
                        putExtra("metaObjetivo",    meta.objetivo)
                        putExtra("metaProgreso",    meta.progresoActual)
                        putExtra("metaRacha",       meta.rachaActual)
                        putExtra("diasSemana",      meta.diasSemana ?: "")
                        putExtra("abrirCamara",     true)
                    }
                    context.startActivity(intent)
                }
                .setNegativeButton("Continuar sin foto") { _, _ ->
                    abrirValidacionIA(meta)
                }
                .show()
        }

        private fun abrirDetalle(meta: Meta) {
            val context = itemView.context
            val intent = Intent(context, MetaDetalleActivity::class.java).apply {
                putExtra("metaId",          meta.id)
                putExtra("metaTitulo",      meta.titulo)
                putExtra("metaTipo",        meta.tipo.name)
                putExtra("metaDescripcion", meta.descripcion ?: "")
                putExtra("metaObjetivo",    meta.objetivo)
                putExtra("metaProgreso",    meta.progresoActual)
                putExtra("metaRacha",       meta.rachaActual)
                putExtra("diasSemana",      meta.diasSemana ?: "")
                putExtra("abrirCamara",     false)
            }
            context.startActivity(intent)
        }

        private fun abrirValidacionIA(meta: Meta) {
            val context = itemView.context
            val intent = Intent(context, ValidarMetaActivity::class.java).apply {
                putExtra("metaId",          meta.id)
                putExtra("metaTitulo",      meta.titulo)
                putExtra("metaDescripcion", meta.descripcion ?: "")
                putExtra("metaTipo",        meta.tipo.name)
                putExtra("metaObjetivo",    meta.objetivo)
                putExtra("metaProgreso",    meta.progresoActual)
                putExtra("metaRacha",       meta.rachaActual)
                putExtra("maxPreguntas",    if (meta.tipo == TipoMeta.DIARIA) 5 else 2)
            }
            context.startActivity(intent)
        }
    }
}