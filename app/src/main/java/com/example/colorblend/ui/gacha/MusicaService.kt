package com.example.colorblend.ui.gacha

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.colorblend.R
import kotlinx.coroutines.*

class MusicaService : Service() {

    private var cancionesCompletas: List<String> = emptyList()
    private var mediaSession: MediaSessionCompat? = null

    // Audio Effects
    var equalizer: Equalizer? = null
    var bassBoost: BassBoost? = null
    var virtualizer: Virtualizer? = null
    var presetReverb: PresetReverb? = null

    private var mediaPlayer: MediaPlayer? = null
    private var canciones: List<String> = emptyList()
    private var indiceActual = 0
    private var shuffleActivo = false
    private var indicesShuffle: MutableList<Int> = mutableListOf()
    private var posicionShuffle = 0
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var playbackJob: Job? = null

    var onCancionCambiada: ((String, Boolean) -> Unit)? = null
    var onShuffleChanged: ((Boolean) -> Unit)? = null
    var onLoadingChanged: ((Boolean) -> Unit)? = null

    fun getCanciones(): List<String> = canciones
    fun getCancionesCompletas(): List<String> = cancionesCompletas
    fun getIndiceActual(): Int = indiceActual
    fun isShuffle(): Boolean = shuffleActivo

    fun agregarCancion(uri: String) {
        if (!cancionesCompletas.contains(uri)) cancionesCompletas = cancionesCompletas + uri
        if (canciones.size == cancionesCompletas.size - 1) canciones = canciones + uri
    }

    fun eliminarCancion(uri: String) {
        val eraLaActual = canciones.isNotEmpty() && canciones[indiceActual] == uri
        val indiceEliminada = canciones.indexOf(uri)

        canciones = canciones.filter { it != uri }
        cancionesCompletas = cancionesCompletas.filter { it != uri }

        if (shuffleActivo) {
            indicesShuffle.remove(indiceEliminada)
            indicesShuffle = indicesShuffle.map { if (it > indiceEliminada) it - 1 else it }.toMutableList()
            posicionShuffle = posicionShuffle.coerceIn(0, (indicesShuffle.size - 1).coerceAtLeast(0))
        }

        when {
            canciones.isEmpty() -> {
                liberarReproductor()
                indiceActual = 0
                onCancionCambiada?.invoke("", false)
                actualizarNotificacion()
            }
            eraLaActual -> {
                if (indiceActual >= canciones.size) indiceActual = canciones.size - 1
                reproducir()
            }
            indiceEliminada < indiceActual -> {
                indiceActual--
            }
        }
    }

    companion object {
        const val CHANNEL_ID        = "musica_channel"
        const val NOTIF_ID          = 1
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_SIGUIENTE  = "ACTION_SIGUIENTE"
        const val ACTION_ANTERIOR   = "ACTION_ANTERIOR"
        private const val TAG = "MusicaService"
    }

    // ── Temporizador ──────────────────────────────────────────────────────
    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    var onTimerTick: ((minutosRestantes: Int) -> Unit)? = null
    var onTimerFin: (() -> Unit)? = null

    fun iniciarTemporizador(minutos: Int) {
        cancelarTemporizador()
        var segundosRestantes = minutos * 60
        timerRunnable = object : Runnable {
            override fun run() {
                segundosRestantes--
                if (segundosRestantes % 60 == 0) {
                    onTimerTick?.invoke(segundosRestantes / 60)
                }
                if (segundosRestantes <= 0) {
                    mediaPlayer?.pause()
                    onCancionCambiada?.invoke(nombreCancion(), false)
                    actualizarNotificacion()
                    onTimerFin?.invoke()
                    timerRunnable = null
                } else {
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
        timerHandler.postDelayed(timerRunnable!!, 1000)
    }

    fun cancelarTemporizador() {
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
        timerRunnable = null
    }

    fun timerActivo(): Boolean = timerRunnable != null

    // ── Reproducción por sección ──────────────────────────────────────────
    fun setCancionesSeccion(lista: List<String>, indice: Int = 0) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            canciones = lista.toList()
            indiceActual = indice.coerceIn(0, (canciones.size - 1).coerceAtLeast(0))
            if (shuffleActivo) generarIndicesShuffle()
            reproducir()
        }
    }

    inner class MusicaBinder : Binder() {
        fun getService() = this@MusicaService
    }
    private val binder = MusicaBinder()
    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
        initMediaSession()
        startForeground(NOTIF_ID, construirNotificacion())
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicaService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { togglePlayPause() }
                override fun onPause() { togglePlayPause() }
                override fun onSkipToNext() { siguiente() }
                override fun onSkipToPrevious() { anterior() }
                override fun onSeekTo(pos: Long) { seekTo(pos.toInt()) }
            })
            isActive = true
        }
    }

    private fun updatePlaybackState() {
        val state = if (isPlaying()) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession?.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(state, getPosicion().toLong(), if (isPlaying()) 1f else 0f)
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SEEK_TO)
            .build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            androidx.media.session.MediaButtonReceiver.handleIntent(mediaSession, intent)
            return START_STICKY
        }
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_SIGUIENTE  -> siguiente()
            ACTION_ANTERIOR   -> anterior()
        }
        return START_STICKY
    }

    fun toggleShuffle() {
        shuffleActivo = !shuffleActivo
        if (shuffleActivo) generarIndicesShuffle()
        onShuffleChanged?.invoke(shuffleActivo)
    }

    private fun generarIndicesShuffle() {
        indicesShuffle = canciones.indices.toMutableList().also { it.shuffle() }
        val posActual = indicesShuffle.indexOf(indiceActual)
        if (posActual > -1) {
            indicesShuffle.removeAt(posActual)
            indicesShuffle.add(0, indiceActual)
        }
        posicionShuffle = 0
    }

    fun setCanciones(lista: List<String>, indice: Int = 0) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            canciones = lista.toList()
            indiceActual = indice.coerceIn(0, (canciones.size - 1).coerceAtLeast(0))
            if (shuffleActivo) generarIndicesShuffle()
            reproducir()
        }
    }

    private fun liberarReproductor() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error liberando reproductor", e)
        } finally {
            mediaPlayer = null
        }
    }

    fun reproducir() {
        if (canciones.isEmpty()) return
        
        onLoadingChanged?.invoke(true)
        liberarReproductor()

        val currentUri = try { canciones[indiceActual] } catch (e: Exception) { 
            onLoadingChanged?.invoke(false)
            return 
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                if (currentUri.startsWith("content://")) {
                    setDataSource(applicationContext, android.net.Uri.parse(currentUri))
                } else {
                    setDataSource(currentUri)
                }
                
                setOnPreparedListener { mp ->
                    onLoadingChanged?.invoke(false)
                    
                    // Initialize effects
                    val sessionId = mp.audioSessionId
                    try {
                        equalizer = Equalizer(0, sessionId).apply { enabled = true }
                        bassBoost = BassBoost(0, sessionId).apply { enabled = true }
                        virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
                        presetReverb = PresetReverb(0, sessionId).apply { enabled = true }
                    } catch (e: Exception) {
                        Log.e(TAG, "No se pudieron iniciar los efectos de audio", e)
                    }
                    
                    mp.start()
                    onCancionCambiada?.invoke(nombreCancion(), true)
                    actualizarNotificacion()
                    updatePlaybackState()
                }

                setOnCompletionListener { siguiente() }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Error MediaPlayer: what=$what, extra=$extra")
                    onLoadingChanged?.invoke(false)
                    siguiente()
                    true
                }

                prepareAsync() // Usar asíncrono para no bloquear la UI
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reproduciendo: ${e.message}")
                onLoadingChanged?.invoke(false)
                siguiente()
            }
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                onCancionCambiada?.invoke(nombreCancion(), false)
            } else {
                it.start()
                onCancionCambiada?.invoke(nombreCancion(), true)
            }
            actualizarNotificacion()
            updatePlaybackState()
        } ?: run {
            // Si el reproductor es nulo pero hay canciones, intentar reproducir
            if (canciones.isNotEmpty()) reproducir()
        }
    }

    fun siguiente() {
        if (canciones.isEmpty()) return
        playbackJob?.cancel()
        if (shuffleActivo && indicesShuffle.isNotEmpty()) {
            posicionShuffle = (posicionShuffle + 1) % indicesShuffle.size
            indiceActual = indicesShuffle[posicionShuffle]
        } else {
            indiceActual = (indiceActual + 1) % canciones.size
        }
        reproducir()
    }

    fun anterior() {
        if (canciones.isEmpty()) return
        playbackJob?.cancel()
        if (shuffleActivo && indicesShuffle.isNotEmpty()) {
            posicionShuffle = if (posicionShuffle > 0) posicionShuffle - 1 else indicesShuffle.size - 1
            indiceActual = indicesShuffle[posicionShuffle]
        } else {
            indiceActual = if (indiceActual > 0) indiceActual - 1 else canciones.size - 1
        }
        reproducir()
    }

    fun setPlaybackParams(speed: Float, pitch: Float) {
        mediaPlayer?.let {
            try {
                val params = it.playbackParams
                params.speed = speed
                params.pitch = pitch
                it.playbackParams = params
            } catch (e: Exception) {
                Log.e(TAG, "Error ajustando parámetros de reproducción", e)
            }
        }
    }

    fun isPlaying()   = mediaPlayer?.isPlaying ?: false
    fun getDuracion() = if (mediaPlayer != null) { try { mediaPlayer!!.duration } catch(e: Exception) { 0 } } else 0
    fun getPosicion() = if (mediaPlayer != null) { try { mediaPlayer!!.currentPosition } catch(e: Exception) { 0 } } else 0
    fun seekTo(posicion: Int) { 
        try { mediaPlayer?.seekTo(posicion) } catch (e: Exception) { }
    }

    fun nombreCancion(): String {
        if (canciones.isEmpty()) return ""
        val ruta = try { canciones[indiceActual] } catch (e: Exception) { return "" }
        return if (ruta.startsWith("content://")) {
            try {
                contentResolver.query(android.net.Uri.parse(ruta), null, null, null, null)?.use {
                    if (it.moveToFirst()) {
                        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) it.getString(idx).substringBeforeLast(".") else "Canción"
                    } else "Canción"
                } ?: "Canción"
            } catch (e: Exception) { "Canción" }
        } else {
            ruta.substringAfterLast("/").substringBeforeLast(".")
        }
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID, "Reproductor de música", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    private fun construirNotificacion(
        titulo: String = "Reproductor",
        subtitulo: String = "Listo"
    ) = NotificationCompat.Builder(this, CHANNEL_ID).run {
        val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val playPauseIntent = PendingIntent.getService(this@MusicaService, 0,
            Intent(this@MusicaService, MusicaService::class.java).apply { action = ACTION_PLAY_PAUSE }, flag)
        val siguienteIntent = PendingIntent.getService(this@MusicaService, 1,
            Intent(this@MusicaService, MusicaService::class.java).apply { action = ACTION_SIGUIENTE }, flag)
        val anteriorIntent  = PendingIntent.getService(this@MusicaService, 2,
            Intent(this@MusicaService, MusicaService::class.java).apply { action = ACTION_ANTERIOR }, flag)
        val abrirApp        = PendingIntent.getActivity(this@MusicaService, 0,
            Intent(this@MusicaService, ReproductorActivity::class.java), flag)

        setContentTitle(titulo)
        setContentText(subtitulo)
        setSmallIcon(android.R.drawable.ic_media_play)
        setContentIntent(abrirApp)
        addAction(android.R.drawable.ic_media_previous, "Anterior", anteriorIntent)
        addAction(
            if (isPlaying()) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying()) "Pausar" else "Play", playPauseIntent
        )
        addAction(android.R.drawable.ic_media_next, "Siguiente", siguienteIntent)
        setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2))
        setPriority(NotificationCompat.PRIORITY_LOW)
        build()
    }

    fun actualizarNotificacion() {
        startForeground(NOTIF_ID, construirNotificacion(
            titulo    = nombreCancion().ifEmpty { "Reproductor" },
            subtitulo = if (isPlaying()) "Reproduciendo" else "Pausado"
        ))
    }

    override fun onDestroy() {
        cancelarTemporizador()
        liberarReproductor()
        scope.cancel()
        super.onDestroy()
    }
}
