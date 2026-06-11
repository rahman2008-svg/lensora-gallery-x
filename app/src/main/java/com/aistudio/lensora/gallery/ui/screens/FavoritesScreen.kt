package com.aistudio.lensora.gallery.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aistudio.lensora.gallery.data.model.MediaItem
import com.aistudio.lensora.gallery.ui.viewmodel.GalleryViewModel

@Composable
fun FavoritesScreen(
    viewModel: GalleryViewModel,
    onMediaClick: (index: Int, filteredList: List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val rawMedia by viewModel.mediaList.collectAsState()
    val favoritedEntities by viewModel.favorites.collectAsState()

    // Filter media items which exist in local favorites table
    val favoritedMedia = remember(rawMedia, favoritedEntities) {
        val favIdSet = favoritedEntities.map { it.mediaId }.toSet()
        // Or if it matches by path
        val favPathSet = favoritedEntities.map { it.filePath }.toSet()
        
        rawMedia.filter { item ->
            favIdSet.contains(item.id) || favPathSet.contains(item.path)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Favorites",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (favoritedMedia.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "No Favorites",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        text = "No favorites yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Double-tap/Hold an item in Gallery or click the Heart in Media Viewer to add it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favoritedMedia) { item ->
                    val indexInFavs = favoritedMedia.indexOf(item)
                    MediaGridItem(
                        item = item,
                        isFavorite = true,
                        onClick = {
                            if (indexInFavs != -1) {
                                onMediaClick(indexInFavs, favoritedMedia)
                            }
                        },
                        onLongClick = {
                            viewModel.toggleFavorite(item)
                        }
                    )
                }
            }
        }
    }
}
