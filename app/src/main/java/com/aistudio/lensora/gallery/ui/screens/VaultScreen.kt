package com.aistudio.lensora.gallery.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.lensora.gallery.data.entity.VaultItem
import com.aistudio.lensora.gallery.data.model.MediaItem
import com.aistudio.lensora.gallery.ui.viewmodel.GalleryViewModel

@Composable
fun VaultScreen(
    viewModel: GalleryViewModel,
    onMediaClick: (index: Int, filteredList: List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val vaultPin by viewModel.vaultPin.collectAsState()
    val isUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val vaultEntities by viewModel.vaultItems.collectAsState()

    AnimatedContent(
        targetState = Pair(vaultPin == null, isUnlocked),
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "VaultStateTransition"
    ) { (isPinUnset, unlocked) ->
        when {
            isPinUnset -> {
                VaultSetupPinScreen(onSetupComplete = { pin ->
                    viewModel.setVaultPin(pin)
                })
            }
            !unlocked -> {
                VaultUnlockPinScreen(
                    onUnlockSubmit = { enteredCode ->
                        viewModel.verifyVaultPin(enteredCode)
                    }
                )
            }
            else -> {
                UnlockedVaultContent(
                    vaultEntities = vaultEntities,
                    onLockClick = { viewModel.lockVault() },
                    onMediaClick = onMediaClick,
                    onRestore = { viewModel.restoreVaultItem(it) },
                    onDeletePermanently = { viewModel.purgeVaultItemPermanently(it) }
                )
            }
        }
    }
}

@Composable
fun VaultSetupPinScreen(onSetupComplete: (String) -> Unit) {
    var step by remember { mutableStateOf(1) } // 1: Enter details, 2: Re-enter confirm
    var tempPin by remember { mutableStateOf("") }
    var inputStr by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EnhancedEncryption,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (step == 1) "Create Secure Vault PIN" else "Confirm Vault PIN",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a 4-digit code to protect your private media files inside an offline sandbox.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        // DOTS indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            for (i in 1..4) {
                val filled = inputStr.length >= i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        if (errorMsg.isNotEmpty()) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Numeric Keypad
        NumericKeypad(onDigitClick = { digit ->
            errorMsg = ""
            if (inputStr.length < 4) {
                inputStr += digit
                if (inputStr.length == 4) {
                    if (step == 1) {
                        tempPin = inputStr
                        inputStr = ""
                        step = 2
                    } else {
                        if (inputStr == tempPin) {
                            onSetupComplete(inputStr)
                        } else {
                            errorMsg = "PINs do not match. Restarting setup."
                            inputStr = ""
                            step = 1
                            tempPin = ""
                        }
                    }
                }
            }
        }, onDeleteClick = {
            if (inputStr.isNotEmpty()) {
                inputStr = inputStr.dropLast(1)
            }
        })
    }
}

@Composable
fun VaultUnlockPinScreen(
    onUnlockSubmit: (String) -> Boolean
) {
    var enteredStr by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Vault is Locked",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your 4-digit passcode to view hidden items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            for (i in 1..4) {
                val filled = enteredStr.length >= i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        if (errorMsg.isNotEmpty()) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Numeric Keypad
        NumericKeypad(onDigitClick = { digit ->
            errorMsg = ""
            if (enteredStr.length < 4) {
                enteredStr += digit
                if (enteredStr.length == 4) {
                    val success = onUnlockSubmit(enteredStr)
                    if (!success) {
                        errorMsg = "Incorrect passcode. Try again."
                        enteredStr = ""
                    }
                }
            }
        }, onDeleteClick = {
            if (enteredStr.isNotEmpty()) {
                enteredStr = enteredStr.dropLast(1)
            }
        })
    }
}

@Composable
fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val keypadList = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keypadList.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { char ->
                    if (char.isEmpty()) {
                        Spacer(modifier = Modifier.size(64.dp))
                    } else if (char == "DEL") {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(onClick = onDeleteClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onDigitClick(char) }
                                .testTag("keypad_btn_$char"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnlockedVaultContent(
    vaultEntities: List<VaultItem>,
    onLockClick: () -> Unit,
    onMediaClick: (index: Int, filteredList: List<MediaItem>) -> Unit,
    onRestore: (VaultItem) -> Unit,
    onDeletePermanently: (VaultItem) -> Unit
) {
    val context = LocalContext.current
    
    // Map VaultEntities to standard MediaItems for the zoomable detail viewer integration
    val vaultMediaItems = remember(vaultEntities) {
        vaultEntities.map { entity ->
            MediaItem(
                id = entity.id.toLong(),
                uriString = entity.encryptedPath, // Coil can load from exact absolute file paths
                name = entity.name,
                path = entity.originalPath,
                width = 1080,
                height = 1080,
                size = entity.size,
                duration = entity.duration,
                isVideo = entity.isVideo,
                dateModified = entity.dateAdded / 1000,
                albumName = "Secure Vault"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Secure Vault",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Encrypted Offline Storage",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onLockClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("lock_vault_btn")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lock")
            }
        }

        if (vaultMediaItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EnhancedEncryption,
                        contentDescription = "No items",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        text = "Your Private Vault is empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "To hide media files, open any item in the main Gallery and choose the Vault icon.",
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("vault_grid")
            ) {
                items(vaultEntities) { entity ->
                    val correspondingItem = vaultMediaItems.first { it.id == entity.id.toLong() }
                    val indexInVault = vaultMediaItems.indexOf(correspondingItem)
                    
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (indexInVault != -1) {
                                    onMediaClick(indexInVault, vaultMediaItems)
                                }
                            }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(entity.encryptedPath)
                                .crossfade(true)
                                .build(),
                            contentDescription = entity.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Restore / Delete icons overlay on top of thumbnail
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .align(Alignment.BottomCenter)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            IconButton(onClick = { onRestore(entity) }) {
                                Icon(Icons.Default.RestorePage, contentDescription = "Restore", tint = Color.Green)
                            }
                            IconButton(onClick = { onDeletePermanently(entity) }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete permanently", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
