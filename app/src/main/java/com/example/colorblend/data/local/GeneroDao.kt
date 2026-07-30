package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.Genero

@Dao
interface GeneroDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(genero: Genero)

    @Query("SELECT * FROM generos")
    suspend fun obtenerTodos(): List<Genero>

    @Delete
    suspend fun eliminar(genero: Genero)

    @Query("DELETE FROM generos WHERE id = :id")
    suspend fun eliminarPorId(id: Int)
}
