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
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
    
    private var anchorX: Int = 100
    private var anchorY: Int = 500

    override fun onCreate() {
        super.onCreate()
        crearNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val newHabitoId = intent?.getIntExtra("habito_id", -1) ?: -1
        
        // Si ya hay una burbuja del MISMO hábito, ignorar el comando para evitar parpadeos
        if (newHabitoId == habitoId && bubbleView != null) {
            return START_NOT_STICKY
        }

        habitoId = newHabitoId
        habitoNombre = intent?.getStringExtra("habito_nombre") ?: "Hábito"
        val forceNow = intent?.getBooleanExtra("force_now", false) ?: false

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hábito activo: $habitoNombre")
            .setContentText("Tu burbuja de recordatorio está en pantalla")
            .setSmallIcon(R.drawable.ic_sparkles)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1001, notification)
        
        if (forceNow) {
            // Posición central para lanzamientos manuales
            anchorX = (resources.displayMetrics.widthPixels / 2) - (40 * resources.displayMetrics.density).toInt()
            anchorY = (resources.displayMetrics.heightPixels / 2) - (40 * resources.displayMetrics.density).toInt()
        }

        scope.launch(Dispatchers.IO) {
            if (habitoId == -99) {
                habitoActual = Habito(
                    id = -99,
                    nombre = "Hábito de Prueba",
                    burbujaTexto = "PRUEBA",
                    burbujaColor = "#E9C400",
                    enabledBurbuja = true
                )
            } else {
                val db = AppDatabase.getDatabase(applicationContext)
                habitoActual = db.habitoDao().getById(habitoId)
            }
            
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
            x = anchorX
            y = anchorY
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

        // Lógica de Arrastre (Drag)
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        bubbleView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moveAnimator?.pause()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    anchorX = initialX + (event.rawX - initialTouchX).toInt()
                    anchorY = initialY + (event.rawY - initialTouchY).toInt()
                    params.x = anchorX
                    params.y = anchorY
                    try {
                        windowManager.updateViewLayout(bubbleView, params)
                    } catch (e: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = Math.abs(event.rawX - initialTouchX)
                    val diffY = Math.abs(event.rawY - initialTouchY)
                    if (diffX < 10 && diffY < 10) {
                        v.performClick()
                    } else {
                        // Reanudar flotación en la nueva posición
                        moveAnimator?.resume()
                    }
                    true
                }
                else -> false
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
        if (habitoId == -99) {
            stopSelf()
            return
        }
        scope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = HabitosRepository(db.habitoDao(), db.registroHabitoDao(), db.identidadDao())
            val habito = db.habitoDao().getById(habitoId)
            habito?.let {
                repository.marcarCompletado(it)
                HabitoAlarmManager.programarBurbuja(applicationContext, it)
                WidgetHabitos.forzarActualizacion(applicationContext)
                WidgetLifeStream.forzarActualizacion(applicationContext)
            }
            withContext(Dispatchers.Main) {
                stopSelf()
            }
        }
    }
    
    private fun iniciarAnimacionMovimiento() {
        moveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4000 
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        
        moveAnimator?.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val bubble = bubbleView ?: return@addUpdateListener
            val params = bubble.layoutParams as WindowManager.LayoutParams
            
            // Movimiento suave "bobbing" (flotación local)
            // Se mueve +-15dp horizontalmente y +-25dp verticalmente desde su ancla
            val rangeX = (10 * resources.displayMetrics.density).toInt()
            val rangeY = (20 * resources.displayMetrics.density).toInt()
            
            params.x = anchorX + (Math.sin(fraction.toDouble() * Math.PI * 2) * rangeX).toInt()
            params.y = anchorY + (Math.cos(fraction.toDouble() * Math.PI * 2) * rangeY).toInt()
            
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

        // REPROGRAMAR si se cerró sin completar (y no es de prueba)
        if (!isCompleted && habitoId != -99 && habitoId != -1) {
            HabitoAlarmManager.reprogramarParaMasTarde(applicationContext, habitoId)
        }

        bubbleView?.let { 
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        job.cancel()
    }
    
    override fun onBind(intent: Intent?) = null
}
