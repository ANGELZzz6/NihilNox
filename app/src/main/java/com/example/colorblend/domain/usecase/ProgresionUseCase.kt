package com.example.colorblend.domain.usecase

import com.example.colorblend.domain.model.*
import kotlin.math.roundToInt

data class SugerenciaProgresion(
    val tipo: TipoSugerencia,
    val mensaje: String,
    val pesoSugeridoKg: Float? = null,
    val alertaMolestia: String? = null
)

enum class TipoSugerencia {
    SUBIR_PESO, BAJAR_PESO, MANTENER, DESCARGA, ALERTA_TENDON
}

class ProgresionUseCase {

    fun evaluarProgresion(
        ejercicio: EjercicioEntity,
        ultimaSesionSeries: List<SerieEntity>,
        volumenSemanalPrevio: Float,
        ultimosRegistrosDiarios: List<RegistroDiarioProgresionEntity>,
        semanasConsecutivas: Int
    ): SugerenciaProgresion {
        
        // 1. Alertas de Molestias (REGLAS ESTRICTAS)
        val alertaMolestia = evaluarMolestias(ultimosRegistrosDiarios)
        
        // 2. Detección de Descarga Automática (Sugerencia visual)
        if (semanasConsecutivas >= 6) {
            return SugerenciaProgresion(
                tipo = TipoSugerencia.DESCARGA,
                mensaje = "Llevas $semanasConsecutivas semanas entrenando este ejercicio. Sugerimos una semana de descarga (-40% peso o -50% series).",
                alertaMolestia = alertaMolestia
            )
        }

        // 3. Lógica de Doble Progresión
        val todasEnMax = ultimaSesionSeries.all { it.reps >= ejercicio.rangoRepsMax }
        val rirBajo = ultimaSesionSeries.lastOrNull()?.rir?.let { it in 1..2 } ?: false
        
        val unidad = if (ejercicio.esIsometrico) "segundos" else "reps"

        if (todasEnMax && rirBajo) {
            val incremento = (ejercicio.pesoActualKg * 0.035f) // Sugerencia media de 3.5%
            val pesoSugeridoRaw = ejercicio.pesoActualKg + incremento
            
            // Step inteligente: 1.25kg o 0.5kg
            val step = if (pesoSugeridoRaw < 15f) 0.5f else 1.25f
            val pesoSugerido = (pesoSugeridoRaw / step).roundToInt() * step
            
            // Regla del 10% de volumen semanal rodante
            val volumenConNuevoPeso = ultimaSesionSeries.size * pesoSugerido * ejercicio.rangoRepsMin
            val limiteVolumen = volumenSemanalPrevio * 1.10f
            
            return if (volumenSemanalPrevio > 0 && volumenConNuevoPeso > limiteVolumen) {
                SugerenciaProgresion(
                    tipo = TipoSugerencia.MANTENER,
                    mensaje = "Has cumplido el rango, pero subir peso ahora excedería el 10% de volumen semanal permitido. Mantén peso o intenta aguantar más $unidad.",
                    pesoSugeridoKg = ejercicio.pesoActualKg,
                    alertaMolestia = alertaMolestia
                )
            } else {
                SugerenciaProgresion(
                    tipo = TipoSugerencia.SUBIR_PESO,
                    mensaje = "¡Excelente! Has dominado los $unidad. Sube a ${pesoSugerido}kg y vuelve al rango min.",
                    pesoSugeridoKg = pesoSugerido,
                    alertaMolestia = alertaMolestia
                )
            }
        }

        // 4. Fallo por debajo del piso
        val algunaBajoMin = ultimaSesionSeries.any { it.reps < ejercicio.rangoRepsMin }
        if (algunaBajoMin) {
            val pesoReducidoRaw = ejercicio.pesoActualKg * 0.90f
            val step = if (pesoReducidoRaw < 15f) 0.5f else 1.25f
            val pesoReducido = (pesoReducidoRaw / step).roundToInt() * step
            
            return SugerenciaProgresion(
                tipo = TipoSugerencia.BAJAR_PESO,
                mensaje = "Has quedado por debajo del tiempo/reps mínimas. Baja un 10% el peso (${pesoReducido}kg) para recuperar técnica.",
                pesoSugeridoKg = pesoReducido,
                alertaMolestia = alertaMolestia
            )
        }

        return SugerenciaProgresion(
            tipo = TipoSugerencia.MANTENER,
            mensaje = "Sigue trabajando en este rango hasta alcanzar el máximo con RIR 1-2.",
            pesoSugeridoKg = ejercicio.pesoActualKg,
            alertaMolestia = alertaMolestia
        )
    }

    private fun evaluarMolestias(registros: List<RegistroDiarioProgresionEntity>): String? {
        if (registros.isEmpty()) return null
        
        val actual = registros[0].molestiaArticular
        val previa = registros.getOrNull(1)?.molestiaArticular
        
        // 1. Aumenta respecto a la anterior
        if (previa != null && actual > previa) {
            return "Alerta: La molestia ha aumentado. Considera bajar carga o cambiar el ejercicio."
        }
        
        // 2. Absoluto >= 5
        if (actual >= 5) {
            return "Cuidado: Molestia articular elevada ($actual/10). No ignores el dolor."
        }
        
        // 3. Persistente >= 3 en sesiones consecutivas (>24h se asume por registros distintos)
        if (previa != null && actual >= 3 && previa >= 3) {
            return "Molestia persistente detected. Sugerimos semana de descarga o ejercicios accesorios."
        }
        
        return null
    }
}
