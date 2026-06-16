package com.example.colorblend.ui.gacha

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.colorblend.R

class IncognitoWebViewActivity : AppCompatActivity() {

    // ─── Modelo de pestaña ───────────────────────────────────────────────────
    data class Pestana(
        val webView: WebView,
        var titulo: String = "Nueva pestaña",
        var url: String = ""
    )

    private val pestanas = mutableListOf<Pestana>()
    private var pestanaActual = 0

    private lateinit var contenedor: FrameLayout
    private lateinit var layoutPestanas: LinearLayout
    private lateinit var scrollPestanas: HorizontalScrollView
    private lateinit var tvUrl: TextView
    private lateinit var btnAtras: ImageButton
    private lateinit var btnAdelante: ImageButton
    private lateinit var progressBar: ProgressBar
    private var notaTitulo = ""
    private var notaContenido = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incognito_webview)

        notaTitulo    = intent.getStringExtra("notaTitulo") ?: ""
        notaContenido = intent.getStringExtra("notaContenido") ?: ""
        contenedor    = findViewById(R.id.contenedorWebViews)
        layoutPestanas = findViewById(R.id.layoutPestanas)
        scrollPestanas = findViewById(R.id.scrollPestanas)
        tvUrl          = findViewById(R.id.tvUrl)
        btnAtras       = findViewById(R.id.btnAtras)
        btnAdelante    = findViewById(R.id.btnAdelante)
        progressBar    = findViewById(R.id.progressBar)

        btnAtras.setOnClickListener {
            pestanaActual().webView.let { if (it.canGoBack()) it.goBack() }
        }
        btnAdelante.setOnClickListener {
            pestanaActual().webView.let { if (it.canGoForward()) it.goForward() }
        }
        findViewById<ImageButton>(R.id.btnNuevaPestana).setOnClickListener {
            val urlsNota = extraerUrls(notaContenido)
            if (urlsNota.isEmpty()) {
                // No hay URLs en la nota — abrir Google directo
                abrirNuevaPestana("https://google.com")
                return@setOnClickListener
            }

            val opciones = mutableListOf("🌐 Nueva pestaña vacía")
            opciones.addAll(urlsNota.map { it.take(50) })

            android.app.AlertDialog.Builder(this)
                .setTitle("Abrir nueva pestaña")
                .setItems(opciones.toTypedArray()) { _, which ->
                    if (which == 0) {
                        abrirNuevaPestana("https://google.com")
                    } else {
                        val url = urlsNota[which - 1]
                        val urlFinal = if (url.startsWith("http")) url else "https://$url"
                        abrirNuevaPestana(urlFinal)
                    }
                }
                .show()
        }
        findViewById<ImageButton>(R.id.btnCerrar).setOnClickListener {
            finish()
        }

        val urlInicial = intent.getStringExtra("url") ?: "https://google.com"
        abrirNuevaPestana(urlInicial)
    }

    // ─── Crear nueva pestaña ─────────────────────────────────────────────────

    private fun abrirNuevaPestana(url: String) {
        val webView = crearWebView()
        val pestana = Pestana(webView = webView, url = url)
        pestanas.add(pestana)

        contenedor.addView(webView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        webView.loadUrl(url)
        cambiarAPestana(pestanas.size - 1)
    }

    // ─── Cambiar pestaña activa ───────────────────────────────────────────────

    private fun cambiarAPestana(index: Int) {
        pestanaActual = index

        // Mostrar solo el WebView de la pestaña activa
        for (i in pestanas.indices) {
            pestanas[i].webView.visibility =
                if (i == index) View.VISIBLE else View.GONE
        }

        actualizarBarraPestanas()
        actualizarNavegacion(pestanas[index].webView)
        tvUrl.text = pestanas[index].url.ifEmpty { "Nueva pestaña" }
    }

    // ─── Cerrar pestaña ──────────────────────────────────────────────────────

    private fun cerrarPestana(index: Int) {
        if (pestanas.size == 1) {
            // Última pestaña — cerrar activity
            finish()
            return
        }

        val pestana = pestanas[index]
        contenedor.removeView(pestana.webView)
        pestana.webView.destroy()
        pestanas.removeAt(index)

        val nuevoIndex = if (index >= pestanas.size) pestanas.size - 1 else index
        cambiarAPestana(nuevoIndex)
    }

    // ─── Barra de pestañas ───────────────────────────────────────────────────

    private fun actualizarBarraPestanas() {
        layoutPestanas.removeAllViews()

        for (i in pestanas.indices) {
            val pestana = pestanas[i]
            val esActiva = i == pestanaActual

            // Contenedor de pestaña
            val tabView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(12, 0, 8, 0)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(if (esActiva) Color.parseColor("#2A2A4A") else Color.parseColor("#111122"))
                    setStroke(1, if (esActiva) Color.parseColor("#FFD700") else Color.parseColor("#333355"))
                    cornerRadius = 6f
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply { setMargins(3, 4, 3, 4) }
            }

            // Título de la pestaña
            val tvTitulo = TextView(this).apply {
                text = pestana.titulo.take(16).ifEmpty { "Nueva pestaña" }
                textSize = 11f
                setTextColor(if (esActiva) Color.parseColor("#FFD700") else Color.parseColor("#AAAACC"))
                maxWidth = 140
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            // Botón cerrar pestaña
            val btnCerrarTab = TextView(this).apply {
                text = " ✕"
                textSize = 11f
                setTextColor(Color.parseColor("#FF5555"))
                setPadding(4, 0, 4, 0)
            }

            tabView.addView(tvTitulo)
            tabView.addView(btnCerrarTab)

            val capturedIndex = i
            tabView.setOnClickListener { cambiarAPestana(capturedIndex) }
            btnCerrarTab.setOnClickListener { cerrarPestana(capturedIndex) }

            layoutPestanas.addView(tabView)
        }

        // Scroll automático a la pestaña activa
        scrollPestanas.post {
            val tabWidth = 170
            scrollPestanas.smoothScrollTo(pestanaActual * tabWidth, 0)
        }
    }

    // ─── Crear WebView configurado ────────────────────────────────────────────

    private fun crearWebView(): WebView {
        val wv = WebView(this)

        android.webkit.CookieManager.getInstance().apply {
            setAcceptCookie(false)
            removeAllCookies(null)
        }

        wv.settings.apply {
            javaScriptEnabled    = true
            domStorageEnabled    = true   // ← cambiar false a true (necesario para videos)
            databaseEnabled      = false
            saveFormData         = false
            cacheMode            = WebSettings.LOAD_NO_CACHE
            setSupportZoom(true)
            builtInZoomControls  = true
            displayZoomControls  = false
            loadWithOverviewMode = true
            useWideViewPort      = true
            mixedContentMode     = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            mediaPlaybackRequiresUserGesture = false  // ← añadir: permite autoplay de video
            setSupportMultipleWindows(true)
        }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                if (view == pestanaActual().webView) {
                    tvUrl.text = url
                    pestanas[pestanaActual].url = url
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                val index = pestanas.indexOfFirst { it.webView == view }
                if (index >= 0) {
                    pestanas[index].titulo = view.title?.take(20) ?: url
                    pestanas[index].url = url
                }
                if (view == pestanaActual().webView) {
                    tvUrl.text = url
                    actualizarNavegacion(view)
                    actualizarBarraPestanas()
                }
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (view == pestanaActual().webView) {
                    if (newProgress < 100) {
                        progressBar.visibility = View.VISIBLE
                        progressBar.progress = newProgress
                    } else {
                        progressBar.visibility = View.GONE
                    }
                }
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                val index = pestanas.indexOfFirst { it.webView == view }
                if (index >= 0) {
                    pestanas[index].titulo = title.take(20)
                    actualizarBarraPestanas()
                }
            }

            // Abrir links que intentan abrir nueva ventana como nueva pestaña
            override fun onCreateWindow(
                view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?
            ): Boolean {
                val newWebView = WebView(this@IncognitoWebViewActivity)
                newWebView.webViewClient = WebViewClient()
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = newWebView
                resultMsg?.sendToTarget()
                abrirNuevaPestana("about:blank")
                return true
            }
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                // Pantalla completa para videos
                window.decorView.let { decor ->
                    (decor as? android.widget.FrameLayout)?.addView(
                        view, android.widget.FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            }

            override fun onHideCustomView() {
                window.decorView.let { decor ->
                    (decor as? android.widget.FrameLayout)?.let { frame ->
                        if (frame.childCount > 1) frame.removeViewAt(frame.childCount - 1)
                    }
                }
            }
        }

        wv.settings.setSupportMultipleWindows(true)

        return wv
    }

    private fun actualizarNavegacion(wv: WebView) {
        btnAtras.alpha    = if (wv.canGoBack()) 1f else 0.3f
        btnAdelante.alpha = if (wv.canGoForward()) 1f else 0.3f
    }

    private fun pestanaActual(): Pestana = pestanas[pestanaActual]

    // ─── Botón físico atrás ───────────────────────────────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val wv = pestanaActual().webView
            if (wv.canGoBack()) {
                wv.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    // ─── Extraer URLs de la nota ──────────────────────────────────────────────

    private fun extraerUrls(texto: String): List<String> {
        val matcher = android.util.Patterns.WEB_URL.matcher(texto)
        val urls = mutableListOf<String>()
        while (matcher.find()) {
            urls.add(matcher.group())
        }
        return urls.distinct()
    }

    // ─── Limpiar al cerrar ────────────────────────────────────────────────────

    override fun onDestroy() {
        pestanas.forEach { pestana ->
            pestana.webView.clearHistory()
            pestana.webView.clearCache(true)
            pestana.webView.clearFormData()
            pestana.webView.destroy()
        }
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()
        super.onDestroy()
    }
}