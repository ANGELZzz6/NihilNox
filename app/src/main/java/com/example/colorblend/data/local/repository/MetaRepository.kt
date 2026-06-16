package com.example.colorblend.data.local.repository

import com.example.colorblend.data.local.MetaDao
import com.example.colorblend.data.local.MetaImagenDao
import com.example.colorblend.data.local.UserStatsDao
import com.example.colorblend.domain.model.Meta
import com.example.colorblend.domain.model.MetaImagenDia
import com.example.colorblend.domain.model.TipoMeta
import com.example.colorblend.domain.model.UserStats
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class MetaRepository(
    private val metaDao: MetaDao,
    private val userStatsDao: UserStatsDao,
    private val metaImagenDao: MetaImagenDao? = null
) {

    fun getMetas(): Flow<List<Meta>> = metaDao.getMetas()

    suspend fun existeTituloRepetido(titulo: String): Boolean =
        metaDao.contarMetasConTitulo(titulo) > 0

    suspend fun crearMeta(meta: Meta): Long = metaDao.insert(meta)
    suspend fun actualizarMeta(meta: Meta) = metaDao.update(meta)

    suspend fun eliminarMeta(meta: Meta) {
        metaImagenDao?.deleteByMeta(meta.id)
        metaDao.delete(meta)
    }

    suspend fun cumplirMeta(meta: Meta, monedas: Int = 50) {
        if (!meta.activa || meta.finalizada) return
        if (meta.tipo == TipoMeta.DIARIA) cumplirMetaDiaria(meta, monedas)
    }

    private suspend fun cumplirMetaDiaria(meta: Meta, monedas: Int) {
        val hoy = obtenerInicioDelDia()
        if (meta.ultimaFecha == hoy) return

        // ── Racha inteligente ─────────────────────────────────────────────────
        // Si la meta tiene días específicos, la racha NO se rompe si entre la
        // última fecha cumplida y hoy no había ningún día habilitado.
        val nuevaRacha = calcularNuevaRacha(meta, hoy)

        val nuevoProgreso = meta.progresoActual + 1
        val finalizada    = nuevoProgreso >= meta.objetivo

        metaDao.update(meta.copy(
            progresoActual = nuevoProgreso,
            rachaActual    = nuevaRacha,
            mejorRacha     = maxOf(meta.mejorRacha, nuevaRacha),
            ultimaFecha    = hoy,
            finalizada     = finalizada,
            activa         = !finalizada
        ))

        sumarMonedas(monedas)
        if (finalizada) sumarMonedas(100)
    }

    /**
     * Calcula la nueva racha teniendo en cuenta los días habilitados.
     * Si entre [ultimaFecha] y [hoy] no había ningún día habilitado intermedio
     * que el usuario debiera haber cumplido, la racha continúa.
     */
    private fun calcularNuevaRacha(meta: Meta, hoy: Long): Int {
        val ultimaFecha = meta.ultimaFecha ?: return 1

        // Sin días específicos: comportamiento original (ayer exacto)
        if (meta.diasSemana.isNullOrEmpty()) {
            val ayer = hoy - 86_400_000L
            return if (ultimaFecha == ayer) meta.rachaActual + 1 else 1
        }

        val diasHabilitados = meta.diasSemana
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

        if (diasHabilitados.isEmpty()) {
            val ayer = hoy - 86_400_000L
            return if (ultimaFecha == ayer) meta.rachaActual + 1 else 1
        }

        // Recorrer hacia atrás desde ayer hasta ultimaFecha para ver
        // si había algún día habilitado que el usuario se saltó
        val cal = Calendar.getInstance()
        var cursor = hoy - 86_400_000L   // empezar desde ayer

        while (cursor > ultimaFecha) {
            cal.timeInMillis = cursor
            val diaCal = cal.get(Calendar.DAY_OF_WEEK)
            val diaFormato = when (diaCal) {
                Calendar.MONDAY    -> 1
                Calendar.TUESDAY   -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY  -> 4
                Calendar.FRIDAY    -> 5
                Calendar.SATURDAY  -> 6
                Calendar.SUNDAY    -> 7
                else -> 1
            }
            if (diaFormato in diasHabilitados) {
                // Había un día habilitado que no cumplió → racha rota
                return 1
            }
            cursor -= 86_400_000L
        }

        // No había días habilitados intermedios → racha continúa
        return meta.rachaActual + 1
    }

    suspend fun sumarProgreso(meta: Meta, cantidad: Int = 1, monedas: Int = 5) {
        if (!meta.activa || meta.finalizada) return
        if (meta.tipo != TipoMeta.ACUMULATIVA) return

        val nuevoProgreso = meta.progresoActual + cantidad
        val finalizada    = nuevoProgreso >= meta.objetivo

        metaDao.update(meta.copy(
            progresoActual = nuevoProgreso,
            finalizada     = finalizada,
            activa         = !finalizada
        ))

        sumarMonedas(monedas)
        if (finalizada) sumarMonedas(50)
    }

    // ─── Imágenes ─────────────────────────────────────────────────────────────

    fun getImagenesPorMeta(metaId: Int): Flow<List<MetaImagenDia>> =
        metaImagenDao!!.getImagenesPorMeta(metaId)

    suspend fun agregarImagen(metaId: Int, rutaImagen: String) {
        metaImagenDao!!.insert(MetaImagenDia(
            metaId     = metaId,
            fecha      = obtenerInicioDelDia(),
            rutaImagen = rutaImagen
        ))
    }

    suspend fun eliminarImagen(imagen: MetaImagenDia) {
        metaImagenDao!!.delete(imagen)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun sumarMonedas(cantidad: Int) {
        val stats = userStatsDao.getStatsOnce()
        if (stats == null) userStatsDao.insert(UserStats(id = 1, monedas = cantidad))
        else userStatsDao.update(stats.copy(monedas = stats.monedas + cantidad))
    }

    fun obtenerInicioDelDia(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}