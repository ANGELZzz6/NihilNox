package com.example.colorblend.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "canciones",
    indices = [Index(value = ["uri_spotify"], name = "idx_uri_spotify")]
)
data class Cancion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "uri_local") val uriLocal: String,        // Ruta en almacenamiento
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "artista", defaultValue = "") val artista: String = "",
    @ColumnInfo(name = "playlist_id") val playlistId: String,    // ID o nombre de playlist
    @ColumnInfo(name = "uri_spotify", defaultValue = "") val uriSpotify: String = "", // URL de Spotify (clave para duplicados)
    @ColumnInfo(name = "fecha_agregada", defaultValue = "1781733156835") val fechaAgregada: Long = System.currentTimeMillis()
)
