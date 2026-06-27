package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.databinding.ActivityEstadisticasBinding
import kotlinx.coroutines.launch

class EstadisticasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEstadisticasBinding
    private lateinit var viewModel: HabitosViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FullScreenHelper.enable(this)
        binding = ActivityEstadisticasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)
        val repository = HabitosRepository(db.habitoDao(), db.registroHabitoDao(), db.identidadDao())
        viewModel = ViewModelProvider(this, HabitosViewModelFactory(application, repository))
            .get(HabitosViewModel::class.java)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // stats globales
                launch {
                    viewModel.habitos.collect { lista ->
                        binding.tvTotalHabitos.text = lista.size.toString()
                        binding.tvMejorRacha.text = (lista.maxOfOrNull { it.habito.rachaMaxima } ?: 0).toString()
                    }
                }

                // calcular porcentajes por día para el gráfico global
                launch {
                    viewModel.consistenciaSemanal.collect { porcentajes ->
                        binding.graficoSemanal.setDatos(porcentajes)
                    }
                }
            }
        }

        iniciarAnimacionesEntrada()
    }

    private fun iniciarAnimacionesEntrada() {
        listOf(binding.tvTituloStats, binding.layoutResumen,
               binding.graficoSemanal, binding.recyclerStatsHabitos)
            .forEachIndexed { i, v ->
                v.alpha = 0f
                v.translationY = 40f
                v.animate().alpha(1f).translationY(0f)
                    .setDuration(350).setStartDelay(150L + i * 70L)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
