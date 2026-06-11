package com.aistudio.lensora.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
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

data class Album(
    val name: String,
    val coverUri: String?,
    val itemCount: Int,
    val items: List<MediaItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: GalleryViewModel,
    onAlbumClick: (albumName: String, albumItems: List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val rawMedia by viewModel.mediaList.collectAsState()
    val customFolders by viewModel.customAlbumFolders.collectAsState()

    var showAddFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var newFolderPath by remember { mutableStateOf("") }

    // Group media elements by their Album bucket displays
    val albums = remember(rawMedia, customFolders) {
        val groupedMap = rawMedia.groupBy { it.albumName }
        val list = mutableListOf<Album>()
        
        // Ensure canonical standard groups exist even if empty
        val standardAlbums = listOf("Camera", "Screenshots", "Downloads", "WhatsApp Images", "Facebook", "Instagram")
        
        standardAlbums.forEach { name ->
            val items = groupedMap[name] ?: emptyList()
            if (items.isNotEmpty()) {
                list.add(Album(name, items.firstOrNull()?.uriString, items.size, items))
            }
        }

        // Add miscellaneous custom/discovered albums
        groupedMap.forEach { (name, items) ->
            if (!standardAlbums.contains(name) && items.isNotEmpty()) {
                list.add(Album(name, items.firstOrNull()?.uriString, items.size, items))
            }
        }
        
        list
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddFolderDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_album_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add custom album path")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Library Albums",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (albums.isEmpty()) {
                Box(
                    modifier = Modifier.fillGridSizeModifier(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "No albums",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = "No albums available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("albums_grid")
                ) {
                    items(albums) { album ->
                        AlbumCard(album = album) {
                            onAlbumClick(album.name, album.items)
                        }
                    }
                }
            }
        }

        // Add custom folder registration dialog
        if (showAddFolderDialog) {
            AlertDialog(
                onDismissRequest = { showAddFolderDialog = false },
                title = { Text("Register Custom Album") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Import local media files from any custom device folder into Lensora.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = { Text("Album Name") },
                            modifier = Modifier.fillMaxWidth().testTag("new_album_name_field"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newFolderPath,
                            onValueChange = { newFolderPath = it },
                            label = { Text("Folder Path (e.g. /Pictures/Family)") },
                            modifier = Modifier.fillMaxWidth().testTag("new_album_path_field"),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                viewModel.addCustomAlbumFolder(newFolderPath.ifBlank { "/storage/emulated/0/$newFolderName" })
                                showAddFolderDialog = false
                                newFolderName = ""
                                newFolderPath = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddFolderDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("album_card_${album.name}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (album.coverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(album.coverUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Count Badge inside Cover
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${album.itemCount} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Custom view helpers to prevent stretching inside dialogs or grids
private fun Modifier.fillGridSizeModifier(): Modifier = this.height(300.dp).fillMaxWidth()
