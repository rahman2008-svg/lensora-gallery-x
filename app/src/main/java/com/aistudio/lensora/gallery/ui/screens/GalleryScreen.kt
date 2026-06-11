package com.aistudio.lensora.gallery.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.lensora.gallery.data.model.MediaItem
import com.aistudio.lensora.gallery.ui.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onMediaClick: (index: Int, filteredList: List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val rawMedia by viewModel.mediaList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isFavoriteMap by viewModel.isFavoriteMap.collectAsState()

    var activeTabFilter by remember { mutableStateOf("all") } // "all", "photos", "videos"

    // Filter by type
    val filteredMedia = remember(rawMedia, activeTabFilter) {
        when (activeTabFilter) {
            "photos" -> rawMedia.filter { !it.isVideo }
            "videos" -> rawMedia.filter { it.isVideo }
            else -> rawMedia
        }
    }

    // Grouping by date
    val groupedSections = remember(filteredMedia, searchQuery) {
        viewModel.groupMediaItems(filteredMedia)
    }

    // Flatten representation for Index retrieval in swiper detail view
    val flatFilteredList = remember(groupedSections) {
        groupedSections.values.flatten()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search photos, videos, albums...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeTabFilter == "all",
                        onClick = { activeTabFilter = "all" },
                        label = { Text("All Media") },
                        leadingIcon = if (activeTabFilter == "all") {
                            { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("filter_all")
                    )
                    FilterChip(
                        selected = activeTabFilter == "photos",
                        onClick = { activeTabFilter = "photos" },
                        label = { Text("Photos") },
                        leadingIcon = if (activeTabFilter == "photos") {
                            { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("filter_photos")
                    )
                    FilterChip(
                        selected = activeTabFilter == "videos",
                        onClick = { activeTabFilter = "videos" },
                        label = { Text("Videos") },
                        leadingIcon = if (activeTabFilter == "videos") {
                            { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("filter_videos")
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (groupedSections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NoPhotography,
                        contentDescription = "No files",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        text = "No matching media found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Try adjusting your search query or filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .testTag("gallery_grid"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                groupedSections.forEach { (dateHeader, itemsInDay) ->
                    // Calendar Day Header (Full Width Span)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = dateHeader,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 6.dp)
                        )
                    }

                    items(itemsInDay) { item ->
                        val indexInFlatList = flatFilteredList.indexOf(item)
                        val isFav = isFavoriteMap[item.id] ?: false

                        MediaGridItem(
                            item = item,
                            isFavorite = isFav,
                            onClick = {
                                if (indexInFlatList != -1) {
                                    onMediaClick(indexInFlatList, flatFilteredList)
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    item: MediaItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("media_item_${item.id}")
    ) {
        // Thumbnail Image loading via Coil
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uriString)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Type Indicators & Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f)
                        ),
                        startY = 120f
                    )
                )
        )

        // Favorite Indicator (Top Right)
        if (isFavorite) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Favorited",
                tint = Color.Red,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(20.dp)
            )
        }

        // Video Duration Indicator (Bottom Right)
        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = formatDuration(item.duration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// Convert video duration in ms to 00:00 format
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
