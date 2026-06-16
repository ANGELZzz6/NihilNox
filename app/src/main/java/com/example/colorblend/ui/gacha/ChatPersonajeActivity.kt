package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.colorblend.R
import com.example.colorblend.domain.model.MensajeChat
import kotlinx.coroutines.launch

class ChatPersonajeActivity : AppCompatActivity() {

    private val viewModel: ChatPersonajeViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private var personajeId = 0
    private var personajeNombre = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_personaje)

        personajeId = intent.getIntExtra("personajeId", 0)
        personajeNombre = intent.getStringExtra("personajeNombre") ?: ""
        val personajeImagen = intent.getStringExtra("personajeImagen") ?: ""

        val tvNombre = findViewById<TextView>(R.id.chatPersonajeNombre)
        val imgPersonaje = findViewById<ImageView>(R.id.chatPersonajeImagen)
        val recycler = findViewById<RecyclerView>(R.id.chatRecycler)
        val inputMensaje = findViewById<EditText>(R.id.inputMensaje)
        val btnEnviar = findViewById<Button>(R.id.btnEnviar)
        val btnBorrar = findViewById<TextView>(R.id.btnBorrarChat)

        tvNombre.text = personajeNombre
        Glide.with(this).load(personajeImagen).into(imgPersonaje)

        chatAdapter = ChatAdapter()
        recycler.adapter = chatAdapter
        recycler.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        viewModel.cargarMensajes(personajeId)

        lifecycleScope.launch {
            viewModel.mensajes.collect { lista ->
                chatAdapter.update(lista)
                if (lista.isNotEmpty()) {
                    recycler.scrollToPosition(lista.size - 1)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.estado.collect { estado ->
                btnEnviar.isEnabled = estado == ChatEstado.Idle
                btnEnviar.text = if (estado == ChatEstado.Cargando) "..." else "➤"
            }
        }

        btnEnviar.setOnClickListener {
            val texto = inputMensaje.text.toString().trim()
            if (texto.isNotEmpty()) {
                viewModel.enviarMensaje(personajeId, personajeNombre, texto)
                inputMensaje.text.clear()
            }
        }

        btnBorrar.setOnClickListener {
            viewModel.borrarChat(personajeId)
        }
    }
}