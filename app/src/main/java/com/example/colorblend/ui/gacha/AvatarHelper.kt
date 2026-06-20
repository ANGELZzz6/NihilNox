package com.example.colorblend.ui.gacha

import android.content.Context
import android.graphics.*
import android.graphics.Color as AndroidColor

object AvatarHelper {

    // ── Recorte circular compartido ────────────────────────────────────
    fun recortarCircular(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, ((size - bitmap.width) / 2f), ((size - bitmap.height) / 2f), paint)
        return output
    }

    // ── Configuración de marco según nivel ────────────────────────────
    data class MarcoConfig(
        val colorMarco: Int,
        val grosorMarco: Float,     // dp
        val colorBrillo: Int?,      // segundo anillo exterior, null = sin brillo
        val alasEscala: Float,      // 0f = sin alas
        val alasColor: Int,
        val nombre: String          // nombre del rango
    )

    fun getMarcoConfig(nivel: Int): MarcoConfig = when {
        nivel >= 50 -> MarcoConfig(
            colorMarco   = AndroidColor.parseColor("#FF00FF"),
            grosorMarco  = 6f,
            colorBrillo  = AndroidColor.parseColor("#FFD700"),
            alasEscala   = 1.0f,
            alasColor    = AndroidColor.parseColor("#FF00FF"),
            nombre        = "Legendario"
        )
        nivel >= 40 -> MarcoConfig(
            colorMarco   = AndroidColor.parseColor("#9B30FF"),
            grosorMarco  = 5f,
            colorBrillo  = AndroidColor.parseColor("#CC44FF"),
            alasEscala   = 0.85f,
            alasColor    = AndroidColor.parseColor("#9B30FF"),
            nombre        = "Épico"
        )
        nivel >= 30 -> MarcoConfig(
            colorMarco   = AndroidColor.parseColor("#C0C0C0"),
            grosorMarco  = 4f,
            colorBrillo  = AndroidColor.parseColor("#FFD700"),
            alasEscala   = 0.70f,
            alasColor    = AndroidColor.parseColor("#C0C0C0"),
            nombre        = "Maestro"
        )
        nivel >= 20 -> MarcoConfig(
            colorMarco   = AndroidColor.parseColor("#FFD700"),
            grosorMarco  = 3.5f,
            colorBrillo  = AndroidColor.parseColor("#FFF176"),
            alasEscala   = 0.55f,
            alasColor    = AndroidColor.parseColor("#FFD700"),
            nombre        = "Experto"
        )
        nivel >= 10 -> MarcoConfig(
            colorMarco   = AndroidColor.parseColor("#FFD700"),
            grosorMarco  = 2.5f,
            colorBrillo  = null,
            alasEscala   = 0.35f,
            alasColor    = AndroidColor.parseColor("#FFD700"),
            nombre        = "Veterano"
        )
        else -> MarcoConfig(
            colorMarco   = AndroidColor.parseColor("#FFD700"),
            grosorMarco  = 2f,
            colorBrillo  = null,
            alasEscala   = 0f,
            alasColor    = AndroidColor.TRANSPARENT,
            nombre        = "Novato"
        )
    }

    // Solo el círculo con marco (sin alas) — para el ImageView principal
    fun dibujarCirculoConMarco(
        context: Context,
        bitmap: Bitmap,
        nivel: Int,
        sizeDp: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        val config = getMarcoConfig(nivel)

        // Bitmap siempre del mismo tamaño, sin importar el nivel
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val radio = sizePx / 2f - (config.grosorMarco * density)

        // Avatar circular primero
        val avatarCircular = recortarCircular(bitmap)
        val avatarScaled = Bitmap.createScaledBitmap(
            avatarCircular,
            (radio * 2).toInt(),
            (radio * 2).toInt(),
            true
        )
        canvas.drawBitmap(avatarScaled, cx - radio, cy - radio, null)

        // Marco principal encima
        val paintMarco = Paint().apply {
            isAntiAlias = true
            color = config.colorMarco
            style = Paint.Style.STROKE
            strokeWidth = config.grosorMarco * density
        }
        canvas.drawCircle(cx, cy, radio, paintMarco)

        // Brillo exterior como anillo interior adicional
        if (config.colorBrillo != null) {
            val paintBrillo = Paint().apply {
                isAntiAlias = true
                color = config.colorBrillo
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * density
                alpha = 180
            }
            canvas.drawCircle(cx, cy, radio - (config.grosorMarco * density) - (1 * density), paintBrillo)
        }

        return output
    }

    // Solo las alas — para un ImageView superpuesto encima
    fun dibujarAlas(
        context: Context,
        nivel: Int,
        sizeDp: Int
    ): Bitmap? {
        val config = getMarcoConfig(nivel)
        if (config.alasEscala <= 0f) return null

        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        val radio = sizePx / 2f
        val alasExtra = (sizePx * 0.5f * config.alasEscala).toInt()
        val totalW = sizePx + alasExtra * 2
        val totalH = (sizePx * 0.8f).toInt()

        val output = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val cx = totalW / 2f
        val cy = totalH * 0.75f

        dibujarAlasInterno(canvas, cx, cy, radio, config, density)
        return output
    }

    // Mantener dibujarAvatarConMarco como wrapper para no romper código existente
    fun dibujarAvatarConMarco(
        context: Context,
        bitmap: Bitmap,
        nivel: Int,
        sizeDp: Int
    ): Bitmap = dibujarCirculoConMarco(context, bitmap, nivel, sizeDp)

    private fun dibujarAlasInterno(
        canvas: Canvas,
        cx: Float, cy: Float, radio: Float,
        config: MarcoConfig,
        density: Float
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = config.alasColor
            style = Paint.Style.FILL
            alpha = 180
        }
        val paintStroke = Paint().apply {
            isAntiAlias = true
            color = config.alasColor
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            alpha = 220
        }

        val alaW = radio * 1.1f * config.alasEscala
        val alaH = radio * 0.9f * config.alasEscala

        // Ala izquierda
        val pathIzq = Path().apply {
            moveTo(cx - radio * 0.85f, cy)
            cubicTo(
                cx - radio - alaW * 0.5f, cy - alaH * 0.3f,
                cx - radio - alaW, cy - alaH * 0.8f,
                cx - radio - alaW * 0.6f, cy - alaH
            )
            cubicTo(
                cx - radio - alaW * 0.3f, cy - alaH * 0.5f,
                cx - radio * 0.5f, cy - alaH * 0.3f,
                cx - radio * 0.85f, cy
            )
            close()
        }

        // Ala derecha (espejo)
        val pathDer = Path().apply {
            moveTo(cx + radio * 0.85f, cy)
            cubicTo(
                cx + radio + alaW * 0.5f, cy - alaH * 0.3f,
                cx + radio + alaW, cy - alaH * 0.8f,
                cx + radio + alaW * 0.6f, cy - alaH
            )
            cubicTo(
                cx + radio + alaW * 0.3f, cy - alaH * 0.5f,
                cx + radio * 0.5f, cy - alaH * 0.3f,
                cx + radio * 0.85f, cy
            )
            close()
        }

        canvas.drawPath(pathIzq, paint)
        canvas.drawPath(pathIzq, paintStroke)
        canvas.drawPath(pathDer, paint)
        canvas.drawPath(pathDer, paintStroke)

        // Plumas internas (líneas decorativas)
        if (config.alasEscala >= 0.55f) {
            val paintPluma = Paint().apply {
                isAntiAlias = true
                color = config.alasColor
                style = Paint.Style.STROKE
                strokeWidth = 1f * density
                alpha = 160
            }
            // Izquierda
            repeat(3) { i ->
                val t = 0.3f + i * 0.2f
                canvas.drawLine(
                    cx - radio * 0.85f - alaW * 0.1f * i,
                    cy - alaH * 0.1f,
                    cx - radio - alaW * (0.3f + t * 0.4f),
                    cy - alaH * (0.3f + t * 0.5f),
                    paintPluma
                )
            }
            // Derecha
            repeat(3) { i ->
                val t = 0.3f + i * 0.2f
                canvas.drawLine(
                    cx + radio * 0.85f + alaW * 0.1f * i,
                    cy - alaH * 0.1f,
                    cx + radio + alaW * (0.3f + t * 0.4f),
                    cy - alaH * (0.3f + t * 0.5f),
                    paintPluma
                )
            }
        }
    }
}
