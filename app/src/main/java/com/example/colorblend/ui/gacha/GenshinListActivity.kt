package com.example.colorblend.ui.gacha

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorblend.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GenshinListActivity : AppCompatActivity() {

    private val viewModel: GenshinViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_genshin_list)

        val rv = findViewById<RecyclerView>(R.id.rvGenshinCharacters)
        val adapter = GenshinAdapter { char ->
            val intent = Intent(this, GenshinEditActivity::class.java).apply {
                putExtra("character_id", char.id)
            }
            startActivity(intent)
        }
        
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<View>(R.id.btnVolverGenshin).setOnClickListener { finish() }
        
        findViewById<View>(R.id.fabAddGenshin).setOnClickListener {
            startActivity(Intent(this, GenshinEditActivity::class.java))
        }

        lifecycleScope.launch {
            viewModel.characters.collectLatest {
                adapter.submitList(it)
            }
        }
    }
}
