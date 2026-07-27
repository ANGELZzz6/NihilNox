package com.example.colorblend.utils

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object DoujinUtils {

    // User-Agent de navegador móvil real actualizado para evitar bloqueos
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
    private const val REFERER_BASE = "https://nhentai.net/"

    // Lista de subdominios más estables
    private val NHENTAI_CDNS = listOf("t")
    private val NHENTAI_IMAGE_CDNS = listOf("i")
    
    val EXTENSIONS_ROTATION = listOf("webp", "jpg", "png", "gif")

    val commonOkHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging) // Diagnóstico de red
            .addInterceptor { chain ->
                val request = chain.request()
                val requestBuilder = request.newBuilder()
                    .header("User-Agent", USER_AGENT)
                
                // No sobreescribir Accept si ya viene de Retrofit (ej. application/json)
                if (request.header("Accept") == null) {
                    requestBuilder.header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                }
                
                // Si la URL es de nHentai e inyectar el Referer
                if (request.url.host.contains("nhentai.net")) {
                    // Si no tiene Referer, inyectar el base o uno dinámico si lo detectamos en la URL
                    if (request.header("Referer") == null) {
                        requestBuilder.header("Referer", REFERER_BASE)
                    }
                }
                
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    /**
     * Genera una URL de nHentai con un subdominio estable
     */
    fun getNHentaiImageUrl(mediaId: String, fileName: String, ext: String, isThumbnail: Boolean): String {
        val cdns = if (isThumbnail) NHENTAI_CDNS else NHENTAI_IMAGE_CDNS
        val sub = cdns[Random.nextInt(cdns.size)]
        return "https://$sub.nhentai.net/galleries/$mediaId/$fileName.$ext"
    }

    fun getGlideUrl(url: String, galleryId: String? = null): Any {
        if (url.isBlank()) return url
        
        // Referer estricto: nHentai requiere la URL de la galería para desbloquear las imágenes
        // Inyectamos también cabeceras para MangaDex para evitar bloqueos por bot
        val referer = if (url.contains("nhentai.net")) {
            if (galleryId != null && galleryId.all { it.isDigit() }) {
                "https://nhentai.net/g/$galleryId/"
            } else {
                "https://nhentai.net/"
            }
        } else if (url.contains("mangadex")) {
            "https://mangadex.org/"
        } else if (url.contains("yande.re")) {
            "https://yande.re/"
        } else if (url.contains("nekobot.xyz")) {
            "https://nekobot.xyz/"
        } else {
            REFERER_BASE
        }
        
        return GlideUrl(
            url,
            LazyHeaders.Builder()
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", referer)
                .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .build()
        )
    }

    fun mapExtension(t: String?): String {
        return when (t?.lowercase()) {
            "p" -> "png"
            "g" -> "gif"
            "w" -> "webp"
            "j" -> "jpg"
            else -> "jpg"
        }
    }
}
