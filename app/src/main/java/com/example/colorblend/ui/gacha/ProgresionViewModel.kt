package com.example.colorblend.ui.gacha

import androidx.lifecycle.*
import com.example.colorblend.data.local.repository.ProgresionRepository
import com.example.colorblend.domain.model.*
import com.example.colorblend.domain.usecase.ProgresionUseCase
import com.example.colorblend.domain.usecase.SugerenciaProgresion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.util.*
import java.text.SimpleDateFormat

data class HistoryItem(
    val sesion: SesionEntity,
    val series: List<SerieEntity>,
    val registro: RegistroDiarioProgresionEntity?,
    val nombreEjercicio: String
)

class ProgresionViewModel(
    private val repository: ProgresionRepository,
    private val useCase: ProgresionUseCase
) : ViewModel() {

    private val _ejercicios = MutableLiveData<List<EjercicioEntity>>()
    val ejercicios: LiveData<List<EjercicioEntity>> = _ejercicios

    private val _ejercicioSeleccionado = MutableLiveData<EjercicioEntity?>()
    val ejercicioSeleccionado: LiveData<EjercicioEntity?> = _ejercicioSeleccionado

    private val _ultimaSesionData = MutableLiveData<Pair<SesionEntity, List<SerieEntity>>?>()
    val ultimaSesionData: LiveData<Pair<SesionEntity, List<SerieEntity>>?> = _ultimaSesionData

    private val _historial = MutableLiveData<List<HistoryItem>>()
    val historial: LiveData<List<HistoryItem>> = _historial

    private val _sugerencia = MutableLiveData<SugerenciaProgresion?>()
    val sugerencia: LiveData<SugerenciaProgresion?> = _sugerencia

    fun cargarEjercicios() {
        viewModelScope.launch {
            _ejercicios.value = repository.getEjerciciosActivos()
        }
    }

    fun seleccionarEjercicio(ejercicio: EjercicioEntity) {
        _ejercicioSeleccionado.value = ejercicio
        cargarDatosEjercicio(ejercicio.id)
    }

    fun agregarEjercicio(
        nombre: String, 
        peso: Float, 
        min: Int, 
        max: Int, 
        esPrincipal: Boolean, 
        esIsometrico: Boolean,
        descanso: Int? = null,
        tempo: String? = null,
        calentamiento: String? = null,
        notasTendon: String? = null
    ) {
        viewModelScope.launch {
            val nuevo = EjercicioEntity(
                nombre = nombre,
                esEjercicioPrincipal = esPrincipal,
                rangoRepsMin = min,
                rangoRepsMax = max,
                pesoActualKg = peso,
                orden = (_ejercicios.value?.size ?: 0) + 1,
                activo = true,
                esIsometrico = esIsometrico,
                descansoSegundos = descanso,
                tempo = tempo,
                protocoloCalentamiento = calentamiento,
                notasTendon = notasTendon,
                requiereCalentamientoEspecifico = !calentamiento.isNullOrBlank()
            )
            repository.insertarEjercicio(nuevo)
            cargarEjercicios()
        }
    }

    fun exportarDiaJson(fecha: Long): String {
        val sesiones = _historial.value?.filter { 
            val cal1 = Calendar.getInstance().apply { timeInMillis = it.sesion.fecha }
            val cal2 = Calendar.getInstance().apply { timeInMillis = fecha }
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        } ?: emptyList()

        val root = org.json.JSONObject()
        val array = org.json.JSONArray()
        
        sesiones.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("ejercicio", item.nombreEjercicio)
            obj.put("fecha", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(item.sesion.fecha)))
            
            val seriesArray = org.json.JSONArray()
            item.series.forEach { s ->
                val sObj = org.json.JSONObject()
                sObj.put("peso", s.pesoKg)
                sObj.put("reps", s.reps)
                if (s.rir != null) sObj.put("rir", s.rir)
                seriesArray.put(sObj)
            }
            obj.put("series", seriesArray)
            obj.put("molestia", item.registro?.molestiaArticular ?: 0)
            obj.put("notas", item.registro?.notas ?: "")
            array.put(obj)
        }
        root.put("sesiones", array)
        return root.toString(2)
    }

    fun importarEjerciciosJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val array = if (jsonString.trim().startsWith("[")) {
                    org.json.JSONArray(jsonString)
                } else {
                    org.json.JSONObject(jsonString).getJSONArray("ejercicios")
                }

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val nuevo = EjercicioEntity(
                        nombre = obj.getString("name"),
                        esEjercicioPrincipal = obj.optBoolean("es_principal", false),
                        rangoRepsMin = obj.optInt("reps_min", 8),
                        rangoRepsMax = obj.optInt("reps_max", 12),
                        pesoActualKg = obj.optDouble("peso_inicial", 0.0).toFloat(),
                        orden = (_ejercicios.value?.size ?: 0) + i + 1,
                        descansoSegundos = if (obj.has("rest_seconds")) obj.getInt("rest_seconds") else null,
                        tempo = if (obj.has("tempo")) obj.getString("tempo") else null,
                        protocoloCalentamiento = if (obj.has("warmup_protocol")) obj.getString("warmup_protocol") else null,
                        notasTendon = if (obj.has("tendon_notes")) obj.getString("tendon_notes") else null,
                        esIsometrico = obj.optBoolean("is_isometric", false),
                        seriesPredeterminadas = obj.optInt("sets", 3)
                    )
                    repository.insertarEjercicio(nuevo)
                }
                cargarEjercicios()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun eliminarTodo() {
        viewModelScope.launch {
            repository.eliminarTodaLaProgresion()
            _ejercicios.value = emptyList()
            _ejercicioSeleccionado.value = null
            _historial.value = emptyList()
            _ultimaSesionData.value = null
            cargarEjercicios()
        }
    }

    private fun cargarDatosEjercicio(ejercicioId: Long) {
        viewModelScope.launch {
            val ejercicio = repository.getEjercicioPorId(ejercicioId) ?: return@launch
            val ultimaSesion = repository.getUltimaSesion(ejercicioId)
            if (ultimaSesion != null) {
                val series = repository.getSeriesPorSesion(ultimaSesion.id)
                _ultimaSesionData.value = Pair(ultimaSesion, series)
            } else {
                _ultimaSesionData.value = null
            }

            // Cargar historial
            val sesiones = repository.getHistorialSesiones(ejercicioId)
            val fullHistorial = sesiones.map { sesion ->
                HistoryItem(
                    sesion = sesion,
                    series = repository.getSeriesPorSesion(sesion.id),
                    registro = repository.getRegistroDiarioPorSesion(sesion.id),
                    nombreEjercicio = ejercicio.nombre
                )
            }
            _historial.value = fullHistorial
        }
    }

    fun guardarSesion(
        series: List<SerieEntity>,
        molestia: Int,
        notas: String
    ) {
        val ejercicio = _ejercicioSeleccionado.value ?: return
        viewModelScope.launch {
            val sesion = SesionEntity(ejercicioId = ejercicio.id, fecha = System.currentTimeMillis())
            val registro = RegistroDiarioProgresionEntity(sesionId = 0, molestiaArticular = molestia, notas = notas)
            
            repository.guardarSesionCompleta(sesion, series, registro)
            
            // ACTUALIZAR PESO ACTUAL DEL EJERCICIO (para que el slider se guarde)
            val maxPesoEnSesion = series.maxOfOrNull { it.pesoKg } ?: ejercicio.pesoActualKg
            if (maxPesoEnSesion != ejercicio.pesoActualKg) {
                repository.actualizarEjercicio(ejercicio.copy(pesoActualKg = maxPesoEnSesion))
            }

            // Calcular sugerencia después de guardar
            val volumenPrevio = repository.getVolumenUltimos7Dias(ejercicio.id)
            val ultimosRegistros = repository.getUltimosDosRegistrosDiarios(ejercicio.id)
            val semanasConsecutivas = calcularSemanasConsecutivas(ejercicio.id)
            
            val result = useCase.evaluarProgresion(
                ejercicio,
                series,
                volumenPrevio,
                ultimosRegistros,
                semanasConsecutivas
            )
            _sugerencia.value = result
            
            cargarDatosEjercicio(ejercicio.id)
        }
    }

    private val _sesionesDelMes = MutableLiveData<List<HistoryItem>>()
    val sesionesDelMes: LiveData<List<HistoryItem>> = _sesionesDelMes

    fun cargarSesionesMes(mes: Int, anio: Int) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(anio, mes, 1, 0, 0, 0)
            val inicioMes = cal.timeInMillis
            cal.set(anio, mes, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            val finMes = cal.timeInMillis

            val ejerciciosMap = repository.getEjerciciosActivos().associateBy { it.id }
            val sesiones = repository.getSesionesEnRango(inicioMes, finMes)
            
            val historyItems = sesiones.map { sesion ->
                HistoryItem(
                    sesion = sesion,
                    series = repository.getSeriesPorSesion(sesion.id),
                    registro = repository.getRegistroDiarioPorSesion(sesion.id),
                    nombreEjercicio = ejerciciosMap[sesion.ejercicioId]?.nombre ?: "Ejercicio Desconocido"
                )
            }
            _sesionesDelMes.value = historyItems
        }
    }

    private suspend fun calcularSemanasConsecutivas(ejercicioId: Long): Int {
        val sesiones = repository.getHistorialSesiones(ejercicioId)
        if (sesiones.isEmpty()) return 0
        
        // Lógica simple: contar grupos de sesiones separadas por máximo 10 días
        var semanas = 1
        for (i in 0 until sesiones.size - 1) {
            val diff = sesiones[i].fecha - sesiones[i+1].fecha
            if (diff > 10 * 24 * 60 * 60 * 1000L) break
            if (diff > 5 * 24 * 60 * 60 * 1000L) semanas++
        }
        return (semanas / 1.5).roundToInt().coerceAtLeast(1) // Ajuste heurístico
    }
}

class ProgresionViewModelFactory(
    private val repository: ProgresionRepository,
    private val useCase: ProgresionUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProgresionViewModel(repository, useCase) as T
    }
}
