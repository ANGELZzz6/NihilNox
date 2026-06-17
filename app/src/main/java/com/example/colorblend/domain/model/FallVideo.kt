package com.example.colorblend.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fall_videos")
data class FallVideo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "file_name")
    val fileName: String,
    
    @ColumnInfo(name = "category")
    val category: String = "NEWS",
    
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis()
)
