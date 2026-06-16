package com.example.colorblend.ui.gacha

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.ImagenGenerada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class PerchanceWebViewActivity : AppCompatActivity() {

    private var carpetaId = 0
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perchance_webview)

        carpetaId = intent.getIntExtra("carpetaId", 0)

        webView = findViewById(R.id.perchanceWebView)
        val btnGuardar = findViewById<TextView>(R.id.btnGuardarImagen)

        // Configurar WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()

        // Cargar Perchance
        webView.loadUrl("https://perchance.org/ai-text-to-image-generator")

        // Botón guardar — captura la imagen visible en el WebView
        btnGuardar.setOnClickListener {
            btnGuardar.text = "Guardando..."
            capturarYGuardarImagen(btnGuardar)
        }
    }

    private fun capturarYGuardarImagen(btnGuardar: TextView) {
        lifecycleScope.launch {
            try {
                // Intentar obtener URL de imagen generada desde el DOM
                webView.evaluateJavascript(
                    """
                    (function() {
                        // Buscar la imagen generada en Perchance
                        var img = document.querySelector('img[src*="blob:"], img[src*="data:"], .image-container img, #output img');
                        if (img) return img.src;
                        // Buscar canvas
                        var canvas = document.querySelector('canvas');
                        if (canvas) return canvas.toDataURL('image/png');
                        return null;
                    })()
                    """.trimIndent()
                ) { result ->
                    if (result != null && result != "null") {
                        val src = result.trim('"')
                        lifecycleScope.launch {
                            guardarImagenDesdeUrl(src, btnGuardar)
                        }
                    } else {
                        // Fallback — capturar screenshot del WebView
                        capturarScreenshot(btnGuardar)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@PerchanceWebViewActivity,
                    "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                btnGuardar.text = "💾 Guardar"
            }
        }
    }

    private fun capturarScreenshot(btnGuardar: TextView) {
        try {
            webView.isDrawingCacheEnabled = true
            val bitmap = webView.drawingCache
            if (bitmap != null) {
                val fileName = "gen_${System.currentTimeMillis()}.jpg"
                val file = File(filesDir, fileName)
                val fos = FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
                fos.close()
                webView.isDrawingCacheEnabled = false

                lifecycleScope.launch {
                    guardarEnRoom(file.absolutePath, btnGuardar)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al capturar", Toast.LENGTH_SHORT).show()
            btnGuardar.text = "💾 Guardar"
        }
    }

    private suspend fun guardarImagenDesdeUrl(src: String, btnGuardar: TextView) {
        withContext(Dispatchers.IO) {
            try {
                val fileName = "gen_${System.currentTimeMillis()}.jpg"
                val file = File(filesDir, fileName)

                when {
                    src.startsWith("data:image") -> {
                        // Base64
                        val base64 = src.substringAfter("base64,")
                        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                        file.writeBytes(bytes)
                    }
                    src.startsWith("http") -> {
                        // URL normal
                        val bytes = URL(src).readBytes()
                        file.writeBytes(bytes)
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            capturarScreenshot(btnGuardar)
                        }
                        return@withContext
                    }
                }

                withContext(Dispatchers.Main) {
                    guardarEnRoom(file.absolutePath, btnGuardar)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    capturarScreenshot(btnGuardar)
                }
            }
        }
    }

    private suspend fun guardarEnRoom(ruta: String, btnGuardar: TextView) {
        val dao = AppDatabase.getDatabase(applicationContext).imagenGeneradaDao()
        dao.insert(ImagenGenerada(
            carpetaId = carpetaId,
            rutaLocal = ruta
        ))
        Toast.makeText(this, "✅ Imagen guardada", Toast.LENGTH_SHORT).show()
        btnGuardar.text = "💾 Guardar"
        finish()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}