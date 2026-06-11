package com.aistudio.lensora.gallery.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_items")
data class TrashItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalPath: String,
    val deletedPath: String,
    val name: String,
    val size: Long,
    val isVideo: Boolean,
    val duration: Long,
    val dateDeleted: Long = System.currentTimeMillis()
)
