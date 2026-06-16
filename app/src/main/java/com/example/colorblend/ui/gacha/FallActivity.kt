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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.colorblend.R
import java.io.File

class FallActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: VideoAdapter
    private lateinit var loadingPopup: View
    private lateinit var tvLoadingStatus: TextView
    private val videoFiles = mutableListOf<File>()
    
    companion object {
        private const val TAG = "FallActivity"
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

        cargarVideos()

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

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        super.onDestroy()
    }

    inner class VideoAdapter(private val videos: List<File>) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {
        
        inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val videoView: VideoView = view.findViewById(R.id.videoView)
            val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_fall, parent, false)
            return VideoViewHolder(view)
        }

        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            val file = videos[position]
            holder.progressBar.visibility = View.VISIBLE
            
            try {
                holder.videoView.setVideoURI(Uri.fromFile(file))
                
                holder.videoView.setOnPreparedListener { mp ->
                    holder.progressBar.visibility = View.GONE
                    mp.isLooping = true
                    
                    val videoRatio = mp.videoWidth / mp.videoHeight.toFloat()
                    val screenRatio = holder.videoView.width / holder.videoView.height.toFloat()
                    val scale = videoRatio / screenRatio
                    if (scale >= 1f) holder.videoView.scaleX = scale else holder.videoView.scaleY = 1f / scale
                    
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
    }
}
