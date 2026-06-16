package com.example.colorblend.ui.gacha.metas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.ui.gacha.SonidoHelper
import com.example.colorblend.ui.gacha.metas.viewmodels.MetaViewModel
import kotlinx.coroutines.launch

class MetasActivity : AppCompatActivity() {

    private val metaViewModel: MetaViewModel by viewModels()
    private lateinit var adapter: MetaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metas)

        val recycler            = findViewById<RecyclerView>(R.id.metasRecycler)
        val btnNuevaMeta        = findViewById<Button>(R.id.btnNuevaMeta)
        val btnVerRecordatorios = findViewById<Button>(R.id.btnVerRecordatorios)
        val tvResumen           = findViewById<TextView>(R.id.tvResumenMetas)

        adapter = MetaAdapter(
            onCumplirClick = { meta ->
                SonidoHelper.reproducir(this)
                metaViewModel.cumplirMeta(meta)
            },
            onSumarClick = { meta ->
                SonidoHelper.reproducir(this)
                metaViewModel.sumarProgreso(meta, 5)
            },
            metaViewModel = metaViewModel
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        lifecycleScope.launch {
            metaViewModel.metas.collect { lista ->
                adapter.update(lista)
                val activas       = lista.count { !it.finalizada }
                val completadas   = lista.count { it.finalizada }
                val mejorRacha    = lista.maxOfOrNull { it.mejorRacha } ?: 0
                val recordatorios = lista.count { !it.horaRecordatorio.isNullOrEmpty() }
                tvResumen.text = "✅ $activas activas · 🏆 $completadas completadas · ⏰ $recordatorios recordatorios"
            }
        }

        btnNuevaMeta.setOnClickListener {
            SonidoHelper.reproducir(this)
            startActivity(Intent(this, CrearMetaActivity::class.java))
        }

        btnVerRecordatorios.setOnClickListener {
            SonidoHelper.reproducir(this)
            startActivity(Intent(this, RecordatoriosActivity::class.java))
        }
    }
}