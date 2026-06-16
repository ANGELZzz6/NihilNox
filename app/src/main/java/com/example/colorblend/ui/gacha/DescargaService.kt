package com.example.colorblend.ui.gacha

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class DescargaService : Service() {

    inner class DescargaBinder : Binder() {
        fun getService(): DescargaService = this@DescargaService
    }

    private val binder = DescargaBinder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var onProgreso: ((procesados: Int, total: Int, cancionActual: String) -> Unit)? = null
    var onTerminado: ((exitosos: Int, omitidos: Int, fallidos: Int, carpeta: String) -> Unit)? = null
    var onFaseChanged: ((fase: FaseDescarga) -> Unit)? = null

    var estado: EstadoDescarga = EstadoDescarga.IDLE
        private set
    var faseActual: FaseDescarga = FaseDescarga.IDLE
        private set

    var totalCanciones = 0
    var procesados = 0
    var exitosos = 0
    var omitidos = 0
    var fallidos = 0
    var nombrePlaylist = ""
    var cancionActual = ""
    var terminado = false
    var mensajeFase = ""
    var _cancionesPendientes: List<YoutubeCancion> = emptyList()
    var _fallidosPendientes: List<String> = emptyList()

    // Flag para saber si fase1 terminó pero fase2 aún no inició
    var fasePendienteTransferencia = false

    companion object {
        const val CHANNEL_ID = "descarga_channel"
        const val NOTIF_ID = 42
        var instancia: DescargaService? = null
    }

    enum class EstadoDescarga { IDLE, DESCARGANDO, TERMINADO, ERROR }
    enum class FaseDescarga { IDLE, OBTENIENDO_INFO, TRANSFIRIENDO, TERMINADO }

    override fun onCreate() {
        super.onCreate()
        instancia = this
        crearCanal()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, crearNotificacion("Preparando...", 0, 0))
        return START_NOT_STICKY
    }

    fun iniciarObtencionInfo(
        playlistUrl: String,
        context: android.content.Context
    ) {
        if (estado == EstadoDescarga.DESCARGANDO) return
        estado = EstadoDescarga.DESCARGANDO
        faseActual = FaseDescarga.OBTENIENDO_INFO
        fasePendienteTransferencia = false
        // Resetear contadores para detección correcta al reconectar
        procesados = 0; exitosos = 0; omitidos = 0; fallidos = 0
        mensajeFase = "El servidor está descargando de YouTube..."
        actualizarNotificacion("Descargando de YouTube...", 0, 0)

        scope.launch {
            withContext(Dispatchers.Main) { onFaseChanged?.invoke(faseActual) }

            val resultado = YoutubeExtractor.obtenerInfoPlaylist(
                context = context,
                playlistUrl = playlistUrl
            ) { msg ->
                mensajeFase = msg
                actualizarNotificacion(msg, 0, 0)
                scope.launch(Dispatchers.Main) {
                    onProgreso?.invoke(0, 0, msg)
                }
            }

            val canciones = resultado.first
            val fallidosYt = resultado.second
            _cancionesPendientes = canciones
            _fallidosPendientes = fallidosYt
            totalCanciones = canciones.size
            nombrePlaylist = canciones.firstOrNull()?.rutaColab
                ?.split("/")?.getOrNull(3) ?: "Playlist"

            // Marcar que fase1 terminó y espera fase2
            faseActual = FaseDescarga.TERMINADO
            estado = EstadoDescarga.TERMINADO
            fasePendienteTransferencia = true
            terminado = true

            actualizarNotificacion("Listo — ${canciones.size} canciones", totalCanciones, totalCanciones)
            stopForeground(false)

            withContext(Dispatchers.Main) {
                onFaseChanged?.invoke(faseActual)
                onTerminado?.invoke(
                    canciones.size, fallidosYt.size, 0,
                    YoutubeExtractor.crearCarpetaPlaylist(context, nombrePlaylist).absolutePath
                )
            }
        }
    }

    fun iniciarTransferencia(
        carpeta: java.io.File,
        playlist: String,
        forzar: Boolean,
        context: android.content.Context
    ) {
        val canciones = _cancionesPendientes
        if (canciones.isEmpty()) return

        estado = EstadoDescarga.DESCARGANDO
        faseActual = FaseDescarga.TRANSFIRIENDO
        fasePendienteTransferencia = false
        terminado = false
        totalCanciones = canciones.size
        procesados = 0; exitosos = 0; omitidos = 0; fallidos = 0
        nombrePlaylist = playlist

        actualizarNotificacion("Transfiriendo al teléfono...", 0, totalCanciones)

        scope.launch {
            withContext(Dispatchers.Main) { onFaseChanged?.invoke(faseActual) }

            canciones.forEach { cancion ->
                val archivoLocal = java.io.File(
                    carpeta, "${YoutubeExtractor.limpiarNombre(cancion.titulo)}.m4a"
                )

                if (!forzar && archivoLocal.exists()) {
                    omitidos++
                } else {
                    cancionActual = cancion.titulo
                    actualizarNotificacion(cancion.titulo, procesados, totalCanciones)

                    val archivo = YoutubeExtractor.descargarAudio(
                        context       = context,
                        rutaColab     = cancion.rutaColab,
                        nombreArchivo = cancion.titulo,
                        carpeta       = carpeta,
                        onProgreso    = { }
                    )
                    if (archivo != null) exitosos++ else fallidos++
                }

                procesados++
                val p = procesados; val t = totalCanciones; val c = cancionActual
                withContext(Dispatchers.Main) { onProgreso?.invoke(p, t, c) }
                actualizarNotificacion(cancionActual, procesados, totalCanciones)
            }

            estado = EstadoDescarga.TERMINADO
            faseActual = FaseDescarga.TERMINADO
            fasePendienteTransferencia = false
            terminado = true
            val carpetaPath = carpeta.absolutePath

            withContext(Dispatchers.Main) {
                onTerminado?.invoke(exitosos, omitidos, fallidos, carpetaPath)
            }

            mostrarNotificacionFinal(exitosos, fallidos)
            stopForeground(false)
            stopSelf()
        }
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID, "Descarga de música",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Progreso de descarga de playlists" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    private fun crearNotificacion(texto: String, progreso: Int, total: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, DescargarPlaylistActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Descargando música")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .apply {
                if (total > 0) setProgress(total, progreso, false)
                else setProgress(0, 0, true)
            }
            .build()
    }

    private fun actualizarNotificacion(texto: String, progreso: Int, total: Int) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, crearNotificacion(texto, progreso, total))
    }

    private fun mostrarNotificacionFinal(exitosos: Int, fallidos: Int) {
        val texto = if (fallidos == 0) "$exitosos canciones descargadas ✓"
        else "$exitosos descargadas, $fallidos fallaron"
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Descarga completada")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID + 1, notif)
    }

    override fun onDestroy() {
        instancia = null
        scope.cancel()
        super.onDestroy()
    }
}