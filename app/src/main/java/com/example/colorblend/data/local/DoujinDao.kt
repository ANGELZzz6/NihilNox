package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.DoujinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoujinDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarDoujin(doujin: DoujinEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(doujin: DoujinEntity)

    @Delete
    suspend fun eliminarDoujin(doujin: DoujinEntity)

    @Query("SELECT * FROM doujins_guardados ORDER BY fechaGuardado DESC")
    fun obtenerTodos(): Flow<List<DoujinEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM doujins_guardados WHERE id = :id)")
    suspend fun esFavorito(id: String): Boolean

    @Query("SELECT * FROM doujins_guardados WHERE id = :id")
    suspend fun obtenerPorId(id: String): DoujinEntity?
}
