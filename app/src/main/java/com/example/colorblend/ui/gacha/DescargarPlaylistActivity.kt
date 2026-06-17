package com.example.colorblend.ui.gacha

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.Cancion
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class DescargarPlaylistActivity : AppCompatActivity() {

    private lateinit var etUrl: EditText
    private lateinit var btnObtenerInfo: Button
    private lateinit var btnDescargar: Button
    private lateinit var btnEscanearImagen: Button
    private lateinit var btnGaleriaImagen: Button
    private lateinit var tvTextoDetectado: TextView
    private lateinit var layoutInfo: LinearLayout
    private lateinit var layoutProgreso: LinearLayout
    private lateinit var tvNombre: TextView
    private lateinit var tvCantidad: TextView
    private lateinit var tvProgreso: TextView
    private lateinit var tvCancionActual: TextView
    private lateinit var tvLog: TextView
    private lateinit var progressBar: ProgressBar

    private var canciones: List<YoutubeCancion> = emptyList()
    private var nombrePlaylist: String = "Playlist"
    private var urlPendiente: String? = null
    private var descargandoSpotify = false

    private var descargaService: DescargaService? = null
    private var serviceConectado = false

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private lateinit var btnImportarExcel: Button
    private lateinit var btnDescargarServidor: Button

    private val seleccionarExcel = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { procesarExcel(it) }
    }

    private val tomarFoto = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { procesarImagenOCR(InputImage.fromBitmap(it, 0)) }
    }

    private val seleccionarDeGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val image = InputImage.fromFilePath(this, it)
                procesarImagenOCR(image)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as DescargaService.DescargaBinder
            descargaService = binder.getService()
            serviceConectado = true
            val url = urlPendiente
            if (url != null) {
                urlPendiente = null
                arrancarObtencionInfo(url)
            } else {
                reconectarAlServicio()
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceConectado = false
            descargaService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_descargar_playlist)

        etUrl                = findViewById(R.id.etUrlPlaylist)
        btnObtenerInfo       = findViewById(R.id.btnObtenerInfo)
        btnDescargar         = findViewById(R.id.btnDescargarPlaylist)
        btnEscanearImagen    = findViewById(R.id.btnEscanearImagen)
        btnGaleriaImagen     = findViewById(R.id.btnGaleriaImagen)
        tvTextoDetectado     = findViewById(R.id.tvTextoDetectado)
        layoutInfo           = findViewById(R.id.layoutInfoPlaylist)
        layoutProgreso       = findViewById(R.id.layoutProgreso)
        tvNombre             = findViewById(R.id.tvNombrePlaylist)
        tvCantidad           = findViewById(R.id.tvCantidadVideos)
        tvProgreso           = findViewById(R.id.tvProgresoDescarga)
        tvCancionActual      = findViewById(R.id.tvCancionActual)
        tvLog                = findViewById(R.id.tvLogDescarga)
        progressBar          = findViewById(R.id.progressDescarga)
        btnImportarExcel     = findViewById(R.id.btnImportarExcel)
        btnDescargarServidor = findViewById(R.id.btnDescargarServidor)

        findViewById<TextView>(R.id.btnVolverDescarga).setOnClickListener { finish() }

        btnEscanearImagen.setOnClickListener {
            SonidoHelper.reproducir(this)
            tomarFoto.launch(null)
        }

        btnGaleriaImagen.setOnClickListener {
            SonidoHelper.reproducir(this)
            seleccionarDeGaleria.launch("image/*")
        }

        btnImportarExcel.setOnClickListener {
            SonidoHelper.reproducir(this)
            seleccionarExcel.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        }

        btnDescargarServidor.setOnClickListener {
            SonidoHelper.reproducir(this)
            mostrarDialogServidor()
        }

        btnObtenerInfo.setOnClickListener {
            if (!YoutubeExtractor.servidorConfigurado(this)) {
                Toast.makeText(this, "Configura la URL y Key del servidor en Ajustes > API Keys", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, ApiKeysActivity::class.java))
                return@setOnClickListener
            }
            val url = etUrl.text.toString().trim()
            if (url.isBlank()) {
                Toast.makeText(this, "Pega una URL primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when {
                url.contains("spotify.com/track/") -> descargarCancionSpotify(url)
                (url.contains("youtube.com/watch") || url.contains("youtu.be/")) &&
                        (!url.contains("list=") || url.contains("list=RD")) -> descargarVideoYoutube(url)
                url.contains("youtube.com") || url.contains("youtu.be") -> obtenerInfoPlaylist(url)
                else -> Toast.makeText(this, "Pega una URL de YouTube o canción de Spotify", Toast.LENGTH_SHORT).show()
            }
        }

        bindService(Intent(this, DescargaService::class.java), connection, Context.BIND_AUTO_CREATE)

        // ── Manejar archivos Excel o URLs externos ───────────────────────
        manejarIntentExterno(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        manejarIntentExterno(intent)
    }

    private fun manejarIntentExterno(intent: Intent?) {
        intent?.let { 
            // 1. URLs desde Dashboard (si se usó el flujo de lista anterior)
            it.getStringArrayListExtra("urls_excel")?.let { urls ->
                if (urls.isNotEmpty()) iniciarDescargaSpotify(urls, "Spotify_Excel")
                return
            }

            // 2. Archivo Excel directo (.xlsx)
            val uri = it.data
            val mimeType = contentResolver.getType(uri ?: Uri.EMPTY)
            val path = uri?.path?.lowercase() ?: ""
            
            if (it.action == Intent.ACTION_VIEW && uri != null) {
                if (mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" || 
                    path.endsWith(".xlsx")) {
                    procesarExcel(uri)
                }
            }
        }
    }

    // ── Dialog de instrucciones del servidor ──────────────────────────────

    private fun mostrarDialogServidor() {
        val instrucciones = """
            📦 SERVIDOR DE MÚSICA — GUÍA RÁPIDA
            
            El servidor es un programa que corre en tu PC con Windows y permite que la app descargue música de YouTube.
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━
            📥 PASO 1 — Descargar los archivos
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Presiona "Descargar archivos" abajo.
            Se guardarán en tu carpeta Descargas:
            • servidor.py  → el servidor en Python
            • servidor.bat → el ejecutable para Windows
            
            Pásalos a tu PC por cable USB, WhatsApp o Google Drive.
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━
            💻 PASO 2 — Requisitos en PC
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Necesitas tener instalado:
            
            1. Python 3.12
               → python.org/downloads/release/python-3120/
               ⚠ Marca "Add Python to PATH" al instalar
            
            2. ngrok (para el link público)
               → ngrok.com/download
               → Crea cuenta gratis en ngrok.com
               → Copia tu Authtoken desde ngrok.com/authtokens
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━
            💻 PASO 2.5 — Dominio Estático (Opcional)
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Si tienes un dominio estático de ngrok, pégalo en el campo NGROK_DOMAIN. 
            Créalo gratis en dashboard.ngrok.com → Domains. 
            Con esto la URL nunca cambia y no tendrás que actualizarla en la app.
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━
            ▶ PASO 3 — Ejecutar el servidor
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━
            1. Pon servidor.py y servidor.bat en la misma carpeta
            2. Doble clic en servidor.bat
            3. La primera vez instala dependencias automáticamente
            4. Pega tu ngrok Authtoken cuando lo pida
            5. El programa mostrará una URL así:
               https://xxxx-xx-xx.ngrok-free.app
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━
            📱 PASO 4 — Configurar la app
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Ve a Ajustes → API Keys y completa:
            • URL del Servidor → la URL de ngrok
            • Key del Servidor → la key que pusiste
              (por defecto: MI_KEY_SECRETA)
            
            ⚠ El servidor debe estar encendido en la PC cada vez que quieras descargar música.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("📦 Descargar Servidor PC")
            .setMessage(instrucciones)
            .setNegativeButton("Cerrar", null)
            .setPositiveButton("⬇ Descargar archivos") { _, _ ->
                descargarArchivosServidor()
            }
            .setNeutralButton("⚙ Ir a API Keys") { _, _ ->
                startActivity(Intent(this, ApiKeysActivity::class.java))
            }
            .show()
    }

    private fun descargarArchivosServidor() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val descargas = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                descargas.mkdirs()

                // ── servidor.py ───────────────────────────────────────────
                val archivoPy = File(descargas, "servidor.py")
                archivoPy.writeText(SERVIDOR_PY)

                // ── servidor.bat ──────────────────────────────────────────
                val archivoBat = File(descargas, "servidor.bat")
                archivoBat.writeText(SERVIDOR_BAT)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DescargarPlaylistActivity,
                        "✅ Archivos guardados en Descargas:\nservidor.py y servidor.bat",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DescargarPlaylistActivity,
                        "✗ Error al guardar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ── OCR → extrae canciones → descarga una por una ─────────────────────

    private fun procesarImagenOCR(image: InputImage) {
        if (!YoutubeExtractor.servidorConfigurado(this)) {
            Toast.makeText(this, "Configura la URL y Key del servidor en Ajustes > API Keys", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, ApiKeysActivity::class.java))
            return
        }

        tvTextoDetectado.visibility = View.VISIBLE
        tvTextoDetectado.text = "🔍 Analizando imagen..."

        recognizer.process(image)
            .addOnSuccessListener { resultado ->
                val textoCompleto = resultado.text
                val canciones = extraerCanciones(textoCompleto)

                if (canciones.isEmpty()) {
                    tvTextoDetectado.text = "⚠ No se detectaron canciones. Texto: ${textoCompleto.take(100)}"
                    Toast.makeText(this, "No se detectaron canciones en la imagen", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                tvTextoDetectado.text = "✓ ${canciones.size} canciones detectadas"
                descargandoSpotify = true
                iniciarDescargaSpotify(canciones)
            }
            .addOnFailureListener {
                tvTextoDetectado.text = "✗ Error al analizar imagen"
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
            }
    }

    private fun extraerCanciones(texto: String): List<String> {
        val lineas = texto.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { !it.contains("spotify", ignoreCase = true) }
            .filter { !it.contains("Video musical", ignoreCase = true) }
            .filter { !it.contains("canciones", ignoreCase = true) }
            .filter { !it.contains("songs", ignoreCase = true) }
            .filter { !it.contains("playlist", ignoreCase = true) }
            .filter { !it.matches(Regex("\\d+")) }
            .filter { !it.matches(Regex("\\d+:\\d+")) }
            .filter { !it.matches(Regex("[A-Z]")) }
            .filter { it.length > 2 }

        val canciones = mutableListOf<String>()
        var i = 0

        while (i < lineas.size) {
            val titulo = lineas[i]
            val artista = if (i + 1 < lineas.size) {
                lineas[i + 1]
                    .replace(Regex("^[EeCcDd]\\s+"), "")
                    .replace(Regex(".*•\\s*"), "")
                    .trim()
            } else ""

            if (artista.isNotBlank() && artista != titulo) {
                canciones.add("$artista - $titulo")
                i += 2
            } else {
                canciones.add(titulo)
                i++
            }
        }

        return canciones
    }

    private fun iniciarDescargaSpotify(queries: List<String>, nombreCarpeta: String = "Spotify_Escaneado") {
        val carpeta = YoutubeExtractor.crearCarpetaPlaylist(this, nombreCarpeta)
        mostrarProgreso()
        layoutInfo.visibility = View.VISIBLE
        tvNombre.text = "📸 ${queries.size} canciones escaneadas"
        tvCantidad.text = "Descargando de YouTube una por una..."
        progressBar.isIndeterminate = false
        progressBar.max = queries.size
        progressBar.progress = 0
        btnEscanearImagen.isEnabled = false
        btnGaleriaImagen.isEnabled = false

        val servidorUrl = com.example.colorblend.data.local.ApiKeysManager.getServidorUrl(this).trimEnd('/')
        val apiKey = com.example.colorblend.data.local.ApiKeysManager.getServidorKey(this)

        lifecycleScope.launch {
            var exitosos = 0
            var fallidos = 0
            val log = StringBuilder()

            queries.forEachIndexed { index, query ->
                runOnUiThread {
                    tvProgreso.text = "${index + 1} / ${queries.size}"
                    tvCancionActual.text = "🔍 $query"
                    progressBar.progress = index + 1
                }

                try {
                    val resultado = withContext(Dispatchers.IO) {
                        // ... (código existente de red) ...
                        val body = """{"query":"$query"}"""
                        val conn = URL("$servidorUrl/buscar-cancion").openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.connectTimeout = 120000
                        conn.readTimeout = 120000
                        conn.doOutput = true
                        conn.outputStream.write(body.toByteArray())

                        if (conn.responseCode != 200) {
                            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Error ${conn.responseCode}"
                            return@withContext null to error
                        }
                        val response = conn.inputStream.bufferedReader().readText()
                        JSONObject(response) to null
                    }

                    val json = resultado.first
                    val error = resultado.second

                    if (json == null) {
                        fallidos++
                        log.appendLine("✗ $query — $error")
                        runOnUiThread { tvLog.text = log.toString() }
                        return@forEachIndexed
                    }

                    val titulo = json.optString("titulo", query)
                    val ruta = json.optString("ruta", "")

                    runOnUiThread { tvCancionActual.text = "⬇ Descargando: $titulo" }

                    val archivo = YoutubeExtractor.descargarAudio(
                        context = this@DescargarPlaylistActivity,
                        rutaColab = ruta,
                        nombreArchivo = titulo,
                        carpeta = carpeta,
                        onProgreso = {}
                    )

                    if (archivo != null) {
                        exitosos++
                        log.appendLine("✓ $titulo")
                        // Guardar en Room tras descarga exitosa
                        withContext(Dispatchers.IO) {
                            val nuevoCancion = Cancion(
                                uriLocal = archivo.absolutePath,
                                titulo = titulo,
                                artista = "", 
                                playlistId = nombreCarpeta,
                                uriSpotify = query
                            )
                            AppDatabase.getDatabase(this@DescargarPlaylistActivity).cancionDao().insertar(nuevoCancion)
                        }
                    } else {
                        fallidos++
                        log.appendLine("✗ $titulo — error al transferir")
                    }

                    runOnUiThread { tvLog.text = log.toString() }

                } catch (e: Exception) {
                    fallidos++
                    log.appendLine("✗ $query — ${e.message}")
                    runOnUiThread { tvLog.text = log.toString() }
                }
            }

            runOnUiThread {
                progressBar.progress = queries.size
                tvProgreso.text = "✓ $exitosos descargadas, $fallidos fallaron"
                tvCancionActual.text = "Guardado en: ${carpeta.absolutePath}"
                btnEscanearImagen.isEnabled = true
                btnGaleriaImagen.isEnabled = true
                descargandoSpotify = false
                sendBroadcast(Intent(MusicaService.ACTION_MUSICA_ACTUALIZADA))
                Toast.makeText(
                    this@DescargarPlaylistActivity,
                    "$exitosos canciones guardadas en $nombreCarpeta",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ── Flujo YouTube normal ───────────────────────────────────────────────

    private fun obtenerInfoPlaylist(url: String) {
        btnObtenerInfo.isEnabled = false
        btnObtenerInfo.text = "Conectando..."
        layoutInfo.visibility = View.GONE
        btnDescargar.visibility = View.GONE
        mostrarProgreso()
        tvLog.text = ""
        progressBar.isIndeterminate = true
        tvCancionActual.text = "Puedes salir, la descarga continúa en background"

        val intent = Intent(this, DescargaService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)

        if (serviceConectado) {
            arrancarObtencionInfo(url)
        } else {
            urlPendiente = url
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun arrancarObtencionInfo(url: String) {
        val svc = descargaService ?: return
        svc.onProgreso = { _, _, msg -> runOnUiThread { tvCancionActual.text = msg } }
        svc.onTerminado = { _, _, _, _ -> runOnUiThread { mostrarResultadoObtencion() } }
        svc.iniciarObtencionInfo(url, this)
    }

    private fun mostrarResultadoObtencion() {
        val svc = descargaService ?: return
        progressBar.isIndeterminate = false
        progressBar.progress = 100
        btnObtenerInfo.isEnabled = true
        btnObtenerInfo.text = "Obtener informacion"

        val cancionesObtenidas = svc._cancionesPendientes
        
        // --- VALIDACIÓN DE DUPLICADOS ---
        lifecycleScope.launch {
            val urlsDescargar = cancionesObtenidas.map { it.rutaColab } // Usamos rutaColab como ID único si no hay URL
            val (eliminadas, urlsLimpias) = svc.validarDuplicados(urlsDescargar, svc.nombrePlaylist)

            if (eliminadas.isNotEmpty()) {
                AlertDialog.Builder(this@DescargarPlaylistActivity)
                    .setTitle("⚠️ Duplicados detectados")
                    .setMessage("Se eliminaron ${eliminadas.size} canciones de la base de datos que ya existían:\n\n" + 
                                eliminadas.joinToString("\n• ", "• "))
                    .setPositiveButton("OK", null)
                    .show()
            }

            // Filtrar las canciones para descargar
            val cancionesParaDescargar = cancionesObtenidas.filter { it.rutaColab in urlsLimpias }
            svc._cancionesPendientes = cancionesParaDescargar
            
            // ... Actualizar UI con la lista filtrada ...
            actualizarUiConResultados(svc, cancionesParaDescargar)
        }
    }

    private fun actualizarUiConResultados(svc: DescargaService, cancionesFiltradas: List<YoutubeCancion>) {
        val fallidosYt = svc._fallidosPendientes
        canciones = cancionesFiltradas

        val logInicial = StringBuilder()
        if (fallidosYt.isNotEmpty()) {
            logInicial.appendLine("Videos no disponibles (${fallidosYt.size}):")
            fallidosYt.forEach { logInicial.appendLine("  X $it") }
        }
        tvLog.text = logInicial.toString()

        nombrePlaylist = svc.nombrePlaylist.ifBlank { "Playlist" }
        val carpeta = YoutubeExtractor.crearCarpetaPlaylist(this, nombrePlaylist)
        val yaExisten = cancionesFiltradas.count { cancion ->
            java.io.File(carpeta, "${YoutubeExtractor.limpiarNombre(cancion.titulo)}.m4a").exists()
        }
        val nuevas = cancionesFiltradas.size - yaExisten

        tvNombre.text = "${cancionesFiltradas.size} canciones listas"
        tvCantidad.text = buildString {
            append("Ya en telefono: $yaExisten  |  Nuevas: $nuevas")
            if (fallidosYt.isNotEmpty()) append("  |  No disponibles: ${fallidosYt.size}")
        }
        layoutInfo.visibility = View.VISIBLE
        tvProgreso.text = "Servidor listo"

        if (nuevas == 0) {
            tvCancionActual.text = "Todo ya esta descargado"
            btnDescargar.visibility = View.VISIBLE
            btnDescargar.text = "Forzar re-descarga"
            btnDescargar.setOnClickListener { iniciarTransferenciaEnServicio(forzar = true) }
        } else {
            tvCancionActual.text = "Hay $nuevas canciones nuevas"
            btnDescargar.visibility = View.VISIBLE
            btnDescargar.text = "Descargar $nuevas nuevas"
            btnDescargar.setOnClickListener { iniciarTransferenciaEnServicio(forzar = false) }
        }
    }

    private fun iniciarTransferenciaEnServicio(forzar: Boolean) {
        val svc = descargaService ?: return
        val carpeta = YoutubeExtractor.crearCarpetaPlaylist(this, nombrePlaylist)

        svc.onProgreso = { procesados, total, cancion ->
            runOnUiThread {
                tvProgreso.text = "$procesados / $total"
                tvCancionActual.text = cancion
                if (total > 0) progressBar.progress = (procesados * 100 / total)
            }
        }
        svc.onTerminado = { exitosos, omitidos, fallidos, carpetaPath ->
            runOnUiThread { mostrarResultadoFinal(exitosos, omitidos, fallidos, carpetaPath) }
        }

        svc.iniciarTransferencia(carpeta, nombrePlaylist, forzar, this)
        progressBar.isIndeterminate = false
        btnDescargar.isEnabled = false
        btnDescargar.text = "Descargando en background..."
        btnObtenerInfo.isEnabled = false
        tvCancionActual.text = "Puedes salir de la app, la descarga continua"
    }

    private fun reconectarAlServicio() {
        val svc = descargaService ?: return
        when (svc.estado) {
            DescargaService.EstadoDescarga.DESCARGANDO -> {
                mostrarProgreso()
                btnObtenerInfo.isEnabled = false
                btnDescargar.isEnabled = false
                if (svc.faseActual == DescargaService.FaseDescarga.OBTENIENDO_INFO) {
                    progressBar.isIndeterminate = true
                    tvNombre.text = "Descargando de YouTube..."
                    tvCancionActual.text = svc.mensajeFase
                    mostrarProgreso()
                    svc.onProgreso = { _, _, msg -> runOnUiThread { tvCancionActual.text = msg } }
                    svc.onTerminado = { _, _, _, _ -> runOnUiThread { mostrarResultadoObtencion() } }
                } else {
                    progressBar.isIndeterminate = false
                    mostrarProgreso()
                    layoutInfo.visibility = View.VISIBLE
                    tvNombre.text = "Transfiriendo ${svc.nombrePlaylist}..."
                    tvProgreso.text = "${svc.procesados} / ${svc.totalCanciones}"
                    progressBar.progress = if (svc.totalCanciones > 0)
                        (svc.procesados * 100 / svc.totalCanciones) else 0
                    tvCancionActual.text = svc.cancionActual
                    svc.onProgreso = { procesados, total, cancion ->
                        runOnUiThread {
                            tvProgreso.text = "$procesados / $total"
                            tvCancionActual.text = cancion
                            if (total > 0) progressBar.progress = (procesados * 100 / total)
                        }
                    }
                    svc.onTerminado = { exitosos, omitidos, fallidos, carpeta ->
                        runOnUiThread { mostrarResultadoFinal(exitosos, omitidos, fallidos, carpeta) }
                    }
                }
            }
            DescargaService.EstadoDescarga.TERMINADO -> {
                if (svc.fasePendienteTransferencia) {
                    canciones = svc._cancionesPendientes
                    nombrePlaylist = svc.nombrePlaylist.ifBlank { "Playlist" }
                    mostrarProgreso()
                    mostrarResultadoObtencion()
                } else {
                    mostrarResultadoFinal(
                        svc.exitosos, svc.omitidos, svc.fallidos,
                        YoutubeExtractor.crearCarpetaPlaylist(this, svc.nombrePlaylist).absolutePath
                    )
                }
            }
            else -> {}
        }
    }

    private fun mostrarProgreso() {
        layoutProgreso.visibility = View.VISIBLE
        tvLog.visibility = View.VISIBLE
    }

    private fun mostrarResultadoFinal(exitosos: Int, omitidos: Int, fallidos: Int, carpeta: String) {
        progressBar.isIndeterminate = false
        progressBar.progress = 100
        tvProgreso.text = buildString {
            append("Completado — $exitosos descargadas")
            if (omitidos > 0) append(", $omitidos ya existian")
            if (fallidos > 0) append(", $fallidos fallaron")
        }
        tvCancionActual.text = "Guardado en: $carpeta"
        btnDescargar.text = "Descarga completa"
        btnDescargar.isEnabled = false
        btnObtenerInfo.isEnabled = true
        Toast.makeText(this, "$exitosos canciones guardadas en $nombrePlaylist", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        descargaService?.onProgreso = null
        descargaService?.onTerminado = null
        if (serviceConectado) unbindService(connection)
        super.onDestroy()
    }

    // ── Importar Excel ─────────────────────────────────────────────────────

    private fun procesarExcel(uri: android.net.Uri) {
        if (!YoutubeExtractor.servidorConfigurado(this)) {
            Toast.makeText(this, "Configura la URL y Key del servidor en Ajustes > API Keys", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, ApiKeysActivity::class.java))
            return
        }

        tvTextoDetectado.visibility = View.VISIBLE
        tvTextoDetectado.text = "📊 Leyendo Excel..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("No se pudo abrir el archivo")

                val zipStream = java.util.zip.ZipInputStream(inputStream)
                var sharedStrings: List<String> = emptyList()
                var sheetXml: String? = null

                var entry = zipStream.nextEntry
                val xmlBytes = mutableMapOf<String, ByteArray>()
                while (entry != null) {
                    if (entry.name == "xl/sharedStrings.xml" || entry.name == "xl/worksheets/sheet1.xml") {
                        xmlBytes[entry.name] = zipStream.readBytes()
                    }
                    entry = zipStream.nextEntry
                }
                zipStream.close()
                inputStream.close()

                val ssXml = xmlBytes["xl/sharedStrings.xml"]
                if (ssXml != null) {
                    val ssStr = String(ssXml)
                    val tRegex = Regex("<t[^>]*>([^<]*)</t>")
                    sharedStrings = tRegex.findAll(ssStr).map { it.groupValues[1] }.toList()
                }

                sheetXml = xmlBytes["xl/worksheets/sheet1.xml"]
                    ?.let { String(it) }
                    ?: throw Exception("No se encontró la hoja de cálculo")

                val rowRegex = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
                val cellRegex = Regex("<c r=\"([A-Z]+)\\d+\"[^>]*t=\"s\"[^>]*><v>(\\d+)</v></c>")

                val rows = rowRegex.findAll(sheetXml).toList()
                if (rows.isEmpty()) throw Exception("No se encontraron filas")

                val headerRow = rows[0].groupValues[1]
                var colTitulo = ""
                var colArtista = ""

                cellRegex.findAll(headerRow).forEach { match ->
                    val col = match.groupValues[1]
                    val idx = match.groupValues[2].toIntOrNull() ?: return@forEach
                    val valor = sharedStrings.getOrNull(idx) ?: return@forEach
                    when {
                        valor.trim().equals("Track Name", ignoreCase = true) -> colTitulo = col
                        valor.trim().equals("Artist Name(s)", ignoreCase = true) -> colArtista = col
                    }
                }

                if (colTitulo.isEmpty() || colArtista.isEmpty()) {
                    runOnUiThread {
                        tvTextoDetectado.text = "✗ No se encontraron columnas 'Track Name' y 'Artist Name(s)'"
                    }
                    return@launch
                }

                val queries = mutableListOf<String>()
                for (rowIndex in 1 until rows.size) {
                    val rowContent = rows[rowIndex].groupValues[1]
                    var titulo = ""
                    var artista = ""

                    cellRegex.findAll(rowContent).forEach { match ->
                        val col = match.groupValues[1]
                        val idx = match.groupValues[2].toIntOrNull() ?: return@forEach
                        val valor = sharedStrings.getOrNull(idx) ?: return@forEach
                        when (col) {
                            colTitulo -> titulo = valor.trim()
                            colArtista -> artista = valor.trim()
                        }
                    }

                    if (titulo.isNotBlank() && artista.isNotBlank()) {
                        val primerArtista = artista.split(",").first().trim()
                        queries.add("$primerArtista - $titulo")
                    }
                }

                runOnUiThread {
                    if (queries.isEmpty()) {
                        tvTextoDetectado.text = "⚠ No se encontraron canciones en el Excel"
                        return@runOnUiThread
                    }
                    tvTextoDetectado.text = "✓ ${queries.size} canciones encontradas en Excel"
                    iniciarDescargaSpotify(queries, "Spotify_Excel")
                }

            } catch (e: Exception) {
                runOnUiThread {
                    tvTextoDetectado.text = "✗ Error leyendo Excel: ${e.message}"
                    Toast.makeText(this@DescargarPlaylistActivity,
                        "Error al leer el archivo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Descarga video YouTube individual ─────────────────────────────────

    private fun descargarVideoYoutube(url: String) {
        val servidorUrl = com.example.colorblend.data.local.ApiKeysManager.getServidorUrl(this).trimEnd('/')
        val apiKey = com.example.colorblend.data.local.ApiKeysManager.getServidorKey(this)

        mostrarProgreso()
        layoutInfo.visibility = View.VISIBLE
        tvNombre.text = "🎵 Video de YouTube"
        tvCantidad.text = "Descargando canción..."
        progressBar.isIndeterminate = true
        tvTextoDetectado.visibility = View.VISIBLE
        tvTextoDetectado.text = "⬇ Descargando video..."
        btnObtenerInfo.isEnabled = false

        lifecycleScope.launch {
            try {
                val resultado = withContext(Dispatchers.IO) {
                    val body = """{"query":"$url"}"""
                    val conn = URL("$servidorUrl/buscar-cancion").openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $apiKey")
                    conn.connectTimeout = 120000
                    conn.readTimeout = 120000
                    conn.doOutput = true
                    conn.outputStream.write(body.toByteArray())

                    if (conn.responseCode != 200) {
                        val error = conn.errorStream?.bufferedReader()?.readText() ?: "Error ${conn.responseCode}"
                        return@withContext null to error
                    }
                    val response = conn.inputStream.bufferedReader().readText()
                    JSONObject(response) to null
                }

                val json = resultado.first
                val error = resultado.second

                if (json == null) {
                    runOnUiThread {
                        tvTextoDetectado.text = "✗ Error: $error"
                        btnObtenerInfo.isEnabled = true
                        progressBar.isIndeterminate = false
                    }
                    return@launch
                }

                val titulo = json.optString("titulo", "cancion")
                val ruta = json.optString("ruta", "")

                runOnUiThread {
                    tvTextoDetectado.text = "⬇ Transfiriendo: $titulo"
                    progressBar.isIndeterminate = false
                }

                val carpeta = YoutubeExtractor.crearCarpetaPlaylist(this@DescargarPlaylistActivity, "YouTube_Canciones")
                val archivo = YoutubeExtractor.descargarAudio(
                    context = this@DescargarPlaylistActivity,
                    rutaColab = ruta,
                    nombreArchivo = titulo,
                    carpeta = carpeta,
                    onProgreso = {}
                )

                runOnUiThread {
                    progressBar.progress = 100
                    btnObtenerInfo.isEnabled = true
                    if (archivo != null) {
                        tvTextoDetectado.text = "✓ Descargada: $titulo"
                        tvProgreso.text = "✓ Completado"
                        tvCancionActual.text = "Guardado en: YouTube_Canciones"
                        sendBroadcast(Intent(MusicaService.ACTION_MUSICA_ACTUALIZADA))
                        Toast.makeText(this@DescargarPlaylistActivity, "✓ $titulo guardada", Toast.LENGTH_LONG).show()
                    } else {
                        tvTextoDetectado.text = "✗ Error al transferir el archivo"
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    tvTextoDetectado.text = "✗ Error: ${e.message}"
                    btnObtenerInfo.isEnabled = true
                    progressBar.isIndeterminate = false
                }
            }
        }
    }

    // ── Descarga canción individual de Spotify ────────────────────────────

    private fun descargarCancionSpotify(urlSpotify: String) {
        if (!YoutubeExtractor.servidorConfigurado(this)) return

        val servidorUrl = com.example.colorblend.data.local.ApiKeysManager.getServidorUrl(this).trimEnd('/')
        val apiKey = com.example.colorblend.data.local.ApiKeysManager.getServidorKey(this)

        mostrarProgreso()
        layoutInfo.visibility = View.VISIBLE
        tvNombre.text = "🎵 Canción de Spotify"
        tvCantidad.text = "Obteniendo información..."
        progressBar.isIndeterminate = true
        tvTextoDetectado.visibility = View.VISIBLE
        tvTextoDetectado.text = "🔍 Buscando canción en Spotify..."
        btnObtenerInfo.isEnabled = false

        lifecycleScope.launch {
            try {
                val query = withContext(Dispatchers.IO) {
                    val body = """{"url":"${urlSpotify.replace("\"", "")}"}"""
                    val conn = URL("$servidorUrl/info-cancion").openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $apiKey")
                    conn.connectTimeout = 30000
                    conn.readTimeout = 30000
                    conn.doOutput = true
                    conn.outputStream.write(body.toByteArray())

                    if (conn.responseCode != 200) {
                        val error = conn.errorStream?.bufferedReader()?.readText() ?: "Error ${conn.responseCode}"
                        return@withContext null to error
                    }
                    val response = conn.inputStream.bufferedReader().readText()
                    JSONObject(response).optString("query") to null
                }

                val queryStr = query.first
                val error = query.second

                if (queryStr.isNullOrBlank()) {
                    runOnUiThread {
                        tvTextoDetectado.text = "✗ No se pudo obtener info: $error"
                        btnObtenerInfo.isEnabled = true
                        progressBar.isIndeterminate = false
                    }
                    return@launch
                }

                runOnUiThread {
                    tvTextoDetectado.text = "✓ Encontrada: $queryStr"
                    progressBar.isIndeterminate = false
                    btnObtenerInfo.isEnabled = true
                    iniciarDescargaSpotify(listOf(queryStr), "Spotify_Canciones")
                }

            } catch (e: Exception) {
                runOnUiThread {
                    tvTextoDetectado.text = "✗ Error: ${e.message}"
                    btnObtenerInfo.isEnabled = true
                    progressBar.isIndeterminate = false
                    Toast.makeText(this@DescargarPlaylistActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Contenido de los archivos del servidor ────────────────────────────

    companion object {

        private val SERVIDOR_PY = """
import os, shutil, subprocess, json as json_mod
from fastapi import FastAPI, HTTPException, Header
from fastapi.responses import FileResponse
from pydantic import BaseModel
import uvicorn, yt_dlp

app = FastAPI()
API_KEY = "MI_KEY_SECRETA"

class DescargaRequest(BaseModel):
    url: str

class BuscarCancionRequest(BaseModel):
    query: str

def es_spotify(url: str) -> bool:
    return "spotify.com" in url

@app.post("/descargar")
def descargar(req: DescargaRequest, authorization: str = Header(...)):
    token = authorization.replace("Bearer ", "")
    if token != API_KEY:
        raise HTTPException(status_code=401, detail="API key invalida")

    if os.path.exists("musica"):
        shutil.rmtree("musica")
    os.makedirs("musica", exist_ok=True)

    fallidos = []

    if es_spotify(req.url):
        try:
            resultado = subprocess.run(
                ["py", "-3.12", "-m", "spotdl", req.url, "--output", "musica/{list-name}/{title}"],
                capture_output=True, text=True, timeout=600
            )
            if resultado.returncode != 0:
                raise HTTPException(status_code=500, detail=resultado.stderr or "Error spotdl")
        except subprocess.TimeoutExpired:
            raise HTTPException(status_code=500, detail="Timeout descargando de Spotify")
        except FileNotFoundError:
            raise HTTPException(status_code=500, detail="spotdl no instalado")

        resultados = []
        nombre_playlist = ""
        for root, dirs, files in os.walk("musica"):
            for f in files:
                if f.endswith(".mp3") or f.endswith(".m4a") or f.endswith(".opus"):
                    ruta = os.path.join(root, f)
                    titulo = os.path.splitext(f)[0]
                    resultados.append({"titulo": titulo, "ruta": ruta})
                    if not nombre_playlist:
                        partes = os.path.relpath(root, "musica").split(os.sep)
                        if partes:
                            nombre_playlist = partes[0]

        return {"playlist": nombre_playlist, "canciones": resultados, "total": len(resultados), "fallidos": fallidos}

    else:
        ydl_opts = {
            "format": "bestaudio/best",
            "outtmpl": "musica/%(playlist_title,title)s/%(title)s.%(ext)s",
            "quiet": True, "no_warnings": True, "ignoreerrors": True,
            "sleep_interval": 2, "max_sleep_interval": 4,
        }
        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(req.url, download=True)
                if info is None:
                    raise HTTPException(status_code=500, detail="No se pudo obtener info")
                entries = info.get("entries", [info])
                for entry in entries:
                    if entry is None:
                        fallidos.append("(video no disponible)")
        except HTTPException:
            raise
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))

        resultados = []
        for root, dirs, files in os.walk("musica"):
            for f in files:
                if f.endswith(".m4a") or f.endswith(".webm") or f.endswith(".mp3"):
                    ruta = os.path.join(root, f)
                    titulo = os.path.splitext(f)[0]
                    resultados.append({"titulo": titulo, "ruta": ruta})

        return {"playlist": info.get("title", ""), "canciones": resultados, "total": len(resultados), "fallidos": fallidos}


@app.post("/buscar-cancion")
def buscar_cancion(req: BuscarCancionRequest, authorization: str = Header(...)):
    token = authorization.replace("Bearer ", "")
    if token != API_KEY:
        raise HTTPException(status_code=401, detail="API key invalida")

    carpeta = "musica_spotify"
    os.makedirs(carpeta, exist_ok=True)

    for f in os.listdir(carpeta):
        try:
            os.remove(os.path.join(carpeta, f))
        except:
            pass

    query_mejorada = f"{req.query} official audio"

    ydl_opts = {
        "format": "bestaudio/best",
        "outtmpl": f"{carpeta}/%(title)s.%(ext)s",
        "quiet": True,
        "no_warnings": True,
        "ignoreerrors": False,
        "default_search": "ytsearch1",
        "noplaylist": True,
        "ffmpeg_location": r"C:\Users\elang\.spotdl\ffmpeg.exe",
        "postprocessors": [{
            "key": "FFmpegExtractAudio",
            "preferredcodec": "mp3",
            "preferredquality": "192",
        }],
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(query_mejorada, download=True)
            if info is None:
                raise HTTPException(status_code=404, detail="No se encontro la cancion")

            if "entries" in info:
                entries = [e for e in info["entries"] if e is not None]
                if not entries:
                    raise HTTPException(status_code=404, detail="No se encontraron resultados")
                info = entries[0]

            archivo_encontrado = None
            for f in os.listdir(carpeta):
                ruta_f = os.path.join(carpeta, f)
                if os.path.isfile(ruta_f):
                    archivo_encontrado = ruta_f
                    titulo = os.path.splitext(f)[0]
                    break

            if not archivo_encontrado:
                raise HTTPException(status_code=500, detail="Archivo no encontrado tras descarga")

            return {"titulo": titulo, "ruta": archivo_encontrado}

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/info-cancion")
def info_cancion(req: DescargaRequest, authorization: str = Header(...)):
    token = authorization.replace("Bearer ", "")
    if token != API_KEY:
        raise HTTPException(status_code=401, detail="API key invalida")
    try:
        resultado = subprocess.run(
            ["py", "-3.12", "-m", "spotdl", "meta", req.url],
            capture_output=True, text=True, timeout=30
        )
        titulo = ""
        artista = ""
        for linea in resultado.stdout.splitlines():
            linea = linea.strip()
            if linea.lower().startswith("title:"):
                titulo = linea.split(":", 1)[1].strip()
            elif linea.lower().startswith("artist:"):
                artista = linea.split(":", 1)[1].strip()

        if titulo and artista:
            return {"query": f"{artista} - {titulo}"}

        resultado2 = subprocess.run(
            ["py", "-3.12", "-m", "spotdl", "save", req.url, "--save-file", "temp_info.spotdl"],
            capture_output=True, text=True, timeout=30
        )
        if os.path.exists("temp_info.spotdl"):
            with open("temp_info.spotdl", "r", encoding="utf-8") as f:
                data = json_mod.load(f)
            os.remove("temp_info.spotdl")
            if isinstance(data, list) and len(data) > 0:
                cancion = data[0]
                titulo = cancion.get("name", "")
                artistas = cancion.get("artists", [])
                artista = artistas[0] if artistas else ""
                if titulo and artista:
                    return {"query": f"{artista} - {titulo}"}

        raise HTTPException(status_code=404, detail="No se pudo obtener info de la cancion")
    except HTTPException:
        raise
    except subprocess.TimeoutExpired:
        raise HTTPException(status_code=500, detail="Timeout")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/archivo")
def obtener_archivo(ruta: str, authorization: str = Header(...)):
    token = authorization.replace("Bearer ", "")
    if token != API_KEY:
        raise HTTPException(status_code=401, detail="API key invalida")
    if not os.path.exists(ruta):
        raise HTTPException(status_code=404, detail="Archivo no encontrado")
    return FileResponse(ruta, media_type="audio/mp4")


@app.get("/health")
def health():
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
        """.trimIndent()

        private val SERVIDOR_BAT = """
@echo off
title Servidor ColorBlend
color 0A
echo ================================
echo   Servidor de Musica ColorBlend
echo ================================
echo.
:: Verificar Python
py -3.12 --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python 3.12 no encontrado.
    echo Descarga Python 3.12 desde https://python.org/downloads/release/python-3120/
    echo Asegurate de marcar "Add Python to PATH" durante la instalacion
    pause
    exit
)
:: Instalar dependencias si no estan
echo Verificando dependencias...
py -3.12 -m pip show fastapi >nul 2>&1
if errorlevel 1 py -3.12 -m pip install fastapi uvicorn yt-dlp spotdl -q
py -3.12 -m pip show spotdl >nul 2>&1
if errorlevel 1 py -3.12 -m pip install spotdl -q
echo Dependencias OK
echo.
echo ================================
echo   Configuracion del servidor
echo ================================
echo.
echo Tu API Key actual es: MI_KEY_SECRETA
echo.
set /p CAMBIAR_KEY="Quieres cambiar la API Key? (s/n): "
if /i "%CAMBIAR_KEY%"=="s" (
    set /p NUEVA_KEY="Escribe tu nueva API Key: "
    powershell -Command "(Get-Content servidor.py) -replace 'MI_KEY_SECRETA', '%NUEVA_KEY%' | Set-Content servidor.py"
    echo Key actualizada a: %NUEVA_KEY%
)
echo.
echo ================================
echo   Configurando ngrok
echo ================================
echo.
set /p NGROK_TOKEN="Pega tu ngrok authtoken (Enter para saltar): "
if not "%NGROK_TOKEN%"=="" (
    ngrok config add-authtoken %NGROK_TOKEN% >nul 2>&1
)
echo.
echo ================================
echo   Iniciando servidor...
echo ================================
echo.
start /B py -3.12 servidor.py
timeout /t 3 /nobreak >nul
echo Servidor corriendo en puerto 8000
echo.
echo Iniciando ngrok...
start /B ngrok http 8000
timeout /t 3 /nobreak >nul
echo.
echo Obteniendo URL publica...
for /f "tokens=*" %%a in ('curl -s http://localhost:4040/api/tunnels ^| python -c "import sys,json; t=json.load(sys.stdin)['tunnels']; print(t[0]['public_url']) if t else print('ERROR')"') do set NGROK_URL=%%a
echo.
echo ================================
echo   TU URL PUBLICA:
echo   %NGROK_URL%
echo ================================
echo.
echo Copia esta URL en la app en:
echo Ajustes ^> API Keys ^> URL del Servidor
echo.
echo Presiona cualquier tecla para detener el servidor...
pause >nul
taskkill /f /im python.exe >nul 2>&1
taskkill /f /im ngrok.exe >nul 2>&1
echo Servidor detenido.
        """.trimIndent()
    }
}