package com.example.colorblend.ui.gacha

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.domain.model.CarpetaImagenes
import kotlinx.coroutines.launch

class GaleriaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_galeria)

        val recycler = findViewById<RecyclerView>(R.id.galeriaRecycler)
        val btnNueva = findViewById<TextView>(R.id.btnNuevaCarpeta)
        val tvVacia = findViewById<TextView>(R.id.tvGaleriaVacia)

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.carpetaImagenesDao()
        val imagenDao = db.imagenGeneradaDao()

        val adapter = CarpetasAdapter(
            onClickCarpeta = { carpeta ->
                val intent = Intent(this, CarpetaDetalleActivity::class.java)
                intent.putExtra("carpetaId", carpeta.id)
                intent.putExtra("carpetaNombre", carpeta.nombre)
                startActivity(intent)
            },
            onEliminarCarpeta = { carpeta ->
                lifecycleScope.launch {
                    dao.delete(carpeta)
                }
            },
            getCantidad = { carpetaId ->
                imagenDao.contarImagenes(carpetaId)
            }
        )

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            dao.getCarpetas().collect { lista ->
                adapter.update(lista)
                tvVacia.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                recycler.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        btnNueva.setOnClickListener {
            val input = EditText(this)
            input.hint = "Nombre de la carpeta"
            input.setTextColor(android.graphics.Color.WHITE)

            AlertDialog.Builder(this)
                .setTitle("Nueva carpeta")
                .setView(input)
                .setPositiveButton("Crear") { _, _ ->
                    val nombre = input.text.toString().trim()
                    if (nombre.isNotEmpty()) {
                        lifecycleScope.launch {
                            dao.insert(CarpetaImagenes(nombre = nombre))
                        }
                    } else {
                        Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}

class CarpetasAdapter(
    private var carpetas: List<CarpetaImagenes> = emptyList(),
    private val onClickCarpeta: (CarpetaImagenes) -> Unit,
    private val onEliminarCarpeta: (CarpetaImagenes) -> Unit,
    private val getCantidad: suspend (Int) -> Int
) : RecyclerView.Adapter<CarpetasAdapter.ViewHolder>() {

    fun update(nuevas: List<CarpetaImagenes>) {
        carpetas = nuevas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carpeta, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(carpetas[position])
    }

    override fun getItemCount() = carpetas.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(carpeta: CarpetaImagenes) {
            itemView.findViewById<TextView>(R.id.carpetaNombre).text = carpeta.nombre
            itemView.findViewById<TextView>(R.id.carpetaCantidad).text = "Cargando..."

            // Cargar cantidad
            kotlinx.coroutines.GlobalScope.launch {
                val cantidad = getCantidad(carpeta.id)
                (itemView.context as? androidx.appcompat.app.AppCompatActivity)
                    ?.runOnUiThread {
                        itemView.findViewById<TextView>(R.id.carpetaCantidad).text =
                            "$cantidad imagen(es)"
                    }
            }

            itemView.setOnClickListener { onClickCarpeta(carpeta) }
            itemView.findViewById<TextView>(R.id.btnEliminarCarpeta).setOnClickListener {
                onEliminarCarpeta(carpeta)
            }
        }
    }
}