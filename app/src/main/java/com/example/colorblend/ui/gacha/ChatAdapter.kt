package com.example.colorblend.ui.gacha

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.MensajeChat

class ChatAdapter(
    private var mensajes: List<MensajeChat> = emptyList()
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    companion object {
        const val TIPO_USUARIO = 0
        const val TIPO_PERSONAJE = 1
    }

    fun update(nuevos: List<MensajeChat>) {
        mensajes = nuevos
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (mensajes[position].esUsuario) TIPO_USUARIO else TIPO_PERSONAJE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (viewType == TIPO_USUARIO)
            R.layout.item_mensaje_usuario
        else
            R.layout.item_mensaje_personaje
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mensajes[position])
    }

    override fun getItemCount() = mensajes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(mensaje: MensajeChat) {
            itemView.findViewById<TextView>(R.id.tvMensaje).text = mensaje.contenido
        }
    }
}