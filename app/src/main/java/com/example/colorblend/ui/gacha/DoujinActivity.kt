package com.example.colorblend.ui.gacha

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.colorblend.R
import com.example.colorblend.data.network.models.DoujinItem
import com.example.colorblend.domain.model.DoujinEntity
import com.example.colorblend.utils.DoujinUtils
import kotlinx.coroutines.launch

class DoujinActivity : AppCompatActivity() {

    private val viewModel: DoujinViewModel by viewModels()
    private lateinit var adapter: DoujinAdapter
    private var showingFavoritos = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doujin)

        val rv = findViewById<RecyclerView>(R.id.rvDoujins)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)
        val rgSource = findViewById<RadioGroup>(R.id.rgSource)
        val loading = findViewById<ProgressBar>(R.id.loadingBar)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val btnFavoritos = findViewById<Button>(R.id.btnVerFavoritos)
        val btnLoadMore = findViewById<Button>(R.id.btnLoadMore)

        adapter = DoujinAdapter(
            onDownload = { item ->
                val entity = viewModel.favoritos.value.find { it.id == item.id }
                if (entity?.isDownloaded == true) {
                    // Borrar
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Borrar descarga")
                        .setMessage("¿Deseas eliminar las imágenes guardadas de este doujin?")
                        .setPositiveButton("Sí") { _, _ ->
                            viewModel.deleteDownload(item)
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    viewModel.startDownload(item)
                    Toast.makeText(this, "Iniciando descarga...", Toast.LENGTH_SHORT).show()
                }
            },
            onClick = { item ->
                val intent = Intent(this, DoujinReaderActivity::class.java).apply {
                    putExtra("DOUJIN_ID", item.id)
                    putExtra("DOUJIN_SOURCE", item.source)
                    putExtra("DOUJIN_TITLE", item.title)
                    putExtra("DOUJIN_PAGES", item.totalPages)
                    putExtra("MEDIA_ID", item.mediaId)
                    putStringArrayListExtra("PAGE_EXTS", ArrayList(item.pageExtensions))
                }
                startActivity(intent)
            }
        )

        rv.layoutManager = GridLayoutManager(this, 2)
        rv.adapter = adapter

        btnSearch.setOnClickListener {
            val query = etSearch.text.toString()
            val source = if (rgSource.checkedRadioButtonId == R.id.rbMangaDex) "MangaDex" else "nHentai"
            
            if (source == "nHentai" && com.example.colorblend.data.local.ApiKeysManager.getNhentaiKey(this).isBlank()) {
                Toast.makeText(this, "⚠️ Configura tu API Key de nHentai en el perfil", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            showingFavoritos = false
            viewModel.search(query, source)
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                btnSearch.performClick()
                true
            } else false
        }

        btnFavoritos.setOnClickListener {
            showingFavoritos = !showingFavoritos
            if (showingFavoritos) {
                val list = viewModel.favoritos.value
                val mapped = list.map { entity ->
                    DoujinItem(
                        id = entity.id,
                        title = entity.title,
                        coverUrl = entity.coverUrl,
                        source = entity.source,
                        totalPages = entity.totalPages
                    )
                }
                adapter.submitList(mapped, list)
                tvEmpty.visibility = if (mapped.isEmpty()) View.VISIBLE else View.GONE
                btnFavoritos.text = "Ver Todo"
            } else {
                adapter.submitList(viewModel.searchResults.value, viewModel.favoritos.value)
                tvEmpty.visibility = if (viewModel.searchResults.value.isEmpty()) View.VISIBLE else View.GONE
                btnFavoritos.text = "Guardados"
            }
            updateLoadMoreVisibility(btnLoadMore)
        }

        btnLoadMore.setOnClickListener {
            viewModel.loadMore()
        }

        lifecycleScope.launch {
            viewModel.searchResults.collect { list ->
                if (!showingFavoritos) {
                    adapter.submitList(list, viewModel.favoritos.value)
                    tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
                updateLoadMoreVisibility(btnLoadMore)
            }
        }

        lifecycleScope.launch {
            viewModel.favoritos.collect { list ->
                if (showingFavoritos) {
                    val mapped = list.map { entity ->
                        DoujinItem(
                            id = entity.id,
                            title = entity.title,
                            coverUrl = entity.coverUrl,
                            source = entity.source,
                            totalPages = entity.totalPages
                        )
                    }
                    adapter.submitList(mapped, list)
                    tvEmpty.visibility = if (mapped.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    adapter.submitList(viewModel.searchResults.value, list)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                loading.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateLoadMoreVisibility(btn: Button) {
        val hasResults = viewModel.searchResults.value.isNotEmpty()
        btn.visibility = if (!showingFavoritos && hasResults) View.VISIBLE else View.GONE
    }

    inner class DoujinAdapter(private val onDownload: (DoujinItem) -> Unit, private val onClick: (DoujinItem) -> Unit) : RecyclerView.Adapter<DoujinAdapter.ViewHolder>() {
        private var items = listOf<DoujinItem>()
        private var downloadedIds = mapOf<String, DoujinEntity>()

        fun submitList(newList: List<DoujinItem>, downloaded: List<DoujinEntity> = emptyList()) {
            items = newList
            downloadedIds = downloaded.associateBy { it.id }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_doujin, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvSource.text = item.source
            
            val entity = downloadedIds[item.id]
            if (entity != null) {
                holder.pbDownload.visibility = if (entity.downloadStatus == "DOWNLOADING") View.VISIBLE else View.GONE
                holder.pbDownload.progress = entity.downloadProgress
                holder.tvStatus.visibility = View.VISIBLE
                holder.tvStatus.text = when(entity.downloadStatus) {
                    "COMPLETED" -> "Descargado ✅"
                    "DOWNLOADING" -> "Descargando... ${entity.downloadProgress}%"
                    "ERROR" -> "Error ❌"
                    else -> ""
                }
                holder.btnDownload.setImageResource(if (entity.isDownloaded) android.R.drawable.ic_menu_delete else android.R.drawable.stat_sys_download)
            } else {
                holder.pbDownload.visibility = View.GONE
                holder.tvStatus.visibility = View.GONE
                holder.btnDownload.setImageResource(android.R.drawable.stat_sys_download)
            }

            // Pasar el ID para que nHentai no bloquee el thumbnail
            val model = DoujinUtils.getGlideUrl(item.coverUrl, galleryId = item.id)
            Glide.with(holder.itemView)
                .load(model)
                .placeholder(R.drawable.card_nutricion)
                .into(holder.ivCover)
                
            holder.itemView.setOnClickListener { onClick(item) }
            holder.btnDownload.setOnClickListener { onDownload(item) }
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val ivCover: ImageView = v.findViewById(R.id.ivCover)
            val tvTitle: TextView = v.findViewById(R.id.tvTitle)
            val tvSource: TextView = v.findViewById(R.id.tvSource)
            val btnDownload: ImageButton = v.findViewById(R.id.btnDownload)
            val pbDownload: ProgressBar = v.findViewById(R.id.pbDownload)
            val tvStatus: TextView = v.findViewById(R.id.tvStatus)
        }
    }
}
