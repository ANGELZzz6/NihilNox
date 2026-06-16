package com.example.colorblend.ui.gacha

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.ImagenGenerada
import kotlinx.coroutines.launch

class CarpetaDetalleActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_GUARDAR_IMAGEN = 1001
    }

    private var carpetaId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carpeta_detalle)

        carpetaId = intent.getIntExtra("carpetaId", 0)
        val carpetaNombre = intent.getStringExtra("carpetaNombre") ?: "Carpeta"

        findViewById<TextView>(R.id.tvCarpetaNombre).text = carpetaNombre

        val recycler = findViewById<RecyclerView>(R.id.imagenesRecycler)
        val btnGenerar = findViewById<TextView>(R.id.btnGenerarImagen)
        val tvVacia = findViewById<TextView>(R.id.tvCarpetaVacia)

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.imagenGeneradaDao()

        val adapter = ImagenesGeneradasAdapter(
            onEliminar = { imagen ->
                lifecycleScope.launch {
                    java.io.File(imagen.rutaLocal).delete()
                    dao.delete(imagen)
                }
            },
            // ✅ Abrir imagen en pantalla completa
            onVerImagen = { imagen ->
                val intent = Intent(this, ImagenDetalleActivity::class.java)
                intent.putExtra("rutaImagen", imagen.rutaLocal)
                startActivity(intent)
            }
        )

        recycler.adapter = adapter
        recycler.layoutManager = GridLayoutManager(this, 2)

        lifecycleScope.launch {
            dao.getImagenesPorCarpeta(carpetaId).collect { lista ->
                adapter.update(lista)
                tvVacia.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                recycler.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        btnGenerar.setOnClickListener {
            val intent = Intent(this, PerchanceWebViewActivity::class.java)
            intent.putExtra("carpetaId", carpetaId)
            startActivityForResult(intent, REQUEST_GUARDAR_IMAGEN)
        }
    }
}

class ImagenesGeneradasAdapter(
    private var imagenes: List<ImagenGenerada> = emptyList(),
    private val onEliminar: (ImagenGenerada) -> Unit,
    private val onVerImagen: (ImagenGenerada) -> Unit  // ← nuevo
) : RecyclerView.Adapter<ImagenesGeneradasAdapter.ViewHolder>() {

    fun update(nuevas: List<ImagenGenerada>) {
        imagenes = nuevas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_imagen_generada, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(imagenes[position])
    }

    override fun getItemCount() = imagenes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(imagen: ImagenGenerada) {
            Glide.with(itemView.context)
                .load(java.io.File(imagen.rutaLocal))
                .into(itemView.findViewById(R.id.imagenGeneradaImg))

            // ✅ Click para ver en grande
            itemView.setOnClickListener { onVerImagen(imagen) }

            itemView.findViewById<TextView>(R.id.btnEliminarImagen).setOnClickListener {
                onEliminar(imagen)
            }
        }
    }
}