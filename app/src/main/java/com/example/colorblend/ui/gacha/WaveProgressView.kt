package com.example.colorblend.ui.gacha

import android.content.Context
import android.graphics.*
import android.graphics.Color as AndroidColor
import android.util.AttributeSet
import android.view.View

class WaveProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var history: List<Boolean> = emptyList()
    private var waveColor: Int = AndroidColor.YELLOW

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }

    fun setHistory(newHistory: List<Boolean>, colorHex: String = "#FFD700") {
        this.history = newHistory
        this.waveColor = try { AndroidColor.parseColor(colorHex) } catch(e: Exception) { AndroidColor.YELLOW }
        invalidate()
    }

    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (history.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val stepX = w / (history.size - 1).coerceAtLeast(1)
        val centerY = h / 2f
        val amplitude = h / 3f

        path.reset()

        history.forEachIndexed { index, success ->
            val x = index * stepX
            val targetY = if (success) centerY - (amplitude * 0.7f) else h - 20f
            
            if (index == 0) {
                path.moveTo(x, targetY)
            } else {
                val prevX = (index - 1) * stepX
                val prevSuccess = history[index - 1]
                val prevY = if (prevSuccess) centerY - (amplitude * 0.7f) else h - 20f
                
                path.cubicTo(
                    prevX + stepX / 2f, prevY,
                    x - stepX / 2f, targetY,
                    x, targetY
                )
            }
        }

        // Dibujar Glow
        glowPaint.color = waveColor
        glowPaint.alpha = 100
        canvas.drawPath(path, glowPaint)

        // Dibujar Línea
        paint.color = waveColor
        paint.alpha = 255
        canvas.drawPath(path, paint)
    }
}
