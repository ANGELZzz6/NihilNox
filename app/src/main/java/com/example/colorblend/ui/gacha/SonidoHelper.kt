package com.example.colorblend.ui.gacha

import android.content.Context
import android.media.MediaPlayer
import com.example.colorblend.R

object SonidoHelper {

    private var turno = 0 // ✅ Intercala entre tock_1 y tock_2

    fun reproducir(context: Context) {
        val resId = if (turno % 2 == 0) R.raw.tock_1 else R.raw.tock_2
        turno++
        try {
            val mp = MediaPlayer.create(context, resId)
            mp?.apply {
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) {
            // Silenciar errores de audio para no interrumpir el flujo
        }
    }
}