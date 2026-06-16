package com.example.colorblend.ui.gacha

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.PersonajeChat
import com.example.colorblend.domain.model.PersonajeObtenido
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SeleccionPersonajeActivity : AppCompatActivity() {

    private val prefs by lazy {
        getSharedPreferences("coleccion_prefs", Context.MODE_PRIVATE)
    }

    private var todosLosPersonajes: List<PersonajeObtenido> = emptyList()
    private lateinit var adapter: SeleccionAdapter
    private var mostrarMasculinos = true
    private var busqueda = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccion_personaje)

        val recycler = findViewById<RecyclerView>(R.id.seleccionRecycler)
        val tvToggle = findViewById<TextView>(R.id.tvToggleMascSeleccion)
        val searchPersonaje = findViewById<EditText>(R.id.searchPersonaje)

        mostrarMasculinos = prefs.getBoolean("mostrar_masculinos", true)
        actualizarToggleUI(tvToggle)

        val db = AppDatabase.getDatabase(applicationContext)

        adapter = SeleccionAdapter(emptyList()) { personaje ->
            // ✅ Agregar a lista de chat en lugar de abrir chat directo
            lifecycleScope.launch {
                val yaEsta = db.personajeChatDao().estaEnChat(personaje.id)
                if (yaEsta) {
                    Toast.makeText(
                        this@SeleccionPersonajeActivity,
                        "${personaje.nombre} ya está en tu lista",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    db.personajeChatDao().insert(PersonajeChat(personaje.id))
                    Toast.makeText(
                        this@SeleccionPersonajeActivity,
                        "✅ ${personaje.nombre} agregado al chat",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        recycler.adapter = adapter
        recycler.layoutManager = GridLayoutManager(this, 3)

        lifecycleScope.launch {
            todosLosPersonajes = db.personajeDao()
                .getPersonajes()
                .first()
            aplicarFiltros()
        }

        searchPersonaje.addTextChangedListener { texto ->
            busqueda = texto.toString()
            aplicarFiltros()
        }

        tvToggle.setOnClickListener {
            mostrarMasculinos = !mostrarMasculinos
            prefs.edit().putBoolean("mostrar_masculinos", mostrarMasculinos).apply()
            actualizarToggleUI(tvToggle)
            aplicarFiltros()
        }
    }

    private fun aplicarFiltros() {
        var filtrados = todosLosPersonajes
        if (!mostrarMasculinos) {
            filtrados = filtrados.filter { it.genero.lowercase() != "male" }
        }
        if (busqueda.isNotBlank()) {
            filtrados = filtrados.filter {
                it.nombre.contains(busqueda, ignoreCase = true)
            }
        }
        adapter.update(filtrados)
    }

    private fun actualizarToggleUI(tvToggle: TextView) {
        tvToggle.text = if (mostrarMasculinos) "♂ ON" else "♂ OFF"
        tvToggle.setTextColor(
            if (mostrarMasculinos)
                android.graphics.Color.parseColor("#4FC3F7")
            else
                android.graphics.Color.parseColor("#666666")
        )
    }
}

class SeleccionAdapter(
    private var personajes: List<PersonajeObtenido>,
    private val onClick: (PersonajeObtenido) -> Unit
) : RecyclerView.Adapter<SeleccionAdapter.ViewHolder>() {

    fun update(nuevos: List<PersonajeObtenido>) {
        personajes = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_character, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(personajes[position])
    }

    override fun getItemCount() = personajes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(personaje: PersonajeObtenido) {
            itemView.findViewById<TextView>(R.id.charName).text = personaje.nombre
            itemView.findViewById<TextView>(R.id.charScore).text = "⭐ ${personaje.favoritos}"
            itemView.findViewById<TextView>(R.id.charRareza)?.text = ""
            itemView.findViewById<TextView>(R.id.charOrigen)?.text = personaje.animeTitulo
            Glide.with(itemView.context)
                .load(personaje.imagenUrl)
                .into(itemView.findViewById(R.id.charImage))
            itemView.setOnClickListener { onClick(personaje) }
        }
    }
}