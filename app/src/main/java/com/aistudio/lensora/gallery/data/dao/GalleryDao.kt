package com.aistudio.lensora.gallery.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aistudio.lensora.gallery.data.entity.MediaFavorite
import com.aistudio.lensora.gallery.data.entity.TrashItem
import com.aistudio.lensora.gallery.data.entity.VaultItem
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {

    // --- Favorites ---
    @Query("SELECT * FROM media_favorites ORDER BY dateAdded DESC")
    fun getAllFavorites(): Flow<List<MediaFavorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: MediaFavorite)

    @Query("DELETE FROM media_favorites WHERE mediaId = :mediaId")
    suspend fun removeFavorite(mediaId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM media_favorites WHERE mediaId = :mediaId LIMIT 1)")
    fun isFavoriteFlow(mediaId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM media_favorites WHERE mediaId = :mediaId LIMIT 1)")
    suspend fun isFavoriteDirect(mediaId: Long): Boolean


    // --- Private Vault ---
    @Query("SELECT * FROM vault_items ORDER BY dateAdded DESC")
    fun getAllVaultItems(): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItem)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItem(id: Int)

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getVaultItemById(id: Int): VaultItem?


    // --- Recycle Bin (Trash) ---
    @Query("SELECT * FROM trash_items ORDER BY dateDeleted DESC")
    fun getAllTrashItems(): Flow<List<TrashItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(item: TrashItem)

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteTrashItem(id: Int)

    @Query("SELECT * FROM trash_items WHERE id = :id LIMIT 1")
    suspend fun getTrashItemById(id: Int): TrashItem?

    @Query("DELETE FROM trash_items WHERE dateDeleted < :timestamp")
    suspend fun deleteOldTrashItems(timestamp: Long)
}
