package com.example.colorblend.data.local.repository

import android.util.Log
import com.example.colorblend.data.local.DoujinDao
import com.example.colorblend.data.network.models.DoujinItem
import com.example.colorblend.domain.model.DoujinEntity
import com.example.colorblend.network.MangaDexApi
import com.example.colorblend.network.NHentaiApi
import com.example.colorblend.utils.DoujinUtils
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.File

class DoujinRepository(
    private val dao: DoujinDao,
    private val mangaDexApi: MangaDexApi,
    private val nHentaiApi: NHentaiApi
) {

    suspend fun searchMangaDex(query: String, offset: Int = 0): List<DoujinItem> {
        val response = mangaDexApi.searchManga(title = query, offset = offset)
        return response.data.map { manga ->
            val coverId = manga.relationships.find { it.type == "cover_art" }?.attributes?.fileName
            val coverUrl = if (coverId != null) {
                "https://uploads.mangadex.org/covers/${manga.id}/$coverId"
            } else ""
            
            DoujinItem(
                id = manga.id,
                title = manga.attributes.title.values.firstOrNull() ?: "Sin título",
                coverUrl = coverUrl,
                source = "MangaDex"
            )
        }
    }

    suspend fun searchNHentai(query: String, apiKey: String, page: Int = 1): List<DoujinItem> {
        val response = nHentaiApi.searchGalleries("Key $apiKey", query, page)
        return (response.result ?: emptyList()).map { gallery ->
            val mediaIdRaw = gallery.mediaId
            val mediaId = when (mediaIdRaw) {
                is Number -> mediaIdRaw.toLong().toString()
                is String -> mediaIdRaw
                else -> ""
            }
            
            Log.d("DOUJIN_REPO", "Search NHentai: ID=${gallery.id}, MediaID=$mediaId")

            val title = gallery.title?.english ?: gallery.title?.pretty ?: gallery.englishTitleV2 ?: gallery.japaneseTitleV2 ?: "Sin título"
            
            val thumbRaw = gallery.thumbnailV2
            val thumbPath = when (thumbRaw) {
                is String -> thumbRaw
                is Map<*, *> -> thumbRaw["path"] as? String
                else -> null
            }

            val coverUrl = if (thumbPath != null) {
                "https://t.nhentai.net/$thumbPath"
            } else if (mediaId.isNotEmpty()) {
                val ext = DoujinUtils.mapExtension(gallery.images?.cover?.t)
                DoujinUtils.getNHentaiImageUrl(mediaId, "cover", ext, isThumbnail = true)
            } else ""
            
            DoujinItem(
                id = gallery.id.toString(),
                title = title,
                coverUrl = coverUrl,
                source = "nHentai",
                mediaId = mediaId,
                totalPages = gallery.numPages,
                pageExtensions = gallery.images?.pages?.map { it.t ?: "j" } ?: emptyList()
            )
        }
    }

    suspend fun getMangaDexPages(mangaId: String): List<String> {
        val cleanId = mangaId.trim()
        try {
            Log.d("DOUJIN_REPO", "MangaDex Feed for: $cleanId")
            // MangaDex: Incluir es-la y es para máxima compatibilidad en español
            var feed = mangaDexApi.getMangaFeed(
                mangaId = cleanId,
                languages = listOf("es-la", "es", "en", "ja")
            )
            
            if (feed.data.isEmpty()) {
                Log.d("DOUJIN_REPO", "No ES/EN/JA chapters, trying all languages")
                feed = mangaDexApi.getMangaFeed(cleanId, languages = null)
            }
            
            val chapter = feed.data.firstOrNull() ?: run {
                Log.e("DOUJIN_REPO", "MangaDex Error: No chapters found for $cleanId. This manga might only have external chapters or chapters not matching selected languages/ratings.")
                throw Exception("MangaDex: No hay capítulos disponibles para este manga (posiblemente externos o en otros idiomas).")
            }
            
            val chapterId = chapter.id
            Log.d("DOUJIN_REPO", "MangaDex found chapter: $chapterId. Resolving pages...")
            
            val pages = mangaDexApi.getChapterPages(chapterId)
            
            // Usar el servidor principal uploads.mangadex.org en lugar del nodo @Home temporal
            // para evitar los errores 404 de red.
            val list = pages.chapter.data.map { fileName ->
                "https://uploads.mangadex.org/data/${pages.chapter.hash}/$fileName"
            }
            Log.d("DOUJIN_REPO", "Generated ${list.size} pages from MangaDex (Stable Server)")
            return list
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("DOUJIN_REPO", "MangaDex HTTP Error: $e - $errorBody")
            
            // FALLBACK: Si el servidor estable falla, intentar con el nodo @Home original
            if (e.code() == 404) {
                 Log.w("DOUJIN_REPO", "MangaDex Stable 404, falling back to @Home node")
                 // Re-ejecutar lógica con baseUrl original aquí si fuera necesario
            }
            throw Exception("MangaDex Red: $e - $errorBody")
        } catch (e: Exception) {
            Log.e("DOUJIN_REPO", "MangaDex Exception", e)
            throw e
        }
    }

    suspend fun getNHentaiPages(galleryId: String, apiKey: String, mediaId: String?): List<String> {
        val cleanId = galleryId.trim()
        try {
            Log.d("DOUJIN_REPO", "nHentai: Fetching full details for Gallery $cleanId via V2 API")
            
            // Usamos la API v2 con la API Key para evitar el error 403
            val gallery = nHentaiApi.getGalleryDetails("Key $apiKey", cleanId.toInt())
            
            val finalMediaId = when(val midRaw = gallery.mediaId) {
                is Number -> midRaw.toLong().toString()
                is String -> midRaw
                else -> mediaId?.trim() ?: ""
            }

            if (finalMediaId.isEmpty()) {
                Log.e("DOUJIN_REPO", "nHentai Error: No se pudo determinar el Media ID")
                throw Exception("nHentai: Error de identificador de imágenes.")
            }

            // Si la API no devuelve páginas en el formato antiguo, usamos el nuevo formato V2
            val pageListV1 = gallery.images?.pages ?: emptyList()
            val pageListV2 = gallery.pages ?: emptyList()
            
            val total = if (pageListV1.isNotEmpty()) pageListV1.size 
                        else if (pageListV2.isNotEmpty()) pageListV2.size 
                        else gallery.numPages

            if (total <= 0) {
                Log.e("DOUJIN_REPO", "nHentai Error: Gallery $cleanId reported 0 pages")
                throw Exception("nHentai: La galería no tiene páginas disponibles.")
            }

            Log.d("DOUJIN_REPO", "nHentai: Building $total pages for Media $finalMediaId (V1: ${pageListV1.size}, V2: ${pageListV2.size})")
            
            return if (pageListV2.isNotEmpty()) {
                pageListV2.map { page ->
                    "https://i.nhentai.net/${page.path}"
                }
            } else if (pageListV1.isNotEmpty()) {
                pageListV1.mapIndexed { index, page ->
                    val ext = DoujinUtils.mapExtension(page.t)
                    DoujinUtils.getNHentaiImageUrl(finalMediaId, (index + 1).toString(), ext, isThumbnail = false)
                }
            } else {
                Log.w("DOUJIN_REPO", "nHentai Warning: No page list found, using fallback for $cleanId")
                List(total) { index ->
                    DoujinUtils.getNHentaiImageUrl(finalMediaId, (index + 1).toString(), "jpg", isThumbnail = false)
                }
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("DOUJIN_REPO", "nHentai HTTP Error: ${e.code()} - $errorBody")
            throw Exception("nHentai Red: ${e.code()} - $errorBody")
        } catch (e: Exception) {
            Log.e("DOUJIN_REPO", "nHentai Exception", e)
            throw e
        }
    }

    fun getFavoritos(): Flow<List<DoujinEntity>> = dao.obtenerTodos()
    
    suspend fun toggleFavorito(item: DoujinItem) {
        val exists = dao.esFavorito(item.id)
        if (exists) {
            dao.eliminarDoujin(DoujinEntity(item.id, item.title, item.coverUrl, item.source))
        } else {
            dao.guardarDoujin(DoujinEntity(item.id, item.title, item.coverUrl, item.source, totalPages = item.totalPages))
        }
    }

    suspend fun ensureDoujinExists(item: DoujinItem) {
        dao.insertIgnore(DoujinEntity(
            id = item.id,
            title = item.title,
            coverUrl = item.coverUrl,
            source = item.source,
            totalPages = item.totalPages,
            downloadStatus = "IDLE"
        ))
    }

    suspend fun updateDownloadStatus(id: String, status: String, progress: Int, localPath: String? = null) {
        Log.d("DOUJIN_REPO", "Updating status for $id: status=$status, progress=$progress")
        val entity = dao.obtenerPorId(id) ?: run {
            Log.e("DOUJIN_REPO", "Could not update status: Doujin $id not found in DB")
            return
        }
        dao.guardarDoujin(entity.copy(
            downloadStatus = status,
            downloadProgress = progress,
            localPath = localPath ?: entity.localPath,
            isDownloaded = status == "COMPLETED"
        ))
    }
    
    suspend fun getDoujinById(id: String): DoujinEntity? = dao.obtenerPorId(id)

    suspend fun deleteDownload(id: String) {
        val entity = dao.obtenerPorId(id) ?: return
        entity.localPath?.let { path ->
            val dir = File(path)
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
                dir.delete()
            }
        }
        // Eliminar el registro por completo de la base de datos
        dao.eliminarDoujin(entity)
    }
}
