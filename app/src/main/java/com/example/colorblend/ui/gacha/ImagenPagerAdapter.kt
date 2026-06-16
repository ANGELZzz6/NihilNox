package com.example.colorblend.ui.gacha

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.colorblend.R
import java.io.File

class ImagenPagerAdapter(
    private var urls: List<String> = emptyList()
) : RecyclerView.Adapter<ImagenPagerAdapter.ViewHolder>() {

    fun update(nuevas: List<String>) {
        urls = nuevas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_imagen_pager, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(urls[position])
    }

    override fun getItemCount() = urls.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagen = itemView.findViewById<ImageView>(R.id.pagerImagen)

        fun bind(url: String) {
            if (url.startsWith("/")) {
                Glide.with(itemView.context)
                    .load(File(url))
                    .into(imagen)
            } else {
                Glide.with(itemView.context)
                    .load(url)
                    .into(imagen)
            }
        }
    }
}