package com.aistudio.lensora.gallery.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_favorites")
data class MediaFavorite(
    @PrimaryKey val mediaId: Long,
    val filePath: String,
    val dateAdded: Long = System.currentTimeMillis()
)
