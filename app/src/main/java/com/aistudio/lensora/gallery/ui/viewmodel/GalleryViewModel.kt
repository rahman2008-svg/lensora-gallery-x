package com.aistudio.lensora.gallery.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.lensora.gallery.data.dao.GalleryDao
import com.aistudio.lensora.gallery.data.database.LensoraDatabase
import com.aistudio.lensora.gallery.data.entity.MediaFavorite
import com.aistudio.lensora.gallery.data.entity.TrashItem
import com.aistudio.lensora.gallery.data.entity.VaultItem
import com.aistudio.lensora.gallery.data.model.MediaItem
import com.aistudio.lensora.gallery.data.preferences.SettingsManager
import com.aistudio.lensora.gallery.data.repository.GalleryRepository
import com.aistudio.lensora.gallery.data.repository.MediaLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LensoraDatabase.getDatabase(application)
    private val settingsManager = SettingsManager(application)
    private val mediaLoader = MediaLoader(application)
    private val repository = GalleryRepository(application, db.galleryDao(), mediaLoader)

    // UI state flows
    val mediaList: StateFlow<List<MediaItem>> = repository.getMediaListFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<MediaFavorite>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultItems: StateFlow<List<VaultItem>> = repository.getVaultItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashItems: StateFlow<List<TrashItem>> = repository.getTrashItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val slideshowSpeed: StateFlow<Int> = settingsManager.slideshowSpeedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000)

    val appTheme: StateFlow<String> = settingsManager.appThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val customAlbumFolders: StateFlow<List<String>> = settingsManager.customAlbumFoldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultPin: StateFlow<String?> = settingsManager.vaultPinFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Session UI State (Search query, Vault unlock, Slideshow index, permissions checked)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _activeSlideshowIndex = MutableStateFlow(0)
    val activeSlideshowIndex: StateFlow<Int> = _activeSlideshowIndex.asStateFlow()

    private val _isSlideshowRunning = MutableStateFlow(false)
    val isSlideshowRunning: StateFlow<Boolean> = _isSlideshowRunning.asStateFlow()

    private var slideshowJob: Job? = null

    init {
        // Run auto-purge of old trash items on VM startup
        viewModelScope.launch {
            try {
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                db.galleryDao().deleteOldTrashItems(thirtyDaysAgo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Search Logic ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Combine media lists and favorite table relations
    val isFavoriteMap: StateFlow<Map<Long, Boolean>> = favorites.combine(mediaList) { favs, _ ->
        favs.associateBy({ it.mediaId }, { true })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Media Action Operations ---
    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            val isFav = isFavoriteMap.value[item.id] ?: false
            repository.setFavorite(item, !isFav)
        }
    }

    fun deleteMediaToTrash(item: MediaItem) {
        viewModelScope.launch {
            repository.moveToTrash(item)
            // also remove favorite associated if deleted
            repository.setFavorite(item, false)
        }
    }

    fun moveMediaToVault(item: MediaItem) {
        viewModelScope.launch {
            repository.moveToVault(item)
            // also remove favorite associated
            repository.setFavorite(item, false)
        }
    }

    // --- Trash Actions ---
    fun restoreTrashItem(item: TrashItem) {
        viewModelScope.launch {
            repository.restoreFromTrash(item)
        }
    }

    fun purgeTrashItemPermanently(item: TrashItem) {
        viewModelScope.launch {
            repository.deleteTrashItemPermanently(item)
        }
    }

    fun clearAllTrash() {
        viewModelScope.launch {
            repository.clearRecycleBin()
        }
    }

    // --- Vault Actions ---
    fun restoreVaultItem(item: VaultItem) {
        viewModelScope.launch {
            repository.restoreFromVault(item)
        }
    }

    fun purgeVaultItemPermanently(item: VaultItem) {
        viewModelScope.launch {
            repository.deleteVaultItemPermanently(item)
        }
    }

    fun setVaultPin(pin: String) {
        viewModelScope.launch {
            settingsManager.saveVaultPin(pin)
            _isVaultUnlocked.value = true
        }
    }

    fun verifyVaultPin(pin: String): Boolean {
        val actualPin = vaultPin.value
        val matches = actualPin == pin
        if (matches) {
            _isVaultUnlocked.value = true
        }
        return matches
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun resetVaultPin(oldPin: String, newPin: String): Boolean {
        return if (vaultPin.value == oldPin) {
            viewModelScope.launch {
                settingsManager.saveVaultPin(newPin)
            }
            true
        } else {
            false
        }
    }

    // --- Preferences / Custom Folders ---
    fun updateAppTheme(theme: String) {
        viewModelScope.launch {
            settingsManager.saveAppTheme(theme)
        }
    }

    fun updateSlideshowSpeed(speedMs: Int) {
        viewModelScope.launch {
            settingsManager.saveSlideshowSpeed(speedMs)
        }
    }

    fun addCustomAlbumFolder(path: String) {
        viewModelScope.launch {
            settingsManager.addCustomAlbumFolder(path)
        }
    }

    fun removeCustomAlbumFolder(path: String) {
        viewModelScope.launch {
            settingsManager.removeCustomAlbumFolder(path)
        }
    }


    // --- Slideshow Controller ---
    fun startSlideshow(mediaCount: Int) {
        _isSlideshowRunning.value = true
        slideshowJob?.cancel()
        slideshowJob = viewModelScope.launch {
            while (_isSlideshowRunning.value && mediaCount > 0) {
                delay(slideshowSpeed.value.toLong())
                val nextIndex = (_activeSlideshowIndex.value + 1) % mediaCount
                _activeSlideshowIndex.value = nextIndex
            }
        }
    }

    fun stopSlideshow() {
        _isSlideshowRunning.value = false
        slideshowJob?.cancel()
    }

    fun setSlideshowIndex(index: Int) {
        _activeSlideshowIndex.value = index
    }


    // --- Grouping Helper inside ViewModel ---
    fun groupMediaItems(items: List<MediaItem>): Map<String, List<MediaItem>> {
        val query = searchQuery.value.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            items
        } else {
            items.filter {
                it.name.lowercase().contains(query) ||
                it.albumName.lowercase().contains(query) ||
                (it.isVideo && "video".contains(query)) ||
                (!it.isVideo && "photo".contains(query))
            }
        }

        val format = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        return filtered.groupBy { item ->
            try {
                val date = Date(item.dateModified * 1000)
                val dayStr = format.format(date)
                
                // Compare with Today / Yesterday
                val todayStr = format.format(Date())
                val yesterdayStr = format.format(Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000))
                
                when (dayStr) {
                    todayStr -> "Today"
                    yesterdayStr -> "Yesterday"
                    else -> dayStr
                }
            } catch (e: Exception) {
                "Unknown Date"
            }
        }
    }
}
