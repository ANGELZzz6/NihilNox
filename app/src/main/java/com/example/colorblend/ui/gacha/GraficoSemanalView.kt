package com.example.colorblend.ui.gacha

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.colorblend.R

class GraficoSemanalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintBarra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FFD700")
        style = Paint.Style.FILL
    }
    private val paintBarraVacia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#2A2A2A")
        style = Paint.Style.FILL
    }
    private val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#AAAAAA")
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.opensauce)
    }

    // datos: lista de 7 valores (0.0 a 1.0) — porcentaje completado ese día
    private var datos: List<Float> = List(7) { 0f }
    private val diasSemanaAbreviados = listOf("D", "L", "M", "X", "J", "V", "S")

    fun setDatos(porcentajes: List<Float>) {
        datos = porcentajes.takeLast(7).let {
            if (it.size < 7) List(7 - it.size) { 0f } + it else it
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val paddingBottom = 40f
        val paddingTop = 16f
        val alturaDisponible = h - paddingBottom - paddingTop
        val anchoBarra = (w / 7) * 0.5f
        val separacion = w / 7

        val cal = java.util.Calendar.getInstance()
        val labels = (0..6).map { i ->
            val c = java.util.Calendar.getInstance()
            c.add(java.util.Calendar.DAY_OF_YEAR, -(6 - i))
            diasSemanaAbreviados[c.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        }

        datos.forEachIndexed { i, valor ->
            val cx = separacion * i + separacion / 2
            val alturaBarra = (alturaDisponible * valor).coerceAtLeast(8f)
            val top = h - paddingBottom - alturaBarra
            val bottom = h - paddingBottom

            // barra de fondo (vacía)
            canvas.drawRoundRect(
                cx - anchoBarra / 2, paddingTop,
                cx + anchoBarra / 2, bottom,
                8f, 8f, paintBarraVacia
            )
            // barra de progreso
            if (valor > 0f) {
                canvas.drawRoundRect(
                    cx - anchoBarra / 2, top,
                    cx + anchoBarra / 2, bottom,
                    8f, 8f, paintBarra
                )
            }
            // etiqueta del día
            canvas.drawText(labels[i], cx, h - 8f, paintTexto)
        }
    }
}
