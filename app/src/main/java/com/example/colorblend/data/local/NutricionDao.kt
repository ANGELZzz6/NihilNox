package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.AlimentoGuardado
import com.example.colorblend.domain.model.AnalisisDia
import com.example.colorblend.domain.model.PerfilNutricion
import com.example.colorblend.domain.model.RegistroAlimento
import kotlinx.coroutines.flow.Flow

@Dao
interface NutricionDao {

    // ── Perfil ────────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPerfil(perfil: PerfilNutricion)

    @Query("SELECT * FROM perfil_nutricion WHERE id = 1")
    fun observarPerfil(): Flow<PerfilNutricion?>

    @Query("SELECT * FROM perfil_nutricion WHERE id = 1")
    suspend fun getPerfil(): PerfilNutricion?

    // ── Registros del día ─────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAlimento(alimento: RegistroAlimento)

    @Delete
    suspend fun eliminarAlimento(alimento: RegistroAlimento)

    @Query("SELECT * FROM registro_alimento WHERE fecha = :fecha ORDER BY categoria, id")
    fun observarAlimentosDia(fecha: String): Flow<List<RegistroAlimento>>

    @Query("SELECT * FROM registro_alimento WHERE fecha = :fecha ORDER BY categoria, id")
    suspend fun getAlimentosDia(fecha: String): List<RegistroAlimento>  // ← sin cuerpo

    @Query("""
        SELECT fecha,
               SUM(calorias)  AS calorias,
               SUM(proteina)  AS proteina,
               SUM(carbos)    AS carbos,
               SUM(grasas)    AS grasas,
               SUM(fibra)     AS fibra,
               SUM(azucares)  AS azucares
        FROM registro_alimento
        WHERE fecha >= :fechaInicio
        GROUP BY fecha
        ORDER BY fecha DESC
    """)
    suspend fun getResumenSemana(fechaInicio: String): List<ResumenDia>

    // ── Alimentos guardados (cache) ───────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarAlimentoGuardado(alimento: AlimentoGuardado)

    @Query("UPDATE alimento_guardado SET vecesUsado = vecesUsado + 1 WHERE nombre = :nombre")
    suspend fun incrementarUso(nombre: String)

    @Query("""
        SELECT * FROM alimento_guardado 
        WHERE nombre LIKE '%' || :query || '%' 
        ORDER BY vecesUsado DESC 
        LIMIT 20
    """)
    suspend fun buscarAlimentosGuardados(query: String): List<AlimentoGuardado>

    @Query("SELECT * FROM alimento_guardado ORDER BY vecesUsado DESC LIMIT 10")
    fun observarFrecuentes(): Flow<List<AlimentoGuardado>>

    // ── Análisis IA ───────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarAnalisis(analisis: AnalisisDia)

    @Query("SELECT * FROM analisis_dia WHERE fecha = :fecha")
    suspend fun getAnalisisPorFecha(fecha: String): AnalisisDia?

    @Query("SELECT * FROM analisis_dia ORDER BY fecha DESC LIMIT 30")
    suspend fun getHistorialAnalisis(): List<AnalisisDia>

    @Query("SELECT * FROM analisis_dia ORDER BY fecha DESC LIMIT 30")
    fun observarHistorialAnalisis(): Flow<List<AnalisisDia>>

    @Query("UPDATE analisis_dia SET recompensaReclamada = 1 WHERE fecha = :fecha")
    suspend fun marcarRecompensaReclamada(fecha: String)

    @Query("UPDATE analisis_dia SET monedasRecompensa = :monedas WHERE fecha = :fecha")
    suspend fun guardarMonedasRecompensa(fecha: String, monedas: Int)
}

data class ResumenDia(
    val fecha: String,
    val calorias: Int,
    val proteina: Float,
    val carbos: Float,
    val grasas: Float,
    val fibra: Float,
    val azucares: Float
)