package com.example.colorblend.ui.gacha

import android.animation.ValueAnimator
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.Habito
import kotlinx.coroutines.*

class BurbujaHabitoService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var isCompleted = false
    private var habitoId: Int = -1
    private var habitoNombre: String = ""
    private var habitoActual: Habito? = null
    
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val CHANNEL_ID = "burbuja_habito_channel"
    private var autoCloseJob: Job? = null
    private var pulseAnimator: ValueAnimator? = null
    private var moveAnimator: ValueAnimator? = null

    override fun onCreate() {
        super.onCreate()
        crearNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        habitoId = intent?.getIntExtra("habito_id", -1) ?: -1
        habitoNombre = intent?.getStringExtra("habito_nombre") ?: "Hábito"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hábito activo: $habitoNombre")
            .setContentText("Tu burbuja de recordatorio está en pantalla")
            .setSmallIcon(R.drawable.ic_sparkles)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1001, notification)
        
        scope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            habitoActual = db.habitoDao().getById(habitoId)
            withContext(Dispatchers.Main) {
                windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                mostrarBurbuja()
                iniciarAnimacionMovimiento()
                iniciarAnimacionPulsacion()
                reiniciarTemporizadorCierre()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun mostrarBurbuja() {
        if (bubbleView != null) {
            try {
                windowManager.removeView(bubbleView)
            } catch (e: Exception) {}
        }
        
        val size = (80 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        val inflater = LayoutInflater.from(this)
        bubbleView = inflater.inflate(R.layout.layout_bubble, null)
        
        aplicarApariencia()

        bubbleView?.setOnClickListener { 
            reiniciarTemporizadorCierre()
            if (!isCompleted) {
                marcarComoCompletado()
            } else {
                confirmarYCerrar()
            }
        }
        
        windowManager.addView(bubbleView, params)
    }

    private fun aplicarApariencia() {
        val h = habitoActual ?: return
        val view = bubbleView ?: return

        // Aplicar color de fondo
        val colorFondo = try {
            Color.parseColor(h.burbujaColor)
        } catch (e: Exception) {
            Color.parseColor("#FFD700")
        }
        
        view.findViewById<ImageView>(R.id.ivBubbleFondo)
            .backgroundTintList = ColorStateList.valueOf(colorFondo)

        // Aplicar texto o imagen
        val usarImagen = h.burbujaUsarImagen && h.burbujaImagenUri != null
        val tvNombre = view.findViewById<TextView>(R.id.tvBubbleNombre)
        val ivImagen = view.findViewById<ImageView>(R.id.ivBubbleImagen)

        tvNombre.apply {
            visibility = if (usarImagen) View.GONE else View.VISIBLE
            text = h.burbujaTexto?.takeIf { it.isNotBlank() } ?: h.nombre
            
            // contraste automático según luminosidad del color
            val luminancia = (0.299 * Color.red(colorFondo) +
                              0.587 * Color.green(colorFondo) +
                              0.114 * Color.blue(colorFondo)) / 255
            setTextColor(if (luminancia > 0.5) Color.parseColor("#121212") else Color.WHITE)
        }

        ivImagen.apply {
            if (usarImagen) {
                visibility = View.VISIBLE
                try {
                    val uri = Uri.parse(h.burbujaImagenUri)
                    setImageURI(uri)
                } catch (e: Exception) {
                    visibility = View.GONE
                    tvNombre.visibility = View.VISIBLE
                }
            } else {
                visibility = View.GONE
            }
        }
    }
    
    private fun marcarComoCompletado() {
        isCompleted = true
        
        // Animación de escala al completar
        bubbleView?.animate()
            ?.scaleX(1.2f)
            ?.scaleY(1.2f)
            ?.setDuration(200)
            ?.withEndAction {
                bubbleView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(200)?.start()
            }
            ?.start()

        bubbleView?.findViewById<ImageView>(R.id.ivBubbleFondo)?.backgroundTintList = 
            ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        
        bubbleView?.findViewById<ImageView>(R.id.ivBubbleCheck)?.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setDuration(300).start()
        }
        
        bubbleView?.findViewById<TextView>(R.id.tvBubbleNombre)?.visibility = View.GONE
        bubbleView?.findViewById<ImageView>(R.id.ivBubbleImagen)?.visibility = View.GONE
    }
    
    private fun confirmarYCerrar() {
        scope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = HabitosRepository(db.habitoDao(), db.registroHabitoDao(), db.identidadDao())
            val habito = db.habitoDao().getById(habitoId)
            habito?.let {
                repository.marcarCompletado(it)
                HabitoAlarmManager.programarBurbuja(applicationContext, it)
                WidgetHabitos.forzarActualizacion(applicationContext)
            }
            withContext(Dispatchers.Main) {
                stopSelf()
            }
        }
    }
    
    private fun iniciarAnimacionMovimiento() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        moveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10000 // un poco más lento
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        
        moveAnimator?.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val bubble = bubbleView ?: return@addUpdateListener
            val params = bubble.layoutParams as WindowManager.LayoutParams
            
            params.x = (screenWidth * 0.05 + (screenWidth * 0.8 * fraction)).toInt()
            params.y = (screenHeight * 0.2 + (Math.sin(fraction.toDouble() * Math.PI * 4) * 150).toInt()).toInt()
            
            try {
                windowManager.updateViewLayout(bubble, params)
            } catch (e: Exception) {}
        }
        
        moveAnimator?.start()
    }

    private fun iniciarAnimacionPulsacion() {
        pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.05f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        pulseAnimator?.addUpdateListener { anim ->
            val scale = anim.animatedValue as Float
            bubbleView?.scaleX = scale
            bubbleView?.scaleY = scale
        }
        pulseAnimator?.start()
    }

    private fun reiniciarTemporizadorCierre() {
        autoCloseJob?.cancel()
        autoCloseJob = scope.launch {
            delay(2 * 60 * 60 * 1000) // 2 horas
            stopSelf()
        }
    }

    private fun crearNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Recordatorios de Burbuja",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        moveAnimator?.cancel()
        pulseAnimator?.cancel()
        bubbleView?.let { 
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        job.cancel()
    }
    
    override fun onBind(intent: Intent?) = null
}
