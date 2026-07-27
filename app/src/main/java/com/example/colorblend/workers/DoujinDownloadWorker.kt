package com.example.colorblend.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.DoujinRepository
import com.example.colorblend.utils.DoujinUtils
import com.example.colorblend.network.MangaDexApi
import com.example.colorblend.network.NHentaiApi
import com.example.colorblend.network.NekobotApi
import com.example.colorblend.network.YandereApi
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream

class DoujinDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "doujin_downloads"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val doujinId = inputData.getString("DOUJIN_ID") ?: return androidx.work.ListenableWorker.Result.failure()
        val source = inputData.getString("DOUJIN_SOURCE") ?: return androidx.work.ListenableWorker.Result.failure()
        val title = inputData.getString("DOUJIN_TITLE") ?: "Doujin"
        val mediaId = inputData.getString("MEDIA_ID")

        val db = AppDatabase.getDatabase(applicationContext)
        val repo = DoujinRepository(
            dao = db.doujinDao(),
            mangaDexApi = Retrofit.Builder()
                .baseUrl("https://api.mangadex.org/")
                .client(DoujinUtils.commonOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MangaDexApi::class.java),
            nHentaiApi = Retrofit.Builder()
                .baseUrl("https://nhentai.net/")
                .client(DoujinUtils.commonOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NHentaiApi::class.java),
            yandereApi = Retrofit.Builder()
                .baseUrl("https://yande.re/")
                .client(DoujinUtils.commonOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(YandereApi::class.java),
            nekobotApi = Retrofit.Builder()
                .baseUrl("https://nekobot.xyz/")
                .client(DoujinUtils.commonOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NekobotApi::class.java)
        )

        try {
            setForeground(createForegroundInfo(title, 0))
            repo.updateDownloadStatus(doujinId, "DOWNLOADING", 0)
            
            val pages = try {
                if (source == "MangaDex") {
                    repo.getMangaDexPages(doujinId)
                } else if (source == "Yande.re" || source == "Gifs Real") {
                    repo.getYanderePages(mediaId ?: "")
                } else {
                    val apiKey = com.example.colorblend.data.local.ApiKeysManager.getNhentaiKey(applicationContext)
                    repo.getNHentaiPages(doujinId, apiKey, mediaId)
                }
            } catch (e: Exception) {
                Log.e("DOWNLOAD_WORKER", "Failed to get pages for $title", e)
                return androidx.work.ListenableWorker.Result.retry()
            }

            if (pages.isEmpty()) throw Exception("No pages found")

            val rootDir = File(applicationContext.filesDir, "doujins/$doujinId")
            if (!rootDir.exists()) rootDir.mkdirs()

            pages.forEachIndexed { index, url ->
                val fileName = "page_${index + 1}"
                var success = false
                
                // Rotación de extensiones para nHentai si falla 404
                val extensions = if (source == "nHentai") {
                    val originalExt = url.substringAfterLast(".")
                    listOf(originalExt) + (DoujinUtils.EXTENSIONS_ROTATION.filter { it != originalExt })
                } else {
                    listOf(url.substringAfterLast("."))
                }

                for (ext in extensions) {
                    val targetUrl = if (source == "nHentai") {
                        val base = url.substringBeforeLast(".")
                        "$base.$ext"
                    } else url
                    
                    val tempFile = File(rootDir, "$fileName.$ext")
                    try {
                        downloadFile(targetUrl, tempFile, doujinId, source)
                        success = true
                        break
                    } catch (e: Exception) {
                        Log.w("DOWNLOAD_WORKER", "Failed to download $targetUrl ($ext): ${e.message}")
                        if (tempFile.exists()) tempFile.delete()
                    }
                }

                if (!success) {
                    Log.e("DOWNLOAD_WORKER", "All extensions failed for page ${index + 1}")
                    return androidx.work.ListenableWorker.Result.retry()
                }
                
                val progress = ((index + 1).toFloat() / pages.size * 100).toInt()
                repo.updateDownloadStatus(doujinId, "DOWNLOADING", progress)
                
                // Update notification and worker progress
                setForeground(createForegroundInfo(title, progress))
                setProgress(workDataOf("PROGRESS" to progress))
            }

            repo.updateDownloadStatus(doujinId, "COMPLETED", 100, rootDir.absolutePath)
            return androidx.work.ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e("DOWNLOAD_WORKER", "Error downloading $title", e)
            repo.updateDownloadStatus(doujinId, "ERROR", 0)
            return androidx.work.ListenableWorker.Result.failure()
        }
    }

    private fun downloadFile(url: String, outputFile: File, doujinId: String, source: String) {
        val requestBuilder = Request.Builder().url(url)
        
        // Inyectar Referer específico para nHentai para evitar 403/404
        if (source == "nHentai") {
            requestBuilder.header("Referer", "https://nhentai.net/g/$doujinId/")
        } else if (source == "MangaDex") {
            requestBuilder.header("Referer", "https://mangadex.org/")
        } else if (source == "Yande.re") {
            requestBuilder.header("Referer", "https://yande.re/")
        } else if (source == "Gifs Real") {
            requestBuilder.header("Referer", "https://nekobot.xyz/")
        }

        DoujinUtils.commonOkHttpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to download $url (Code: ${response.code})")
            val body = response.body ?: throw Exception("Empty body")
            FileOutputStream(outputFile).use { output ->
                body.byteStream().copyTo(output)
            }
        }
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Doujin Downloads", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Descargando: $title")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
