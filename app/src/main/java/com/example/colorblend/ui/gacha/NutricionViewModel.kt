package com.example.colorblend.ui.gacha

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.ResumenDia
import com.example.colorblend.data.local.repository.MacrosEstimados
import com.example.colorblend.data.local.repository.NutricionRepository
import com.example.colorblend.data.local.repository.UserStatsRepository
import com.example.colorblend.domain.model.AlimentoGuardado
import com.example.colorblend.domain.model.AnalisisDia
import com.example.colorblend.domain.model.PerfilNutricion
import com.example.colorblend.domain.model.RegistroAlimento
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ResumenNutricionDia(
    val calorias: Int = 0,
    val proteina: Float = 0f,
    val carbos: Float = 0f,
    val grasas: Float = 0f,
    val fibra: Float = 0f,
    val azucares: Float = 0f
)

data class RecompensaEvento(val monedas: Int, val mensaje: String)

class NutricionViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = NutricionRepository(
        AppDatabase.getDatabase(application).nutricionDao()
    )
    private val userStatsRepo = UserStatsRepository(
        AppDatabase.getDatabase(application).userStatsDao()
    )

    val hoy: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // ── Estados ───────────────────────────────────────────────────────────────
    private val _perfil = MutableStateFlow<PerfilNutricion?>(null)
    val perfil: StateFlow<PerfilNutricion?> = _perfil

    private val _alimentosDia = MutableStateFlow<List<RegistroAlimento>>(emptyList())
    val alimentosDia: StateFlow<List<RegistroAlimento>> = _alimentosDia

    private val _resumenHoy = MutableStateFlow(ResumenNutricionDia())
    val resumenHoy: StateFlow<ResumenNutricionDia> = _resumenHoy

    private val _resumenSemana = MutableStateFlow<List<ResumenDia>>(emptyList())
    val resumenSemana: StateFlow<List<ResumenDia>> = _resumenSemana

    private val _busquedaResultados = MutableStateFlow<List<AlimentoGuardado>>(emptyList())
    val busquedaResultados: StateFlow<List<AlimentoGuardado>> = _busquedaResultados

    private val _frecuentes = MutableStateFlow<List<AlimentoGuardado>>(emptyList())
    val frecuentes: StateFlow<List<AlimentoGuardado>> = _frecuentes

    private val _cargandoBusqueda = MutableStateFlow(false)
    val cargandoBusqueda: StateFlow<Boolean> = _cargandoBusqueda

    private val _analisisHoy = MutableStateFlow<AnalisisDia?>(null)
    val analisisHoy: StateFlow<AnalisisDia?> = _analisisHoy

    private val _cargandoAnalisis = MutableStateFlow(false)
    val cargandoAnalisis: StateFlow<Boolean> = _cargandoAnalisis

    private val _errorAnalisis = MutableStateFlow<String?>(null)
    val errorAnalisis: StateFlow<String?> = _errorAnalisis

    private val _historial = MutableStateFlow<List<AnalisisDia>>(emptyList())
    val historial: StateFlow<List<AnalisisDia>> = _historial

    private val _recompensaEvento = MutableSharedFlow<RecompensaEvento>(replay = 1)
    val recompensaEvento = _recompensaEvento

    // ── Estados estimación IA ─────────────────────────────────────────────────
    private val _macrosEstimados = MutableSharedFlow<MacrosEstimados>(replay = 1)
    val macrosEstimados = _macrosEstimados

    private val _cargandoEstimacion = MutableStateFlow(false)
    val cargandoEstimacion: StateFlow<Boolean> = _cargandoEstimacion

    private val _errorEstimacion = MutableStateFlow<String?>(null)
    val errorEstimacion: StateFlow<String?> = _errorEstimacion

    init {
        observarPerfil()
        observarAlimentosDia()
        observarFrecuentes()
        cargarResumenSemana()
        cargarAnalisisHoy()
        cargarHistorial()
        verificarAnalisisAyer()
    }

    // ── Observadores ──────────────────────────────────────────────────────────
    private fun observarPerfil() = viewModelScope.launch {
        repo.observarPerfil().collectLatest { _perfil.value = it }
    }

    private fun observarAlimentosDia() = viewModelScope.launch {
        repo.observarAlimentosDia(hoy).collectLatest { lista ->
            _alimentosDia.value = lista
            _resumenHoy.value = ResumenNutricionDia(
                calorias = lista.sumOf { it.calorias },
                proteina = lista.sumOf { it.proteina.toDouble() }.toFloat(),
                carbos   = lista.sumOf { it.carbos.toDouble() }.toFloat(),
                grasas   = lista.sumOf { it.grasas.toDouble() }.toFloat(),
                fibra    = lista.sumOf { it.fibra.toDouble() }.toFloat(),
                azucares = lista.sumOf { it.azucares.toDouble() }.toFloat()
            )
        }
    }

    private fun observarFrecuentes() = viewModelScope.launch {
        repo.observarFrecuentes().collectLatest { _frecuentes.value = it }
    }

    private fun cargarResumenSemana() = viewModelScope.launch {
        _resumenSemana.value = repo.getResumenSemana()
    }

    private fun cargarAnalisisHoy() = viewModelScope.launch {
        _analisisHoy.value = repo.getAnalisisPorFecha(hoy)
    }

    private fun cargarHistorial() = viewModelScope.launch {
        _historial.value = repo.getHistorialAnalisis()
    }

    // ── Perfil ────────────────────────────────────────────────────────────────
    fun guardarPerfil(
        peso: Float, altura: Int, edad: Int,
        sexo: String, nivelActividad: String, objetivo: String
    ) = viewModelScope.launch {
        val perfil = repo.calcularMetas(peso, altura, edad, sexo, nivelActividad, objetivo)
        repo.guardarPerfil(perfil)
    }

    // ── Alimentos ─────────────────────────────────────────────────────────────
    fun agregarAlimento(alimento: RegistroAlimento) = viewModelScope.launch {
        repo.agregarAlimento(alimento)
    }

    fun eliminarAlimento(alimento: RegistroAlimento) = viewModelScope.launch {
        repo.eliminarAlimento(alimento)
        cargarResumenSemana()
    }

    // ── Búsqueda ──────────────────────────────────────────────────────────────
    fun buscar(query: String) = viewModelScope.launch {
        if (query.length < 2) {
            _busquedaResultados.value = emptyList()
            return@launch
        }
        _cargandoBusqueda.value = true
        val locales = repo.buscarAlimentosGuardados(query)
        _busquedaResultados.value = locales
        val remotos = repo.buscarEnFuentesRemotas(query)
        val combinados = (locales + remotos)
            .distinctBy { it.nombre.lowercase() }
            .take(15)
        _busquedaResultados.value = combinados
        _cargandoBusqueda.value = false
    }

    fun limpiarBusqueda() {
        _busquedaResultados.value = emptyList()
    }

    // ── Estimación de macros con IA ───────────────────────────────────────────
    fun estimarMacros(
        context: android.content.Context,
        nombrePlato: String,
        pesoGramos: Float
    ) = viewModelScope.launch {
        if (nombrePlato.isBlank()) {
            _errorEstimacion.value = "Escribe el nombre del plato primero"
            return@launch
        }
        if (pesoGramos <= 0) {
            _errorEstimacion.value = "Ingresa el peso en gramos"
            return@launch
        }
        val groqKey = com.example.colorblend.data.local.ApiKeysManager.get(
            context, com.example.colorblend.data.local.ApiKeysManager.KEY_GROQ
        )
        if (groqKey.isBlank()) {
            _errorEstimacion.value = "Configura tu API key de Groq primero"
            return@launch
        }
        _cargandoEstimacion.value = true
        _errorEstimacion.value = null

        repo.estimarMacrosConIA(nombrePlato, pesoGramos, groqKey)
            .onSuccess { macros ->
                _macrosEstimados.emit(macros)
            }
            .onFailure {
                _errorEstimacion.value = "No se pudo estimar. Intenta de nuevo."
            }

        _cargandoEstimacion.value = false
    }

    // ── Análisis Nutrición (durante el día — SIN monedas) ────────────────────
    fun analizarNutricion(context: android.content.Context) = viewModelScope.launch {
        val perfil = _perfil.value ?: run {
            _errorAnalisis.value = "No tienes perfil configurado"
            return@launch
        }
        val alimentos = _alimentosDia.value
        if (alimentos.isEmpty()) {
            _errorAnalisis.value = "No hay alimentos registrados hoy"
            return@launch
        }
        val groqKey = com.example.colorblend.data.local.ApiKeysManager.get(
            context, com.example.colorblend.data.local.ApiKeysManager.KEY_GROQ
        )
        if (groqKey.isBlank()) {
            _errorAnalisis.value = "Configura tu API key de Groq primero"
            return@launch
        }
        _cargandoAnalisis.value = true
        _errorAnalisis.value = null
        val resultado = repo.analizarDiaConIA(hoy, alimentos, perfil, groqKey)
        resultado.fold(
            onSuccess = { analisis ->
                _analisisHoy.value = analisis
                cargarHistorial()
            },
            onFailure = {
                _errorAnalisis.value = "Error al analizar: ${it.message}"
            }
        )
        _cargandoAnalisis.value = false
    }

    // ── Análisis automático ayer (CON monedas — día completo) ─────────────────
    private fun verificarAnalisisAyer() = viewModelScope.launch {
        val ayer = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).let { fmt ->
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            fmt.format(cal.time)
        }
        val yaAnalizado = repo.getAnalisisPorFecha(ayer)
        if (yaAnalizado != null) return@launch

        val alimentosAyer = repo.getAlimentosDia(ayer)
        if (alimentosAyer.isEmpty()) return@launch

        val perfil = repo.getPerfil() ?: return@launch
        val groqKey = com.example.colorblend.data.local.ApiKeysManager.get(
            getApplication(), com.example.colorblend.data.local.ApiKeysManager.KEY_GROQ
        )
        if (groqKey.isBlank()) return@launch

        repo.analizarDiaCompletadoConIA(ayer, alimentosAyer, perfil, groqKey)
            .onSuccess { analisis ->
                val recompensa = repo.calcularRecompensaConIA(
                    analisis.resumenTexto, perfil, groqKey
                )
                repo.guardarMonedasRecompensa(ayer, recompensa.monedas)
                cargarHistorial()
            }
    }

    // ── El usuario reclama su recompensa manualmente ──────────────────────────────
    fun reclamarRecompensa(analisis: AnalisisDia) = viewModelScope.launch {
        if (analisis.recompensaReclamada || analisis.monedasRecompensa <= 0) return@launch
        userStatsRepo.addMonedas(analisis.monedasRecompensa)
        repo.marcarRecompensaReclamada(analisis.fecha)
        cargarHistorial()
        _recompensaEvento.emit(
            RecompensaEvento(analisis.monedasRecompensa, "¡Recompensa reclamada!")
        )
    }
}