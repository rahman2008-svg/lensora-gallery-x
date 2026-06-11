package com.aistudio.lensora.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aistudio.lensora.gallery.data.model.MediaItem
import com.aistudio.lensora.gallery.ui.screens.*
import com.aistudio.lensora.gallery.ui.theme.LensoraTheme
import com.aistudio.lensora.gallery.ui.viewmodel.GalleryViewModel
import androidx.compose.foundation.isSystemInDarkTheme

// Navigation Route IDs as strings
const val ROUTE_PHOTOS = "photos"
const val ROUTE_ALBUMS = "albums"
const val ROUTE_FAVORITES = "favorites"
const val ROUTE_VAULT = "vault"
const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GalleryViewModel = viewModel()
            val currentTheme by viewModel.appTheme.collectAsState()
            val isDark = when (currentTheme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            LensoraTheme(darkTheme = isDark) {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: GalleryViewModel = viewModel()) {
    val context = LocalContext.current

    
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_PHOTOS

    // Local states for sub-flows (Media swiper viewer and Album sub-grid browsing)
    var selectedMediaIndex by remember { mutableStateOf<Int?>(null) }
    var activeViewerList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    var selectedAlbumName by remember { mutableStateOf<String?>(null) }
    var selectedAlbumItems by remember { mutableStateOf<List<MediaItem>?>(null) }

    // Permissions requesting flow
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    var permissionsGranted by remember {
        mutableStateOf(
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        permissionsGranted = granted
        if (!granted) {
            Toast.makeText(
                context, 
                "Permissions denied. Running in Offline Sandboxed Demo mode.", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Proactively check and request on mount
    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            launcher.launch(permissionsToRequest)
        }
    }

    Scaffold(
        bottomBar = {
            // Display standard Material 3 NavigationBar
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar"),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentRoute == ROUTE_PHOTOS,
                    onClick = {
                        // Clear sub-browsers upon tab swapping
                        selectedAlbumName = null
                        selectedAlbumItems = null
                        navController.navigate(ROUTE_PHOTOS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Photos") },
                    label = { Text("Photos") },
                    modifier = Modifier.testTag("nav_btn_photos")
                )
                NavigationBarItem(
                    selected = currentRoute == ROUTE_ALBUMS,
                    onClick = {
                        selectedAlbumName = null
                        selectedAlbumItems = null
                        navController.navigate(ROUTE_ALBUMS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Albums") },
                    label = { Text("Albums") },
                    modifier = Modifier.testTag("nav_btn_albums")
                )
                NavigationBarItem(
                    selected = currentRoute == ROUTE_FAVORITES,
                    onClick = {
                        selectedAlbumName = null
                        selectedAlbumItems = null
                        navController.navigate(ROUTE_FAVORITES) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                    label = { Text("Favorites") },
                    modifier = Modifier.testTag("nav_btn_favorites")
                )
                NavigationBarItem(
                    selected = currentRoute == ROUTE_VAULT,
                    onClick = {
                        selectedAlbumName = null
                        selectedAlbumItems = null
                        navController.navigate(ROUTE_VAULT) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.EnhancedEncryption, contentDescription = "Vault") },
                    label = { Text("Vault") },
                    modifier = Modifier.testTag("nav_btn_vault")
                )
                NavigationBarItem(
                    selected = currentRoute == ROUTE_SETTINGS,
                    onClick = {
                        selectedAlbumName = null
                        selectedAlbumItems = null
                        navController.navigate(ROUTE_SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_btn_settings")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_PHOTOS,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ROUTE_PHOTOS) {
                // If permissions not granted, we show a light user notice but proceed with Demo files elegantly
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!permissionsGranted) {
                        PermissionWarningPanel {
                            launcher.launch(permissionsToRequest)
                        }
                    }
                    GalleryScreen(
                        viewModel = viewModel,
                        onMediaClick = { idx, list ->
                            activeViewerList = list
                            selectedMediaIndex = idx
                        }
                    )
                }
            }
            
            composable(ROUTE_ALBUMS) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AlbumsScreen(
                        viewModel = viewModel,
                        onAlbumClick = { albumName, albumItems ->
                            selectedAlbumName = albumName
                            selectedAlbumItems = albumItems
                        }
                    )

                    // Overlay Album Detail Sub-grid
                    AnimatedVisibility(
                        visible = selectedAlbumName != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    selectedAlbumName = null
                                    selectedAlbumItems = null
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Albums"
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedAlbumName ?: "",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            selectedAlbumItems?.let { itemsList ->
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 110.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    items(itemsList) { item ->
                                        val indexInAlbum = itemsList.indexOf(item)
                                        MediaGridItem(
                                            item = item,
                                            isFavorite = false,
                                            onClick = {
                                                activeViewerList = itemsList
                                                selectedMediaIndex = indexInAlbum
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
            }
            
            composable(ROUTE_FAVORITES) {
                FavoritesScreen(
                    viewModel = viewModel,
                    onMediaClick = { idx, list ->
                        activeViewerList = list
                        selectedMediaIndex = idx
                    }
                )
            }
            
            composable(ROUTE_VAULT) {
                VaultScreen(
                    viewModel = viewModel,
                    onMediaClick = { idx, list ->
                        activeViewerList = list
                        selectedMediaIndex = idx
                    }
                )
            }
            
            composable(ROUTE_SETTINGS) {
                SettingsScreen(viewModel = viewModel)
            }
        }

        // Full Screen Overlay Swiper Reader
        AnimatedVisibility(
            visible = selectedMediaIndex != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            selectedMediaIndex?.let { startIdx ->
                MediaDetailScreen(
                    viewModel = viewModel,
                    initialIndex = startIdx,
                    filteredMediaList = activeViewerList,
                    onClose = {
                        selectedMediaIndex = null
                        activeViewerList = emptyList()
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionWarningPanel(onRequest: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Limited access. Allow permissions to view your device photos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Allow", fontSize = 11.sp)
            }
        }
    }
}
