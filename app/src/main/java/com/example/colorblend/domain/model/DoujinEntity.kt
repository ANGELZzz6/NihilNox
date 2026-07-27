package com.example.colorblend.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doujins_guardados")
data class DoujinEntity(
    @PrimaryKey val id: String,
    val title: String,
    val coverUrl: String,
    val source: String,
    val fechaGuardado: Long = System.currentTimeMillis(),
    val artist: String = "",
    val totalPages: Int = 0,
    val isDownloaded: Boolean = false,
    val localPath: String? = null,
    val downloadProgress: Int = 0,
    val downloadStatus: String = "IDLE" // IDLE, DOWNLOADING, COMPLETED, ERROR
)
