package com.example.colorblend.ui.gacha

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.PersonajeChat
import com.example.colorblend.domain.model.PersonajeObtenido
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MisPersonajesChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_personajes_chat)

        val recycler = findViewById<RecyclerView>(R.id.misPersonajesChatRecycler)
        val btnAgregar = findViewById<TextView>(R.id.btnAgregarPersonaje)
        val tvVacia = findViewById<TextView>(R.id.tvListaVacia)

        val db = AppDatabase.getDatabase(applicationContext)
        val personajeChatDao = db.personajeChatDao()
        val personajeDao = db.personajeDao()

        val adapter = MisPersonajesChatAdapter(
            onChatClick = { personaje ->
                val intent = Intent(this, ChatPersonajeActivity::class.java)
                intent.putExtra("personajeId", personaje.id)
                intent.putExtra("personajeNombre", personaje.nombre)
                intent.putExtra("personajeImagen", personaje.imagenUrl)
                startActivity(intent)
            },
            onEliminarClick = { personaje ->
                lifecycleScope.launch {
                    personajeChatDao.delete(PersonajeChat(personaje.id))
                }
            }
        )

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            combine(
                personajeChatDao.getPersonajesChat(),
                personajeDao.getPersonajes()
            ) { chats, personajes ->
                val idsEnChat = chats.map { it.personajeId }.toSet()
                personajes.filter { it.id in idsEnChat }
                    .sortedByDescending {
                        chats.find { c -> c.personajeId == it.id }?.fechaAgregado
                    }
            }.collect { lista ->
                adapter.update(lista)
                tvVacia.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                recycler.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        btnAgregar.setOnClickListener {
            startActivity(Intent(this, SeleccionPersonajeActivity::class.java))
        }
    }
}

class MisPersonajesChatAdapter(
    private var personajes: List<PersonajeObtenido> = emptyList(),
    private val onChatClick: (PersonajeObtenido) -> Unit,
    private val onEliminarClick: (PersonajeObtenido) -> Unit
) : RecyclerView.Adapter<MisPersonajesChatAdapter.ViewHolder>() {

    fun update(nuevos: List<PersonajeObtenido>) {
        personajes = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_personaje_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(personajes[position])
    }

    override fun getItemCount() = personajes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(personaje: PersonajeObtenido) {
            itemView.findViewById<TextView>(R.id.chatPersonajeNombreItem).text = personaje.nombre
            itemView.findViewById<TextView>(R.id.chatPersonajeOrigen).text = personaje.animeTitulo
            Glide.with(itemView.context)
                .load(personaje.imagenUrl)
                .into(itemView.findViewById<ImageView>(R.id.chatPersonajeImg))

            itemView.setOnClickListener { onChatClick(personaje) }

            itemView.findViewById<TextView>(R.id.btnEliminarDeChat).setOnClickListener {
                onEliminarClick(personaje)
            }
        }
    }
}