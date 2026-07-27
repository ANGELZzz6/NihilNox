package com.example.colorblend.ui.gacha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.ApiKeysManager
import com.example.colorblend.data.local.repository.DoujinRepository
import com.example.colorblend.data.network.models.*
import com.example.colorblend.domain.model.DoujinEntity
import com.example.colorblend.network.MangaDexApi
import com.example.colorblend.network.NHentaiApi
import com.example.colorblend.utils.DoujinUtils
import com.example.colorblend.workers.DoujinDownloadWorker
import androidx.work.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DoujinViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    
    private val repo = DoujinRepository(
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
            .create(NHentaiApi::class.java)
    )

    private val _searchResults = MutableStateFlow<List<DoujinItem>>(emptyList())
    val searchResults: StateFlow<List<DoujinItem>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var lastQuery = ""
    private var lastSource = ""
    private var currentPage = 1
    private var currentOffset = 0

    val favoritos: StateFlow<List<DoujinEntity>> = repo.getFavoritos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun search(query: String, source: String) {
        lastQuery = query
        lastSource = source
        currentPage = 1
        currentOffset = 0
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (source == "MangaDex") {
                    _searchResults.value = repo.searchMangaDex(query, offset = 0)
                } else {
                    val apiKey = ApiKeysManager.getNhentaiKey(getApplication())
                    if (apiKey.isNotBlank()) {
                        _searchResults.value = repo.searchNHentai(query, apiKey, page = 1)
                    } else {
                        _searchResults.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoading.value || lastQuery.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val nextItems = if (lastSource == "MangaDex") {
                    currentOffset += 20
                    repo.searchMangaDex(lastQuery, offset = currentOffset)
                } else {
                    currentPage += 1
                    val apiKey = ApiKeysManager.getNhentaiKey(getApplication())
                    if (apiKey.isNotBlank()) {
                        repo.searchNHentai(lastQuery, apiKey, page = currentPage)
                    } else emptyList()
                }
                
                if (nextItems.isNotEmpty()) {
                    _searchResults.value = _searchResults.value + nextItems
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getPages(id: String, source: String, mediaId: String? = null): List<String> {
        return try {
            if (source == "MangaDex") {
                repo.getMangaDexPages(id)
            } else {
                val apiKey = ApiKeysManager.getNhentaiKey(getApplication())
                repo.getNHentaiPages(id, apiKey, mediaId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun toggleFavorito(item: DoujinItem) {
        viewModelScope.launch {
            repo.toggleFavorito(item)
        }
    }

    fun startDownload(item: DoujinItem) {
        viewModelScope.launch {
            repo.ensureDoujinExists(item)
            val workRequest = OneTimeWorkRequestBuilder<DoujinDownloadWorker>()
                .setInputData(workDataOf(
                    "DOUJIN_ID" to item.id,
                    "DOUJIN_SOURCE" to item.source,
                    "DOUJIN_TITLE" to item.title,
                    "MEDIA_ID" to item.mediaId
                ))
                .addTag("download_${item.id}")
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            
            WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                "download_${item.id}",
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    suspend fun getDoujinById(id: String): DoujinEntity? = repo.getDoujinById(id)

    fun deleteDownload(item: DoujinItem) {
        viewModelScope.launch {
            repo.deleteDownload(item.id)
            WorkManager.getInstance(getApplication()).cancelUniqueWork("download_${item.id}")
        }
    }
}
