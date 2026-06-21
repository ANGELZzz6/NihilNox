package com.example.colorblend.ui.gacha

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import com.example.colorblend.domain.model.LearnTopic
import kotlinx.coroutines.launch

class LearnActivity : AppCompatActivity() {

    private lateinit var viewModel: LearnViewModel
    private lateinit var adapter: LearnTopicAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn)
        FullScreenHelper.enable(this)

        viewModel = ViewModelProvider(this)[LearnViewModel::class.java]
        setupRecyclerView()
        observeTopics()
        iniciarAnimacionesEntrada()

        // Programar recordatorio diario a las 7pm
        LearnScheduler.programarRecordatorioDiario(this, 19, 0)

        findViewById<Button>(R.id.btnLearnNuevoTema).setOnClickListener {
            startActivity(Intent(this, NewTopicActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = LearnTopicAdapter(
            onEstudiar = { topic ->
                val intent = Intent(this, SessionActivity::class.java)
                intent.putExtra("TOPIC_ID", topic.id)
                intent.putExtra("TOPIC_TITULO", topic.titulo)
                startActivity(intent)
            },
            onQuiz = { topic ->
                val intent = Intent(this, SessionActivity::class.java)
                intent.putExtra("TOPIC_ID", topic.id)
                intent.putExtra("TOPIC_TITULO", topic.titulo)
                intent.putExtra("MODO_QUIZ", true)
                startActivity(intent)
            },
            onEliminar = { topic ->
                AlertDialog.Builder(this)
                    .setTitle("¿Eliminar tema?")
                    .setMessage("Se borrarán \"${topic.titulo}\" y todas sus tarjetas permanentemente.")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.eliminarTema(topic.id)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )
        findViewById<RecyclerView>(R.id.rvLearnTopics).apply {
            layoutManager = LinearLayoutManager(this@LearnActivity)
            adapter = this@LearnActivity.adapter
        }
    }

    private fun observeTopics() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.topics.collect { topics ->
                    adapter.submitList(topics)
                    val vacio = findViewById<View>(R.id.layoutLearnVacio)
                    val rv = findViewById<View>(R.id.rvLearnTopics)
                    if (topics.isEmpty()) {
                        vacio.visibility = View.VISIBLE
                        rv.visibility = View.GONE
                    } else {
                        vacio.visibility = View.GONE
                        rv.visibility = View.VISIBLE
                    }
                    val rachaMax = topics.maxOfOrNull { it.rachaEstudio } ?: 0
                    findViewById<TextView>(R.id.tvLearnRacha).text = "🔥 $rachaMax días"
                    findViewById<TextView>(R.id.tvLearnTemas).text = "${topics.size} temas activos"
                }
            }
        }
    }

    private fun iniciarAnimacionesEntrada() {
        val header = findViewById<View>(R.id.headerLearn)
        val btn = findViewById<View>(R.id.btnLearnNuevoTema)
        listOf(header, btn).forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = if (i == 0) -40f else 40f
            v.animate().alpha(1f).translationY(0f)
                .setDuration(400).setStartDelay(i * 100L)
                .setInterpolator(DecelerateInterpolator()).start()
        }
    }
}
