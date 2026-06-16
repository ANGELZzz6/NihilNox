package com.example.colorblend.ui.gacha

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.colorblend.R
import java.io.File

class ImagenDetalleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_imagen_detalle)

        val ruta = intent.getStringExtra("rutaImagen") ?: return

        Glide.with(this)
            .load(File(ruta))
            .into(findViewById(R.id.imagenDetalleImg))

        findViewById<TextView>(R.id.btnCerrarDetalle).setOnClickListener {
            finish()
        }
    }
}