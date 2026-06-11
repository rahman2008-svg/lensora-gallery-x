package com.aistudio.lensora.gallery.data.model

data class MediaItem(
    val id: Long,
    val uriString: String,
    val name: String,
    val path: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val duration: Long = 0,
    val isVideo: Boolean,
    val dateModified: Long, // timestamp for date organization
    val albumName: String
)
