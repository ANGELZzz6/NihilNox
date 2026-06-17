package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.Cancion

@Dao
interface CancionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(cancion: Cancion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarMultiples(canciones: List<Cancion>)

    @Delete
    suspend fun eliminar(cancion: Cancion)

    @Query("DELETE FROM canciones WHERE id = :id")
    suspend fun eliminarPorId(id: Int)

    @Query("SELECT * FROM canciones WHERE playlist_id = :playlistId")
    suspend fun obtenerCancionesPorPlaylist(playlistId: String): List<Cancion>

    @Query("SELECT * FROM canciones WHERE uri_spotify = :uriSpotify")
    suspend fun buscarPorUriSpotify(uriSpotify: String): Cancion?

    @Query("SELECT uri_spotify FROM canciones WHERE uri_spotify IS NOT NULL AND uri_spotify != ''")
    suspend fun obtenerTodosLosUrisSpotify(): List<String>

    @Query("SELECT titulo FROM canciones WHERE uri_spotify = :uriSpotify LIMIT 1")
    suspend fun obtenerTituloPorUriSpotify(uriSpotify: String): String?

    @Query("DELETE FROM canciones WHERE uri_spotify = :uriSpotify")
    suspend fun eliminarPorUriSpotify(uriSpotify: String)

    @Query("SELECT * FROM canciones")
    suspend fun obtenerTodas(): List<Cancion>
}
