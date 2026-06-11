package com.aistudio.lensora.gallery.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aistudio.lensora.gallery.data.dao.GalleryDao
import com.aistudio.lensora.gallery.data.entity.MediaFavorite
import com.aistudio.lensora.gallery.data.entity.TrashItem
import com.aistudio.lensora.gallery.data.entity.VaultItem

@Database(
    entities = [MediaFavorite::class, VaultItem::class, TrashItem::class],
    version = 1,
    exportSchema = false
)
abstract class LensoraDatabase : RoomDatabase() {
    abstract fun galleryDao(): GalleryDao

    companion object {
        @Volatile
        private var INSTANCE: LensoraDatabase? = null

        fun getDatabase(context: Context): LensoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LensoraDatabase::class.java,
                    "lensora_gallery_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
