package com.example.colorblend.ui.gacha

import android.graphics.*
import android.graphics.Color as AndroidColor

object WaveGenerator {

    fun generateWaveBitmap(width: Int, height: Int, history: List<Boolean>, waveColorHex: String = "#FFD700"): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val waveColor = try { AndroidColor.parseColor(waveColorHex) } catch(e: Exception) { AndroidColor.YELLOW }
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = waveColor
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        if (history.isEmpty()) return bitmap

        val path = Path()
        val stepX = width.toFloat() / (history.size - 1).coerceAtLeast(1)
        val centerY = height / 2f
        val amplitude = height / 3f

        history.forEachIndexed { index, success ->
            val x = index * stepX
            val targetY = if (success) centerY - (amplitude * 0.8f) else height.toFloat() - 15f
            
            if (index == 0) {
                path.moveTo(x, targetY)
            } else {
                val prevX = (index - 1) * stepX
                val prevSuccess = history[index - 1]
                val prevY = if (prevSuccess) centerY - (amplitude * 0.8f) else height.toFloat() - 15f
                
                path.cubicTo(
                    prevX + stepX / 2f, prevY,
                    x - stepX / 2f, targetY,
                    x, targetY
                )
            }
        }

        // 1. Dibujar resplandor (Glow)
        paint.maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        paint.alpha = 100
        canvas.drawPath(path, paint)

        // 2. Dibujar línea principal
        paint.maskFilter = null
        paint.alpha = 255
        canvas.drawPath(path, paint)

        return bitmap
    }
}
