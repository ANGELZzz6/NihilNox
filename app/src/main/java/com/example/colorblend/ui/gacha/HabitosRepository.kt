package com.example.colorblend.ui.gacha

import com.example.colorblend.data.local.HabitoDao
import com.example.colorblend.data.local.IdentidadDao
import com.example.colorblend.data.local.RegistroHabitoDao
import com.example.colorblend.domain.model.EstadoRacha
import com.example.colorblend.domain.model.Habito
import com.example.colorblend.domain.model.Identidad
import com.example.colorblend.domain.model.RegistroHabito
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class HabitosRepository(
    private val dao: HabitoDao,
    private val registroDao: RegistroHabitoDao,
    private val identidadDao: IdentidadDao
) {
    val todosLosHabitos: Flow<List<Habito>> = dao.getTodos()

    suspend fun insertar(habito: Habito) = dao.insertar(habito)
    suspend fun actualizar(habito: Habito) = dao.actualizar(habito)
    suspend fun eliminar(habito: Habito) = dao.eliminar(habito)

    suspend fun marcarCompletado(habito: Habito) {
        val hoy = obtenerInicioDelDia()
        if ((habito.ultimaFechaCompletado ?: 0L) >= hoy) return // ya completado hoy, ignorar

        val ayer = hoy - 86_400_000L
        val ultimaFecha = habito.ultimaFechaCompletado ?: 0L

        val nuevaRacha = when {
            ultimaFecha >= ayer -> habito.rachaActual + 1
            else -> 1
        }

        dao.actualizar(habito.copy(
            completadoHoy = true,
            ultimaFechaCompletado = System.currentTimeMillis(),
            penultimaFechaCompletado = habito.ultimaFechaCompletado,
            rachaActual = nuevaRacha,
            rachaMaxima = maxOf(nuevaRacha, habito.rachaMaxima),
            totalCompletados = habito.totalCompletados + 1
        ))

        // Guardar registro histórico del día
        registroDao.insertar(
            RegistroHabito(habitoId = habito.id, fechaDia = hoy)
        )

        // Si el hábito tiene identidad vinculada, sumar voto
        habito.identidadId?.let { idIdentidad ->
            identidadDao.incrementarVotos(idIdentidad)
        }
    }

    suspend fun resetearCompletadosDelDia() {
        val hoy = obtenerInicioDelDia()
        val lista = dao.getTodosUnaVez()
        lista.forEach { habito ->
            val ultima = habito.ultimaFechaCompletado ?: 0L
            if (ultima < hoy && habito.completadoHoy) {
                dao.actualizar(habito.copy(completadoHoy = false))
            }
        }
    }

    fun estadoRacha(habito: Habito): EstadoRacha {
        val hoy = obtenerInicioDelDia()
        val ayer = hoy - 86_400_000L
        val ultima = habito.ultimaFechaCompletado ?: 0L
        val penultima = habito.penultimaFechaCompletado ?: 0L

        return when {
            !habito.completadoHoy && ultima < ayer && habito.rachaActual > 0 -> EstadoRacha.ROTA
            penultima < ayer && !habito.completadoHoy && ultima < hoy -> EstadoRacha.EN_RIESGO  // no completó ayer
            habito.completadoHoy -> EstadoRacha.COMPLETADO
            else -> EstadoRacha.PENDIENTE
        }
    }

    suspend fun contarCompletadosEnFecha(fecha: Long): Int =
        registroDao.contarHabitosCompletadosEnFecha(fecha)

    fun obtenerInicioDelDia(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // Identidades
    val todasLasIdentidades: Flow<List<Identidad>> = identidadDao.getTodas()
    suspend fun getIdentidadesUnaVez() = identidadDao.getTodasUnaVez()
    suspend fun insertarIdentidad(identidad: Identidad) = identidadDao.insertar(identidad)
    suspend fun eliminarIdentidad(identidad: Identidad) = identidadDao.eliminar(identidad)
    suspend fun getIdentidadById(id: Int) = identidadDao.getById(id)
}
