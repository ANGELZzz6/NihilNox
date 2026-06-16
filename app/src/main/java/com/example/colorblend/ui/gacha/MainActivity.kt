package com.example.colorblend.ui.gacha

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.adapters.CharacterAdapter
import com.example.colorblend.data.local.ApiKeysManager
import com.example.colorblend.ui.gacha.metas.MetasActivity
import com.example.colorblend.ui.gacha.metas.viewmodels.AnimeViewModel
import com.example.colorblend.ui.gacha.metas.viewmodels.GachaEstado
import com.example.colorblend.ui.gacha.metas.viewmodels.TipoGacha
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val animeViewModel: AnimeViewModel by viewModels()
    private val userStatsViewModel: UserStatsViewModel by viewModels()
    private lateinit var adapter: CharacterAdapter

    private val launcherApiKeys = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (ApiKeysManager.estanConfiguradas(this)) {
            recreate()
        } else {
            verificarApiKeys()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiKeysManager.init(applicationContext)
        setContentView(R.layout.activity_main)
        if (!verificarApiKeys()) return
        inicializarUI()
    }

    override fun onResume() {
        super.onResume()
        if (!ApiKeysManager.estanConfiguradas(this)) {
            verificarApiKeys()
        }
    }

    private fun verificarApiKeys(): Boolean {
        if (ApiKeysManager.estanConfiguradas(this)) return true
        val faltantes = ApiKeysManager.keysFaltantes(this)
        val mensaje = "Configura las API Keys para usar la app:\n• ${faltantes.joinToString("\n• ")}"
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        launcherApiKeys.launch(Intent(this, ApiKeysActivity::class.java))
        return false
    }

    private fun inicializarUI() {
        val tvMonedas         = findViewById<TextView>(R.id.tvMonedas)
        val recycler          = findViewById<RecyclerView>(R.id.charactersRecycler)
        val oneRollButton     = findViewById<Button>(R.id.oneRollButton)
        val tenRollButton     = findViewById<Button>(R.id.tenRollButton)
        val btnIrAMetas       = findViewById<Button>(R.id.btnIrAMetas)
        val btnColeccion      = findViewById<Button>(R.id.btnColeccion)
        val oneRollFemButton  = findViewById<Button>(R.id.oneRollFemButton)
        val tenRollFemButton  = findViewById<Button>(R.id.tenRollFemButton)
        val oneRollMascButton = findViewById<Button>(R.id.oneRollMascButton)
        val tenRollMascButton = findViewById<Button>(R.id.tenRollMascButton)
        val btnPerfil         = findViewById<Button>(R.id.btnPerfil)
        val overlayLoading    = findViewById<View>(R.id.overlayLoading)
        val tvLoadingMensaje  = findViewById<TextView>(R.id.tvLoadingMensaje)

        // ✅ Definido aquí para que el observe lo pueda usar
        val todosLosBotones = listOf(
            oneRollButton, tenRollButton,
            oneRollFemButton, tenRollFemButton,
            oneRollMascButton, tenRollMascButton
        )

        adapter = CharacterAdapter(emptyList())
        recycler.adapter = adapter
        recycler.layoutManager = GridLayoutManager(this, 3)

        lifecycleScope.launch {
            userStatsViewModel.stats.collect { stats ->
                tvMonedas.text = "🪙 ${stats?.monedas ?: 0} monedas"
            }
        }

        val mensajesCarga = listOf(
            "Invocando personajes...",
            "Buscando héroes...",
            "Explorando el multiverso...",
            "Despertando poderes...",
            "Eligiendo tu destino..."
        )
        var mensajeIndex = 0
        val handlerMensajes = Handler(Looper.getMainLooper())
        val runnableMensajes = object : Runnable {
            override fun run() {
                tvLoadingMensaje.text = mensajesCarga[mensajeIndex % mensajesCarga.size]
                mensajeIndex++
                handlerMensajes.postDelayed(this, 1200)
            }
        }

        animeViewModel.gachaEstado.observe(this) { estado ->
            when (estado) {
                is GachaEstado.Cargando -> {
                    todosLosBotones.forEach { btn -> btn.isEnabled = false }
                    overlayLoading.visibility = View.VISIBLE
                    overlayLoading.alpha = 0f
                    overlayLoading.animate().alpha(1f).setDuration(300).start()
                    mensajeIndex = 0
                    handlerMensajes.post(runnableMensajes)
                }
                is GachaEstado.Exito -> {
                    todosLosBotones.forEach { btn -> btn.isEnabled = true }
                    handlerMensajes.removeCallbacks(runnableMensajes)
                    overlayLoading.animate().alpha(0f).setDuration(200)
                        .withEndAction { overlayLoading.visibility = View.GONE }.start()
                    adapter.update(estado.personajes)
                    val serializables = ArrayList(estado.personajes.map { it.toSerializable() })
                    startActivity(
                        Intent(this, GachaResultadoActivity::class.java)
                            .putExtra("personajes", serializables)
                    )
                }
                is GachaEstado.SinMonedas -> {
                    todosLosBotones.forEach { btn -> btn.isEnabled = true }
                    handlerMensajes.removeCallbacks(runnableMensajes)
                    overlayLoading.visibility = View.GONE
                    Toast.makeText(this, "🪙 No tienes monedas suficientes. ¡Cumple tus metas!", Toast.LENGTH_LONG).show()
                }
                is GachaEstado.Error -> {
                    todosLosBotones.forEach { btn -> btn.isEnabled = true }
                    handlerMensajes.removeCallbacks(runnableMensajes)
                    overlayLoading.visibility = View.GONE
                    Toast.makeText(this, "Error: ${estado.mensaje}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }

        oneRollButton.setOnClickListener     { animarBoton(it) { SonidoHelper.reproducir(this); animeViewModel.tirarGacha(1) } }
        tenRollButton.setOnClickListener     { animarBoton(it) { SonidoHelper.reproducir(this); animeViewModel.tirarGacha(10) } }
        oneRollFemButton.setOnClickListener  { animarBoton(it) { SonidoHelper.reproducir(this); animeViewModel.tirarGacha(1, TipoGacha.FEMENINO) } }
        tenRollFemButton.setOnClickListener  { animarBoton(it) { SonidoHelper.reproducir(this); animeViewModel.tirarGacha(10, TipoGacha.FEMENINO) } }
        oneRollMascButton.setOnClickListener { animarBoton(it) { SonidoHelper.reproducir(this); animeViewModel.tirarGacha(1, TipoGacha.MASCULINO) } }
        tenRollMascButton.setOnClickListener { animarBoton(it) { SonidoHelper.reproducir(this); animeViewModel.tirarGacha(10, TipoGacha.MASCULINO) } }
        btnIrAMetas.setOnClickListener       { animarBoton(it) { SonidoHelper.reproducir(this); startActivity(Intent(this, MetasActivity::class.java)) } }
        btnColeccion.setOnClickListener      { animarBoton(it) { SonidoHelper.reproducir(this); startActivity(Intent(this, ColeccionActivity::class.java)) } }
        btnPerfil.setOnClickListener         { animarBoton(it) { SonidoHelper.reproducir(this); startActivity(Intent(this, PerfilActivity::class.java)) } }
    }

    private fun animarBoton(btn: View, accion: () -> Unit) {
        btn.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80)
            .withEndAction {
                btn.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                accion()
            }.start()
    }
}