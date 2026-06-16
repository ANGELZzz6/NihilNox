package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.AnalisisDia
import kotlinx.coroutines.launch

class HistorialNutricionActivity : AppCompatActivity() {

    private val viewModel: NutricionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_nutricion)

        val recycler  = findViewById<RecyclerView>(R.id.recyclerHistorial)
        val tvVacio   = findViewById<TextView>(R.id.tvHistorialVacio)
        val btnVolver = findViewById<Button>(R.id.btnVolverHistorial)

        btnVolver.setOnClickListener { finish() }

        val adapter = HistorialAdapter(
            onReclamar = { analisis ->
                viewModel.reclamarRecompensa(analisis)
                Toast.makeText(
                    this,
                    "🪙 +${analisis.monedasRecompensa} monedas reclamadas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            viewModel.historial.collect { lista ->
                if (lista.isEmpty()) {
                    tvVacio.visibility  = View.VISIBLE
                    recycler.visibility = View.GONE
                } else {
                    tvVacio.visibility  = View.GONE
                    recycler.visibility = View.VISIBLE
                    adapter.update(lista)
                }
            }
        }
    }
}

class HistorialAdapter(
    private val onReclamar: (AnalisisDia) -> Unit
) : RecyclerView.Adapter<HistorialAdapter.VH>() {

    private var lista = listOf<AnalisisDia>()

    fun update(nueva: List<AnalisisDia>) {
        lista = nueva
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial_dia, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(lista[position])
    override fun getItemCount() = lista.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(a: AnalisisDia) {
            itemView.findViewById<TextView>(R.id.tvHistorialFecha).text    = a.fecha
            itemView.findViewById<TextView>(R.id.tvHistorialCalorias).text = "${a.calorias} kcal"

            itemView.findViewById<TextView>(R.id.tvHistorialProteina).text        = "${a.proteina.toInt()}g"
            itemView.findViewById<ProgressBar>(R.id.pbHistorialProteina).progress = a.proteina.toInt()
            itemView.findViewById<TextView>(R.id.tvHistorialCarbos).text          = "${a.carbos.toInt()}g"
            itemView.findViewById<ProgressBar>(R.id.pbHistorialCarbos).progress   = a.carbos.toInt()
            itemView.findViewById<TextView>(R.id.tvHistorialGrasas).text          = "${a.grasas.toInt()}g"
            itemView.findViewById<ProgressBar>(R.id.pbHistorialGrasas).progress   = a.grasas.toInt()
            itemView.findViewById<TextView>(R.id.tvHistorialFibra).text           = "${a.fibra.toInt()}g"
            itemView.findViewById<ProgressBar>(R.id.pbHistorialFibra).progress    = a.fibra.toInt()
            itemView.findViewById<TextView>(R.id.tvHistorialAzucares).text        = "${a.azucares.toInt()}g"
            itemView.findViewById<ProgressBar>(R.id.pbHistorialAzucares).progress = a.azucares.toInt()

            // Análisis IA
            val tvAnalisis = itemView.findViewById<TextView>(R.id.tvHistorialAnalisis)
            val btnVer     = itemView.findViewById<Button>(R.id.btnVerAnalisis)
            if (a.analisisIA.isNotBlank()) {
                btnVer.visibility = View.VISIBLE
                btnVer.setOnClickListener {
                    if (tvAnalisis.visibility == View.VISIBLE) {
                        tvAnalisis.visibility = View.GONE
                        btnVer.text = "🤖 Ver análisis IA"
                    } else {
                        tvAnalisis.text       = a.analisisIA
                        tvAnalisis.visibility = View.VISIBLE
                        btnVer.text           = "Ocultar"
                    }
                }
            }

            // Recompensa
            val layoutRecompensa  = itemView.findViewById<LinearLayout>(R.id.layoutRecompensa)
            val tvMonedas         = itemView.findViewById<TextView>(R.id.tvMonedasRecompensa)
            val btnReclamar       = itemView.findViewById<Button>(R.id.btnReclamarRecompensa)
            val tvYaReclamado     = itemView.findViewById<TextView>(R.id.tvYaReclamado)

            when {
                a.recompensaReclamada -> {
                    layoutRecompensa.visibility = View.GONE
                    tvYaReclamado.visibility    = View.VISIBLE
                }
                a.monedasRecompensa > 0 -> {
                    layoutRecompensa.visibility = View.VISIBLE
                    tvYaReclamado.visibility    = View.GONE
                    tvMonedas.text = "🪙 +${a.monedasRecompensa} monedas disponibles"
                    btnReclamar.setOnClickListener { onReclamar(a) }
                }
                else -> {
                    // Sin análisis de ayer todavía — no mostrar nada
                    layoutRecompensa.visibility = View.GONE
                    tvYaReclamado.visibility    = View.GONE
                }
            }
        }
    }
}