package com.example.colorblend.ui.gacha

import android.graphics.Color
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.databinding.ActivityIdentidadBinding
import kotlinx.coroutines.launch

class IdentidadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIdentidadBinding
    private lateinit var viewModel: HabitosViewModel
    private lateinit var adapter: IdentidadAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FullScreenHelper.enable(this)
        binding = ActivityIdentidadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)
        val repository = HabitosRepository(db.habitoDao(), db.registroHabitoDao(), db.identidadDao())
        viewModel = ViewModelProvider(this, HabitosViewModelFactory(application, repository))
            .get(HabitosViewModel::class.java)

        adapter = IdentidadAdapter(
            onEliminar = { viewModel.eliminarIdentidad(it) }
        )

        binding.recyclerIdentidades.layoutManager = LinearLayoutManager(this)
        binding.recyclerIdentidades.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.identidades.collect { adapter.submitList(it) }
            }
        }

        binding.fabAgregarIdentidad.setOnClickListener { mostrarDialogoAgregar() }

        iniciarAnimacionesEntrada()
    }

    private fun iniciarAnimacionesEntrada() {
        val elementos = listOf(
            binding.tvTituloIdentidad,
            binding.tvIdentidadDesc,
            binding.recyclerIdentidades
        )
        elementos.forEachIndexed { i, vista ->
            vista.alpha = 0f
            vista.translationY = 40f
            vista.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setStartDelay(150L + i * 70L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun mostrarDialogoAgregar() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
        }
        val input = EditText(this).apply {
            hint = "Ej: alguien que se mueve cada día"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            typeface = ResourcesCompat.getFont(context, R.font.opensauce)
        }
        container.addView(input)

        AlertDialog.Builder(this, R.style.DialogoOscuro)
            .setTitle("Nueva Identidad")
            .setMessage("Define quién quieres ser. Cada hábito completado será un voto por esta identidad.")
            .setView(container)
            .setPositiveButton("Agregar") { _, _ ->
                viewModel.agregarIdentidad(input.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
