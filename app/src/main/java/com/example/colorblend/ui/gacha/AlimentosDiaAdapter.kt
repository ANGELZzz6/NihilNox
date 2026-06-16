package com.example.colorblend.ui.gacha

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.RegistroAlimento

class AlimentosDiaAdapter(
    private val onEliminar: (RegistroAlimento) -> Unit
) : RecyclerView.Adapter<AlimentosDiaAdapter.ViewHolder>() {

    private var lista = listOf<RegistroAlimento>()

    fun update(nuevaLista: List<RegistroAlimento>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comida, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(lista[position])

    override fun getItemCount() = lista.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(alimento: RegistroAlimento) {
            itemView.findViewById<TextView>(R.id.tvNombreAlimento).text = alimento.nombre
            itemView.findViewById<TextView>(R.id.tvCaloriasAlimento).text =
                "${alimento.calorias} kcal"
            itemView.findViewById<TextView>(R.id.tvMacrosAlimento).text =
                "P: ${alimento.proteina.toInt()}g  C: ${alimento.carbos.toInt()}g  G: ${alimento.grasas.toInt()}g"
            itemView.setOnLongClickListener {
                onEliminar(alimento)
                true
            }
        }
    }
}