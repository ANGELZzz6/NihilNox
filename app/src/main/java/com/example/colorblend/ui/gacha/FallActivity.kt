package com.example.colorblend.ui.gacha

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.VideoView
import android.widget.Toast
import android.widget.TextView
import android.widget.ImageView
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.lifecycle.lifecycleScope
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.FallVideo
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FallActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: VideoAdapter
    private lateinit var loadingPopup: View
    private lateinit var tvLoadingStatus: TextView
    private lateinit var tvCategoryBadge: TextView
    private val videoFiles = mutableListOf<File>()
    
    companion object {
        private const val TAG = "FallActivity"
        private const val SWIPE_THRESHOLD = 250
        private const val SWIPE_VELOCITY_THRESHOLD = 300
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Broadcast recibido: ${intent?.action}")
            if (intent?.action == "FALL_VIDEO_READY") {
                val success = intent.getBooleanExtra("success", false)
                
                // Forzar UI update en hilo principal
                runOnUiThread {
                    loadingPopup.visibility = View.GONE
                    if (success) {
                        cargarVideos()
                        adapter.notifyDataSetChanged()
                        if (videoFiles.isNotEmpty()) {
                            viewPager.setCurrentItem(0, false)
                        }
                        Toast.makeText(this@FallActivity, "✅ Video listo", Toast.LENGTH_SHORT).show()
                    } else {
                        val error = intent.getStringExtra("error") ?: "Error desconocido"
                        Toast.makeText(this@FallActivity, "❌ $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FullScreenHelper.enable(this)
        setContentView(R.layout.activity_fall)

        viewPager = findViewById(R.id.viewPagerVideos)
        loadingPopup = findViewById(R.id.loadingLayout)
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus)
        tvCategoryBadge = findViewById(R.id.tvCategoryBadge)

        val categoria = intent.getStringExtra("video_category") ?: "RANDOM"
        
        if (categoria == "RANDOM") {
            cargarVideos()
        } else {
            cargarVideosPorCategoria(categoria)
        }

        adapter = VideoAdapter(videoFiles)
        viewPager.adapter = adapter

        // Registro de receiver con flags de seguridad
        val filter = IntentFilter("FALL_VIDEO_READY")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { handleSharedText(it) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Actualiza el intent de la actividad
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { handleSharedText(it) }
        }
    }

    private fun handleSharedText(text: String) {
        Log.d(TAG, "Manejando texto compartido: $text")
        if (text.contains("instagram.com") || text.contains("http")) {
            loadingPopup.visibility = View.VISIBLE
            tvLoadingStatus.text = "Analizando enlace..."
            
            val downloadIntent = Intent(this, FallDescargaService::class.java).apply {
                action = FallDescargaService.ACTION_START_DOWNLOAD
                putExtra(FallDescargaService.EXTRA_URL, text)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(downloadIntent)
            } else {
                startService(downloadIntent)
            }

            // Regresar a la app anterior inmediatamente
            moveTaskToBack(true)
            Toast.makeText(this, "⬇ Descargando en segundo plano...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarVideos() {
        val folder = File(getExternalFilesDir(null), "FALL")
        if (!folder.exists()) folder.mkdirs()
        
        val files = folder.listFiles { file -> 
            file.isFile && (file.extension == "mp4" || file.extension == "mkv")
        }
        
        videoFiles.clear()
        if (files != null && files.isNotEmpty()) {
            // Ordenar por fecha para encontrar el último
            val sorted = files.sortedByDescending { it.lastModified() }.toMutableList()
            
            // EL PLAN: El primero siempre es el último descargado.
            // El resto de la lista se baraja para que sea al azar.
            val ultimoDescargado = sorted.removeAt(0)
            val restoAlAzar = sorted.shuffled()
            
            videoFiles.add(ultimoDescargado)
            videoFiles.addAll(restoAlAzar)
            
            Log.d(TAG, "Videos cargados: ${videoFiles.size}. Primero: ${ultimoDescargado.name}")
        }
        
        findViewById<View>(R.id.tvEmpty).visibility = if (videoFiles.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun cargarVideosPorCategoria(categoria: String) {
        val folder = File(getExternalFilesDir(null), "FALL")
        if (!folder.exists()) folder.mkdirs()
        
        val archivosFisicos = folder.listFiles { file -> 
            file.isFile && (file.extension == "mp4" || file.extension == "mkv")
        } ?: emptyArray()

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(this@FallActivity).fallVideoDao()
            val videosFinales = mutableListOf<File>()

            when (categoria) {
                "NEWS" -> {
                    val enRoom = dao.getNewsVideos().map { File(it.filePath) }
                    val rutasEnRoom = dao.getAllVideos().map { it.filePath }.toSet()
                    val sinRegistro = archivosFisicos.filter { it.absolutePath !in rutasEnRoom }
                    
                    videosFinales.addAll(enRoom)
                    videosFinales.addAll(sinRegistro)
                }
                "FUEGO", "REFLEXION" -> {
                    val principales = dao.getVideosByCategory(categoria).map { File(it.filePath) }
                    videosFinales.addAll(principales)
                    
                    // Fallback si hay pocos o terminan
                    val otros = dao.getVideosByOtherCategory(categoria).map { File(it.filePath) }
                    videosFinales.addAll(otros)
                }
            }

            // Filtrar solo los que existen de verdad físicamente
            val existentes = videosFinales.filter { it.exists() }.distinctBy { it.absolutePath }

            withContext(Dispatchers.Main) {
                videoFiles.clear()
                videoFiles.addAll(existentes)
                adapter.notifyDataSetChanged()
                actualizarUIBadge(categoria)
                findViewById<View>(R.id.tvEmpty).visibility = if (videoFiles.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun actualizarUIBadge(categoria: String) {
        tvCategoryBadge.visibility = View.VISIBLE
        when (categoria) {
            "FUEGO" -> {
                tvCategoryBadge.text = "🔥 FUEGO"
                tvCategoryBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF5722"))
            }
            "REFLEXION" -> {
                tvCategoryBadge.text = "💜 REFLEXIÓN"
                tvCategoryBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7C4DFF"))
            }
            "NEWS" -> {
                tvCategoryBadge.text = "✨ NUEVOS"
                tvCategoryBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2A2A2A"))
            }
            else -> tvCategoryBadge.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        super.onDestroy()
    }

    inner class VideoAdapter(private val videos: List<File>) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {
        
        private val dao = AppDatabase.getDatabase(this@FallActivity).fallVideoDao()

        inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val videoView: VideoView = view.findViewById(R.id.videoView)
            val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
            val iconFuego: ImageView = view.findViewById(R.id.iconFuego)
            val iconReflexion: ImageView = view.findViewById(R.id.iconReflexion)
            val overlay: View = view.findViewById(R.id.overlayClasificacion)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_fall, parent, false)
            return VideoViewHolder(view)
        }

        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            val file = videos[position]
            holder.progressBar.visibility = View.VISIBLE
            
            // Reset visuales por el reciclaje
            holder.iconFuego.alpha = 0.3f
            holder.iconFuego.scaleX = 1f
            holder.iconFuego.scaleY = 1f
            holder.iconReflexion.alpha = 0.3f
            holder.iconReflexion.scaleX = 1f
            holder.iconReflexion.scaleY = 1f

            // Consultar si ya tiene categoría
            lifecycleScope.launch(Dispatchers.IO) {
                val existing = dao.getVideoByPath(file.absolutePath)
                withContext(Dispatchers.Main) {
                    existing?.let { 
                        if (it.category == "FUEGO") holder.iconFuego.alpha = 0.6f
                        if (it.category == "REFLEXION") holder.iconReflexion.alpha = 0.6f
                    }
                }
            }

            configurarGestos(holder, file)

            try {
                holder.videoView.setVideoURI(Uri.fromFile(file))
                
                holder.videoView.setOnPreparedListener { mp ->
                    holder.progressBar.visibility = View.GONE
                    mp.isLooping = true

                    val videoRatio = mp.videoWidth.toFloat() / mp.videoHeight.toFloat()
                    val parent = holder.videoView.parent as View
                    val parentWidth = parent.width
                    val parentHeight = parent.height
                    val parentRatio = parentWidth.toFloat() / parentHeight.toFloat()

                    val layoutParams = holder.videoView.layoutParams
                    if (videoRatio > parentRatio) {
                        layoutParams.width = parentWidth
                        layoutParams.height = (parentWidth / videoRatio).toInt()
                    } else {
                        layoutParams.height = parentHeight
                        layoutParams.width = (parentHeight * videoRatio).toInt()
                    }
                    holder.videoView.layoutParams = layoutParams
                    
                    mp.start()
                }
                
                holder.videoView.setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "Error en VideoView: $file")
                    holder.progressBar.visibility = View.GONE
                    try { if (file.exists()) file.delete() } catch (e: Exception) {}
                    
                    val currentPos = holder.bindingAdapterPosition
                    if (currentPos != RecyclerView.NO_POSITION && currentPos < videoFiles.size) {
                        videoFiles.removeAt(currentPos)
                        notifyItemRemoved(currentPos)
                        if (videoFiles.isEmpty()) findViewById<View>(R.id.tvEmpty).visibility = View.VISIBLE
                    }
                    true
                }
            } catch (e: Exception) {
                holder.progressBar.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = videos.size

        private fun configurarGestos(holder: VideoViewHolder, file: File) {
            val detector = GestureDetector(this@FallActivity, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    val deltaX = e2.x - (e1?.x ?: e2.x)
                    val deltaY = e2.y - (e1?.y ?: e2.y)

                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
                        if (Math.abs(deltaX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (deltaX > 0) {
                                clasificarVideo(holder, file, "FUEGO")
                            } else {
                                clasificarVideo(holder, file, "REFLEXION")
                            }
                            return true
                        }
                    }
                    return false
                }

                override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                    val deltaX = e2.x - (e1?.x ?: e2.x)
                    val deltaY = e2.y - (e1?.y ?: e2.y)

                    if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 80) {
                        holder.itemView.parent.requestDisallowInterceptTouchEvent(true)
                        actualizarVisualesSwipe(holder, deltaX)
                        return true
                    }
                    return false
                }
            })

            holder.overlay.setOnTouchListener { _, event ->
                val result = detector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                    resetearIconos(holder)
                }
                result
            }
        }

        private fun actualizarVisualesSwipe(holder: VideoViewHolder, deltaX: Float) {
            val progress = (Math.abs(deltaX) / 400f).coerceIn(0f, 1f)
            val alpha = 0.3f + (progress * 0.7f)
            val scale = 1f + (progress * 0.3f)

            if (deltaX > 0) { // FUEGO
                holder.iconFuego.alpha = alpha
                holder.iconFuego.scaleX = scale
                holder.iconFuego.scaleY = scale
                holder.iconReflexion.alpha = 0.3f
                holder.iconReflexion.scaleX = 1f
                holder.iconReflexion.scaleY = 1f
            } else { // REFLEXION
                holder.iconReflexion.alpha = alpha
                holder.iconReflexion.scaleX = scale
                holder.iconReflexion.scaleY = scale
                holder.iconFuego.alpha = 0.3f
                holder.iconFuego.scaleX = 1f
                holder.iconFuego.scaleY = 1f
            }
        }

        private fun resetearIconos(holder: VideoViewHolder) {
            holder.iconFuego.animate().alpha(0.3f).scaleX(1f).scaleY(1f).setDuration(300).start()
            holder.iconReflexion.animate().alpha(0.3f).scaleX(1f).scaleY(1f).setDuration(300).start()
        }

        private fun clasificarVideo(holder: VideoViewHolder, file: File, categoria: String) {
            val icon = if (categoria == "FUEGO") holder.iconFuego else holder.iconReflexion
            val msg = if (categoria == "FUEGO") "🔥 Guardado en Fuego" else "💜 Guardado en Reflexión"

            icon.animate().scaleX(1.5f).scaleY(1.5f).setDuration(200).withEndAction {
                icon.animate().scaleX(1.2f).scaleY(1.2f).alpha(0.8f).setDuration(200).start()
            }.start()

            lifecycleScope.launch(Dispatchers.IO) {
                val existing = dao.getVideoByPath(file.absolutePath)
                if (existing != null) {
                    dao.updateVideo(existing.copy(category = categoria))
                } else {
                    dao.insertVideo(FallVideo(
                        filePath = file.absolutePath,
                        fileName = file.name,
                        category = categoria,
                        dateAdded = file.lastModified()
                    ))
                }
                withContext(Dispatchers.Main) {
                    Snackbar.make(holder.itemView, msg, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}
