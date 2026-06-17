package com.example.colorblend.ui.gacha

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class ItemLista {
    data class Header(val titulo: String) : ItemLista()
    data class Cancion(val uri: String, val indiceGlobal: Int) : ItemLista()
}

class ReproductorActivity : AppCompatActivity() {

    private var musicaService: MusicaService? = null
    private var serviceConectado = false
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var cancionesAdapter: CancionesAdapter
    private lateinit var btnShuffle: TextView
    private lateinit var btnTemporizador: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnReproducirTodo: android.widget.Button
    private lateinit var btnCrearPlaylist: android.widget.Button
    private lateinit var barraSeleccion: LinearLayout
    private lateinit var tvContadorSeleccion: TextView
    private lateinit var loadingOverlay: View

    private val musicaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MusicaService.ACTION_MUSICA_ACTUALIZADA) {
                cargarCancionesDescargadas()
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicaService.MusicaBinder
            musicaService = binder.getService()
            serviceConectado = true
            
            musicaService?.onCancionCambiada = { nombre, playing ->
                runOnUiThread { actualizarUI(nombre, playing) }
            }
            musicaService?.onShuffleChanged = { activo ->
                runOnUiThread { actualizarBotonShuffle(activo) }
            }
            musicaService?.onLoadingChanged = { isLoading ->
                runOnUiThread { loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE }
            }

            val cancionesServicio = musicaService?.getCancionesCompletas() ?: emptyList()
            if (cancionesServicio.isNotEmpty()) {
                actualizarLista(cancionesServicio)
            } else {
                cargarCancionesDescargadas()
            }
            musicaService?.let { actualizarUI(it.nombreCancion(), it.isPlaying()) }
            actualizarBotonShuffle(musicaService?.isShuffle() ?: false)
            iniciarActualizacionProgreso()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceConectado = false
        }
    }

    private val seleccionarCanciones = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            musicaService?.agregarCancion(uri.toString())
        }
        actualizarLista(musicaService?.getCanciones() ?: emptyList())
        val lista = musicaService?.getCanciones() ?: emptyList()
        if (lista.isNotEmpty() && musicaService?.isPlaying() == false)
            musicaService?.setCanciones(lista, 0)
    }

    private val seleccionarCarpeta = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            cargarCancionesDesdeCarpeta(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reproductor)
        FullScreenHelper.enable(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        btnShuffle           = findViewById(R.id.btnShuffle)
        btnTemporizador      = findViewById(R.id.btnTemporizador)
        tvTimer              = findViewById(R.id.tvTimer)
        btnReproducirTodo    = findViewById(R.id.btnReproducirTodo)
        btnCrearPlaylist     = findViewById(R.id.btnCrearPlaylist)
        barraSeleccion       = findViewById(R.id.barraSeleccion)
        tvContadorSeleccion  = findViewById(R.id.tvContadorSeleccion)
        loadingOverlay       = findViewById(R.id.loadingMusica)

        val etBuscar = findViewById<EditText>(R.id.etBuscarCancion)
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                cancionesAdapter.filtrar(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnReproducirTodo.setOnClickListener {
            SonidoHelper.reproducir(this)
            val todas = musicaService?.getCancionesCompletas() ?: emptyList()
            if (todas.isEmpty()) {
                Toast.makeText(this, "No hay canciones", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            musicaService?.setCanciones(todas, 0)
            Toast.makeText(this, "▶ Reproduciendo todo (${todas.size} canciones)", Toast.LENGTH_SHORT).show()
        }

        // ── Crear playlist ────────────────────────────────────────────────
        btnCrearPlaylist.setOnClickListener {
            SonidoHelper.reproducir(this)
            mostrarDialogoCrearPlaylist()
        }

        // ── Temporizador ──────────────────────────────────────────────────
        btnTemporizador.setOnClickListener {
            SonidoHelper.reproducir(this)
            if (musicaService?.timerActivo() == true) {
                musicaService?.cancelarTemporizador()
                tvTimer.visibility = View.GONE
                btnTemporizador.setTextColor(android.graphics.Color.parseColor("#AAAACC"))
                Toast.makeText(this, "Temporizador cancelado", Toast.LENGTH_SHORT).show()
            } else {
                val opciones = arrayOf("15 minutos", "30 minutos", "45 minutos", "60 minutos", "Personalizado")
                android.app.AlertDialog.Builder(this)
                    .setTitle("⏱ Temporizador")
                    .setItems(opciones) { _, which ->
                        val minutos = when (which) {
                            0 -> 15; 1 -> 30; 2 -> 45; 3 -> 60
                            else -> {
                                val input = android.widget.EditText(this).apply {
                                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                                    hint = "Minutos"
                                }
                                android.app.AlertDialog.Builder(this)
                                    .setTitle("¿Cuántos minutos?")
                                    .setView(input)
                                    .setPositiveButton("OK") { _, _ ->
                                        val mins = input.text.toString().toIntOrNull() ?: 30
                                        arrancarTimer(mins)
                                    }
                                    .setNegativeButton("Cancelar", null)
                                    .show()
                                -1
                            }
                        }
                        if (minutos > 0) arrancarTimer(minutos)
                    }
                    .show()
            }
        }

        // ── RecyclerView + Adapter ────────────────────────────────────────
        val recycler = findViewById<RecyclerView>(R.id.listaCancionesRecycler)
        cancionesAdapter = CancionesAdapter(
            context = this,
            items = emptyList(),
            getNombre = { uri -> obtenerNombreCancion(uri) },
            onClick = { indiceGlobal ->
                SonidoHelper.reproducir(this)
                val lista = musicaService?.getCancionesCompletas() ?: return@CancionesAdapter
                musicaService?.setCanciones(lista, indiceGlobal)
            },
            onHeaderClick = { nombreSeccion, cancionesSeccion ->
                SonidoHelper.reproducir(this)
                if (cancionesSeccion.isNotEmpty()) {
                    musicaService?.setCancionesSeccion(cancionesSeccion, 0)
                    Toast.makeText(this, "▶ $nombreSeccion", Toast.LENGTH_SHORT).show()
                }
            },
            onEliminar = { uri, nombre ->
                confirmarEliminar(uri, nombre)
            },
            onSeleccionCambiada = { cantidad ->
                tvContadorSeleccion.text = "$cantidad seleccionadas"
            },
            onModoSeleccionActivado = {
                barraSeleccion.visibility = View.VISIBLE
                tvContadorSeleccion.text = "1 seleccionadas"
            }
        )
        recycler.adapter = cancionesAdapter
        recycler.layoutManager = LinearLayoutManager(this)

        // ── Barra de selección múltiple ───────────────────────────────────
        findViewById<android.widget.Button>(R.id.btnAgregarSeleccionadas).setOnClickListener {
            SonidoHelper.reproducir(this)
            mostrarAccionesSeleccion()
        }
        findViewById<android.widget.Button>(R.id.btnCancelarSeleccion).setOnClickListener {
            SonidoHelper.reproducir(this)
            cancionesAdapter.desactivarModoSeleccion()
            barraSeleccion.visibility = View.GONE
        }

        // ── Servicio ──────────────────────────────────────────────────────
        val intent = Intent(this, MusicaService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // ── Controles reproductor ─────────────────────────────────────────
        btnShuffle.setOnClickListener {
            SonidoHelper.reproducir(this)
            musicaService?.toggleShuffle()
        }
        findViewById<TextView>(R.id.btnPlayPause).setOnClickListener {
            SonidoHelper.reproducir(this)
            musicaService?.togglePlayPause()
        }
        findViewById<TextView>(R.id.btnSiguiente).setOnClickListener {
            SonidoHelper.reproducir(this)
            musicaService?.siguiente()
        }
        findViewById<TextView>(R.id.btnAnterior).setOnClickListener {
            SonidoHelper.reproducir(this)
            musicaService?.anterior()
        }
        findViewById<SeekBar>(R.id.seekBarMusica).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) musicaService?.seekTo(progress)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            }
        )

        // ── Agregar música ────────────────────────────────────────────────
        findViewById<TextView>(R.id.btnAgregarMusica).setOnClickListener {
            SonidoHelper.reproducir(this)
            android.app.AlertDialog.Builder(this)
                .setTitle("Agregar música")
                .setItems(arrayOf("📁 Seleccionar carpeta", "🎵 Seleccionar canciones")) { _, which ->
                    when (which) {
                        0 -> seleccionarCarpeta.launch(null)
                        1 -> seleccionarCanciones.launch(arrayOf("audio/*"))
                    }
                }
                .show()
        }

        findViewById<TextView>(R.id.btnDescargarYoutube).setOnClickListener {
            SonidoHelper.reproducir(this)
            startActivity(Intent(this, DescargarPlaylistActivity::class.java))
        }

        findViewById<TextView>(R.id.btnEqualizador).setOnClickListener {
            SonidoHelper.reproducir(this)
            startActivity(Intent(this, EqualizadorActivity::class.java))
        }

        val filter = IntentFilter(MusicaService.ACTION_MUSICA_ACTUALIZADA)
        ContextCompat.registerReceiver(this, musicaReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    // ── Temporizador ──────────────────────────────────────────────────────

    private fun arrancarTimer(minutos: Int) {
        musicaService?.iniciarTemporizador(minutos)
        tvTimer.visibility = View.VISIBLE
        tvTimer.text = "⏱ $minutos min"
        btnTemporizador.setTextColor(android.graphics.Color.parseColor("#FFD700"))
        Toast.makeText(this, "Música se pausará en $minutos min", Toast.LENGTH_SHORT).show()
        musicaService?.onTimerTick = { minutosRestantes ->
            runOnUiThread { tvTimer.text = "⏱ $minutosRestantes min" }
        }
        musicaService?.onTimerFin = {
            runOnUiThread {
                tvTimer.visibility = View.GONE
                btnTemporizador.setTextColor(android.graphics.Color.parseColor("#AAAACC"))
                Toast.makeText(this, "⏱ Música pausada", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Eliminar canción ──────────────────────────────────────────────────

    private fun confirmarEliminar(uri: String, nombre: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("¿Eliminar canción?")
            .setMessage("¿Estás seguro de eliminar \"$nombre\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                try {
                    if (uri.startsWith("/")) {
                        java.io.File(uri).delete()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ELIMINAR", "No se pudo borrar el archivo: ${e.message}")
                }
                musicaService?.eliminarCancion(uri)
                actualizarLista(musicaService?.getCanciones() ?: emptyList())
                Toast.makeText(this, "🗑 \"$nombre\" eliminada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Selección múltiple → agregar a playlist ───────────────────────────

    private fun mostrarAccionesSeleccion() {
        val seleccionadas = cancionesAdapter.getSeleccionadas()
        if (seleccionadas.isEmpty()) {
            Toast.makeText(this, "Ninguna canción seleccionada", Toast.LENGTH_SHORT).show()
            return
        }
        val carpetaPlaylists = java.io.File(getExternalFilesDir(null), "Playlists")
        val playlists = carpetaPlaylists.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.toTypedArray()

        if (playlists.isNullOrEmpty()) {
            Toast.makeText(this, "No hay playlists — crea una primero", Toast.LENGTH_SHORT).show()
            cancionesAdapter.desactivarModoSeleccion()
            barraSeleccion.visibility = View.GONE
            return
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Agregar ${seleccionadas.size} canción(es) a...")
            .setItems(playlists) { _, which ->
                val playlist = playlists[which]
                val carpetaDest = java.io.File(carpetaPlaylists, playlist)
                lifecycleScope.launch(Dispatchers.IO) {
                    var ok = 0
                    var fallidas = 0
                    if (!carpetaDest.exists()) carpetaDest.mkdirs()

                    seleccionadas.forEach { uri ->
                        try {
                            val nombre = obtenerNombreCancion(uri)
                                .replace("/", "_").replace("\\", "_").trim()
                            val ext = if (uri.startsWith("content://")) "mp3"
                            else uri.substringAfterLast(".").lowercase().ifBlank { "mp3" }
                            val dest = java.io.File(carpetaDest, "$nombre.$ext")

                            if (uri.startsWith("content://")) {
                                val inputStream = contentResolver.openInputStream(Uri.parse(uri))
                                if (inputStream != null) {
                                    inputStream.use { input ->
                                        dest.outputStream().use { output -> input.copyTo(output) }
                                    }
                                    ok++
                                } else { fallidas++ }
                            } else {
                                val origen = java.io.File(uri)
                                if (origen.exists()) {
                                    origen.copyTo(dest, overwrite = true)
                                    ok++
                                } else { fallidas++ }
                            }
                        } catch (e: Exception) { fallidas++ }
                    }

                    runOnUiThread {
                        val msg = if (fallidas == 0) "✓ $ok canción(es) agregadas a $playlist"
                        else "✓ $ok agregadas, $fallidas fallaron"
                        Toast.makeText(this@ReproductorActivity, msg, Toast.LENGTH_LONG).show()
                        cancionesAdapter.desactivarModoSeleccion()
                        barraSeleccion.visibility = View.GONE
                        cargarCancionesDescargadas()
                    }
                }
            }
            .show()
    }

    private fun mostrarDialogoCrearPlaylist() {
        val todas = musicaService?.getCanciones() ?: emptyList()
        if (todas.isEmpty()) {
            Toast.makeText(this, "No hay canciones", Toast.LENGTH_SHORT).show()
            return
        }
        val inputNombre = android.widget.EditText(this).apply { hint = "Nombre de la playlist" }
        android.app.AlertDialog.Builder(this)
            .setTitle("🎼 Nueva playlist")
            .setView(inputNombre)
            .setPositiveButton("Siguiente →") { _, _ ->
                val nombre = inputNombre.text.toString().trim()
                if (nombre.isNotBlank()) mostrarSelectorCanciones(nombre, todas)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarSelectorCanciones(nombrePlaylist: String, todas: List<String>) {
        val nombres = todas.map { obtenerNombreCancion(it) }.toTypedArray()
        val seleccionadas = BooleanArray(todas.size) { false }

        android.app.AlertDialog.Builder(this)
            .setTitle("Agregar a \"$nombrePlaylist\"")
            .setMultiChoiceItems(nombres, seleccionadas) { _, which, isChecked ->
                seleccionadas[which] = isChecked
            }
            .setPositiveButton("✓ Crear playlist") { _, _ ->
                val elegidas = todas.filterIndexed { i, _ -> seleccionadas[i] }
                if (elegidas.isNotEmpty()) crearPlaylistEnDisco(nombrePlaylist, elegidas)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearPlaylistEnDisco(nombre: String, canciones: List<String>) {
        val carpetaPlaylist = java.io.File(getExternalFilesDir(null), "Playlists/$nombre")
        carpetaPlaylist.mkdirs()

        val progressDialog = android.app.ProgressDialog(this).apply {
            setTitle("Creando \"$nombre\"...")
            max = canciones.size
            setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
            show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            canciones.forEachIndexed { index, uri ->
                try {
                    val nombreArchivo = obtenerNombreCancion(uri).replace("[/\\\\]".toRegex(), "_").trim()
                    val extension = if (uri.startsWith("content://")) "mp3"
                    else uri.substringAfterLast(".").lowercase().ifBlank { "mp3" }
                    val destino = java.io.File(carpetaPlaylist, "$nombreArchivo.$extension")

                    if (uri.startsWith("content://")) {
                        contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                            destino.outputStream().use { output -> input.copyTo(output) }
                        }
                    } else {
                        java.io.File(uri).copyTo(destino, overwrite = true)
                    }
                } catch (e: Exception) { }
                runOnUiThread { progressDialog.progress = index + 1 }
            }
            runOnUiThread {
                progressDialog.dismiss()
                Toast.makeText(this@ReproductorActivity, "✓ \"$nombre\" creada", Toast.LENGTH_SHORT).show()
                cargarCancionesDescargadas()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val todas = musicaService?.getCancionesCompletas() ?: emptyList()
        if (todas.isNotEmpty()) actualizarLista(todas)
        else cargarCancionesDescargadas()
    }

    private fun actualizarLista(canciones: List<String>) {
        val items = construirItems(canciones)
        cancionesAdapter.update(items)
    }

    private fun construirItems(canciones: List<String>): List<ItemLista> {
        val items = mutableListOf<ItemLista>()
        val carpetaPlaylists = java.io.File(getExternalFilesDir(null), "Playlists")
        val porPlaylist = mutableMapOf<String, MutableList<Pair<String, Int>>>()
        val delDispositivo = mutableListOf<Pair<String, Int>>()

        canciones.forEachIndexed { indice, uri ->
            if (uri.startsWith("/") && carpetaPlaylists.exists() && uri.startsWith(carpetaPlaylists.absolutePath)) {
                val nombrePlaylist = java.io.File(uri).parentFile?.name ?: "Sin nombre"
                porPlaylist.getOrPut(nombrePlaylist) { mutableListOf() }.add(Pair(uri, indice))
            } else {
                delDispositivo.add(Pair(uri, indice))
            }
        }

        porPlaylist.forEach { (nombrePlaylist, cancionesPlaylist) ->
            items.add(ItemLista.Header(nombrePlaylist)) // EL TÍTULO YA NO TIENE ▶
            cancionesPlaylist.forEach { (uri, indice) -> items.add(ItemLista.Cancion(uri, indice)) }
        }
        if (delDispositivo.isNotEmpty()) {
            items.add(ItemLista.Header("Mi dispositivo"))
            delDispositivo.forEach { (uri, indice) -> items.add(ItemLista.Cancion(uri, indice)) }
        }
        return items
    }

    private fun cargarCancionesDescargadas() {
        val carpetaPlaylists = java.io.File(getExternalFilesDir(null), "Playlists")
        val cancionesActuales = musicaService?.getCancionesCompletas() ?: emptyList()
        if (carpetaPlaylists.exists()) {
            carpetaPlaylists.walkTopDown().forEach { archivo ->
                if (archivo.isFile && archivo.extension.lowercase() in listOf("m4a", "mp3", "flac", "ogg")) {
                    if (archivo.absolutePath !in cancionesActuales) musicaService?.agregarCancion(archivo.absolutePath)
                }
            }
        }
        actualizarLista(musicaService?.getCanciones() ?: emptyList())
    }

    private fun actualizarBotonShuffle(activo: Boolean) {
        btnShuffle.setTextColor(if (activo) android.graphics.Color.parseColor("#FFD700") else android.graphics.Color.parseColor("#444466"))
    }

    private fun obtenerNombreCancion(uri: String): String {
        return try {
            if (uri.startsWith("content://")) {
                contentResolver.query(Uri.parse(uri), null, null, null, null)?.use {
                    if (it.moveToFirst()) {
                        val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) it.getString(idx).substringBeforeLast(".") else "Canción"
                    } else "Canción"
                } ?: "Canción"
            } else uri.substringAfterLast("/").substringBeforeLast(".")
        } catch (e: Exception) { "Canción" }
    }

    private fun iniciarActualizacionProgreso() {
        handler.post(object : Runnable {
            override fun run() {
                musicaService?.let { service ->
                    val duracion = service.getDuracion()
                    val posicion = service.getPosicion()
                    if (duracion > 0) {
                        findViewById<SeekBar>(R.id.seekBarMusica).apply { max = duracion; progress = posicion }
                        findViewById<TextView>(R.id.tvTiempoActual).text = formatearTiempo(posicion)
                        findViewById<TextView>(R.id.tvTiempoTotal).text = formatearTiempo(duracion)
                    }
                }
                handler.postDelayed(this, 500)
            }
        })
    }

    private fun cargarCancionesDesdeCarpeta(uri: Uri) {
        val canciones = mutableListOf<String>()
        val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(uri, android.provider.DocumentsContract.getTreeDocumentId(uri))
        contentResolver.query(childrenUri, arrayOf(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID, android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0)
                val nombre = cursor.getString(1)
                if (listOf("mp3", "m4a", "flac", "ogg").any { nombre.endsWith(it) }) {
                    canciones.add(android.provider.DocumentsContract.buildDocumentUriUsingTree(uri, docId).toString())
                }
            }
        }
        if (canciones.isNotEmpty()) {
            musicaService?.setCanciones(canciones, 0)
            actualizarLista(canciones)
        }
    }

    private fun formatearTiempo(ms: Int): String {
        val segundos = (ms / 1000) % 60
        val minutos = ms / 1000 / 60
        return "$minutos:${segundos.toString().padStart(2, '0')}"
    }

    private fun actualizarUI(nombre: String, playing: Boolean) {
        findViewById<TextView>(R.id.tvNombreCancion).text = nombre.ifEmpty { "Sin canción" }
        findViewById<TextView>(R.id.btnPlayPause).text = if (playing) "⏸" else "▶"
    }

    override fun onDestroy() {
        unregisterReceiver(musicaReceiver)
        handler.removeCallbacksAndMessages(null)
        if (serviceConectado) unbindService(connection)
        super.onDestroy()
    }
}

class CancionesAdapter(
    private val context: Context,
    private var items: List<ItemLista>,
    private val getNombre: (String) -> String,
    private val onClick: (Int) -> Unit,
    private val onHeaderClick: (nombreSeccion: String, canciones: List<String>) -> Unit,
    private val onEliminar: (uri: String, nombre: String) -> Unit,
    private val onSeleccionCambiada: (cantidad: Int) -> Unit,
    private val onModoSeleccionActivado: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val prefs = context.getSharedPreferences("reproductor_adapter_prefs", Context.MODE_PRIVATE)
    private var itemsOriginales: List<ItemLista> = items
    private var queryActual = ""
    private var indiceActivo = -1
    private var modoSeleccion = false
    private val seleccionadas = mutableSetOf<String>()
    private val headersColapsados = mutableSetOf<String>()

    init {
        // Cargar headers colapsados guardados
        val guardados = prefs.getStringSet("headers_colapsados", emptySet())
        headersColapsados.addAll(guardados ?: emptySet())
    }

    companion object {
        const val TIPO_HEADER  = 0
        const val TIPO_CANCION = 1
    }

    fun update(nuevos: List<ItemLista>) {
        itemsOriginales = nuevos
        aplicarFiltroColapso()
        seleccionadas.clear()
    }

    fun filtrar(nuevaQuery: String) {
        queryActual = nuevaQuery
        aplicarFiltroColapso()
    }

    private fun aplicarFiltroColapso() {
        val nuevosItems = mutableListOf<ItemLista>()

        if (queryActual.isBlank()) {
            var saltar = false
            for (item in itemsOriginales) {
                if (item is ItemLista.Header) {
                    nuevosItems.add(item)
                    saltar = headersColapsados.contains(item.titulo)
                } else if (!saltar) {
                    nuevosItems.add(item)
                }
            }
        } else {
            var headerActual: ItemLista.Header? = null
            var cancionesEnSeccion = mutableListOf<ItemLista.Cancion>()

            for (item in itemsOriginales) {
                if (item is ItemLista.Header) {
                    if (cancionesEnSeccion.isNotEmpty()) {
                        headerActual?.let { nuevosItems.add(it) }
                        nuevosItems.addAll(cancionesEnSeccion)
                    }
                    headerActual = item
                    cancionesEnSeccion = mutableListOf()
                } else if (item is ItemLista.Cancion) {
                    if (getNombre(item.uri).contains(queryActual, ignoreCase = true)) {
                        cancionesEnSeccion.add(item)
                    }
                }
            }
            if (cancionesEnSeccion.isNotEmpty()) {
                headerActual?.let { nuevosItems.add(it) }
                nuevosItems.addAll(cancionesEnSeccion)
            }
        }

        items = nuevosItems
        notifyDataSetChanged()
    }

    private fun guardarEstadoColapso() {
        prefs.edit().putStringSet("headers_colapsados", headersColapsados.toSet()).apply()
    }

    fun setIndiceActivo(indice: Int) { indiceActivo = indice; notifyDataSetChanged() }
    fun desactivarModoSeleccion() { modoSeleccion = false; seleccionadas.clear(); notifyDataSetChanged() }
    fun getSeleccionadas(): List<String> = seleccionadas.toList()

    override fun getItemViewType(position: Int) = if (items[position] is ItemLista.Header) TIPO_HEADER else TIPO_CANCION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIPO_HEADER) {
            val tv = TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setPadding(32, 24, 32, 12)
                setTextColor(android.graphics.Color.parseColor("#FFD700"))
                textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            object : RecyclerView.ViewHolder(tv) {}
        } else {
            CancionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_cancion, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (item is ItemLista.Header) {
            val tv = holder.itemView as TextView
            tv.text = item.titulo
            val collapsed = headersColapsados.contains(item.titulo)
            
            tv.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_media_play, 0,
                if (collapsed) android.R.drawable.arrow_up_float else android.R.drawable.arrow_down_float, 0
            )
            tv.compoundDrawablePadding = 24
            tv.minHeight = 120

            tv.setOnTouchListener { v, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    if (event.x < 120) {
                        reproducirSeccion(item.titulo)
                        v.performClick()
                        return@setOnTouchListener true
                    } else if (event.x > v.width - 120) {
                        if (headersColapsados.contains(item.titulo)) {
                            headersColapsados.remove(item.titulo)
                        } else {
                            headersColapsados.add(item.titulo)
                        }
                        guardarEstadoColapso()
                        aplicarFiltroColapso()
                        return@setOnTouchListener true
                    }
                }
                false
            }
            tv.setOnClickListener { }
        } else if (item is ItemLista.Cancion) {
            (holder as CancionViewHolder).bind(item.uri, item.indiceGlobal)
        }
    }

    private fun reproducirSeccion(titulo: String) {
        val cancionesSeccion = mutableListOf<String>()
        val idxOrig = itemsOriginales.indexOfFirst { it is ItemLista.Header && it.titulo == titulo }
        if (idxOrig != -1) {
            var j = idxOrig + 1
            while (j < itemsOriginales.size && itemsOriginales[j] is ItemLista.Cancion) {
                cancionesSeccion.add((itemsOriginales[j] as ItemLista.Cancion).uri)
                j++
            }
            onHeaderClick(titulo, cancionesSeccion)
        }
    }

    override fun getItemCount() = items.size

    inner class CancionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(uri: String, indiceGlobal: Int) {
            val tvNombre = itemView.findViewById<TextView>(R.id.cancionNombre)
            val tvActiva = itemView.findViewById<TextView>(R.id.cancionActiva)
            val checkbox = itemView.findViewById<CheckBox>(R.id.cancionCheckbox)
            val btnEliminar = itemView.findViewById<TextView>(R.id.btnEliminarCancion)

            tvNombre.text = getNombre(uri)
            tvActiva.visibility = if (indiceGlobal == indiceActivo) View.VISIBLE else View.GONE
            itemView.setBackgroundColor(android.graphics.Color.parseColor(if (indiceGlobal == indiceActivo) "#2A2A2A" else "#1E1E1E"))

            checkbox.visibility = if (modoSeleccion) View.VISIBLE else View.GONE
            btnEliminar.visibility = if (modoSeleccion) View.GONE else View.VISIBLE
            checkbox.isChecked = seleccionadas.contains(uri)

            itemView.setOnClickListener {
                if (modoSeleccion) {
                    if (seleccionadas.contains(uri)) seleccionadas.remove(uri) else seleccionadas.add(uri)
                    checkbox.isChecked = seleccionadas.contains(uri)
                    onSeleccionCambiada(seleccionadas.size)
                } else onClick(indiceGlobal)
            }
            itemView.setOnLongClickListener {
                if (!modoSeleccion) {
                    modoSeleccion = true
                    seleccionadas.add(uri)
                    onModoSeleccionActivado()
                    notifyDataSetChanged()
                }
                true
            }
            btnEliminar.setOnClickListener { onEliminar(uri, getNombre(uri)) }
        }
    }
}
