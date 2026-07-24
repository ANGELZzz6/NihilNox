package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.Habito
import com.example.colorblend.domain.model.Tarea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class LifeStreamActivity : AppCompatActivity() {

    private val tareaViewModel: TareaViewModel by viewModels()
    private lateinit var rvLifeStream: RecyclerView
    private lateinit var adapter: LifeStreamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lifestream)
        FullScreenHelper.enable(this)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        rvLifeStream = findViewById(R.id.rvLifeStream)
        rvLifeStream.layoutManager = LinearLayoutManager(this)
        
        adapter = LifeStreamAdapter()
        rvLifeStream.adapter = adapter

        observeData()
    }

    private fun observeData() {
        lifecycleScope.launch {
            tareaViewModel.todosLosHabitos.collect { habitos ->
                tareaViewModel.todasLasTareas.collect { tareas ->
                    cargarHistoriales(habitos, tareas)
                }
            }
        }
    }

    private fun cargarHistoriales(habitos: List<Habito>, tareas: List<Tarea>) {
        val items = mutableListOf<StreamData>()
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val hoy = cal.timeInMillis
            val desde = hoy - (13 * 86400000L)

            habitos.forEach { h ->
                val registros = db.registroHabitoDao().getRegistrosDesdeFecha(h.id, desde)
                val history = mutableListOf<Boolean>()
                for (i in 0..13) {
                    history.add(registros.contains(desde + (i * 86400000L)))
                }
                items.add(StreamData(h.nombre, history, h.burbujaColor))
            }

            tareas.filter { it.recurrencia != "UNA_VEZ" }.forEach { t ->
                val registros = db.registroTareaDao().getRegistrosDesde(t.id, desde)
                val history = mutableListOf<Boolean>()
                for (i in 0..13) {
                    history.add(registros.contains(desde + (i * 86400000L)))
                }
                items.add(StreamData(t.titulo, history, t.color))
            }

            withContext(Dispatchers.Main) {
                adapter.submitList(items)
            }
        }
    }

    data class StreamData(val name: String, val history: List<Boolean>, val color: String)

    inner class LifeStreamAdapter : RecyclerView.Adapter<LifeStreamAdapter.ViewHolder>() {
        private var list: List<StreamData> = emptyList()

        fun submitList(newList: List<StreamData>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lifestream, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val data = list[position]
            holder.tvName.text = data.name
            holder.waveView.setHistory(data.history, data.color)
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvItemName)
            val waveView: WaveProgressView = view.findViewById(R.id.waveProgressView)
        }
    }
}
