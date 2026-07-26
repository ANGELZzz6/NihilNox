package com.example.colorblend.ui.gacha.metas.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.GachaPoolRepository
import com.example.colorblend.data.local.repository.PersonajeRepository
import com.example.colorblend.data.local.repository.UserStatsRepository
import com.example.colorblend.domain.model.PersonajeObtenido
import com.example.colorblend.network.ApolloClientProvider
import com.example.colorblend.graphql.GetRandomCharactersQuery
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.util.Collections
import com.example.colorblend.domain.model.Rareza
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class CharacterUI(
    val title: String?,
    val imageUrl: String?,
    val score: Int?,
    val origen: String? = null,
    val categoria: String? = "anime"
)

sealed class GachaEstado {
    object Idle : GachaEstado()
    object Cargando : GachaEstado()
    object SinMonedas : GachaEstado()
    data class Exito(val personajes: List<CharacterUI>) : GachaEstado()
    data class Error(val mensaje: String) : GachaEstado()
}

enum class TipoGacha {
    NORMAL,
    FEMENINO,
    MASCULINO
}

class AnimeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val personajeRepository = PersonajeRepository(db.personajeDao())
    private val userStatsRepository = UserStatsRepository(db.userStatsDao())
    private val gachaPoolRepository = GachaPoolRepository(application, db.personajePoolDao())

    private val _gachaEstado = MutableLiveData<GachaEstado>(GachaEstado.Idle)
    val gachaEstado: LiveData<GachaEstado> = _gachaEstado

    init {
        // Recargar pool silenciosamente al iniciar
        viewModelScope.launch {
            gachaPoolRepository.recargarPoolSiEsNecesario()
        }
    }

    companion object {
        const val COSTO_1X = 5
        const val COSTO_10X = 45
    }

    fun tirarGacha(cantidad: Int, tipo: TipoGacha = TipoGacha.NORMAL) {
        viewModelScope.launch {
            try {
                val costo = if (cantidad == 1) COSTO_1X else COSTO_10X

                val puedeTirar = userStatsRepository.restarMonedas(costo)
                if (!puedeTirar) {
                    _gachaEstado.postValue(GachaEstado.SinMonedas)
                    return@launch
                }

                _gachaEstado.postValue(GachaEstado.Cargando)

                val generoFiltro = when (tipo) {
                    TipoGacha.FEMENINO -> "Female"
                    TipoGacha.MASCULINO -> "Male"
                    else -> null
                }

                // ✅ Consumir del Pool Local (Diversidad automática)
                val personajesObtenidos = gachaPoolRepository.obtenerTiradaDivera(cantidad, generoFiltro)

                // Guardar en la colección del usuario
                withContext(Dispatchers.IO) {
                    for (p in personajesObtenidos) {
                        personajeRepository.guardarPersonaje(p)
                    }
                }

                // Convertir a UI
                val personajesUI = personajesObtenidos.map { p ->
                    CharacterUI(
                        title = p.nombre,
                        imageUrl = p.imagenUrl,
                        score = p.favoritos,
                        origen = p.animeTitulo,
                        categoria = p.categoria
                    )
                }

                _gachaEstado.postValue(GachaEstado.Exito(personajesUI))
                
                // Recargar pool para la próxima vez
                gachaPoolRepository.recargarPoolSiEsNecesario()

            } catch (e: Exception) {
                _gachaEstado.postValue(GachaEstado.Error(e.message ?: "Error desconocido"))
            }
        }
    }
}