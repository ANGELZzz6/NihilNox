package com.example.colorblend.ui.gacha

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import android.widget.AdapterView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.FileProvider
import java.io.File
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.AlimentoGuardado
import com.example.colorblend.domain.model.RegistroAlimento
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AgregarAlimentoActivity : AppCompatActivity() {

    private val viewModel: NutricionViewModel by viewModels()
    private var categoriaSeleccionada = "Desayuno"
    private var busquedaJob: Job? = null

    private lateinit var etNombre: EditText
    private lateinit var etCantidad: EditText
    private lateinit var spinnerUnidad: Spinner
    private lateinit var etCalorias: EditText
    private lateinit var etProteina: EditText
    private lateinit var etCarbos: EditText
    private lateinit var etGrasas: EditText
    private lateinit var etFibra: EditText
    private lateinit var etAzucares: EditText
    private lateinit var spinnerCategoria: Spinner
    private var fotoUri: Uri? = null

    private val tomarFoto = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fotoUri?.let { procesarImagenOCR(it) }
        }
    }

    private val seleccionarImagen = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { procesarImagenOCR(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_alimento)

        categoriaSeleccionada = intent.getStringExtra("categoria") ?: "Desayuno"

        etNombre       = findViewById(R.id.etNombreAlimento)
        etCantidad     = findViewById(R.id.etCantidad)
        spinnerUnidad  = findViewById(R.id.spinnerUnidad)
        etCalorias     = findViewById(R.id.etCalorias)
        etProteina     = findViewById(R.id.etProteina)
        etCarbos       = findViewById(R.id.etCarbos)
        etGrasas       = findViewById(R.id.etGrasas)
        etFibra        = findViewById(R.id.etFibra)
        etAzucares     = findViewById(R.id.etAzucares)
        spinnerCategoria = findViewById(R.id.spinnerCategoriaAlimento)

        val etBusqueda          = findViewById<EditText>(R.id.etBusquedaAlimento)
        val recyclerSugerencias = findViewById<RecyclerView>(R.id.recyclerSugerencias)
        val tvCargandoBusqueda  = findViewById<TextView>(R.id.tvCargandoBusqueda)
        val btnGuardar          = findViewById<Button>(R.id.btnGuardarAlimento)
        val btnFoto             = findViewById<Button>(R.id.btnEscanearFoto)
        val btnGaleria          = findViewById<Button>(R.id.btnDesdeGaleria)
        val btnVolver           = findViewById<Button>(R.id.btnVolverAgregar)
        val tvOcrResultado      = findViewById<TextView>(R.id.tvOcrResultado)
        val btnEstimarIA        = findViewById<Button>(R.id.btnEstimarIA)
        val tvEstimacionInfo    = findViewById<TextView>(R.id.tvEstimacionInfo)

        ArrayAdapter.createFromResource(
            this, R.array.unidades_array, android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerUnidad.adapter = it
        }
        spinnerUnidad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                etCalorias.hint = when (position) {
                    0    -> "kcal por 100g"
                    1    -> "kcal por 100ml"
                    else -> "kcal por porción"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        ArrayAdapter.createFromResource(
            this, R.array.categorias_comida, android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategoria.adapter = it
            val index = listOf("Desayuno","Almuerzo","Cena","Snack").indexOf(categoriaSeleccionada)
            if (index >= 0) spinnerCategoria.setSelection(index)
        }

        val sugerenciasAdapter = SugerenciasAdapter { alimento ->
            rellenarFormulario(alimento)
            recyclerSugerencias.visibility = View.GONE
        }
        recyclerSugerencias.adapter = sugerenciasAdapter
        recyclerSugerencias.layoutManager = LinearLayoutManager(this)

        etBusqueda.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                busquedaJob?.cancel()
                if (query.length < 2) { recyclerSugerencias.visibility = View.GONE; return }
                busquedaJob = lifecycleScope.launch {
                    delay(400)
                    viewModel.buscar(query)
                }
            }
        })

        // ── Botón estimar con IA ──────────────────────────────────────────────
        btnEstimarIA.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val peso   = etCantidad.text.toString().toFloatOrNull() ?: 0f
            viewModel.estimarMacros(this, nombre, peso)
        }

        lifecycleScope.launch {
            launch {
                viewModel.busquedaResultados.collect { lista ->
                    if (lista.isEmpty()) recyclerSugerencias.visibility = View.GONE
                    else { sugerenciasAdapter.update(lista); recyclerSugerencias.visibility = View.VISIBLE }
                }
            }
            launch {
                viewModel.cargandoBusqueda.collect { cargando ->
                    tvCargandoBusqueda.visibility = if (cargando) View.VISIBLE else View.GONE
                }
            }
            // ── Collector: loading del botón IA ──────────────────────────────
            launch {
                viewModel.cargandoEstimacion.collect { cargando ->
                    btnEstimarIA.isEnabled = !cargando
                    btnEstimarIA.text = if (cargando) "⏳ Estimando..." else "✨ Estimar con IA"
                }
            }
            // ── Collector: macros estimados — rellena los campos ──────────────
            launch {
                viewModel.macrosEstimados.collect { macros ->
                    etCalorias.setText(macros.calorias.toString())
                    etProteina.setText(macros.proteina.toString())
                    etCarbos.setText(macros.carbos.toString())
                    etGrasas.setText(macros.grasas.toString())
                    etFibra.setText(macros.fibra.toString())
                    etAzucares.setText(macros.azucares.toString())
                    // ── Fix: cantidad = 1 porción para que factor = 1 ────────────
                    etCantidad.setText("1")
                    spinnerUnidad.setSelection(2) // porción → factor = 1
                    tvEstimacionInfo.text = "⚠️ Valores estimados por IA para la cantidad indicada. Revisa antes de guardar."
                    tvEstimacionInfo.visibility = View.VISIBLE
                }
            }
            // ── Collector: error estimación ───────────────────────────────────
            launch {
                viewModel.errorEstimacion.collect { error ->
                    if (!error.isNullOrBlank()) {
                        Toast.makeText(this@AgregarAlimentoActivity, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btnFoto.setOnClickListener {
            val archivo = File(filesDir, "ocr_temp.jpg")
            fotoUri = FileProvider.getUriForFile(this, "${packageName}.provider", archivo)
            tomarFoto.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, fotoUri)
            })
        }

        btnGaleria.setOnClickListener { seleccionarImagen.launch("image/*") }
        btnGuardar.setOnClickListener { guardarAlimento(tvOcrResultado) }
        btnVolver.setOnClickListener  { finish() }
    }

    // [resto de funciones igual — procesarImagenOCR, extraerMacrosDeTexto, etc.]

    private fun procesarImagenOCR(uri: Uri) {
        val tvOcrResultado = findViewById<TextView>(R.id.tvOcrResultado)
        tvOcrResultado.text = "⏳ Leyendo tabla nutricional..."
        tvOcrResultado.visibility = View.VISIBLE
        try {
            val image      = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { resultado ->
                    val texto = resultado.text
                    android.util.Log.d("OCR_TEXTO", "=== TEXTO RECONOCIDO ===\n$texto")
                    val macros = extraerMacrosDeTexto(texto)
                    if (macros != null) {
                        etCalorias.setText(macros.calorias.toString())
                        etProteina.setText(macros.proteina.toString())
                        etCarbos.setText(macros.carbos.toString())
                        etGrasas.setText(macros.grasas.toString())
                        etFibra.setText(macros.fibra.toString())
                        etAzucares.setText(macros.azucares.toString())
                        tvOcrResultado.text = "✅ Tabla leída. Revisa y ajusta si es necesario."
                    } else {
                        tvOcrResultado.text = "⚠️ No se reconoció la tabla. Ingresa los datos manualmente."
                    }
                }
                .addOnFailureListener { tvOcrResultado.text = "❌ Error al leer la imagen." }
        } catch (e: Exception) {
            findViewById<TextView>(R.id.tvOcrResultado).text = "❌ Error al procesar la imagen."
        }
    }

    private fun extraerMacrosDeTexto(texto: String): MacrosOCR? {
        val lineas = texto.lines().map { it.trim() }.filter { it.isNotBlank() }

        fun normalizar(s: String) = s.lowercase()
            .replace("á","a").replace("é","e").replace("í","i")
            .replace("ó","o").replace("ú","u")
            .replace("ã³","o").replace("ã¡","a").replace("ã©","e")
            .replace("ãº","u").replace("ã±","n").replace("ã","")

        fun numerosEnLinea(linea: String): List<Float> =
            Regex("""(\d+[.,]\d+|\d+)""").findAll(linea)
                .mapNotNull { it.value.replace(",", ".").toFloatOrNull() }
                .filter { it >= 0 && it < 1000 }
                .toList()

        fun primerNumero(linea: String): Float? = numerosEnLinea(linea).firstOrNull()

        val lineasRuido = listOf(
            "sodio", "sal", "calcio", "hierro", "vitamina", "zinc", "potasio",
            "magnesio", "fosforo", "colesterol", "omega", "polalcohol",
            "monoinsaturad", "polinsaturad", "saturad", "trans",
            "porcion", "porc", "envase", "tamano", "numero", "informacion",
            "serv", "%", "vd", "idr", "bimbo", "resuelve", "fuente"
        )

        fun esRuido(linea: String): Boolean {
            val n = normalizar(linea)
            return lineasRuido.any { n.contains(it) }
        }

        val palabrasCal  = listOf("caloria", "calorias", "energia", "energi", "kcal")
        val palabrasGras = listOf("grasa total", "grasas totales", "total fat", "grasa tot", "grasas")
        val palabrasCar  = listOf("carbohidrato", "carbohidratos", "hidratos", "carbohydr", "carb")
        val palabrasFib  = listOf("fibra dietaria", "fibra alimentaria", "fibra total", "fibra", "fiber")
        val palabrasProt = listOf("proteina", "proteinas", "protein", "prolena", "protena")
        val palabrasAzu  = listOf("azucares totales", "azucar total", "azucares", "azucar", "sugar")

        var calorias: Int? = null
        var grasas:   Float? = null
        var carbos:   Float? = null
        var fibra:    Float? = null
        var proteina: Float? = null
        var azucares: Float? = null

        for (linea in lineas) {
            if (esRuido(linea)) continue
            val n   = normalizar(linea)
            val num = primerNumero(linea) ?: continue
            when {
                calorias == null && palabrasCal.any  { n.contains(it) } -> calorias  = num.toInt()
                grasas == null   && palabrasGras.any { n.contains(it) } -> grasas    = num
                carbos == null   && palabrasCar.any  { n.contains(it) } -> carbos    = num
                fibra == null    && palabrasFib.any  { n.contains(it) } -> fibra     = num
                proteina == null && palabrasProt.any { n.contains(it) } -> proteina  = num
                azucares == null && palabrasAzu.any  { n.contains(it) } -> azucares  = num
            }
        }

        for (i in lineas.indices) {
            if (esRuido(lineas[i])) continue
            val n = normalizar(lineas[i])
            if (primerNumero(lineas[i]) == null) {
                for (j in 1..3) {
                    val sig = lineas.getOrNull(i + j) ?: break
                    if (esRuido(sig)) break
                    val num = primerNumero(sig) ?: continue
                    when {
                        calorias == null && palabrasCal.any  { n.contains(it) } -> { calorias  = num.toInt(); break }
                        grasas == null   && palabrasGras.any { n.contains(it) } -> { grasas    = num; break }
                        carbos == null   && palabrasCar.any  { n.contains(it) } -> { carbos    = num; break }
                        fibra == null    && palabrasFib.any  { n.contains(it) } -> { fibra     = num; break }
                        proteina == null && palabrasProt.any { n.contains(it) } -> { proteina  = num; break }
                        azucares == null && palabrasAzu.any  { n.contains(it) } -> { azucares  = num; break }
                    }
                }
            }
        }

        if (calorias == null && proteina == null) {
            val iPor100 = lineas.indexOfFirst {
                val n = normalizar(it)
                n.contains("por 100") || n.contains("per 100") || n.contains("100g")
            }
            if (iPor100 >= 0) {
                val numeros = mutableListOf<Float>()
                for (j in 1..25) {
                    val sig = lineas.getOrNull(iPor100 + j) ?: break
                    if (esRuido(sig)) continue
                    primerNumero(sig)?.let { numeros.add(it) }
                }
                if (numeros.size >= 1 && calorias == null)  calorias  = numeros[0].toInt()
                if (numeros.size >= 2 && grasas == null)    grasas    = numeros[1]
                if (numeros.size >= 3 && carbos == null)    carbos    = numeros[2]
                if (numeros.size >= 4 && fibra == null)     fibra     = numeros[3]
                if (numeros.size >= 5 && azucares == null)  azucares  = numeros[4]
                if (numeros.size >= 6 && proteina == null)  proteina  = numeros[5]
            }
        }

        android.util.Log.d("OCR_PARSE",
            "cal=$calorias prot=$proteina carb=$carbos gras=$grasas fib=$fibra azuc=$azucares")

        if (calorias == null && proteina == null) return null

        return MacrosOCR(
            calorias = calorias ?: 0, proteina = proteina ?: 0f,
            carbos = carbos ?: 0f, grasas = grasas ?: 0f,
            fibra = fibra ?: 0f, azucares = azucares ?: 0f
        )
    }

    data class MacrosOCR(
        val calorias: Int, val proteina: Float, val carbos: Float,
        val grasas: Float, val fibra: Float, val azucares: Float
    )

    private fun rellenarFormulario(alimento: AlimentoGuardado) {
        etNombre.setText(alimento.nombre)
        etCalorias.setText(alimento.calorias.toString())
        etProteina.setText(alimento.proteina.toString())
        etCarbos.setText(alimento.carbos.toString())
        etGrasas.setText(alimento.grasas.toString())
        etFibra.setText(alimento.fibra.toString())
        etAzucares.setText(alimento.azucares.toString())
        etCantidad.setText("100")
        val idx = listOf("g","ml","porción").indexOf(alimento.unidadPorDefecto)
        if (idx >= 0) spinnerUnidad.setSelection(idx)
    }

    private fun guardarAlimento(tvOcr: TextView) {
        val nombre   = etNombre.text.toString().trim()
        val cantidad = etCantidad.text.toString().toFloatOrNull()
        val calBase  = etCalorias.text.toString().toIntOrNull()
        val protBase = etProteina.text.toString().toFloatOrNull() ?: 0f
        val carbBase = etCarbos.text.toString().toFloatOrNull() ?: 0f
        val grasBase = etGrasas.text.toString().toFloatOrNull() ?: 0f
        val fibBase  = etFibra.text.toString().toFloatOrNull() ?: 0f
        val azuBase  = etAzucares.text.toString().toFloatOrNull() ?: 0f
        val unidad   = spinnerUnidad.selectedItem.toString()
        val categoria = spinnerCategoria.selectedItem.toString()

        if (nombre.isBlank()) {
            Toast.makeText(this, "⚠️ Escribe el nombre del alimento", Toast.LENGTH_SHORT).show()
            return
        }
        if (cantidad == null || cantidad <= 0) {
            Toast.makeText(this, "⚠️ Cantidad inválida", Toast.LENGTH_SHORT).show()
            return
        }
        if (calBase == null) {
            Toast.makeText(this, "⚠️ Ingresa las calorías", Toast.LENGTH_SHORT).show()
            return
        }

        val factor = when (unidad) {
            "g", "ml" -> cantidad / 100f
            else      -> cantidad
        }

        val alimento = RegistroAlimento(
            fecha        = viewModel.hoy,
            categoria    = categoria,
            nombre       = nombre,
            cantidad     = cantidad,
            unidad       = unidad,
            calorias     = (calBase * factor).toInt(),
            proteina     = protBase * factor,
            carbos       = carbBase * factor,
            grasas       = grasBase * factor,
            fibra        = fibBase * factor,
            azucares     = azuBase * factor,
            fueEscaneado = tvOcr.visibility == View.VISIBLE
        )

        viewModel.agregarAlimento(alimento)
        Toast.makeText(this, "✅ $nombre agregado (${cantidad}${unidad})", Toast.LENGTH_SHORT).show()
        finish()
    }
}

class SugerenciasAdapter(
    private val onClick: (AlimentoGuardado) -> Unit
) : RecyclerView.Adapter<SugerenciasAdapter.VH>() {

    private var lista = listOf<AlimentoGuardado>()

    fun update(nueva: List<AlimentoGuardado>) {
        lista = nueva
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sugerencia_alimento, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(lista[position])
    override fun getItemCount() = lista.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(a: AlimentoGuardado) {
            itemView.findViewById<TextView>(R.id.tvSugerenciaNombre).text = a.nombre
            itemView.findViewById<TextView>(R.id.tvSugerenciaCalorias).text = "${a.calorias} kcal"
            itemView.setOnClickListener { onClick(a) }
        }
    }
}