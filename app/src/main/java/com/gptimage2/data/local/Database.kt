package com.gptimage2.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.gptimage2.data.model.GalleryImage
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    @Query("SELECT * FROM gallery_images ORDER BY createdAt DESC")
    fun getAllImages(): Flow<List<GalleryImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GalleryImage)

    @Delete
    suspend fun deleteImage(image: GalleryImage)

    @Query("UPDATE gallery_images SET savedToAlbum = 1 WHERE id = :id")
    suspend fun markSavedToAlbum(id: String)
}

@Database(entities = [GalleryImage::class], version = 1, exportSchema = false)
abstract class GptImageDatabase : RoomDatabase() {
    abstract fun galleryDao(): GalleryDao

    companion object {
        const val DATABASE_NAME = "gptimage2_db"
    }
}
