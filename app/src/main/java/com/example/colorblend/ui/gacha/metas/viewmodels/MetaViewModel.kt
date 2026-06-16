package com.example.colorblend.ui.gacha.metas.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblend.data.local.AppDatabase
import com.example.colorblend.data.local.repository.MetaRepository
import com.example.colorblend.domain.model.Meta
import com.example.colorblend.domain.model.MetaImagenDia
import com.example.colorblend.domain.model.TipoMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MetaViewModel(application: Application) : AndroidViewModel(application) {

    private val database   = AppDatabase.getDatabase(application)
    private val repository = MetaRepository(
        database.metaDao(),
        database.userStatsDao(),
        database.metaImagenDao()
    )

    val metas = repository.getMetas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun crearMeta(
        titulo           : String,
        descripcion      : String?,
        tipo             : TipoMeta,
        objetivo         : Int,
        diasSemana       : String? = null,
        horaRecordatorio : String? = null,
        onError          : (String) -> Unit = {},
        onExito          : (Meta) -> Unit = {}   // ← devuelve la Meta creada
    ) {
        viewModelScope.launch {
            if (repository.existeTituloRepetido(titulo)) {
                onError("Ya existe una meta con ese título.")
                return@launch
            }
            val meta = Meta(
                titulo           = titulo,
                descripcion      = descripcion,
                tipo             = tipo,
                objetivo         = objetivo,
                diasSemana       = diasSemana,
                horaRecordatorio = horaRecordatorio
            )
            val id = repository.crearMeta(meta)
            onExito(meta.copy(id = id.toInt()))
        }
    }

    fun actualizarRecordatorio(meta: Meta, horaRecordatorio: String?) {
        viewModelScope.launch {
            repository.actualizarMeta(meta.copy(horaRecordatorio = horaRecordatorio))
        }
    }

    fun eliminarMeta(meta: Meta) {
        viewModelScope.launch { repository.eliminarMeta(meta) }
    }

    fun cumplirMeta(meta: Meta, monedas: Int = 50) {
        viewModelScope.launch { repository.cumplirMeta(meta, monedas) }
    }

    fun sumarProgreso(meta: Meta, cantidad: Int = 1, monedas: Int = 5) {
        viewModelScope.launch { repository.sumarProgreso(meta, cantidad, monedas) }
    }

    // ─── Imágenes ─────────────────────────────────────────────────────────────

    fun getImagenesPorMeta(metaId: Int): Flow<List<MetaImagenDia>> =
        repository.getImagenesPorMeta(metaId)

    fun agregarImagen(metaId: Int, rutaImagen: String) {
        viewModelScope.launch { repository.agregarImagen(metaId, rutaImagen) }
    }

    fun eliminarImagen(imagen: MetaImagenDia) {
        viewModelScope.launch { repository.eliminarImagen(imagen) }
    }
}