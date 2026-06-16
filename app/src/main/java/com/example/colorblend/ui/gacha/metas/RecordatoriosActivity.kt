package com.example.colorblend.ui.gacha.metas

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.Meta
import com.example.colorblend.ui.gacha.metas.viewmodels.MetaViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

class RecordatoriosActivity : AppCompatActivity() {

    private val metaViewModel: MetaViewModel by viewModels()
    private lateinit var adapter: RecordatoriosAdapter
    private lateinit var tvSinRecordatorios: TextView
    private lateinit var tvResumen: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recordatorios)

        tvSinRecordatorios = findViewById(R.id.tvSinRecordatorios)
        tvResumen          = findViewById(R.id.tvResumenRecordatorios)
        val recycler       = findViewById<RecyclerView>(R.id.recordatoriosRecycler)

        adapter = RecordatoriosAdapter(
            onCambiarHora = { meta -> mostrarTimePicker(meta) },
            onEliminar    = { meta -> confirmarEliminar(meta) }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        lifecycleScope.launch {
            metaViewModel.metas.collect { lista ->
                val conRecordatorio = lista.filter { !it.horaRecordatorio.isNullOrEmpty() }
                adapter.update(conRecordatorio)

                if (conRecordatorio.isEmpty()) {
                    tvSinRecordatorios.visibility = View.VISIBLE
                    recycler.visibility = View.GONE
                    tvResumen.text = "Sin recordatorios activos"
                } else {
                    tvSinRecordatorios.visibility = View.GONE
                    recycler.visibility = View.VISIBLE
                    tvResumen.text = "${conRecordatorio.size} recordatorio${if (conRecordatorio.size != 1) "s" else ""} activo${if (conRecordatorio.size != 1) "s" else ""}"
                }
            }
        }

        findViewById<Button>(R.id.btnVolverRecordatorios).setOnClickListener { finish() }
    }

    private fun mostrarTimePicker(meta: Meta) {
        val partes = meta.horaRecordatorio?.split(":")?.mapNotNull { it.toIntOrNull() }
        val horaActual   = partes?.getOrNull(0) ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val minutoActual = partes?.getOrNull(1) ?: Calendar.getInstance().get(Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, hora, minuto ->
                val nuevaHora = String.format("%02d:%02d", hora, minuto)
                metaViewModel.actualizarRecordatorio(meta, nuevaHora)
                MetaRecordatorioScheduler.programar(applicationContext, meta.copy(horaRecordatorio = nuevaHora))
                Toast.makeText(this, "⏰ Recordatorio actualizado: $nuevaHora", Toast.LENGTH_SHORT).show()
            },
            horaActual,
            minutoActual,
            true
        ).show()
    }

    private fun confirmarEliminar(meta: Meta) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar recordatorio")
            .setMessage("¿Quitar el recordatorio de \"${meta.titulo}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                metaViewModel.actualizarRecordatorio(meta, null)
                MetaRecordatorioScheduler.cancelar(applicationContext, meta.id)
                Toast.makeText(this, "Recordatorio eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

class RecordatoriosAdapter(
    private val onCambiarHora: (Meta) -> Unit,
    private val onEliminar   : (Meta) -> Unit
) : RecyclerView.Adapter<RecordatoriosAdapter.ViewHolder>() {

    private var lista = listOf<Meta>()

    fun update(nuevaLista: List<Meta>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_recordatorio, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(lista[position])

    override fun getItemCount() = lista.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(meta: Meta) {
            itemView.findViewById<TextView>(R.id.tvRecordatorioTitulo).text = meta.titulo
            itemView.findViewById<TextView>(R.id.tvRecordatorioHora).text  = meta.horaRecordatorio ?: "--:--"
            itemView.findViewById<TextView>(R.id.tvRecordatorioDias).text  = "📅 ${meta.diasSemanaTexto()}"

            itemView.findViewById<Button>(R.id.btnCambiarHora).setOnClickListener {
                onCambiarHora(meta)
            }
            itemView.findViewById<Button>(R.id.btnEliminarRecordatorio).setOnClickListener {
                onEliminar(meta)
            }
        }
    }
}