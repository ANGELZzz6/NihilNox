package com.example.colorblend.ui.gacha

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.colorblend.data.local.ApiKeysManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class FallDescargaService : Service() {

    inner class FallBinder : Binder() {
        fun getService(): FallDescargaService = this@FallDescargaService
    }

    private val binder = FallBinder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "fall_descarga_channel"
        const val NOTIF_ID = 1001
        const val ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD"
        const val EXTRA_URL = "EXTRA_URL"
        private const val TAG = "FallDescargaService"
    }

    override fun onCreate() {
        super.onCreate()
        crearCanal()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_DOWNLOAD) {
            val url = intent.getStringExtra(EXTRA_URL)
            if (url != null) {
                iniciarDescargaDesdeServidor(url)
            }
        }
        return START_NOT_STICKY
    }

    private fun iniciarDescargaDesdeServidor(urlStr: String) {
        val serverBaseUrl = ApiKeysManager.get(this, ApiKeysManager.KEY_SERVIDOR_URL)
        
        if (serverBaseUrl.isBlank()) {
            errorFinal("Configura la URL de tu servidor Colab en ajustes.")
            return
        }

        Log.d(TAG, "Conectando al servidor Colab: $serverBaseUrl")
        startForeground(NOTIF_ID, crearNotificacion("Anclando al Servidor Colab...", 0))
        
        scope.launch {
            try {
                // 1. Petición al servidor Colab
                val endpoint = if (serverBaseUrl.endsWith("/")) "${serverBaseUrl}download-instagram" 
                              else "$serverBaseUrl/download-instagram"
                
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 60000 // Timeout largo para Colab
                conn.readTimeout = 60000
                conn.doOutput = true

                val payload = JSONObject().apply { put("url", urlStr) }
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }

                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error $responseCode"
                    throw Exception("Servidor Colab: $errorMsg")
                }

                // 2. Recibir binario y guardar
                actualizarNotificacion("Descargando archivo del servidor...", 0)
                
                val folder = File(getExternalFilesDir(null), "FALL")
                if (!folder.exists()) folder.mkdirs()
                val file = File(folder, "video_${System.currentTimeMillis()}.mp4")
                
                val fileLength = conn.contentLength
                val input = conn.inputStream
                val output = FileOutputStream(file)
                
                val data = ByteArray(65536)
                var total: Long = 0
                var count: Int
                var lastProgress = -1
                
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toInt()
                        if (progress != lastProgress) {
                            actualizarNotificacion("Guardando: $progress%", progress)
                            lastProgress = progress
                        }
                    }
                    output.write(data, 0, count)
                }
                
                output.flush()
                output.close()
                input.close()

                Log.d(TAG, "Descarga servidor exitosa: ${file.absolutePath}")
                mostrarNotificacionFinal("¡Video descargado de Instagram!")
                sendBroadcast(Intent("FALL_VIDEO_READY").apply { putExtra("success", true) })
                
            } catch (e: Exception) {
                Log.e(TAG, "Fallo servidor Colab: ${e.message}")
                errorFinal(e.localizedMessage ?: "Error de conexión")
            } finally {
                detenerServicio()
            }
        }
    }

    private fun errorFinal(msg: String) {
        mostrarNotificacionFinal("Error: $msg")
        sendBroadcast(Intent("FALL_VIDEO_READY").apply { 
            putExtra("success", false)
            putExtra("error", msg)
        })
    }

    private fun detenerServicio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
        stopSelf()
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID, "Descargas FALL",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    private fun crearNotificacion(texto: String, progreso: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FALL Downloader (Server)")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progreso, progreso <= 0)
            .build()
    }

    private fun actualizarNotificacion(texto: String, progreso: Int) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, crearNotificacion(texto, progreso))
    }

    private fun mostrarNotificacionFinal(texto: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        // Cancelar la notificación de progreso activa (la de Foreground)
        notificationManager.cancel(NOTIF_ID)

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FALL Downloader")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true) // Se elimina al tocarla
            .setTimeoutAfter(3000) // Se elimina sola tras 3 segundos
            .build()
        
        notificationManager.notify(NOTIF_ID + 1, notif)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
