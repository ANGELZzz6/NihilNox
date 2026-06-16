package com.example.colorblend.ui.gacha

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent

// ── Modelos ───────────────────────────────────────────────────────────────────

enum class TipoNota { TEXTO, LISTA }

data class ItemCheckbox(val texto: String, val marcado: Boolean)

data class Nota(
    val id: Long,
    val titulo: String,
    val contenido: String,
    val fecha: String,
    val tipo: TipoNota = TipoNota.TEXTO,
    val items: List<ItemCheckbox> = emptyList()
)

// ── Activity ──────────────────────────────────────────────────────────────────

class BlockNotasActivity : AppCompatActivity() {

    private lateinit var adapter: NotasAdapter
    private val notas = mutableListOf<Nota>()          // lista maestra
    private val notasFiltradas = mutableListOf<Nota>() // lo que muestra el adapter

    private val PREFS_NOTAS   = "block_notas"
    private val PREFS_PASS    = "block_notas_prefs"
    private val KEY_PASSWORD  = "password"
    private val KEY_MOSTRAR_DESC = "mostrar_descripcion"
    private val DEFAULT_PASSWORD = "1234"

    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var etBuscarNota: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_notas)

        val recycler    = findViewById<RecyclerView>(R.id.recyclerNotas)
        val tvSinNotas  = findViewById<TextView>(R.id.tvSinNotas)
        val etBuscar    = findViewById<EditText>(R.id.etBuscarNota)
        etBuscarNota    = etBuscar  // ← inicializa la propiedad de clase

        // ── Adapter ───────────────────────────────────────────────────────
        adapter = NotasAdapter(
            notas = notasFiltradas,
            onVer = { nota -> mostrarDialogoVerNota(nota) },
            onEditar = { nota -> mostrarDialogoNota(nota) },
            onEliminar = { nota ->
                SonidoHelper.reproducir(this)
                AlertDialog.Builder(this)
                    .setTitle("Eliminar nota")
                    .setMessage("¿Seguro que quieres borrar \"${nota.titulo}\"?")
                    .setPositiveButton("Borrar") { _, _ ->
                        notas.removeIf { it.id == nota.id }
                        guardarNotas()
                        aplicarFiltro(etBuscar.text.toString())
                        tvSinNotas.visibility = if (notas.isEmpty()) View.VISIBLE else View.GONE
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onStartDrag = { viewHolder -> itemTouchHelper.startDrag(viewHolder) }
        )

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)

        // ── Drag & drop ───────────────────────────────────────────────────
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView,
                from: RecyclerView.ViewHolder,
                to: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = from.adapterPosition
                val toPos   = to.adapterPosition
                // Mover en la lista filtrada y en la maestra a la vez
                Collections.swap(notasFiltradas, fromPos, toPos)
                // Sincronizar orden en la lista maestra
                sincronizarOrdenDesdeVista()
                adapter.notifyItemMoved(fromPos, toPos)
                return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                guardarNotas()
            }
        }
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recycler)

        // ── Cargar notas ──────────────────────────────────────────────────
        cargarNotas()
        aplicarFiltro("")
        tvSinNotas.visibility = if (notas.isEmpty()) View.VISIBLE else View.GONE

        // ── Toggle descripción ────────────────────────────────────────────
        val mostrarDesc = getSharedPreferences(PREFS_PASS, Context.MODE_PRIVATE)
            .getBoolean(KEY_MOSTRAR_DESC, true)
        adapter.setMostrarDescripcion(mostrarDesc)
        actualizarIconoToggle(mostrarDesc)

        findViewById<Button>(R.id.btnToggleDescripcion).setOnClickListener {
            SonidoHelper.reproducir(this)
            val actual = getSharedPreferences(PREFS_PASS, Context.MODE_PRIVATE)
                .getBoolean(KEY_MOSTRAR_DESC, true)
            val nuevo = !actual
            getSharedPreferences(PREFS_PASS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_MOSTRAR_DESC, nuevo).apply()
            adapter.setMostrarDescripcion(nuevo)
            actualizarIconoToggle(nuevo)
        }

        // ── Buscador ──────────────────────────────────────────────────────
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                aplicarFiltro(s?.toString() ?: "")
            }
        })

        // ── Botones ───────────────────────────────────────────────────────
        findViewById<Button>(R.id.btnAgregarNota).setOnClickListener {
            SonidoHelper.reproducir(this)
            mostrarDialogoNota(null)
        }
        findViewById<Button>(R.id.btnCambiarContrasena).setOnClickListener {
            SonidoHelper.reproducir(this)
            mostrarDialogoCambiarContrasena()
        }
        findViewById<Button>(R.id.btnExportarPdf).setOnClickListener {
            SonidoHelper.reproducir(this)
            exportarPdf()
        }
    }

    // ── Filtro de búsqueda ────────────────────────────────────────────────────

    private fun aplicarFiltro(query: String) {
        notasFiltradas.clear()
        if (query.isBlank()) {
            notasFiltradas.addAll(notas)
        } else {
            val q = query.lowercase()
            notasFiltradas.addAll(notas.filter {
                it.titulo.lowercase().contains(q) ||
                        it.contenido.lowercase().contains(q) ||
                        it.items.any { item -> item.texto.lowercase().contains(q) }
            })
        }
        adapter.notifyDataSetChanged()
    }

    // Cuando el drag reordena notasFiltradas, aplica ese orden en la lista maestra
    private fun sincronizarOrdenDesdeVista() {
        // Reconstruye la lista maestra respetando el nuevo orden de notasFiltradas
        val nuevasMaestra = mutableListOf<Nota>()
        nuevasMaestra.addAll(notasFiltradas)
        // Agrega las que no están en la vista (filtradas fuera)
        notas.forEach { if (!notasFiltradas.any { f -> f.id == it.id }) nuevasMaestra.add(it) }
        notas.clear()
        notas.addAll(nuevasMaestra)
    }

    private fun actualizarIconoToggle(mostrar: Boolean) {
        findViewById<Button>(R.id.btnToggleDescripcion).text = if (mostrar) "👁️" else "🙈"
    }

    // ── Diálogo ver nota ──────────────────────────────────────────────────────

    private fun mostrarDialogoVerNota(nota: Nota) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ver_nota, null)
        dialogView.findViewById<TextView>(R.id.tvDialogTitulo).text = nota.titulo.ifEmpty { "Sin título" }
        dialogView.findViewById<TextView>(R.id.tvDialogFecha).text = "📅 ${nota.fecha}"

        val tvContenido = dialogView.findViewById<TextView>(R.id.tvDialogContenido)

        if (nota.tipo == TipoNota.LISTA) {
            // Mostrar items como lista con estado
            tvContenido.visibility = View.GONE
            // Inflar dinámicamente los items en el ScrollView
            val scroll = dialogView.findViewById<ScrollView>(R.id.scrollContenidoVer)
            val contenedor = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, 0)
            }
            nota.items.forEach { item ->
                val cb = CheckBox(this).apply {
                    text = item.texto
                    isChecked = item.marcado
                    setTextColor(if (item.marcado) 0xFF888888.toInt() else 0xFFDDDDDD.toInt())
                    setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFFFD700.toInt()))
                    textSize = 15f
                    setPadding(0, 8, 0, 8)
                    paintFlags = if (item.marcado)
                        paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    else
                        paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    isEnabled = true

                    // ── NUEVO: guardar al marcar/desmarcar ────────────────────────
                    setOnCheckedChangeListener { _, isChecked ->
                        paintFlags = if (isChecked)
                            paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        else
                            paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        setTextColor(if (isChecked) 0xFF888888.toInt() else 0xFFDDDDDD.toInt())

                        val indiceNota = notas.indexOfFirst { it.id == nota.id }
                        if (indiceNota >= 0) {
                            val indiceItem = nota.items.indexOf(item)
                            if (indiceItem >= 0) {
                                val itemsActualizados = nota.items.toMutableList()
                                itemsActualizados[indiceItem] = ItemCheckbox(item.texto, isChecked)
                                notas[indiceNota] = nota.copy(items = itemsActualizados)
                                guardarNotas()
                                // ← ahora usa la variable capturada, no findViewById dentro del diálogo
                                aplicarFiltro(etBuscarNota.text.toString())
                            }
                        }
                    }
                }
                contenedor.addView(cb)
            }
            scroll.removeAllViews()
            scroll.addView(contenedor)
        } else {
            tvContenido.text = nota.contenido
            android.text.util.Linkify.addLinks(tvContenido, android.text.util.Linkify.WEB_URLS)
            tvContenido.movementMethod = object : android.text.method.LinkMovementMethod() {
                override fun onTouchEvent(
                    widget: TextView, buffer: android.text.Spannable, event: MotionEvent
                ): Boolean {
                    if (event.action == MotionEvent.ACTION_UP) {
                        val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
                        val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
                        val layout = widget.layout
                        val line = layout.getLineForVertical(y)
                        val offset = layout.getOffsetForHorizontal(line, x.toFloat())
                        val spans = buffer.getSpans(offset, offset, android.text.style.URLSpan::class.java)
                        if (spans.isNotEmpty()) {
                            abrirEnIncognito(spans[0].url, nota)
                            return true
                        }
                    }
                    return super.onTouchEvent(widget, buffer, event)
                }
            }
        }

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar)
            .setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<Button>(R.id.btnCerrarVer).setOnClickListener {
            SonidoHelper.reproducir(this)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun abrirEnIncognito(url: String, nota: Nota) {
        val urlFinal = if (url.startsWith("http")) url else "https://$url"
        startActivity(Intent(this, IncognitoWebViewActivity::class.java).apply {
            putExtra("url", urlFinal)
            putExtra("notaTitulo", nota.titulo)
            putExtra("notaContenido", nota.contenido)
        })
    }

    // ── Diálogo crear / editar nota ───────────────────────────────────────────

    private fun mostrarDialogoNota(notaExistente: Nota?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nota, null)
        val etTitulo         = dialogView.findViewById<EditText>(R.id.etNotaTitulo)
        val etContenido      = dialogView.findViewById<EditText>(R.id.etNotaContenido)
        val contenedorLista  = dialogView.findViewById<LinearLayout>(R.id.contenedorLista)
        val btnAgregarItem   = dialogView.findViewById<Button>(R.id.btnAgregarItem)
        val btnTipoNota      = dialogView.findViewById<Button>(R.id.btnTipoNota)
        val btnTipoLista     = dialogView.findViewById<Button>(R.id.btnTipoLista)
        val tvHeader         = dialogView.findViewById<TextView>(R.id.tvDialogEditarHeader)
        val tvLabelContenido = dialogView.findViewById<TextView>(R.id.tvLabelContenido)

        // Estado local del tipo seleccionado
        var tipoSeleccionado = notaExistente?.tipo ?: TipoNota.TEXTO

        // Items de lista en edición (lista de pares texto/marcado)
        val itemsEnEdicion = mutableListOf<Pair<String, Boolean>>()
        if (notaExistente?.tipo == TipoNota.LISTA) {
            notaExistente.items.forEach { itemsEnEdicion.add(Pair(it.texto, it.marcado)) }
        }

        fun actualizarVistaTipo() {
            if (tipoSeleccionado == TipoNota.TEXTO) {
                etContenido.visibility      = View.VISIBLE
                contenedorLista.visibility  = View.GONE
                btnAgregarItem.visibility   = View.GONE
                tvLabelContenido.text       = "CONTENIDO"
                btnTipoNota.setTextColor(0xFF1A1200.toInt())
                btnTipoNota.backgroundTintList = null
                btnTipoNota.background = resources.getDrawable(R.drawable.btn_gacha_gold_gradient, null)
                btnTipoLista.setTextColor(0xFFAAAACC.toInt())
                btnTipoLista.background = resources.getDrawable(R.drawable.btn_nav_gradient, null)
            } else {
                etContenido.visibility      = View.GONE
                contenedorLista.visibility  = View.VISIBLE
                btnAgregarItem.visibility   = View.VISIBLE
                tvLabelContenido.text       = "ÍTEMS DE LA LISTA"
                btnTipoLista.setTextColor(0xFF1A1200.toInt())
                btnTipoLista.backgroundTintList = null
                btnTipoLista.background = resources.getDrawable(R.drawable.btn_gacha_gold_gradient, null)
                btnTipoNota.setTextColor(0xFFAAAACC.toInt())
                btnTipoNota.background = resources.getDrawable(R.drawable.btn_nav_gradient, null)
                refrescarItemsEnDialog(contenedorLista, itemsEnEdicion)
            }
        }

        // Rellenar si es edición
        if (notaExistente != null) {
            etTitulo.setText(notaExistente.titulo)
            tvHeader.text = "✏️  EDITAR NOTA"
            if (notaExistente.tipo == TipoNota.TEXTO) etContenido.setText(notaExistente.contenido)
        } else {
            tvHeader.text = "✏️  NUEVA NOTA"
        }

        actualizarVistaTipo()

        btnTipoNota.setOnClickListener {
            tipoSeleccionado = TipoNota.TEXTO
            actualizarVistaTipo()
        }
        btnTipoLista.setOnClickListener {
            tipoSeleccionado = TipoNota.LISTA
            actualizarVistaTipo()
        }
        btnAgregarItem.setOnClickListener {
            itemsEnEdicion.add(Pair("", false))
            refrescarItemsEnDialog(contenedorLista, itemsEnEdicion)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(null)
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                SonidoHelper.reproducir(this)
                val titulo    = etTitulo.text.toString().trim()
                val fecha     = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val tvSinNotas = findViewById<TextView>(R.id.tvSinNotas)
                val etBuscar   = findViewById<EditText>(R.id.etBuscarNota)

                if (tipoSeleccionado == TipoNota.TEXTO) {
                    val contenido = etContenido.text.toString().trim()
                    if (titulo.isEmpty() && contenido.isEmpty()) return@setPositiveButton
                    val nuevaNota = if (notaExistente != null)
                        Nota(notaExistente.id, titulo, contenido, fecha, TipoNota.TEXTO)
                    else
                        Nota(System.currentTimeMillis(), titulo, contenido, fecha, TipoNota.TEXTO)
                    guardarOActualizar(nuevaNota, notaExistente)
                } else {
                    // Recolectar items desde los EditTexts del dialog
                    val itemsFinales = mutableListOf<ItemCheckbox>()
                    for (i in 0 until contenedorLista.childCount) {
                        val fila = contenedorLista.getChildAt(i) as? LinearLayout ?: continue
                        val cb   = fila.getChildAt(0) as? CheckBox ?: continue
                        val et   = fila.getChildAt(1) as? EditText ?: continue
                        val txt  = et.text.toString().trim()
                        if (txt.isNotEmpty()) itemsFinales.add(ItemCheckbox(txt, cb.isChecked))
                    }
                    if (titulo.isEmpty() && itemsFinales.isEmpty()) return@setPositiveButton
                    val nuevaNota = if (notaExistente != null)
                        Nota(notaExistente.id, titulo, "", fecha, TipoNota.LISTA, itemsFinales)
                    else
                        Nota(System.currentTimeMillis(), titulo, "", fecha, TipoNota.LISTA, itemsFinales)
                    guardarOActualizar(nuevaNota, notaExistente)
                }

                aplicarFiltro(etBuscar.text.toString())
                tvSinNotas.visibility = if (notas.isEmpty()) View.VISIBLE else View.GONE
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun guardarOActualizar(nueva: Nota, existente: Nota?) {
        if (existente != null) {
            val idx = notas.indexOfFirst { it.id == existente.id }
            if (idx >= 0) notas[idx] = nueva
        } else {
            notas.add(0, nueva)
        }
        guardarNotas()
    }

    // Construye dinámicamente las filas de ítem en el diálogo
    private fun refrescarItemsEnDialog(
        contenedor: LinearLayout,
        items: MutableList<Pair<String, Boolean>>
    ) {
        contenedor.removeAllViews()
        items.forEachIndexed { index, (texto, marcado) ->
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }
            val cb = CheckBox(this).apply {
                isChecked = marcado
                setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFFFD700.toInt()))
                setOnCheckedChangeListener { _, checked -> items[index] = Pair(items[index].first, checked) }
            }
            val et = EditText(this).apply {
                setText(texto)
                textSize = 14f
                setTextColor(0xFFDDDDDD.toInt())
                setHintTextColor(0xFF444444.toInt())
                hint = "Ítem ${index + 1}"
                background = resources.getDrawable(R.drawable.input_background, null)
                setPadding(16, 8, 8, 8)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        items[index] = Pair(s?.toString() ?: "", items[index].second)
                    }
                })
            }
            val btnBorrar = TextView(this).apply {
                text = "✕"
                textSize = 16f
                setTextColor(0xFFFF5555.toInt())
                setPadding(16, 0, 8, 0)
                setOnClickListener {
                    items.removeAt(index)
                    refrescarItemsEnDialog(contenedor, items)
                }
            }
            fila.addView(cb)
            fila.addView(et)
            fila.addView(btnBorrar)
            contenedor.addView(fila)
        }
    }

    // ── Diálogo cambiar contraseña ────────────────────────────────────────────

    private fun mostrarDialogoCambiarContrasena() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cambiar_contrasena, null)
        val etActual    = dialogView.findViewById<EditText>(R.id.etContrasenaActual)
        val etNueva     = dialogView.findViewById<EditText>(R.id.etContrasenaNueva)
        val etConfirmar = dialogView.findViewById<EditText>(R.id.etContrasenaConfirmar)
        val d = AlertDialog.Builder(this)
            .setTitle(null).setView(dialogView)
            .setPositiveButton("Cambiar") { _, _ ->
                SonidoHelper.reproducir(this)
                val prefs = getSharedPreferences(PREFS_PASS, Context.MODE_PRIVATE)
                val actual = prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD)
                when {
                    etActual.text.toString() != actual ->
                        Toast.makeText(this, "❌ Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
                    etNueva.text.toString().isEmpty() ->
                        Toast.makeText(this, "⚠️ La nueva contraseña no puede estar vacía", Toast.LENGTH_SHORT).show()
                    etNueva.text.toString() != etConfirmar.text.toString() ->
                        Toast.makeText(this, "❌ Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                    else -> {
                        prefs.edit().putString(KEY_PASSWORD, etNueva.text.toString()).apply()
                        Toast.makeText(this, "✅ Contraseña cambiada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null).create()
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        d.show()
    }

    // ── Exportar PDF ──────────────────────────────────────────────────────────

    private fun exportarPdf() {
        if (notas.isEmpty()) {
            Toast.makeText(this, "No hay notas para exportar", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val document  = PdfDocument()
            val pageWidth = 595; val pageHeight = 842
            val margin    = 40f; val lineHeight  = 22f
            val pTitPag   = Paint().apply { color = Color.BLACK; textSize = 22f; isFakeBoldText = true }
            val pFechaExp = Paint().apply { color = Color.GRAY;  textSize = 13f }
            val pTitNota  = Paint().apply { color = Color.BLACK; textSize = 16f; isFakeBoldText = true }
            val pFechaNota= Paint().apply { color = Color.GRAY;  textSize = 12f }
            val pContenido= Paint().apply { color = Color.DKGRAY; textSize = 13f }
            val pLinea    = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
            val pItem     = Paint().apply { color = Color.DKGRAY; textSize = 13f }
            val pItemTach = Paint().apply { color = Color.GRAY; textSize = 13f
                flags = Paint.STRIKE_THRU_TEXT_FLAG }

            var pageNumber = 1
            var pageInfo   = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page       = document.startPage(pageInfo)
            var canvas: Canvas = page.canvas
            var y = margin + 10f

            canvas.drawText("Block de Notas", margin, y, pTitPag); y += lineHeight + 4f
            val fechaExp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Exportado el $fechaExp  |  ${notas.size} notas", margin, y, pFechaExp)
            y += lineHeight
            canvas.drawLine(margin, y, pageWidth - margin, y, pLinea); y += lineHeight + 8f

            fun nuevaPaginaSiNecesario() {
                if (y > pageHeight - margin) {
                    document.finishPage(page); pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo); canvas = page.canvas; y = margin + 10f
                }
            }

            for (nota in notas) {
                canvas.drawText(nota.titulo.ifEmpty { "Sin título" }.take(70), margin, y, pTitNota); y += lineHeight
                canvas.drawText(nota.fecha, margin, y, pFechaNota); y += lineHeight
                nuevaPaginaSiNecesario()

                if (nota.tipo == TipoNota.LISTA) {
                    nota.items.forEach { item ->
                        val prefijo = if (item.marcado) "✓  " else "○  "
                        val paint   = if (item.marcado) pItemTach else pItem
                        canvas.drawText("$prefijo${item.texto}", margin + 10f, y, paint)
                        y += lineHeight; nuevaPaginaSiNecesario()
                    }
                } else {
                    val anchoMax = pageWidth - margin * 2 - 20f
                    nota.contenido.split("\n").forEach { lineaOriginal ->
                        if (lineaOriginal.isBlank()) { y += lineHeight * 0.6f; nuevaPaginaSiNecesario(); return@forEach }
                        var lineaActual = ""
                        lineaOriginal.split(" ").forEach { palabra ->
                            val candidata = if (lineaActual.isEmpty()) palabra else "$lineaActual $palabra"
                            if (pContenido.measureText(candidata) > anchoMax) {
                                canvas.drawText(lineaActual, margin + 10f, y, pContenido)
                                y += lineHeight; lineaActual = palabra; nuevaPaginaSiNecesario()
                            } else lineaActual = candidata
                        }
                        if (lineaActual.isNotEmpty()) {
                            canvas.drawText(lineaActual, margin + 10f, y, pContenido)
                            y += lineHeight; nuevaPaginaSiNecesario()
                        }
                    }
                }
                y += 8f; canvas.drawLine(margin, y, pageWidth - margin, y, pLinea); y += 18f
            }

            document.finishPage(page)
            val dir     = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val archivo = File(dir, "notas_${System.currentTimeMillis()}.pdf")
            document.writeTo(FileOutputStream(archivo)); document.close()
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", archivo)
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Abrir PDF con..."
            ))
            Toast.makeText(this, "✅ PDF exportado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ── Persistencia ──────────────────────────────────────────────────────────

    private fun guardarNotas() {
        val prefs = getSharedPreferences(PREFS_NOTAS, Context.MODE_PRIVATE)
        val array = JSONArray()
        notas.forEach { nota ->
            val obj = JSONObject().apply {
                put("id", nota.id)
                put("titulo", nota.titulo)
                put("contenido", nota.contenido)
                put("fecha", nota.fecha)
                put("tipo", nota.tipo.name)
                val itemsArr = JSONArray()
                nota.items.forEach { item ->
                    itemsArr.put(JSONObject().apply {
                        put("texto", item.texto)
                        put("marcado", item.marcado)
                    })
                }
                put("items", itemsArr)
            }
            array.put(obj)
        }
        prefs.edit().putString("notas", array.toString()).apply()
    }

    private fun cargarNotas() {
        val prefs = getSharedPreferences(PREFS_NOTAS, Context.MODE_PRIVATE)
        notas.clear()
        val array = JSONArray(prefs.getString("notas", "[]") ?: "[]")
        for (i in 0 until array.length()) {
            val obj   = array.getJSONObject(i)
            val tipo  = try { TipoNota.valueOf(obj.optString("tipo", "TEXTO")) } catch (e: Exception) { TipoNota.TEXTO }
            val items = mutableListOf<ItemCheckbox>()
            val itemsArr = obj.optJSONArray("items")
            if (itemsArr != null) {
                for (j in 0 until itemsArr.length()) {
                    val it = itemsArr.getJSONObject(j)
                    items.add(ItemCheckbox(it.getString("texto"), it.getBoolean("marcado")))
                }
            }
            notas.add(Nota(
                obj.getLong("id"), obj.getString("titulo"),
                obj.optString("contenido", ""), obj.getString("fecha"),
                tipo, items
            ))
        }
        adapter.notifyDataSetChanged()
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

class NotasAdapter(
    private val notas: List<Nota>,
    private val onVer: (Nota) -> Unit,
    private val onEditar: (Nota) -> Unit,
    private val onEliminar: (Nota) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<NotasAdapter.ViewHolder>() {

    private var mostrarDescripcion = true

    fun setMostrarDescripcion(mostrar: Boolean) {
        mostrarDescripcion = mostrar
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nota, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(notas[position])
    override fun getItemCount() = notas.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(nota: Nota) {
            val tvTitulo            = itemView.findViewById<TextView>(R.id.tvNotaTitulo)
            val tvFecha             = itemView.findViewById<TextView>(R.id.tvNotaFecha)
            val tvContenido         = itemView.findViewById<TextView>(R.id.tvNotaContenido)
            val tvTipo              = itemView.findViewById<TextView>(R.id.tvTipoNota)
            val dragHandle          = itemView.findViewById<TextView>(R.id.ivDragHandle)
            val contenedorPreview   = itemView.findViewById<LinearLayout>(R.id.contenedorPreviewLista)

            tvTitulo.text = nota.titulo.ifEmpty { "Sin título" }
            tvFecha.text  = nota.fecha

            // Badge tipo
            tvTipo.text = if (nota.tipo == TipoNota.LISTA) "☑️" else "📝"

            // Drag handle
            dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }

            // Contenido según tipo
            if (nota.tipo == TipoNota.LISTA) {
                tvContenido.visibility = View.GONE
                if (mostrarDescripcion) {
                    contenedorPreview.visibility = View.VISIBLE
                    contenedorPreview.removeAllViews()
                    // Mostrar máx 3 items como preview
                    nota.items.take(3).forEach { item ->
                        val cb = CheckBox(itemView.context).apply {
                            text = item.texto
                            isChecked = item.marcado
                            isEnabled = false
                            setTextColor(if (item.marcado) 0xFF888888.toInt() else 0xFF8888AA.toInt())
                            setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFFFD700.toInt()))
                            textSize = 13f
                            paintFlags = if (item.marcado)
                                paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                            else
                                paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        }
                        contenedorPreview.addView(cb)
                    }
                    if (nota.items.size > 3) {
                        val tvMas = TextView(itemView.context).apply {
                            text = "  +${nota.items.size - 3} más..."
                            textSize = 12f
                            setTextColor(0xFF444466.toInt())
                        }
                        contenedorPreview.addView(tvMas)
                    }
                } else {
                    contenedorPreview.visibility = View.GONE
                }
            } else {
                contenedorPreview.visibility = View.GONE
                tvContenido.text = nota.contenido
                tvContenido.visibility = if (mostrarDescripcion) View.VISIBLE else View.GONE
            }

            itemView.setOnClickListener { onVer(nota) }
            itemView.findViewById<Button>(R.id.btnEditarNota).setOnClickListener { onEditar(nota) }
            itemView.findViewById<Button>(R.id.btnEliminarNota).setOnClickListener { onEliminar(nota) }
        }
    }
}