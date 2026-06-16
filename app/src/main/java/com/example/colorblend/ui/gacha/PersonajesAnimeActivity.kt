package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import kotlinx.coroutines.launch

class PersonajesAnimeActivity : AppCompatActivity() {

    private val viewModel: PersonajesAnimeViewModel by viewModels()
    private lateinit var adapter: ColeccionPersonajesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personajes_anime)

        val animeId = intent.getIntExtra("animeId", 0)
        val animeTitulo = intent.getStringExtra("animeTitulo") ?: "Personajes"

        findViewById<TextView>(R.id.tvAnimeTitulo).text = animeTitulo

        val recycler = findViewById<RecyclerView>(R.id.personajesRecycler)

        adapter = ColeccionPersonajesAdapter(
            lifecycleOwner = this,
            activity = this
        )
        recycler.adapter = adapter
        recycler.layoutManager = GridLayoutManager(this, 3)

        // ✅ Si animeId = 0 es superhéroe o videojuego → buscar por título
        if (animeId == 0) {
            viewModel.cargarPersonajesPorTitulo(animeTitulo)
        } else {
            viewModel.cargarPersonajes(animeId)
        }

        lifecycleScope.launch {
            viewModel.personajes.collect { lista ->
                adapter.update(lista)
            }
        }
    }
}