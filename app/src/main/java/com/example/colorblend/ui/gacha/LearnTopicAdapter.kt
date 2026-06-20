package com.example.colorblend.ui.gacha

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.LearnTopic

class LearnTopicAdapter(
    private val onEstudiar: (LearnTopic) -> Unit,
    private val onQuiz: (LearnTopic) -> Unit
) : RecyclerView.Adapter<LearnTopicAdapter.ViewHolder>() {

    private var topics = listOf<LearnTopic>()

    fun submitList(newList: List<LearnTopic>) {
        topics = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_learn_topic, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(topics[position])
    }

    override fun getItemCount() = topics.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(topic: LearnTopic) {
            itemView.findViewById<TextView>(R.id.tvTopicTitulo).text = topic.titulo
            itemView.findViewById<TextView>(R.id.tvTopicDescripcion).text = topic.descripcion
            itemView.findViewById<TextView>(R.id.tvTopicCategoria).text = topic.categoria
            itemView.findViewById<TextView>(R.id.tvTopicRacha).text =
                if (topic.rachaEstudio > 0) "🔥 ${topic.rachaEstudio} días" else ""

            val pct = (topic.dominioTotal * 100).toInt()
            itemView.findViewById<ProgressBar>(R.id.progressTopicDominio).progress = pct
            itemView.findViewById<TextView>(R.id.tvTopicDominioPct).text = "$pct%"

            itemView.findViewById<Button>(R.id.btnTopicEstudiar).setOnClickListener {
                animarClick(it) { onEstudiar(topic) }
            }
            itemView.findViewById<Button>(R.id.btnTopicQuiz).setOnClickListener {
                animarClick(it) { onQuiz(topic) }
            }
        }

        private fun animarClick(v: View, action: () -> Unit) {
            v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80).withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80)
                    .withEndAction { action() }.start()
            }.start()
        }
    }
}
