package com.example.colorblend.ui.gacha

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.RegistroAlimento
import kotlinx.coroutines.launch

class NutricionActivity : AppCompatActivity() {

    private val viewModel: NutricionViewModel by viewModels()

    private val colorProteina = Color.parseColor("#4FC3F7")
    private val colorCarbos   = Color.parseColor("#FFD700")
    private val colorGrasas   = Color.parseColor("#FF69B4")
    private val colorFibra    = Color.parseColor("#69FF87")
    private val colorCalorias = Color.parseColor("#FFD700")
    private val colorAzucares = Color.parseColor("#FF8C00")
    private val colorRojo     = Color.parseColor("#FF3333")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nutricion)

        val tvCalorias         = findViewById<TextView>(R.id.tvCaloriasHoy)
        val tvCaloriasMeta     = findViewById<TextView>(R.id.tvCaloriasMeta)
        val progressCalorias   = findViewById<ProgressBar>(R.id.progressCalorias)
        val tvProteina         = findViewById<TextView>(R.id.tvProteina)
        val progressProteina   = findViewById<ProgressBar>(R.id.progressProteina)
        val tvCarbos           = findViewById<TextView>(R.id.tvCarbos)
        val progressCarbos     = findViewById<ProgressBar>(R.id.progressCarbos)
        val tvGrasas           = findViewById<TextView>(R.id.tvGrasas)
        val progressGrasas     = findViewById<ProgressBar>(R.id.progressGrasas)
        val tvFibra            = findViewById<TextView>(R.id.tvFibra)
        val progressFibra      = findViewById<ProgressBar>(R.id.progressFibra)
        val tvAzucares         = findViewById<TextView>(R.id.tvAzucares)
        val progressAzucares   = findViewById<ProgressBar>(R.id.progressAzucares)
        val recyclerDesayuno   = findViewById<RecyclerView>(R.id.recyclerDesayuno)
        val recyclerAlmuerzo   = findViewById<RecyclerView>(R.id.recyclerAlmuerzo)
        val recyclerCena       = findViewById<RecyclerView>(R.id.recyclerCena)
        val recyclerSnack      = findViewById<RecyclerView>(R.id.recyclerSnack)
        val btnAgregarDesayuno = findViewById<Button>(R.id.btnAgregarDesayuno)
        val btnAgregarAlmuerzo = findViewById<Button>(R.id.btnAgregarAlmuerzo)
        val btnAgregarCena     = findViewById<Button>(R.id.btnAgregarCena)
        val btnAgregarSnack    = findViewById<Button>(R.id.btnAgregarSnack)
        val btnVolver          = findViewById<Button>(R.id.btnVolverNutricion)
        val btnEditarPerfil    = findViewById<Button>(R.id.btnEditarPerfilNutricion)
        val btnHistorial       = findViewById<Button>(R.id.btnHistorialNutricion)
        val btnAnalizarDia     = findViewById<Button>(R.id.btnAnalizarDia)
        val tvAnalisisResult   = findViewById<TextView>(R.id.tvAnalisisResultado)

        val adapterDesayuno = AlimentosDiaAdapter { eliminarAlimento(it) }
        val adapterAlmuerzo = AlimentosDiaAdapter { eliminarAlimento(it) }
        val adapterCena     = AlimentosDiaAdapter { eliminarAlimento(it) }
        val adapterSnack    = AlimentosDiaAdapter { eliminarAlimento(it) }

        recyclerDesayuno.adapter = adapterDesayuno
        recyclerAlmuerzo.adapter = adapterAlmuerzo
        recyclerCena.adapter     = adapterCena
        recyclerSnack.adapter    = adapterSnack

        listOf(recyclerDesayuno, recyclerAlmuerzo, recyclerCena, recyclerSnack).forEach {
            it.layoutManager = LinearLayoutManager(this)
            it.isNestedScrollingEnabled = false
        }

        btnAgregarDesayuno.setOnClickListener { abrirAgregarAlimento("Desayuno") }
        btnAgregarAlmuerzo.setOnClickListener { abrirAgregarAlimento("Almuerzo") }
        btnAgregarCena.setOnClickListener     { abrirAgregarAlimento("Cena") }
        btnAgregarSnack.setOnClickListener    { abrirAgregarAlimento("Snack") }
        btnVolver.setOnClickListener          { finish() }
        btnEditarPerfil.setOnClickListener {
            startActivity(Intent(this, NutricionOnboardingActivity::class.java))
        }
        btnHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialNutricionActivity::class.java))
        }
        btnAnalizarDia.setOnClickListener {
            viewModel.analizarNutricion(this)
        }

        lifecycleScope.launch {
            launch {
                viewModel.perfil.collect { perfil ->
                    perfil ?: return@collect
                    tvCaloriasMeta.text  = "/ ${perfil.metaCalorias} kcal"
                    progressCalorias.max = perfil.metaCalorias
                    progressProteina.max = perfil.metaProteina
                    progressCarbos.max   = perfil.metaCarbos
                    progressGrasas.max   = perfil.metaGrasas
                    progressFibra.max    = perfil.metaFibra
                    tvProteina.text = "Proteína  0 / ${perfil.metaProteina}g"
                    tvCarbos.text   = "Carbos    0 / ${perfil.metaCarbos}g"
                    tvGrasas.text   = "Grasas    0 / ${perfil.metaGrasas}g"
                    tvFibra.text    = "Fibra     0 / ${perfil.metaFibra}g"
                }
            }
            launch {
                viewModel.resumenHoy.collect { resumen ->
                    val perfil = viewModel.perfil.value
                    tvCalorias.text           = "${resumen.calorias}"
                    progressCalorias.progress = resumen.calorias
                    progressProteina.progress = resumen.proteina.toInt()
                    progressCarbos.progress   = resumen.carbos.toInt()
                    progressGrasas.progress   = resumen.grasas.toInt()
                    progressFibra.progress    = resumen.fibra.toInt()
                    progressAzucares.progress = resumen.azucares.toInt()
                    perfil?.let { p ->
                        tvProteina.text = "Proteína  ${resumen.proteina.toInt()} / ${p.metaProteina}g"
                        tvCarbos.text   = "Carbos    ${resumen.carbos.toInt()} / ${p.metaCarbos}g"
                        tvGrasas.text   = "Grasas    ${resumen.grasas.toInt()} / ${p.metaGrasas}g"
                        tvFibra.text    = "Fibra     ${resumen.fibra.toInt()} / ${p.metaFibra}g"
                        tvAzucares.text = "Azúcares  ${resumen.azucares.toInt()} / 50g"
                        actualizarColorBarra(progressCalorias, tvCalorias,
                            resumen.calorias, p.metaCalorias, colorCalorias, esCalorias = true)
                        actualizarColorBarra(progressProteina, tvProteina,
                            resumen.proteina.toInt(), p.metaProteina, colorProteina)
                        actualizarColorBarra(progressCarbos, tvCarbos,
                            resumen.carbos.toInt(), p.metaCarbos, colorCarbos)
                        actualizarColorBarra(progressGrasas, tvGrasas,
                            resumen.grasas.toInt(), p.metaGrasas, colorGrasas)
                        actualizarColorBarra(progressFibra, tvFibra,
                            resumen.fibra.toInt(), p.metaFibra, colorFibra)
                        actualizarColorBarra(progressAzucares, tvAzucares,
                            resumen.azucares.toInt(), 50, colorAzucares)
                    }
                }
            }
            launch {
                viewModel.alimentosDia.collect { lista ->
                    adapterDesayuno.update(lista.filter { it.categoria == "Desayuno" })
                    adapterAlmuerzo.update(lista.filter { it.categoria == "Almuerzo" })
                    adapterCena.update(lista.filter    { it.categoria == "Cena" })
                    adapterSnack.update(lista.filter   { it.categoria == "Snack" })
                }
            }
            launch {
                viewModel.cargandoAnalisis.collect { cargando ->
                    btnAnalizarDia.isEnabled = !cargando
                    btnAnalizarDia.text = if (cargando) "⏳ Analizando..." else "🥗 Analizar nutrición"
                }
            }
            launch {
                viewModel.analisisHoy.collect { analisis ->
                    if (analisis != null && analisis.analisisIA.isNotBlank()) {
                        tvAnalisisResult.text       = analisis.analisisIA
                        tvAnalisisResult.visibility = View.VISIBLE
                    }
                }
            }
            launch {
                viewModel.errorAnalisis.collect { error ->
                    if (!error.isNullOrBlank()) {
                        AlertDialog.Builder(this@NutricionActivity)
                            .setTitle("⚠️ Error")
                            .setMessage(error)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
            launch {
                // ── Collector del evento de recompensa (monedas + mensaje juntos)
                viewModel.recompensaEvento.collect { evento ->
                    Toast.makeText(
                        this@NutricionActivity,
                        "🪙 +${evento.monedas} monedas por ayer — ${evento.mensaje}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun actualizarColorBarra(
        progressBar: ProgressBar,
        textView: TextView,
        valor: Int,
        meta: Int,
        colorNormal: Int,
        esCalorias: Boolean = false
    ) {
        val superado = valor > meta
        val color = if (superado) colorRojo else colorNormal
        progressBar.progressTintList = ColorStateList.valueOf(color)
        textView.setTextColor(color)
        if (esCalorias && superado) {
            findViewById<TextView>(R.id.tvCaloriasHoy).setTextColor(colorRojo)
        } else if (esCalorias) {
            findViewById<TextView>(R.id.tvCaloriasHoy).setTextColor(colorCalorias)
        }
    }

    private fun abrirAgregarAlimento(categoria: String) {
        startActivity(
            Intent(this, AgregarAlimentoActivity::class.java)
                .putExtra("categoria", categoria)
        )
    }

    private fun eliminarAlimento(alimento: RegistroAlimento) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar alimento")
            .setMessage("¿Eliminar \"${alimento.nombre}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarAlimento(alimento)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}