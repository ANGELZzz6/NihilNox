package com.example.colorblend.data.local.repository

import com.example.colorblend.data.local.ProgresionDao
import com.example.colorblend.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProgresionRepository(private val dao: ProgresionDao) {

    suspend fun getEjerciciosActivos(): List<EjercicioEntity> = withContext(Dispatchers.IO) {
        dao.obtenerEjerciciosActivos()
    }

    suspend fun getEjercicioPorId(id: Long): EjercicioEntity? = withContext(Dispatchers.IO) {
        dao.obtenerEjercicioPorId(id)
    }

    suspend fun insertarEjercicio(ejercicio: EjercicioEntity) = withContext(Dispatchers.IO) {
        dao.insertarEjercicio(ejercicio)
    }

    suspend fun actualizarEjercicio(ejercicio: EjercicioEntity) = withContext(Dispatchers.IO) {
        dao.actualizarEjercicio(ejercicio)
    }

    suspend fun guardarSesionCompleta(
        sesion: SesionEntity,
        series: List<SerieEntity>,
        registro: RegistroDiarioProgresionEntity
    ) = withContext(Dispatchers.IO) {
        dao.insertarSesionConDetalles(sesion, series, registro)
    }

    suspend fun getUltimaSesion(ejercicioId: Long): SesionEntity? = withContext(Dispatchers.IO) {
        dao.obtenerUltimaSesion(ejercicioId)
    }

    suspend fun getSeriesPorSesion(sesionId: Long): List<SerieEntity> = withContext(Dispatchers.IO) {
        dao.obtenerSeriesPorSesion(sesionId)
    }

    suspend fun getHistorialSesiones(ejercicioId: Long): List<SesionEntity> = withContext(Dispatchers.IO) {
        dao.obtenerSesionesPorEjercicio(ejercicioId)
    }

    suspend fun getSesionesEnRango(inicio: Long, fin: Long): List<SesionEntity> = withContext(Dispatchers.IO) {
        dao.obtenerSesionesEnRango(inicio, fin)
    }

    suspend fun getRegistroDiarioPorSesion(sesionId: Long): RegistroDiarioProgresionEntity? = withContext(Dispatchers.IO) {
        dao.obtenerRegistroDiarioPorSesion(sesionId)
    }
    
    suspend fun getUltimosDosRegistrosDiarios(ejercicioId: Long): List<RegistroDiarioProgresionEntity> = withContext(Dispatchers.IO) {
        dao.obtenerUltimosDosRegistrosDiarios(ejercicioId)
    }

    suspend fun getVolumenUltimos7Dias(ejercicioId: Long): Float = withContext(Dispatchers.IO) {
        val sieteDiasAtras = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        dao.obtenerVolumenPorEjercicioDesde(ejercicioId, sieteDiasAtras) ?: 0f
    }

    suspend fun eliminarTodaLaProgresion() = withContext(Dispatchers.IO) {
        dao.limpiarTodaLaProgresion()
    }
}
