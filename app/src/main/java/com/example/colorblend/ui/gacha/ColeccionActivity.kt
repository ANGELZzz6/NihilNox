package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import kotlinx.coroutines.launch

class ColeccionActivity : AppCompatActivity() {

    private val viewModel: ColeccionViewModel by viewModels()
    private lateinit var adapter: ColeccionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coleccion)

        val recycler = findViewById<RecyclerView>(R.id.coleccionRecycler)
        val searchAnime = findViewById<EditText>(R.id.searchAnime)
        val tvToggle = findViewById<TextView>(R.id.tvToggleMasc)
        val tvContador = findViewById<TextView>(R.id.tvContadorAnimes)

        adapter = ColeccionAdapter()
        recycler.adapter = adapter
        // ✅ Grid de 2 columnas
        recycler.layoutManager = GridLayoutManager(this, 2)

        searchAnime.addTextChangedListener { texto ->
            viewModel.buscar(texto.toString())
        }

        lifecycleScope.launch {
            viewModel.animes.collect { lista ->
                adapter.update(lista)
                tvContador.text = "${lista.size} ${if (lista.size == 1) "anime desbloqueado" else "animes desbloqueados"}"
            }
        }

        lifecycleScope.launch {
            viewModel.mostrarMasculinos.collect { mostrar ->
                tvToggle.text = if (mostrar) "♂ ON" else "♂ OFF"
                tvToggle.setTextColor(
                    if (mostrar)
                        android.graphics.Color.parseColor("#4FC3F7")
                    else
                        android.graphics.Color.parseColor("#556677")
                )
            }
        }

        tvToggle.setOnClickListener {
            SonidoHelper.reproducir(this)
            viewModel.toggleMasculinos()
        }
    }
}