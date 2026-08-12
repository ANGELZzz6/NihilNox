package com.example.colorblend.ui.gacha

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.ProgresionRepository
import com.example.colorblend.domain.model.*
import com.example.colorblend.domain.usecase.ProgresionUseCase
import com.example.colorblend.domain.usecase.TipoSugerencia
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class ProgresionActivity : AppCompatActivity() {

    private lateinit var viewModel: ProgresionViewModel
    private lateinit var autoCompleteEjercicios: MaterialAutoCompleteTextView
    private lateinit var layoutSeriesContainer: LinearLayout
    private lateinit var sliderMolestia: Slider
    private lateinit var tvValorMolestia: TextView
    private lateinit var etNotas: TextInputEditText
    private lateinit var btnGuardar: MaterialButton
    private lateinit var rvHistorial: RecyclerView
    private lateinit var adapter: HistorialProgresionAdapter

    private lateinit var tvTimer: TextView
    private var timerDescanso: android.os.CountDownTimer? = null

    private lateinit var cardDetalles: MaterialCardView
    private lateinit var tvDetalleDescanso: TextView
    private lateinit var tvDetalleTempo: TextView
    private lateinit var tvDetalleCalentamiento: TextView
    private lateinit var tvDetalleNotasTendon: TextView
    private lateinit var btnIniciarDescanso: MaterialButton

    private var currentExerciseId: Long = -1
    private var draftLoadedForCurrentExercise = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progresion)
        FullScreenHelper.enable(this)

        val repository = ProgresionRepository(AppDatabase.getDatabase(this).progresionDao())
        val useCase = ProgresionUseCase()
        val factory = ProgresionViewModelFactory(repository, useCase)
        viewModel = ViewModelProvider(this, factory)[ProgresionViewModel::class.java]

        initViews()
        setupObservers()
        viewModel.cargarEjercicios()
    }

    private fun initViews() {
        autoCompleteEjercicios = findViewById(R.id.autoCompleteEjercicios)
        layoutSeriesContainer = findViewById(R.id.layoutSeriesContainer)
        sliderMolestia = findViewById(R.id.sliderMolestia)
        tvValorMolestia = findViewById(R.id.tvValorMolestia)
        etNotas = findViewById(R.id.etNotasProgresion)
        btnGuardar = findViewById(R.id.btnGuardarSesion)
        rvHistorial = findViewById(R.id.rvHistorialProgresion)
        tvTimer = findViewById(R.id.tvTimerProgresion)

        cardDetalles = findViewById(R.id.cardDetallesTecnicos)
        tvDetalleDescanso = findViewById(R.id.tvDetalleDescanso)
        tvDetalleTempo = findViewById(R.id.tvDetalleTempo)
        tvDetalleCalentamiento = findViewById(R.id.tvDetalleCalentamiento)
        tvDetalleNotasTendon = findViewById(R.id.tvDetalleNotasTendon)
        btnIniciarDescanso = findViewById(R.id.btnIniciarDescanso)

        findViewById<View>(R.id.btnBackProgresion).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAgregarEjercicio).setOnClickListener { mostrarDialogoNuevoEjercicio() }
        findViewById<View>(R.id.btnOpcionesProgresion).setOnClickListener { mostrarMenuOpciones(it) }
        findViewById<View>(R.id.btnCalendarioProgresion).setOnClickListener {
            startActivity(android.content.Intent(this, ProgresionCalendarioActivity::class.java))
        }

        findViewById<Button>(R.id.btnAnadirSerie).setOnClickListener { anadirSerie() }
        findViewById<Button>(R.id.btnQuitarSerie).setOnClickListener { quitarSerie() }

        tvDetalleDescanso.setOnClickListener { 
            viewModel.ejercicioSeleccionado.value?.descansoSegundos?.let { iniciarTimer(it) }
        }

        btnIniciarDescanso.setOnClickListener {
            viewModel.ejercicioSeleccionado.value?.descansoSegundos?.let { iniciarTimer(it) }
        }

        sliderMolestia.addOnChangeListener { _, value, _ ->
            tvValorMolestia.text = "${value.toInt()}/10"
            actualizarColorMolestia(value.toInt())
            autoGuardarBorrador()
        }

        etNotas.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { autoGuardarBorrador() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnGuardar.setOnClickListener { guardarSesion() }

        rvHistorial.layoutManager = LinearLayoutManager(this)
        adapter = HistorialProgresionAdapter()
        rvHistorial.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.ejercicios.observe(this) { ejercicios ->
            val nombres = ejercicios.map { it.nombre }
            val adapter = ArrayAdapter(this, R.layout.item_spinner_white, nombres)
            autoCompleteEjercicios.setAdapter(adapter)

            autoCompleteEjercicios.setOnItemClickListener { _, _, position, _ ->
                val selectedName = autoCompleteEjercicios.text.toString()
                val selectedEj = ejercicios.find { it.nombre == selectedName }
                selectedEj?.let { 
                    currentExerciseId = it.id
                    draftLoadedForCurrentExercise = false // Reset flag
                    viewModel.seleccionarEjercicio(it)
                    ocultarTeclado()
                }
            }
        }

        viewModel.ultimaSesionData.observe(this) { data ->
            if (!draftLoadedForCurrentExercise) {
                crearCardsDeSeries(data?.second)
            }
        }

        viewModel.historial.observe(this) { historial ->
            adapter.submitList(historial)
        }

        viewModel.ejercicioSeleccionado.observe(this) { ej ->
            if (ej != null) {
                actualizarCardDetalles(ej)
            } else {
                cardDetalles.visibility = View.GONE
            }
        }

        viewModel.borradorCargado.observe(this) { borrador ->
            if (borrador != null) {
                restaurarBorrador(borrador)
            }
        }
    }

    private fun restaurarBorrador(borrador: SesionBorradorEntity) {
        if (borrador.ejercicioId != currentExerciseId) return // Safety check
        
        try {
            draftLoadedForCurrentExercise = true
            val jsonArray = org.json.JSONArray(borrador.jsonSeries)
            val seriesDraft = mutableListOf<SerieEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                seriesDraft.add(SerieEntity(
                    sesionId = 0,
                    numeroSerie = i + 1,
                    pesoKg = obj.getDouble("peso").toFloat(),
                    reps = obj.getInt("reps"),
                    rir = if (obj.has("rir")) obj.getInt("rir") else null
                ))
            }
            
            crearCardsDeSeries(seriesDraft)
            sliderMolestia.value = borrador.molestia.toFloat().coerceIn(0f, 10f)
            etNotas.setText(borrador.notas)
            
            Toast.makeText(this, "Borrador recuperado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun autoGuardarBorrador() {
        val series = leerSeriesDeUI()
        val molestia = sliderMolestia.value.toInt()
        val notas = etNotas.text.toString()
        viewModel.guardarBorradorActual(series, molestia, notas)
    }

    private fun leerSeriesDeUI(): List<SerieEntity> {
        val series = mutableListOf<SerieEntity>()
        for (i in 0 until layoutSeriesContainer.childCount) {
            val card = layoutSeriesContainer.getChildAt(i)
            val peso = card.findViewById<Slider>(R.id.sliderPeso).value
            val reps = card.findViewById<Slider>(R.id.sliderReps).value.toInt()
            val rir = if (i == layoutSeriesContainer.childCount - 1) {
                card.findViewById<Slider>(R.id.sliderRir).value.toInt()
            } else null
            
            series.add(SerieEntity(sesionId = 0, numeroSerie = i + 1, pesoKg = peso, reps = reps, rir = rir))
        }
        return series
    }

    private fun iniciarTimer(segundos: Int) {
        timerDescanso?.cancel()
        tvTimer.visibility = View.VISIBLE
        
        timerDescanso = object : android.os.CountDownTimer(segundos * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seg = (millisUntilFinished / 1000).toInt()
                val min = seg / 60
                val s = seg % 60
                tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", min, s)
            }

            override fun onFinish() {
                tvTimer.text = "¡TIEMPO!"
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
                
                tvTimer.postDelayed({ tvTimer.visibility = View.GONE }, 3000)
            }
        }.start()
    }

    private fun ocultarTeclado() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun actualizarCardDetalles(ej: EjercicioEntity) {
        val tieneDetalles = ej.descansoSegundos != null || !ej.tempo.isNullOrBlank() || 
                           !ej.protocoloCalentamiento.isNullOrBlank() || !ej.notasTendon.isNullOrBlank()
        
        if (tieneDetalles) {
            cardDetalles.visibility = View.VISIBLE
            tvDetalleDescanso.text = "⏳ Descanso: ${ej.descansoSegundos ?: 0}s"
            tvDetalleTempo.text = "⏱️ Tempo: ${ej.tempo ?: "N/A"}"
            
            if (!ej.protocoloCalentamiento.isNullOrBlank()) {
                tvDetalleCalentamiento.visibility = View.VISIBLE
                tvDetalleCalentamiento.text = "🔥 Calentamiento: ${ej.protocoloCalentamiento}"
            } else {
                tvDetalleCalentamiento.visibility = View.GONE
            }

            if (!ej.notasTendon.isNullOrBlank()) {
                tvDetalleNotasTendon.visibility = View.VISIBLE
                tvDetalleNotasTendon.text = "⚠️ Tendón: ${ej.notasTendon}"
            } else {
                tvDetalleNotasTendon.visibility = View.GONE
            }
        } else {
            cardDetalles.visibility = View.GONE
        }
    }

    private fun mostrarMenuOpciones(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_progresion_opciones, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_exportar_json -> exportarDia("JSON")
                R.id.menu_exportar_pdf -> exportarDia("PDF")
                R.id.menu_importar_ejercicios -> lanzarSelectorJson()
                R.id.menu_ver_formato -> mostrarFormatoJsonIA()
                R.id.menu_eliminar_todo -> confirmarEliminacionTotal()
            }
            true
        }
        popup.show()
    }

    private fun confirmarEliminacionTotal() {
        androidx.appcompat.app.AlertDialog.Builder(this, R.style.DialogoOscuro)
            .setTitle("⚠️ ELIMINAR TODO")
            .setMessage("¿Estás seguro de que deseas eliminar TODOS los ejercicios y sus historiales? Esta acción no se puede deshacer.")
            .setPositiveButton("ELIMINAR") { _, _ ->
                viewModel.eliminarTodo()
                Toast.makeText(this, "Todos los datos han sido borrados", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportarDia(formato: String) {
        val json = viewModel.exportarDiaJson(System.currentTimeMillis())
        if (formato == "JSON") {
            guardarArchivo("sesion_${System.currentTimeMillis()}.json", json.toByteArray())
        } else {
            generarPdfDeHistorial(json)
        }
    }

    private fun generarPdfDeHistorial(jsonString: String) {
        try {
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint()
            
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("REPORTE DE ENTRENAMIENTO - ColorBlend", 50f, 50f, paint)
            
            paint.textSize = 12f
            paint.isFakeBoldText = false
            var y = 80f
            
            val json = org.json.JSONObject(jsonString)
            val sesiones = json.getJSONArray("sesiones")
            
            for (i in 0 until sesiones.length()) {
                val s = sesiones.getJSONObject(i)
                canvas.drawText("${s.getString("ejercicio")} (${s.getString("fecha")})", 50f, y, paint)
                y += 20f
                val series = s.getJSONArray("series")
                val sb = StringBuilder()
                for (j in 0 until series.length()) {
                    val ser = series.getJSONObject(j)
                    sb.append("${ser.getDouble("peso")}kg x ${ser.getInt("reps")}")
                    if (j < series.length() - 1) sb.append(" | ")
                }
                canvas.drawText(sb.toString(), 70f, y, paint)
                y += 20f
                canvas.drawText("Molestia: ${s.getInt("molestia")} | Notas: ${s.getString("notas")}", 70f, y, paint)
                y += 40f
                
                if (y > 800) break // Simplemente cortamos por ahora
            }

            pdf.finishPage(page)
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "entrenamiento_${System.currentTimeMillis()}.pdf")
            pdf.writeTo(FileOutputStream(file))
            pdf.close()
            Toast.makeText(this, "PDF guardado en Documentos", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarArchivo(nombre: String, content: ByteArray) {
        try {
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), nombre)
            FileOutputStream(file).use { it.write(content) }
            Toast.makeText(this, "Archivo guardado: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar archivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun lanzarSelectorJson() {
        val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT)
        intent.type = "application/json"
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    val json = InputStreamReader(input).readText()
                    viewModel.importarEjerciciosJson(json)
                    Toast.makeText(this, "Ejercicios importados", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al leer JSON", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarFormatoJsonIA() {
        val ejemplo = """
        {
          "ejercicios": [
            {
              "name": "Press banca",
              "peso_inicial": 60.0,
              "reps_min": 6,
              "reps_max": 10,
              "sets": 4,
              "rest_seconds": 150,
              "tempo": "3-1-1",
              "warmup_protocol": "barra vacia x10 -> 40% x10",
              "tendon_notes": "Controlar bajada",
              "is_isometric": false,
              "es_principal": true
            }
          ]
        }
        """.trimIndent()

        val tv = TextView(this).apply {
            text = ejemplo
            setPadding(40, 40, 40, 40)
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 12f
        }

        androidx.appcompat.app.AlertDialog.Builder(this, R.style.DialogoOscuro)
            .setTitle("Formato JSON para IA")
            .setView(tv)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Copiar") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("JSON IA", ejemplo))
                Toast.makeText(this, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun crearCardsDeSeries(seriesAnteriores: List<SerieEntity>?) {
        layoutSeriesContainer.removeAllViews()
        val ejercicio = viewModel.ejercicioSeleccionado.value ?: return
        
        val numSeries = seriesAnteriores?.size ?: ejercicio.seriesPredeterminadas
        for (i in 0 until numSeries) {
            agregarVistaSerie(i, seriesAnteriores?.getOrNull(i), ejercicio)
        }
        actualizarVisibilidadRir()
    }

    private fun agregarVistaSerie(index: Int, datosAnteriores: SerieEntity?, ejercicio: EjercicioEntity) {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_progresion_serie, layoutSeriesContainer, false)
        val tvNumero = view.findViewById<TextView>(R.id.tvNumeroSerie)
        val sliderPeso = view.findViewById<Slider>(R.id.sliderPeso)
        val tvPesoValue = view.findViewById<TextView>(R.id.tvValorPeso)
        val sliderReps = view.findViewById<Slider>(R.id.sliderReps)
        val tvRepsValue = view.findViewById<TextView>(R.id.tvValorReps)
        val tvLabelReps = view.findViewById<TextView>(R.id.tvLabelReps)
        val btnAumentarPeso = view.findViewById<Button>(R.id.btnAumentarRangoPeso)

        tvNumero.text = "SERIE ${index + 1}"
        
        if (ejercicio.esIsometrico) {
            tvLabelReps.text = "SEGUNDOS"
            sliderReps.valueFrom = 0f
            sliderReps.valueTo = 180f
        }

        // Configurar Sliders con Rango Dinámico (±30%)
        val pesoBase = datosAnteriores?.pesoKg ?: ejercicio.pesoActualKg
        val repsBase = datosAnteriores?.reps ?: ejercicio.rangoRepsMin
        
        val stepSize = if (pesoBase < 15f) 0.5f else 1.25f
        val rawMaxPeso = (pesoBase * 1.3f).coerceAtLeast(pesoBase + 10f)
        var maxPesoActual = (kotlin.math.ceil(rawMaxPeso / stepSize) * stepSize).toFloat()
        
        sliderPeso.valueFrom = 0f
        sliderPeso.valueTo = maxPesoActual
        sliderPeso.stepSize = stepSize
        
        // Ajustar el valor inicial para que sea múltiplo exacto del stepSize y evitar el crash
        val validValue = (kotlin.math.round(pesoBase / stepSize) * stepSize).toFloat()
        sliderPeso.value = validValue.coerceIn(0f, maxPesoActual)
        tvPesoValue.text = "$validValue kg"

        btnAumentarPeso.setOnClickListener {
            maxPesoActual += 20f
            // Asegurar que el nuevo max siga siendo múltiplo del step
            maxPesoActual = (kotlin.math.ceil(maxPesoActual / stepSize) * stepSize).toFloat()
            sliderPeso.valueTo = maxPesoActual
            Toast.makeText(this, "Rango aumentado a ${maxPesoActual}kg", Toast.LENGTH_SHORT).show()
        }

        sliderReps.value = repsBase.toFloat().coerceIn(sliderReps.valueFrom, sliderReps.valueTo)
        tvRepsValue.text = "${sliderReps.value.toInt()}"

        sliderPeso.addOnChangeListener { _, value, _ -> 
            tvPesoValue.text = "$value kg"
            autoGuardarBorrador()
        }
        sliderReps.addOnChangeListener { _, value, _ -> 
            tvRepsValue.text = "${value.toInt()}"
            autoGuardarBorrador()
        }

        layoutSeriesContainer.addView(view)
    }

    private fun anadirSerie() {
        val ejercicio = viewModel.ejercicioSeleccionado.value ?: return
        val count = layoutSeriesContainer.childCount
        if (count >= 8) return // Límite razonable
        
        // Copiar valores de la última serie si existe para facilitar la anotación
        val ultimaSerieView = layoutSeriesContainer.getChildAt(count - 1)
        val ultimoPeso = ultimaSerieView?.findViewById<Slider>(R.id.sliderPeso)?.value ?: ejercicio.pesoActualKg
        val ultimasReps = ultimaSerieView?.findViewById<Slider>(R.id.sliderReps)?.value?.toInt() ?: ejercicio.rangoRepsMin
        
        val dummyData = SerieEntity(sesionId = 0, numeroSerie = count + 1, pesoKg = ultimoPeso, reps = ultimasReps, rir = null)
        agregarVistaSerie(count, dummyData, ejercicio)
        actualizarVisibilidadRir()
        autoGuardarBorrador()
    }

    private fun quitarSerie() {
        val count = layoutSeriesContainer.childCount
        if (count > 1) {
            layoutSeriesContainer.removeViewAt(count - 1)
            actualizarVisibilidadRir()
            autoGuardarBorrador()
        }
    }

    private fun actualizarVisibilidadRir() {
        for (i in 0 until layoutSeriesContainer.childCount) {
            val card = layoutSeriesContainer.getChildAt(i)
            val layoutRir = card.findViewById<View>(R.id.layoutRir)
            val sliderRir = card.findViewById<Slider>(R.id.sliderRir)
            val tvRirValue = card.findViewById<TextView>(R.id.tvValorRir)

            if (i == layoutSeriesContainer.childCount - 1) {
                layoutRir.visibility = View.VISIBLE
                if (sliderRir.value == 0f) sliderRir.value = 2f
                tvRirValue.text = "${sliderRir.value.toInt()}"
                sliderRir.clearOnChangeListeners()
                sliderRir.addOnChangeListener { _, value, _ -> 
                    tvRirValue.text = "${value.toInt()}"
                    autoGuardarBorrador()
                }
            } else {
                layoutRir.visibility = View.GONE
            }
        }
    }

    private fun actualizarColorMolestia(valor: Int) {
        val color = when {
            valor < 3 -> ContextCompat.getColor(this, R.color.dash_primary) // Verde/Cian
            valor < 5 -> ContextCompat.getColor(this, R.color.dash_secondary_gold) // Amarillo
            else -> ContextCompat.getColor(this, R.color.dash_error) // Rojo
        }
        sliderMolestia.thumbTintList = ColorStateList.valueOf(color)
        sliderMolestia.trackActiveTintList = ColorStateList.valueOf(color)
        tvValorMolestia.setTextColor(color)
    }

    private fun mostrarDialogoNuevoEjercicio() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nuevo_ejercicio, null)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreEjercicio)
        val etPeso = dialogView.findViewById<TextInputEditText>(R.id.etPesoInicial)
        val etMin = dialogView.findViewById<TextInputEditText>(R.id.etRepsMin)
        val etMax = dialogView.findViewById<TextInputEditText>(R.id.etRepsMax)
        val etDescanso = dialogView.findViewById<TextInputEditText>(R.id.etDescansoEjercicio)
        val etTempo = dialogView.findViewById<TextInputEditText>(R.id.etTempoEjercicio)
        val etCalentamiento = dialogView.findViewById<TextInputEditText>(R.id.etCalentamientoEjercicio)
        val etNotasTendon = dialogView.findViewById<TextInputEditText>(R.id.etNotasTendonEjercicio)
        val swPrincipal = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchPrincipal)
        val swIsometrico = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchIsometrico)

        androidx.appcompat.app.AlertDialog.Builder(this, R.style.DialogoOscuro)
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = etNombre.text.toString()
                val peso = etPeso.text.toString().toFloatOrNull() ?: 0f
                val min = etMin.text.toString().toIntOrNull() ?: 8
                val max = etMax.text.toString().toIntOrNull() ?: 12
                val descanso = etDescanso.text.toString().toIntOrNull()
                val tempo = etTempo.text.toString().takeIf { it.isNotBlank() }
                val calentamiento = etCalentamiento.text.toString().takeIf { it.isNotBlank() }
                val notasT = etNotasTendon.text.toString().takeIf { it.isNotBlank() }
                val principal = swPrincipal.isChecked
                val isometrico = swIsometrico.isChecked
                
                if (nombre.isNotBlank()) {
                    viewModel.agregarEjercicio(nombre, peso, min, max, principal, isometrico, descanso, tempo, calentamiento, notasT)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarSesion() {
        val series = leerSeriesDeUI()
        val molestia = sliderMolestia.value.toInt()
        val notas = etNotas.text.toString()

        viewModel.guardarSesion(series, molestia, notas)
    }

    private fun mostrarSugerencia(sug: com.example.colorblend.domain.usecase.SugerenciaProgresion) {
        val color = when (sug.tipo) {
            TipoSugerencia.SUBIR_PESO -> Color.parseColor("#4CAF50")
            TipoSugerencia.BAJAR_PESO -> Color.parseColor("#F44336")
            TipoSugerencia.DESCARGA -> Color.parseColor("#2196F3")
            else -> Color.parseColor("#FFD700")
        }

        val snack = Snackbar.make(btnGuardar, sug.mensaje, Snackbar.LENGTH_INDEFINITE)
        snack.setAction("OK") { snack.dismiss() }
        snack.setBackgroundTint(color)
        snack.setTextColor(Color.BLACK)
        snack.setActionTextColor(Color.BLACK)
        
        val textView = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.maxLines = 5
        
        snack.show()

        if (!sug.alertaMolestia.isNullOrEmpty()) {
            Snackbar.make(btnGuardar, sug.alertaMolestia, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.dash_error))
                .show()
        }
    }
}

class HistorialProgresionAdapter : RecyclerView.Adapter<HistorialProgresionAdapter.ViewHolder>() {

    private var items = listOf<HistoryItem>()
    private val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())

    fun submitList(newList: List<HistoryItem>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_historial_sesion, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val sesion = item.sesion
        val series = item.series
        val registro = item.registro
        
        holder.tvFecha.text = sdf.format(Date(sesion.fecha)).uppercase()
        holder.tvNombre.text = item.nombreEjercicio
        
        holder.tvS1.text = series.getOrNull(0)?.let { "${it.pesoKg}x${it.reps}" } ?: "-"
        holder.tvS2.text = series.getOrNull(1)?.let { "${it.pesoKg}x${it.reps}" } ?: "-"
        holder.tvS3.text = series.getOrNull(2)?.let { "${it.pesoKg}x${it.reps}" } ?: "-"
        holder.tvS4.text = series.getOrNull(3)?.let { "${it.pesoKg}x${it.reps}" } ?: "-"
        holder.tvS5.text = series.getOrNull(4)?.let { "${it.pesoKg}x${it.reps}" } ?: "-"
        holder.tvS6.text = series.getOrNull(5)?.let { "${it.pesoKg}x${it.reps}" } ?: "-"
        
        holder.tvRir.text = series.lastOrNull()?.rir?.toString() ?: "-"
        holder.tvMolestia.text = registro?.molestiaArticular?.toString() ?: "-"
        
        val totalSeries = series.size
        holder.tvNotas.text = "[$totalSeries series] ${registro?.notas ?: ""}"
    }

    override fun getItemCount() = items.size

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvFecha: TextView = v.findViewById(R.id.tvFechaHistorial)
        val tvNombre: TextView = v.findViewById(R.id.tvNombreEjercicioHistorial)
        val tvS1: TextView = v.findViewById(R.id.tvSerie1Historial)
        val tvS2: TextView = v.findViewById(R.id.tvSerie2Historial)
        val tvS3: TextView = v.findViewById(R.id.tvSerie3Historial)
        val tvS4: TextView = v.findViewById(R.id.tvSerie4Historial)
        val tvS5: TextView = v.findViewById(R.id.tvSerie5Historial)
        val tvS6: TextView = v.findViewById(R.id.tvSerie6Historial)
        val tvRir: TextView = v.findViewById(R.id.tvRirHistorial)
        val tvMolestia: TextView = v.findViewById(R.id.tvMolestiaHistorial)
        val tvNotas: TextView = v.findViewById(R.id.tvNotasHistorial)
    }
}
