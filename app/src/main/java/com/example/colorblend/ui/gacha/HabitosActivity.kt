package com.example.colorblend.ui.gacha

import android.graphics.Color
import android.os.Bundle
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
import com.example.colorblend.databinding.ActivityHabitosBinding
import com.example.colorblend.databinding.DialogNuevoHabitoBinding
import kotlinx.coroutines.launch

class HabitosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHabitosBinding
    private lateinit var viewModel: HabitosViewModel
    private lateinit var adapter: HabitosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FullScreenHelper.enable(this)
        binding = ActivityHabitosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)
        val repository = HabitosRepository(db.habitoDao(), db.registroHabitoDao(), db.identidadDao())
        viewModel = ViewModelProvider(this, HabitosViewModelFactory(application, repository))
            .get(HabitosViewModel::class.java)

        adapter = HabitosAdapter(
            onCompletar = { viewModel.marcarCompletado(it) },
            onEliminar  = { viewModel.eliminar(it) },
            onConfigurarNotif = { mostrarTimePicker(it) },
            onDesactivarNotif = { viewModel.desactivarNotificacion(it, this) }
        )

        binding.recyclerHabitos.layoutManager = LinearLayoutManager(this)
        binding.recyclerHabitos.adapter = adapter

        lifecycleScope.launch {
            viewModel.resetearSiCambioElDia()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.habitos.collect { adapter.submitList(it) }
                }
                launch {
                    viewModel.progresoDiario.collect { (completados, total) ->
                        val progreso = if (total > 0) (completados * 100) / total else 0
                        binding.progressDiario.progress = progreso
                        binding.tvProgresoTexto.text = "$completados/$total"
                    }
                }
                launch {
                    viewModel.mensajeRefuerzo.collect { mensaje ->
                        android.widget.Toast.makeText(
                            this@HabitosActivity, mensaje, android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        binding.fabAgregarHabito.setOnClickListener { mostrarDialogoAgregar() }
        
        binding.btnEstadisticas.setOnClickListener {
            startActivity(android.content.Intent(this, EstadisticasActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.btnIdentidad.setOnClickListener {
            startActivity(android.content.Intent(this, IdentidadActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        iniciarAnimacionesEntrada()
    }

    private fun iniciarAnimacionesEntrada() {
        val elementos = listOf(
            binding.tvTituloHabitos,
            binding.progressDiario,
            binding.tvProgresoTexto,
            binding.recyclerHabitos
        )
        elementos.forEachIndexed { i, vista ->
            vista.alpha = 0f
            vista.translationY = 40f
            vista.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setStartDelay(150L + i * 70L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun mostrarDialogoAgregar() {
        lifecycleScope.launch {
            val identidades = viewModel.getIdentidadesUnaVez()
            val opcionesIdentidad = listOf("Sin identidad") + identidades.map { "Soy alguien que ${it.declaracion}" }
            
            val dialogBinding = DialogNuevoHabitoBinding.inflate(layoutInflater)
            
            val adapterIdentidad = android.widget.ArrayAdapter(
                this@HabitosActivity,
                android.R.layout.simple_spinner_dropdown_item,
                opcionesIdentidad
            )
            dialogBinding.spinnerIdentidad.adapter = adapterIdentidad

            val dialog = AlertDialog.Builder(this@HabitosActivity)
                .setView(dialogBinding.root)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }

            dialogBinding.btnAgregar.setOnClickListener {
                val nombre = dialogBinding.etNombreHabito.text.toString()
                val ancla = dialogBinding.etAnclaHabito.text.toString()
                val pos = dialogBinding.spinnerIdentidad.selectedItemPosition
                val identidadId = if (pos == 0) null else identidades[pos - 1].id

                if (nombre.isNotBlank()) {
                    viewModel.agregarHabito(nombre, "", ancla, identidadId)
                    dialog.dismiss()
                } else {
                    dialogBinding.tilNombre.error = "Campo obligatorio"
                }
            }

            dialog.show()
        }
    }

    private fun mostrarTimePicker(habito: com.example.colorblend.domain.model.Habito) {
        val picker = android.app.TimePickerDialog(
            this,
            R.style.DialogoOscuro,
            { _, hora, minuto ->
                viewModel.activarNotificacion(habito, hora, minuto, this)
            },
            habito.notificacionHora,
            habito.notificacionMinuto,
            true
        )
        picker.setTitle("Recordatorio para: ${habito.nombre}")
        picker.show()
    }
}
