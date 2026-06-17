package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.FallVideo

@Dao
interface FallVideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: FallVideo)

    @Query("SELECT * FROM fall_videos ORDER BY date_added DESC")
    suspend fun getAllVideos(): List<FallVideo>

    @Update
    suspend fun updateVideo(video: FallVideo)

    @Query("SELECT * FROM fall_videos WHERE file_path = :path LIMIT 1")
    suspend fun getVideoByPath(path: String): FallVideo?

    @Query("SELECT COUNT(*) FROM fall_videos WHERE category IN ('FUEGO', 'REFLEXION')")
    suspend fun getCategorizedCount(): Int

    @Query("SELECT * FROM fall_videos WHERE category = :category ORDER BY RANDOM()")
    suspend fun getVideosByCategory(category: String): List<FallVideo>

    @Query("SELECT * FROM fall_videos WHERE category = 'NEWS' ORDER BY date_added DESC")
    suspend fun getNewsVideos(): List<FallVideo>

    @Query("SELECT * FROM fall_videos WHERE category != 'NEWS' AND category != :excludeCategory ORDER BY RANDOM()")
    suspend fun getVideosByOtherCategory(excludeCategory: String): List<FallVideo>
}
