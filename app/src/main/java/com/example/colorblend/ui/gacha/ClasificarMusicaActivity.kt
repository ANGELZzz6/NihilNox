package com.example.colorblend.ui.gacha

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.Cancion
import com.example.colorblend.domain.model.Genero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class ClasificarMusicaActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var musicaService: MusicaService? = null
    private var serviceConectado = false

    private lateinit var contenedorPunto: View
    private lateinit var punto: View
    private lateinit var colIzquierda: LinearLayout
    private lateinit var colDerecha: LinearLayout
    private lateinit var tvTitulo: TextView
    private lateinit var tvArtista: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var tvGenerosAcumulados: TextView
    private lateinit var btnAceptarDropZone: View

    private var cancionActual: String? = null
    private var listaGeneros = mutableListOf<Genero>()
    private var generosSeleccionados = mutableSetOf<String>()
    
    // Modo Reasignar
    private var generoFiltroReasignar: String? = null
    
    private var dX = 0f
    private var dY = 0f
    private var initialX = 0f
    private var initialY = 0f
    private var generoSeleccionadoActual: TextView? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicaService.MusicaBinder
            musicaService = binder.getService()
            serviceConectado = true
            reproducirSiguienteAleatoria()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceConectado = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clasificar_musica)
        FullScreenHelper.enable(this)

        db = AppDatabase.getDatabase(this)
        
        contenedorPunto = findViewById(R.id.contenedorPunto)
        punto = findViewById(R.id.puntoArrastrable)
        colIzquierda = findViewById(R.id.columnaIzquierda)
        colDerecha = findViewById(R.id.columnaDerecha)
        tvTitulo = findViewById(R.id.tvTituloClasificar)
        tvArtista = findViewById(R.id.tvArtistaClasificar)
        tvFeedback = findViewById(R.id.tvFeedbackClasificar)
        tvGenerosAcumulados = findViewById(R.id.tvGenerosAcumulados)
        btnAceptarDropZone = findViewById(R.id.btnAceptarClasificacion)

        findViewById<Button>(R.id.btnNuevoGenero).setOnClickListener {
            sonidoBoton()
            mostrarDialogoNuevoGenero()
        }

        findViewById<Button>(R.id.btnModoReasignar).setOnClickListener {
            sonidoBoton()
            mostrarDialogoReasignar()
        }

        configurarArrastre()
        cargarGeneros()

        val intent = Intent(this, MusicaService::class.java)
        bindService(intent, connection, BIND_AUTO_CREATE)
    }

    private fun sonidoBoton() {
        SonidoHelper.reproducir(this)
    }

    private fun configurarArrastre() {
        contenedorPunto.post {
            initialX = contenedorPunto.x
            initialY = contenedorPunto.y
        }

        punto.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = contenedorPunto.x - event.rawX
                    dY = contenedorPunto.y - event.rawY
                    tvFeedback.visibility = View.INVISIBLE
                }
                MotionEvent.ACTION_MOVE -> {
                    contenedorPunto.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                    verificarColisionesCercanas()
                }
                MotionEvent.ACTION_UP -> {
                    val generoDetectado = detectarColision()
                    val esAceptar = isPointInsideView(
                        contenedorPunto.x + contenedorPunto.width / 2,
                        contenedorPunto.y + contenedorPunto.height / 2,
                        btnAceptarDropZone
                    )

                    if (esAceptar) {
                        guardarGenerosACancion()
                    } else if (generoDetectado != null) {
                        toggleGenero(generoDetectado)
                        regresarAlCentro()
                    } else {
                        regresarAlCentro()
                    }
                }
            }
            true
        }
    }

    private fun toggleGenero(genero: String) {
        if (generosSeleccionados.contains(genero)) {
            generosSeleccionados.remove(genero)
        } else {
            generosSeleccionados.add(genero)
        }
        actualizarTextoAcumulado()
    }

    private fun actualizarTextoAcumulado() {
        tvGenerosAcumulados.text = if (generosSeleccionados.isEmpty()) "" 
                                   else "Seleccionados: ${generosSeleccionados.joinToString(", ")}"
    }

    private fun verificarColisionesCercanas() {
        val pX = contenedorPunto.x + contenedorPunto.width / 2
        val pY = contenedorPunto.y + contenedorPunto.height / 2
        val threshold = 500f 

        generoSeleccionadoActual = null
        revelarEnColumna(colIzquierda, pX, pY, threshold)
        revelarEnColumna(colDerecha, pX, pY, threshold)
        
        // Efecto visual en botón Aceptar
        if (isPointInsideView(pX, pY, btnAceptarDropZone)) {
            btnAceptarDropZone.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#444444")))
            btnAceptarDropZone.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).start()
        } else {
            btnAceptarDropZone.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#222222")))
            btnAceptarDropZone.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
        }
    }

    private fun revelarEnColumna(col: LinearLayout, pX: Float, pY: Float, threshold: Float) {
        for (i in 0 until col.childCount) {
            val child = col.getChildAt(i) as? TextView ?: continue
            val location = IntArray(2)
            child.getLocationOnScreen(location)
            
            val cWidth = child.width
            val cHeight = child.height
            val cX = location[0] + cWidth / 2f
            val cY = location[1] + cHeight / 2f

            val dist = sqrt((pX - cX) * (pX - cX) + (pY - cY) * (pY - cY))
            val fullName = child.tag as? String ?: child.text.toString()
            val firstWord = fullName.split(" ").firstOrNull() ?: fullName

            val isInside = pX >= location[0] && pX <= location[0] + cWidth &&
                           pY >= location[1] && pY <= location[1] + cHeight

            val estaSeleccionado = generosSeleccionados.contains(fullName)

            if (isInside) {
                generoSeleccionadoActual = child
                child.text = fullName
                child.setTextColor(if (estaSeleccionado) Color.RED else Color.YELLOW)
                child.alpha = 1.0f
                child.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).start()
            } else if (dist < threshold) {
                child.text = fullName
                child.setTextColor(if (estaSeleccionado) Color.RED else Color.WHITE)
                child.alpha = 1.0f
                child.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
            } else {
                child.text = firstWord
                child.setTextColor(if (estaSeleccionado) Color.RED else Color.WHITE)
                child.alpha = 0.5f
                child.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
        }
    }

    private fun detectarColision(): String? {
        return generoSeleccionadoActual?.tag as? String
    }

    private fun isPointInsideView(x: Float, y: Float, view: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return x >= location[0] && x <= location[0] + view.width &&
                y >= location[1] && y <= location[1] + view.height
    }

    private fun regresarAlCentro() {
        contenedorPunto.animate()
            .x(initialX)
            .y(initialY)
            .setDuration(300)
            .start()
        
        resetearEstilosGeneros()
    }

    private fun resetearEstilosGeneros() {
        listOf(colIzquierda, colDerecha).forEach { col ->
            for (i in 0 until col.childCount) {
                val child = col.getChildAt(i) as? TextView ?: continue
                val fullName = child.tag as? String ?: child.text.toString()
                val estaSeleccionado = generosSeleccionados.contains(fullName)
                
                child.text = fullName.split(" ").firstOrNull() ?: fullName
                child.setTextColor(if (estaSeleccionado) Color.RED else Color.WHITE)
                child.alpha = if (estaSeleccionado) 1.0f else 0.5f
                child.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
        }
    }

    private fun cargarGeneros() {
        lifecycleScope.launch(Dispatchers.IO) {
            val generos = db.generoDao().obtenerTodos()
            withContext(Dispatchers.Main) {
                listaGeneros.clear()
                listaGeneros.addAll(generos)
                actualizarColumnas()
            }
        }
    }

    private fun actualizarColumnas() {
        colIzquierda.removeAllViews()
        colDerecha.removeAllViews()

        listaGeneros.forEachIndexed { index, genero ->
            val fullName = genero.nombre
            val estaSeleccionado = generosSeleccionados.contains(fullName)
            val firstWord = fullName.split(" ").firstOrNull() ?: fullName
            
            val tv = TextView(this).apply {
                text = firstWord
                tag = fullName
                setTextColor(if (estaSeleccionado) Color.RED else Color.WHITE)
                textSize = 16f
                alpha = if (estaSeleccionado) 1.0f else 0.5f
                setPadding(32, 40, 32, 40)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            if (index % 2 == 0) {
                tv.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                colIzquierda.addView(tv)
            } else {
                tv.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                colDerecha.addView(tv)
            }
        }
    }

    private fun mostrarDialogoNuevoGenero() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Nuevo Género")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotBlank()) {
                    guardarNuevoGenero(nombre)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarNuevoGenero(nombre: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.generoDao().insertar(Genero(nombre = nombre))
            val generos = db.generoDao().obtenerTodos()
            withContext(Dispatchers.Main) {
                listaGeneros.clear()
                listaGeneros.addAll(generos)
                actualizarColumnas()
            }
        }
    }

    private fun mostrarDialogoReasignar() {
        if (listaGeneros.isEmpty()) {
            Toast.makeText(this, "Crea géneros primero", Toast.LENGTH_SHORT).show()
            return
        }
        val nombres = listaGeneros.map { it.nombre }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Filtrar por género para reasignar:")
            .setItems(nombres) { _, which ->
                generoFiltroReasignar = nombres[which]
                Toast.makeText(this, "Modo reasignar: ${nombres[which]}", Toast.LENGTH_SHORT).show()
                reproducirSiguienteAleatoria()
            }
            .setNeutralButton("Ver todos") { _, _ ->
                generoFiltroReasignar = null
                reproducirSiguienteAleatoria()
            }
            .show()
    }

    private fun guardarGenerosACancion() {
        val uri = cancionActual ?: return
        val listaFinal = generosSeleccionados.toList()
        
        lifecycleScope.launch(Dispatchers.IO) {
            val cancionExistente = db.cancionDao().obtenerTodas().find { it.uriLocal == uri }
            if (cancionExistente != null) {
                db.cancionDao().insertar(cancionExistente.copy(generos = if (listaFinal.isEmpty()) null else listaFinal))
            } else {
                db.cancionDao().insertar(Cancion(
                    uriLocal = uri,
                    titulo = tvTitulo.text.toString(),
                    artista = tvArtista.text.toString(),
                    playlistId = "Clasificadas",
                    generos = if (listaFinal.isEmpty()) null else listaFinal
                ))
            }

            withContext(Dispatchers.Main) {
                tvFeedback.text = "¡Guardado!"
                tvFeedback.visibility = View.VISIBLE
                tvFeedback.alpha = 0f
                tvFeedback.animate().alpha(1f).setDuration(200).start()

                regresarAlCentro()
                
                contenedorPunto.postDelayed({
                    tvFeedback.animate().alpha(0f).setDuration(200).withEndAction {
                        tvFeedback.visibility = View.INVISIBLE
                        reproducirSiguienteAleatoria()
                    }.start()
                }, 800)
            }
        }
    }

    private fun reproducirSiguienteAleatoria() {
        val service = musicaService ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val cancionesDB = db.cancionDao().obtenerTodas()
            val cancionesServicio = service.getCancionesCompletas()
            
            // Filtrar si estamos en modo reasignar
            val candidatas = if (generoFiltroReasignar != null) {
                cancionesServicio.filter { uri ->
                    val c = cancionesDB.find { it.uriLocal == uri }
                    c?.generos?.contains(generoFiltroReasignar) == true
                }
            } else {
                cancionesServicio
            }

            if (candidatas.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ClasificarMusicaActivity, "No hay canciones que coincidan", Toast.LENGTH_SHORT).show()
                    if (generoFiltroReasignar != null) {
                        generoFiltroReasignar = null
                        reproducirSiguienteAleatoria()
                    } else {
                        finish()
                    }
                }
                return@launch
            }

            val aleatoria = candidatas.random()
            cancionActual = aleatoria
            
            // Cargar géneros ya existentes para esta canción
            val cancionInfo = cancionesDB.find { it.uriLocal == aleatoria }
            
            withContext(Dispatchers.Main) {
                generosSeleccionados.clear()
                cancionInfo?.generos?.let { generosSeleccionados.addAll(it) }
                
                service.setCanciones(listOf(aleatoria), 0)
                tvTitulo.text = obtenerNombreCancion(aleatoria)
                tvArtista.text = if (generoFiltroReasignar != null) "Reasignando: $generoFiltroReasignar" else "Clasificando..."
                
                actualizarTextoAcumulado()
                resetearEstilosGeneros()
            }
        }
    }

    private fun obtenerNombreCancion(uri: String): String {
        return try {
            if (uri.startsWith("content://")) {
                contentResolver.query(android.net.Uri.parse(uri), null, null, null, null)?.use {
                    if (it.moveToFirst()) {
                        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) it.getString(idx).substringBeforeLast(".") else "Canción"
                    } else "Canción"
                } ?: "Canción"
            } else uri.substringAfterLast("/").substringBeforeLast(".")
        } catch (e: Exception) { "Canción" }
    }

    override fun onDestroy() {
        if (serviceConectado) unbindService(connection)
        super.onDestroy()
    }
}
