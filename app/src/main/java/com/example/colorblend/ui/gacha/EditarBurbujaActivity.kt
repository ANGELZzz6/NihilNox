package com.example.colorblend.ui.gacha

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.databinding.ActivityEditarBurbujaBinding
import com.example.colorblend.domain.model.Habito
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditarBurbujaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarBurbujaBinding
    private lateinit var viewModel: HabitosViewModel
    private var habitoId: Int = -1
    private var habito: Habito? = null
    private var colorSeleccionado: String = "#FFD700"
    private var imagenUri: String? = null

    private val coloresDisponibles = listOf(
        "#FFD700", "#4CAF50", "#2196F3", "#E91E63",
        "#FF5722", "#9C27B0", "#00BCD4", "#FFFFFF"
    )

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val destino = copiarImagenInterna(it)
            imagenUri = destino
            binding.tvImagenSeleccionada.text = "Imagen seleccionada ✓"
            actualizarPreview()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarBurbujaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        habitoId = intent.getIntExtra("habito_id", -1)
        if (habitoId == -1) { finish(); return }

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = HabitosRepository(db.habitoDao(), db.registroHabitoDao(), db.identidadDao())
        viewModel = ViewModelProvider(this, HabitosViewModelFactory(application, repository))
            .get(HabitosViewModel::class.java)

        construirSelectorColores()
        configurarListeners()

        lifecycleScope.launch {
            habito = viewModel.getHabitoById(habitoId)
            habito?.let { cargarDatosActuales(it) }
        }
    }

    private fun construirSelectorColores() {
        coloresDisponibles.forEach { hex ->
            val circulo = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp).apply {
                    marginEnd = 10.dp
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(hex))
                    if (hex == "#FFFFFF") setStroke(1.dp, Color.parseColor("#333333"))
                }
                setOnClickListener {
                    colorSeleccionado = hex
                    actualizarPreview()
                    resaltarColorSeleccionado(this)
                }
            }
            binding.layoutColores.addView(circulo)
        }
    }

    private fun resaltarColorSeleccionado(vistaSeleccionada: View) {
        for (i in 0 until binding.layoutColores.childCount) {
            val hijo = binding.layoutColores.getChildAt(i)
            hijo.scaleX = if (hijo == vistaSeleccionada) 1.25f else 1f
            hijo.scaleY = if (hijo == vistaSeleccionada) 1.25f else 1f
        }
    }

    private fun configurarListeners() {
        binding.switchUsarImagen.setOnCheckedChangeListener { _, checked ->
            binding.btnSeleccionarImagen.visibility = if (checked) View.VISIBLE else View.GONE
            binding.tvImagenSeleccionada.visibility = if (checked) View.VISIBLE else View.GONE
            actualizarPreview()
        }

        binding.btnSeleccionarImagen.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.etBurbujaTexto.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { actualizarPreview() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnGuardarBurbuja.setOnClickListener { guardar() }

        binding.btnLanzarAhora.setOnClickListener { lanzarBurbujaReal() }
    }

    private fun lanzarBurbujaReal() {
        val h = habito ?: return
        val intent = android.content.Intent(this, BurbujaHabitoService::class.java).apply {
            putExtra("habito_id", h.id)
            putExtra("habito_nombre", h.nombre)
            putExtra("force_now", true)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        android.widget.Toast.makeText(this, "🚀 Lanzando burbuja real...", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun cargarDatosActuales(h: Habito) {
        colorSeleccionado = h.burbujaColor
        imagenUri = h.burbujaImagenUri
        binding.etBurbujaTexto.setText(h.burbujaTexto ?: "")
        binding.switchUsarImagen.isChecked = h.burbujaUsarImagen
        if (h.burbujaUsarImagen) {
            binding.btnSeleccionarImagen.visibility = View.VISIBLE
            binding.tvImagenSeleccionada.visibility = View.VISIBLE
            binding.tvImagenSeleccionada.text = if (h.burbujaImagenUri != null) "Imagen seleccionada ✓" else "Sin imagen seleccionada"
        }
        actualizarPreview()
    }

    private fun actualizarPreview() {
        val preview = binding.previewBurbuja
        val colorFondo = try { Color.parseColor(colorSeleccionado) }
                         catch (e: Exception) { Color.parseColor("#FFD700") }

        preview.ivBubbleFondo.backgroundTintList = android.content.res.ColorStateList.valueOf(colorFondo)

        val usarImagen = binding.switchUsarImagen.isChecked && imagenUri != null
        val texto = binding.etBurbujaTexto.text.toString()
            .takeIf { it.isNotBlank() } ?: habito?.nombre ?: "Hábito"

        preview.tvBubbleNombre.visibility = if (usarImagen) View.GONE else View.VISIBLE
        preview.tvBubbleNombre.text = texto

        val luminancia = (0.299 * Color.red(colorFondo) +
                          0.587 * Color.green(colorFondo) +
                          0.114 * Color.blue(colorFondo)) / 255
        preview.tvBubbleNombre.setTextColor(
            if (luminancia > 0.5) Color.parseColor("#121212")
            else Color.WHITE
        )

        if (usarImagen) {
            preview.ivBubbleImagen.visibility = View.VISIBLE
            try { preview.ivBubbleImagen.setImageURI(Uri.parse(imagenUri)) }
            catch (e: Exception) { preview.ivBubbleImagen.visibility = View.GONE }
        } else {
            preview.ivBubbleImagen.visibility = View.GONE
        }
    }

    private fun guardar() {
        val h = habito ?: return
        val actualizado = h.copy(
            burbujaTexto = binding.etBurbujaTexto.text.toString().takeIf { it.isNotBlank() },
            burbujaColor = colorSeleccionado,
            burbujaImagenUri = imagenUri,
            burbujaUsarImagen = binding.switchUsarImagen.isChecked
        )
        lifecycleScope.launch {
            viewModel.actualizarHabito(actualizado)
            if (actualizado.enabledBurbuja) {
                HabitoAlarmManager.programarBurbuja(applicationContext, actualizado)
            }
            withContext(Dispatchers.Main) {
                finish()
            }
        }
    }

    private fun copiarImagenInterna(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri) ?: return uri.toString()
        val archivo = File(filesDir, "burbuja_${habitoId}_${System.currentTimeMillis()}.jpg")
        archivo.outputStream().use { inputStream.copyTo(it) }
        return archivo.absolutePath
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
}
