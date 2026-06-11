package com.aistudio.lensora.gallery.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalPath: String,
    val encryptedPath: String,
    val name: String,
    val size: Long,
    val duration: Long,
    val isVideo: Boolean,
    val dateAdded: Long = System.currentTimeMillis()
)
