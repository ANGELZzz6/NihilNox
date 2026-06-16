package com.example.colorblend.ui.gacha.metas

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.colorblend.R
import com.example.colorblend.domain.model.Meta
import com.example.colorblend.domain.model.MetaImagenDia
import com.example.colorblend.domain.model.TipoMeta
import com.example.colorblend.ui.gacha.SonidoHelper
import com.example.colorblend.ui.gacha.metas.viewmodels.MetaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MetaDetalleActivity : AppCompatActivity() {

    private val metaViewModel: MetaViewModel by viewModels()

    private var metaId           = 0
    private var metaTitulo       = ""
    private var metaTipo         = ""
    private var metaDescripcion  = ""
    private var metaObjetivo     = 0
    private var metaProgreso     = 0
    private var metaRacha        = 0
    private var diasSemanaStr    = ""
    private var horaRecordatorio: String? = null

    private lateinit var layoutGrupos: LinearLayout
    private lateinit var tvSinImagenes: TextView
    private lateinit var tvContadorFotos: TextView
    private lateinit var btnRecordatorio: Button

    private var uriCamaraTemp: Uri? = null

    // ── Launchers ─────────────────────────────────────────────────────────────

    private val launcherGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { copiarYGuardar(it) } }

    private val launcherCamara = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito: Boolean ->
        if (exito) uriCamaraTemp?.let { guardarDesdeUri(it) }
    }

    private val launcherPermisoCamara = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido: Boolean ->
        if (concedido) {
            lanzarCamara()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Permiso de cámara")
                .setMessage("Sin permiso de cámara no se pueden tomar fotos. Puedes usar la galería.")
                .setPositiveButton("Usar galería") { _, _ -> launcherGaleria.launch("image/*") }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meta_detalle)

        metaId           = intent.getIntExtra("metaId", 0)
        metaTitulo       = intent.getStringExtra("metaTitulo") ?: ""
        metaTipo         = intent.getStringExtra("metaTipo") ?: ""
        metaDescripcion  = intent.getStringExtra("metaDescripcion") ?: ""
        metaObjetivo     = intent.getIntExtra("metaObjetivo", 0)
        metaProgreso     = intent.getIntExtra("metaProgreso", 0)
        metaRacha        = intent.getIntExtra("metaRacha", 0)
        diasSemanaStr    = intent.getStringExtra("diasSemana") ?: ""
        horaRecordatorio = intent.getStringExtra("horaRecordatorio")
        val abrirCamara  = intent.getBooleanExtra("abrirCamara", false)

        val tvTitulo      = findViewById<TextView>(R.id.tvDetalleTitulo)
        val tvBadge       = findViewById<TextView>(R.id.tvDetalleBadge)
        val tvDescripcion = findViewById<TextView>(R.id.tvDetalleDescripcion)
        val tvDias        = findViewById<TextView>(R.id.tvDetalleDias)
        val tvProgreso    = findViewById<TextView>(R.id.tvDetalleProgreso)
        val pbProgreso    = findViewById<ProgressBar>(R.id.pbDetalleProgreso)
        val layoutRacha   = findViewById<View>(R.id.layoutDetalleRacha)
        val tvRacha       = findViewById<TextView>(R.id.tvDetalleRacha)
        val tvMejorRacha  = findViewById<TextView>(R.id.tvDetalleMejorRacha)
        val btnCamara     = findViewById<Button>(R.id.btnAgregarCamara)
        val btnGaleria    = findViewById<Button>(R.id.btnAgregarGaleria)
        val btnValidarIA  = findViewById<Button>(R.id.btnDetalleValidarIA)
        btnRecordatorio   = findViewById(R.id.btnRecordatorio)
        layoutGrupos      = findViewById(R.id.layoutGruposFechas)
        tvSinImagenes     = findViewById(R.id.tvSinImagenes)
        tvContadorFotos   = findViewById(R.id.tvContadorFotos)

        tvTitulo.text = metaTitulo

        if (metaTipo == TipoMeta.DIARIA.name) {
            tvBadge.text = "DIARIA"
            layoutRacha.visibility = View.VISIBLE
            tvRacha.text      = "🔥 Racha: $metaRacha días"
            tvMejorRacha.text = "🏆 Mejor: $metaRacha días"
        } else {
            tvBadge.text = "ACUMULATIVA"
            layoutRacha.visibility = View.GONE
        }

        if (metaDescripcion.isNotEmpty()) {
            tvDescripcion.text = metaDescripcion
            tvDescripcion.visibility = View.VISIBLE
        }

        if (diasSemanaStr.isNotEmpty()) {
            val metaTemp = Meta(titulo = "", descripcion = null,
                tipo = TipoMeta.DIARIA, objetivo = 0, diasSemana = diasSemanaStr)
            tvDias.text = "📅 ${metaTemp.diasSemanaTexto()}"
            tvDias.visibility = View.VISIBLE
        }

        val pct = if (metaObjetivo > 0)
            (metaProgreso * 100 / metaObjetivo).coerceIn(0, 100) else 0
        tvProgreso.text = "$metaProgreso / $metaObjetivo  ($pct%)"
        pbProgreso.max = 100
        pbProgreso.progress = pct

        actualizarBotonRecordatorio()

        btnRecordatorio.setOnClickListener {
            SonidoHelper.reproducir(this)
            mostrarDialogRecordatorio()
        }

        btnCamara.setOnClickListener {
            SonidoHelper.reproducir(this)
            abrirCamara()
        }
        btnGaleria.setOnClickListener {
            SonidoHelper.reproducir(this)
            launcherGaleria.launch("image/*")
        }

        btnValidarIA.visibility = View.VISIBLE
        btnValidarIA.setOnClickListener {
            SonidoHelper.reproducir(this)
            abrirValidacionIA()
        }

        lifecycleScope.launch {
            metaViewModel.getImagenesPorMeta(metaId).collect { imagenes ->
                renderizarGruposFechas(imagenes)
            }
        }

        if (abrirCamara) abrirCamara()
    }

    // ── Recordatorio ──────────────────────────────────────────────────────────

    private fun actualizarBotonRecordatorio() {
        if (horaRecordatorio != null) {
            btnRecordatorio.text = "⏰ Recordatorio: $horaRecordatorio  (toca para cambiar)"
            btnRecordatorio.setTextColor(android.graphics.Color.parseColor("#FFD700"))
        } else {
            btnRecordatorio.text = "🔔 Agregar recordatorio"
            btnRecordatorio.setTextColor(android.graphics.Color.parseColor("#AAAACC"))
        }
    }

    private fun mostrarDialogRecordatorio() {
        val opciones = if (horaRecordatorio != null) {
            arrayOf("Cambiar hora", "Cancelar recordatorio")
        } else {
            arrayOf("Seleccionar hora")
        }

        AlertDialog.Builder(this)
            .setTitle("Recordatorio")
            .setItems(opciones) { _, which ->
                when {
                    opciones[which] == "Cancelar recordatorio" -> cancelarRecordatorio()
                    else -> abrirTimePicker()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun abrirTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hora, minuto ->
                horaRecordatorio = String.format("%02d:%02d", hora, minuto)
                guardarRecordatorio()
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun guardarRecordatorio() {
        lifecycleScope.launch {
            val meta = withContext(Dispatchers.IO) {
                com.example.colorblend.data.local.AppDatabase
                    .getDatabase(applicationContext).metaDao().getMetaById(metaId)
            } ?: return@launch

            metaViewModel.actualizarRecordatorio(meta, horaRecordatorio)
            MetaRecordatorioScheduler.programar(applicationContext, meta.copy(horaRecordatorio = horaRecordatorio))
            actualizarBotonRecordatorio()
            Toast.makeText(this@MetaDetalleActivity,
                "⏰ Recordatorio programado para las $horaRecordatorio",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelarRecordatorio() {
        lifecycleScope.launch {
            val meta = withContext(Dispatchers.IO) {
                com.example.colorblend.data.local.AppDatabase
                    .getDatabase(applicationContext).metaDao().getMetaById(metaId)
            } ?: return@launch

            horaRecordatorio = null
            metaViewModel.actualizarRecordatorio(meta, null)
            MetaRecordatorioScheduler.cancelar(applicationContext, metaId)
            actualizarBotonRecordatorio()
            Toast.makeText(this@MetaDetalleActivity, "Recordatorio cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Permiso + cámara ──────────────────────────────────────────────────────

    private fun abrirCamara() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> lanzarCamara()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(this)
                    .setTitle("Permiso de cámara necesario")
                    .setMessage("La app necesita acceso a la cámara para tomar fotos de tu progreso.")
                    .setPositiveButton("Conceder") { _, _ ->
                        launcherPermisoCamara.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            else -> launcherPermisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    private fun lanzarCamara() {
        try {
            val dir     = File(filesDir, "metas_fotos").also { it.mkdirs() }
            val archivo = File(dir, "cam_${metaId}_${System.currentTimeMillis()}.jpg")
            val uri     = FileProvider.getUriForFile(this, "${packageName}.provider", archivo)
            uriCamaraTemp = uri
            launcherCamara.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error al abrir la cámara: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun guardarDesdeUri(uri: Uri) {
        val dir      = File(filesDir, "metas_fotos")
        val archivos = dir.listFiles()?.sortedByDescending { it.lastModified() }
        val ultimo   = archivos?.firstOrNull { it.name.startsWith("cam_${metaId}_") }
        if (ultimo != null && ultimo.exists()) {
            metaViewModel.agregarImagen(metaId, ultimo.absolutePath)
            Toast.makeText(this, "📸 Foto guardada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copiarYGuardar(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val dir     = File(filesDir, "metas_fotos").also { it.mkdirs() }
            val archivo = File(dir, "meta_${metaId}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(archivo).use { out -> inputStream.copyTo(out) }
            inputStream.close()
            metaViewModel.agregarImagen(metaId, archivo.absolutePath)
            Toast.makeText(this, "📸 Foto guardada", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error al guardar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Renderizar fotos ──────────────────────────────────────────────────────

    private fun renderizarGruposFechas(imagenes: List<MetaImagenDia>) {
        layoutGrupos.removeAllViews()
        if (imagenes.isEmpty()) {
            tvSinImagenes.visibility = View.VISIBLE
            tvContadorFotos.text = ""
            return
        }
        tvSinImagenes.visibility = View.GONE
        tvContadorFotos.text = "${imagenes.size} foto${if (imagenes.size != 1) "s" else ""}"

        val grupos   = imagenes.groupBy { it.fecha }.toSortedMap(reverseOrder())
        val fmtFecha = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES"))

        grupos.forEach { (fechaMs, fotos) ->
            val tvFechaGrupo = TextView(this).apply {
                text = fmtFecha.format(Date(fechaMs)).replaceFirstChar { it.uppercase() }
                setTextColor(android.graphics.Color.parseColor("#8888AA"))
                textSize = 11f
                letterSpacing = 0.05f
                setPadding(0, 12, 0, 8)
            }
            layoutGrupos.addView(tvFechaGrupo)

            val scroll = HorizontalScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, 4)
            }

            fotos.forEach { imagen ->
                val itemView  = LayoutInflater.from(this).inflate(R.layout.item_imagen_dia, fila, false)
                val imgView   = itemView.findViewById<ImageView>(R.id.imgDia)
                val tvHora    = itemView.findViewById<TextView>(R.id.tvFechaImagen)
                val btnBorrar = itemView.findViewById<TextView>(R.id.btnBorrarImagen)

                val file = File(imagen.rutaImagen)
                if (file.exists()) {
                    imgView.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                    tvHora.text = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(file.lastModified()))
                }
                imgView.setOnClickListener {
                    try {
                        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
                        startActivity(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "image/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    } catch (e: Exception) { }
                }
                btnBorrar.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("Eliminar foto")
                        .setMessage("¿Borrar esta foto del registro?")
                        .setPositiveButton("Borrar") { _, _ ->
                            metaViewModel.eliminarImagen(imagen)
                            file.delete()
                        }
                        .setNegativeButton("Cancelar", null).show()
                }
                fila.addView(itemView)
            }

            scroll.addView(fila)
            layoutGrupos.addView(scroll)
            layoutGrupos.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).also { it.topMargin = 8; it.bottomMargin = 4 }
                setBackgroundColor(android.graphics.Color.parseColor("#2A2A4A"))
            })
        }
    }

    private fun abrirValidacionIA() {
        startActivity(Intent(this, ValidarMetaActivity::class.java).apply {
            putExtra("metaId",          metaId)
            putExtra("metaTitulo",      metaTitulo)
            putExtra("metaDescripcion", metaDescripcion)
            putExtra("metaTipo",        metaTipo)
            putExtra("metaObjetivo",    metaObjetivo)
            putExtra("metaProgreso",    metaProgreso)
            putExtra("metaRacha",       metaRacha)
            putExtra("maxPreguntas",    if (metaTipo == TipoMeta.DIARIA.name) 5 else 2)
        })
    }
}