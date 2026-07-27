package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.colorblend.R
import com.example.colorblend.utils.DoujinUtils
import kotlinx.coroutines.launch
import android.util.Log
import java.io.File

class DoujinReaderActivity : AppCompatActivity() {

    private val viewModel: DoujinViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doujin_reader)

        val vp = findViewById<ViewPager2>(R.id.viewPager)
        val tvPage = findViewById<TextView>(R.id.tvPageNumber)
        val btnClose = findViewById<View>(R.id.btnClose)
        val loading = findViewById<ProgressBar>(R.id.loadingBar)

        val id = intent.getStringExtra("DOUJIN_ID") ?: ""
        val source = intent.getStringExtra("DOUJIN_SOURCE") ?: ""
        val mediaId = intent.getStringExtra("MEDIA_ID")

        lifecycleScope.launch {
            try {
                loading.visibility = View.VISIBLE
                val entity = viewModel.getDoujinById(id)
                var isLocal = false
                val pageUrls = if (entity != null && entity.isDownloaded && entity.localPath != null) {
                    val dir = File(entity.localPath)
                    if (dir.exists() && dir.isDirectory) {
                        isLocal = true
                        dir.listFiles()?.filter { it.isFile }?.sortedBy { it.name }?.map { it.absolutePath } ?: emptyList()
                    } else {
                        viewModel.getPages(id, source, mediaId)
                    }
                } else {
                    viewModel.getPages(id, source, mediaId)
                }
                
                loading.visibility = View.GONE
                
                if (pageUrls.isNotEmpty()) {
                    val adapter = PageAdapter(pageUrls, isLocal)
                    vp.adapter = adapter
                    
                    vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            tvPage.text = "${position + 1} / ${pageUrls.size}"
                        }
                    })
                    tvPage.text = "1 / ${pageUrls.size}"
                } else {
                    mostrarErrorDiagnostico("No se encontraron páginas. Es posible que el contenido haya sido borrado o no tenga capítulos subidos.")
                }
            } catch (e: Exception) {
                loading.visibility = View.GONE
                mostrarErrorDiagnostico(e.message ?: e.toString())
            }
        }

        btnClose.setOnClickListener { finish() }
    }

    private fun mostrarErrorDiagnostico(mensaje: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Fallo de Carga")
            .setMessage("Detalle técnico:\n\n$mensaje")
            .setPositiveButton("Cerrar") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    inner class PageAdapter(private val urls: List<String>, private val isLocal: Boolean) : RecyclerView.Adapter<PageAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_doujin_page, parent, false)
            return ViewHolder(v)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val path = urls[position]
            if (isLocal) {
                Glide.with(holder.ivPage)
                    .load(File(path))
                    .into(holder.ivPage)
            } else {
                intent.getStringExtra("DOUJIN_ID")?.let { gid ->
                    tryLoadingWithRotation(holder.ivPage, path, gid, 0)
                }
            }
        }

        private fun tryLoadingWithRotation(imageView: ImageView, currentUrl: String, galleryId: String, rotationIndex: Int) {
            if (rotationIndex >= DoujinUtils.EXTENSIONS_ROTATION.size) {
                Log.e("READER_GLIDE", "All extensions failed for $currentUrl")
                imageView.setImageResource(android.R.drawable.stat_notify_error)
                return
            }

            val ext = DoujinUtils.EXTENSIONS_ROTATION[rotationIndex]
            val baseWithoutExt = currentUrl.substringBeforeLast(".")
            val targetUrl = "$baseWithoutExt.$ext"
            val model = DoujinUtils.getGlideUrl(targetUrl, galleryId = galleryId)

            Glide.with(imageView)
                .load(model)
                .placeholder(android.R.drawable.progress_horizontal)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(e: com.bumptech.glide.load.engine.GlideException?, m: Any?, t: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?, isFirst: Boolean): Boolean {
                        Log.w("READER_GLIDE", "Attempt $rotationIndex failed ($ext) for $galleryId. Trying next...")
                        imageView.post {
                            tryLoadingWithRotation(imageView, currentUrl, galleryId, rotationIndex + 1)
                        }
                        return true // Manejamos nosotros el error
                    }
                    override fun onResourceReady(r: android.graphics.drawable.Drawable?, m: Any?, t: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?, d: com.bumptech.glide.load.DataSource?, isFirst: Boolean): Boolean {
                        return false
                    }
                })
                .into(imageView)
        }

        override fun getItemCount() = urls.size
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val ivPage: ImageView = v.findViewById(R.id.ivPage)
        }
    }
}
