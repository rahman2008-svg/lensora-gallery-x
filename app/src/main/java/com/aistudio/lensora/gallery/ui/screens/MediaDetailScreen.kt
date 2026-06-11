package com.aistudio.lensora.gallery.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.lensora.gallery.data.model.MediaItem
import com.aistudio.lensora.gallery.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaDetailScreen(
    viewModel: GalleryViewModel,
    initialIndex: Int,
    filteredMediaList: List<MediaItem>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (filteredMediaList.isEmpty()) {
        onClose()
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberScope()
    
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, filteredMediaList.size -1),
        pageCount = { filteredMediaList.size }
    )

    val currentItem = filteredMediaList.getOrNull(pagerState.currentPage) ?: filteredMediaList.first()
    val isFavMap by viewModel.isFavoriteMap.collectAsState()
    val isFavorite = isFavMap[currentItem.id] ?: false

    val isSlideshowRunning by viewModel.isSlideshowRunning.collectAsState()
    val slideshowActiveIndex by viewModel.activeSlideshowIndex.collectAsState()

    // Control bar visibility
    var showOverlayControls by remember { mutableStateOf(true) }

    // Sync Slideshow ticker to HorizontalPager page position smoothly
    LaunchedEffect(isSlideshowRunning, slideshowActiveIndex) {
        if (isSlideshowRunning) {
            pagerState.animateScrollToPage(slideshowActiveIndex)
        }
    }

    // Update ViewModel on page scroll
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setSlideshowIndex(pagerState.currentPage)
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            AnimatedVisibility(
                visible = showOverlayControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(top = 40.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Close Viewer", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentItem.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} of ${filteredMediaList.size}",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Row {
                        // Slideshow toggle
                        IconButton(onClick = {
                            if (isSlideshowRunning) {
                                viewModel.stopSlideshow()
                            } else {
                                viewModel.startSlideshow(filteredMediaList.size)
                            }
                        }) {
                            Icon(
                                imageVector = if (isSlideshowRunning) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Slideshow toggle",
                                tint = if (isSlideshowRunning) Color.Green else Color.White
                            )
                        }

                        // Share Intent
                        IconButton(onClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (currentItem.isVideo) "video/*" else "image/*"
                                    // Try custom pathway or load parsed uri
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(currentItem.uriString))
                                    putExtra(Intent.EXTRA_TEXT, "Shared from Lensora Gallery X: ${currentItem.name}")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showOverlayControls && !isSlideshowRunning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(bottom = 24.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Toggle
                    IconButton(onClick = { viewModel.toggleFavorite(currentItem) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else Color.White
                        )
                    }

                    // Vault Hide Trigger
                    IconButton(onClick = {
                        viewModel.moveMediaToVault(currentItem)
                        coroutineScope.launch {
                            val next = (pagerState.currentPage + 1).coerceAtMost(filteredMediaList.size -1)
                            if (next == pagerState.currentPage) {
                                onClose()
                            } else {
                                pagerState.scrollToPage(next)
                            }
                        }
                    }, modifier = Modifier.testTag("vault_item_btn")) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = "Move to Vault", tint = Color.LightGray)
                    }

                    // Delete to Trash Bin trigger
                    IconButton(onClick = {
                        viewModel.deleteMediaToTrash(currentItem)
                        coroutineScope.launch {
                            val next = (pagerState.currentPage + 1).coerceAtMost(filteredMediaList.size - 1)
                            if (next == pagerState.currentPage) {
                                onClose()
                            } else {
                                pagerState.scrollToPage(next)
                            }
                        }
                    }, modifier = Modifier.testTag("delete_item_btn")) {
                        Icon(Icons.Default.Delete, contentDescription = "Move to Trash", tint = Color.Red)
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, _, _ ->
                        // Detect taps/gestures to toggles controls UI overlay
                    }
                }
        ) { pageIndex ->
            val pageItem = filteredMediaList[pageIndex]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showOverlayControls = !showOverlayControls },
                contentAlignment = Alignment.Center
            ) {
                if (pageItem.isVideo) {
                    VideoPlayCell(path = pageItem.path)
                } else {
                    ZoomableImageCell(uri = pageItem.uriString)
                }
            }
        }
    }
}

@Composable
fun ZoomableImageCell(uri: String) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = "Photo Viewer",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

@Composable
fun VideoPlayCell(path: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Standard Android VideoView wrapper for offline fluid MP4 playing
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    val mediaController = MediaController(ctx)
                    mediaController.setAnchorView(this)
                    setMediaController(mediaController)
                    
                    // Support online streams or local paths (or default mock test streams)
                    val uriToPlay = if (path.startsWith("http")) {
                        Uri.parse(path)
                    } else {
                        Uri.fromFile(java.io.File(path))
                    }
                    
                    setVideoURI(uriToPlay)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp)),
            update = { videoView ->
                // Lifecycle updating could trigger play or pauses here if needed
            }
        )

        // Custom touch overlay indication for quick usage
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tap Play on controller above", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// Custom view helper to prevent recompositions or state resets in pager scope
@Composable
private fun rememberScope() = rememberCoroutineScope()
