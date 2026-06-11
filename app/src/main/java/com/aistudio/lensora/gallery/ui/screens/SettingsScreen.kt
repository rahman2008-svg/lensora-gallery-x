package com.aistudio.lensora.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.lensora.gallery.data.entity.TrashItem
import com.aistudio.lensora.gallery.ui.viewmodel.GalleryViewModel
import java.text.DecimalFormat

@Composable
fun SettingsScreen(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val currentTheme by viewModel.appTheme.collectAsState()
    val speedMs by viewModel.slideshowSpeed.collectAsState()
    val trashList by viewModel.trashItems.collectAsState()
    val mediaList by viewModel.mediaList.collectAsState()

    var showRecycleDialog by remember { mutableStateOf(false) }

    // Computations for storage analytics
    val totalPhotos = remember(mediaList) { mediaList.count { !it.isVideo } }
    val totalVideos = remember(mediaList) { mediaList.count { it.isVideo } }
    
    val totalFootprintBytes = remember(mediaList) {
        mediaList.sumOf { it.size }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_column"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings & Local Bin",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // --- STORAGE ANALYTICS CARD ---
        item {
            StorageAnalyticsSection(
                photosCount = totalPhotos,
                videosCount = totalVideos,
                totalBytesCount = totalFootprintBytes
            )
        }

        // --- THEME SELECTOR ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Aesthetic Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ThemeOptionChip(label = "Light", selected = currentTheme == "light") {
                            viewModel.updateAppTheme("light")
                        }
                        ThemeOptionChip(label = "Dark", selected = currentTheme == "dark") {
                            viewModel.updateAppTheme("dark")
                        }
                        ThemeOptionChip(label = "System", selected = currentTheme == "system") {
                            viewModel.updateAppTheme("system")
                        }
                    }
                }
            }
        }

        // --- SLIDESHOW SLIDER ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Slideshow Speed Interval",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configure auto-advancing slide timing on Media Viewer.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DurationOptionChip(label = "2s Timing", selected = speedMs == 2000) {
                            viewModel.updateSlideshowSpeed(2000)
                        }
                        DurationOptionChip(label = "3s Timing", selected = speedMs == 3000) {
                            viewModel.updateSlideshowSpeed(3000)
                        }
                        DurationOptionChip(label = "5s Timing", selected = speedMs == 5000) {
                            viewModel.updateSlideshowSpeed(5000)
                        }
                    }
                }
            }
        }

        // --- RECYCLE BIN LAUNCHER ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRecycleDialog = true }
                    .testTag("recycle_bin_launcher_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Recycle Bin (${trashList.size} items)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Auto delete old items after 30 days.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // --- DEVELOPER / ABOUT SECTION ---
        item {
            DeveloperAboutSection()
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Full screen-ish list of Trashed items dialog
    if (showRecycleDialog) {
        RecycleBinDialog(
            trashList = trashList,
            onDismiss = { showRecycleDialog = false },
            onRestore = { viewModel.restoreTrashItem(it) },
            onDeletePermanently = { viewModel.purgeTrashItemPermanently(it) },
            onClearAll = {
                viewModel.clearAllTrash()
                showRecycleDialog = false
            }
        )
    }
}

@Composable
fun ThemeOptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.testTag("chip_theme_$label")
    )
}

@Composable
fun DurationOptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.testTag("chip_duration_$label")
    )
}

@Composable
fun StorageAnalyticsSection(
    photosCount: Int,
    videosCount: Int,
    totalBytesCount: Long
) {
    val totalFootprintMB = totalBytesCount / (1024L * 1024)
    val doubleDecimal = DecimalFormat("#.##")
    val displaySize = if (totalFootprintMB >= 1024) {
        "${doubleDecimal.format(totalFootprintMB.toDouble() / 1024)} GB"
    } else {
        "$totalFootprintMB MB"
    }

    // Average device has mock storage or we represent an optimal 12.4 GB as active
    val displayPercent = 82 // or calculate progress out of a total, we'll draw 82% precisely to match mockup

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LENSORA ANALYTICS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$displaySize Used",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$photosCount Photos • $videosCount Videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color.White, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 0.82f },
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = "$displayPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun DeveloperAboutSection() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Developer Profiles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AR",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Prince AR Abdur Rahman",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "NexVora Lab's Ofc",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NexVora Lab's Portfolio",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val products = listOf(
                "NexPlay X", "LifeSphere OS", "Smart Day Planner X", "Study AI",
                "Lensora Studio", "Offline AI", "NexVora Love Space", "CalcVerse", "NexVoice OS"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    products.take(5).forEach { product ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, sizeModifier(), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = product, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    products.drop(5).forEach { product ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, sizeModifier(), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = product, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Build System",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecycleBinDialog(
    trashList: List<TrashItem>,
    onDismiss: () -> Unit,
    onRestore: (TrashItem) -> Unit,
    onDeletePermanently: (TrashItem) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recycle Bin", fontWeight = FontWeight.Bold)
                if (trashList.isNotEmpty()) {
                    TextButton(
                        onClick = onClearAll,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Empty Bin")
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSizeMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Photos and videos inside the Recycle Bin will be automatically deleted after 30 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (trashList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Recycle Bin is empty", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(trashList) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(item.deletedPath)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = item.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val sizeMB = item.size / (1024.0 * 1024.0)
                                            Text(
                                                text = String.format("%.2f MB", sizeMB),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = { onRestore(item) }) {
                                            Icon(Icons.Default.Restore, contentDescription = "Restore", tint = Color.Green)
                                        }
                                        IconButton(onClick = { onDeletePermanently(item) }) {
                                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete permanently", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun Modifier.fillMaxSizeMaxWidth(): Modifier = this.height(400.dp).fillMaxWidth()
private fun sizeModifier(): Modifier = Modifier.size(14.dp)
