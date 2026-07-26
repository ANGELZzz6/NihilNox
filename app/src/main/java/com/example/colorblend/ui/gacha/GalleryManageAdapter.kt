package com.example.colorblend.ui.gacha

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.colorblend.R
import java.io.File

class GalleryManageAdapter(
    private var urls: List<String> = emptyList()
) : RecyclerView.Adapter<GalleryManageAdapter.ViewHolder>() {

    private val selectedUrls = mutableSetOf<String>()

    fun getSelectedUrls(): List<String> = selectedUrls.toList()

    fun update(nuevas: List<String>) {
        urls = nuevas
        selectedUrls.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_manage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(urls[position])
    }

    override fun getItemCount() = urls.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagen = itemView.findViewById<ImageView>(R.id.ivGalleryThumb)
        private val checkBox = itemView.findViewById<CheckBox>(R.id.cbSelected)

        fun bind(url: String) {
            if (url.startsWith("/")) {
                Glide.with(itemView.context).load(File(url)).into(imagen)
            } else {
                Glide.with(itemView.context).load(url).into(imagen)
            }

            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = selectedUrls.contains(url)
            
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedUrls.add(url) else selectedUrls.remove(url)
            }
            
            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }
        }
    }
}
