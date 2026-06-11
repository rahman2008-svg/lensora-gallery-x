package com.aistudio.lensora.gallery.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.aistudio.lensora.gallery.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaLoader(private val context: Context) {

    suspend fun loadLocalMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()

        // 1. Load Images
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val imageSortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                imageUri,
                imageProjection,
                null,
                null,
                imageSortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val albumColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Image_$id"
                    val path = cursor.getString(pathColumn) ?: ""
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateColumn)
                    
                    // Album determination
                    val albumName = if (albumColumn != -1) {
                        cursor.getString(albumColumn)
                    } else {
                        null
                    } ?: determineAlbumFromPath(path)

                    val contentUri = ContentUris.withAppendedId(imageUri, id).toString()

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uriString = contentUri,
                            name = name,
                            path = path,
                            width = width,
                            height = height,
                            size = size,
                            duration = 0,
                            isVideo = false,
                            dateModified = dateModified,
                            albumName = albumName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Load Videos
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )
        val videoSortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                videoUri,
                videoProjection,
                null,
                null,
                videoSortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val albumColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Video_$id"
                    val path = cursor.getString(pathColumn) ?: ""
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateColumn)
                    val duration = cursor.getLong(durationColumn)
                    
                    val albumName = if (albumColumn != -1) {
                        cursor.getString(albumColumn)
                    } else {
                        null
                    } ?: determineAlbumFromPath(path)

                    val contentUri = ContentUris.withAppendedId(videoUri, id).toString()

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uriString = contentUri,
                            name = name,
                            path = path,
                            width = width,
                            height = height,
                            size = size,
                            duration = duration,
                            isVideo = true,
                            dateModified = dateModified,
                            albumName = albumName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort combined list by date modified DESC
        mediaList.sortByDescending { it.dateModified }
        mediaList
    }

    private fun determineAlbumFromPath(path: String): String {
        if (path.isEmpty()) return "Other"
        val file = File(path)
        val parent = file.parentFile ?: return "Other"
        val parentName = parent.name
        
        return when {
            parentName.equals("Camera", ignoreCase = true) || parentName.contains("DCIM", ignoreCase = true) -> "Camera"
            parentName.equals("Screenshots", ignoreCase = true) -> "Screenshots"
            parentName.equals("Download", ignoreCase = true) || parentName.equals("Downloads", ignoreCase = true) -> "Downloads"
            parentName.contains("WhatsApp", ignoreCase = true) -> "WhatsApp Images"
            parentName.contains("Facebook", ignoreCase = true) -> "Facebook"
            parentName.contains("Instagram", ignoreCase = true) -> "Instagram"
            else -> parentName
        }
    }
}
