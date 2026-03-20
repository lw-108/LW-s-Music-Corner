package com.example.lwsmusiccorner.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.lwsmusiccorner.R
import com.example.lwsmusiccorner.data.Song
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(viewModel: MusicPlayerViewModel) {
    val songs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()

    // Splash Screen Overlay
    AnimatedVisibility(
        visible = isInitialLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFF6600)), // Orange background from your logo
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.androidicon),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(200.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "LW's Music Corner",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }
    }

    if (!isInitialLoading) {
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true
            )
        )

        val isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 110.dp,
            sheetContent = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = !isExpanded,
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MiniPlayerContent(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            onPlayPause = { viewModel.playPause() }
                        )
                    }
                    
                    FullPlayerContent(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        volume = volume,
                        onPlayPause = { viewModel.playPause() },
                        onSkipNext = { viewModel.skipNext() },
                        onSkipPrevious = { viewModel.skipPrevious() },
                        onSeek = { viewModel.seekTo(it) },
                        onVolumeChange = { viewModel.setVolume(it) }
                    )
                }
            },
            sheetDragHandle = { BottomSheetDefaults.DragHandle() },
            sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            sheetContainerColor = MaterialTheme.colorScheme.primary
        ) { innerPadding ->
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            val showGoToTop by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 2 }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var isSearchExpanded by remember { mutableStateOf(false) }

                        if (!isSearchExpanded) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.androidicon),
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "LW's Music Corner",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = if (isSearchExpanded) Modifier.weight(1f) else Modifier
                        ) {
                            if (isSearchExpanded) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.updateSearchQuery(it) },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Search songs...") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    ),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { 
                                            viewModel.updateSearchQuery("")
                                            isSearchExpanded = false 
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close search")
                                        }
                                    }
                                )
                            } else {
                                IconButton(onClick = { isSearchExpanded = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            var showSortMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.primary)
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    SortMenuItem("A-Z", SortOrder.A_Z, sortOrder) { viewModel.updateSortOrder(it); showSortMenu = false }
                                    SortMenuItem("Z-A", SortOrder.Z_A, sortOrder) { viewModel.updateSortOrder(it); showSortMenu = false }
                                    SortMenuItem("Album", SortOrder.ALBUM, sortOrder) { viewModel.updateSortOrder(it); showSortMenu = false }
                                    SortMenuItem("Artist", SortOrder.ARTIST, sortOrder) { viewModel.updateSortOrder(it); showSortMenu = false }
                                    SortMenuItem("Year", SortOrder.YEAR, sortOrder) { viewModel.updateSortOrder(it); showSortMenu = false }
                                }
                            }
                        }
                    }

                    if (songs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (searchQuery.isEmpty()) "No songs found" else "No results for \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val scrollbarColor = MaterialTheme.colorScheme.primary

                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawWithContent {
                                        drawContent()
                                        val layoutInfo = listState.layoutInfo
                                        val visibleItemsInfo = layoutInfo.visibleItemsInfo
                                        if (visibleItemsInfo.isNotEmpty()) {
                                            val totalItemsCount = layoutInfo.totalItemsCount
                                            val visibleItemsCount = visibleItemsInfo.size
                                            if (totalItemsCount > visibleItemsCount) {
                                                val elementHeight = size.height / totalItemsCount
                                                val firstVisibleItem = visibleItemsInfo.first()
                                                val scrollbarOffsetY = firstVisibleItem.index * elementHeight
                                                val scrollbarHeight = (visibleItemsCount * elementHeight * 1.5f).coerceAtMost(size.height - scrollbarOffsetY)
                                                
                                                val scrollbarWidth = 10.dp.toPx()
                                                val scrollbarPadding = 6.dp.toPx()

                                                drawRoundRect(
                                                    color = scrollbarColor,
                                                    topLeft = Offset(size.width - scrollbarWidth - scrollbarPadding, scrollbarOffsetY + 8.dp.toPx()),
                                                    size = Size(scrollbarWidth, scrollbarHeight - 16.dp.toPx()),
                                                    cornerRadius = CornerRadius(scrollbarWidth / 2, scrollbarWidth / 2),
                                                    alpha = 0.5f
                                                )
                                            }
                                        }
                                    },
                                contentPadding = PaddingValues(bottom = 20.dp)
                            ) {
                                items(songs) { song ->
                                    SongListItem(
                                        song = song,
                                        isSelected = song == currentSong,
                                        onClick = { viewModel.playSong(song) }
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(40.dp)
                                    .pointerInput(songs) {
                                        detectDragGestures { change, _ ->
                                            val totalItemsCount = listState.layoutInfo.totalItemsCount
                                            if (totalItemsCount > 0) {
                                                val y = change.position.y.coerceIn(0f, size.height.toFloat())
                                                val targetIndex = ((y / size.height) * totalItemsCount).toInt()
                                                coroutineScope.launch {
                                                    listState.scrollToItem(targetIndex.coerceIn(0, totalItemsCount - 1))
                                                }
                                            }
                                            change.consume()
                                        }
                                    }
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showGoToTop,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Go to top")
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerContent(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = currentSong?.albumArtUri,
            contentDescription = null,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.2f)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentSong?.title ?: "Not Playing",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
                    modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formatTime(currentPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        IconButton(onClick = onPlayPause) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerContent(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            AsyncImage(
                model = currentSong?.albumArtUri,
                contentDescription = "Album Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = currentSong?.title ?: "Select a song",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${currentSong?.artist ?: "Unknown Artist"} • ${currentSong?.year ?: "Unknown"}",
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Slider(
            value = if (duration > 0) currentPosition.toFloat() else 0f,
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
            modifier = Modifier.fillMaxWidth(),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(Color.White, CircleShape)
                )
            },
            colors = SliderDefaults.colors(
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(currentPosition), color = Color.White, fontSize = 12.sp)
            Text(text = formatTime(duration), color = Color.White, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSkipPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(56.dp))
            }
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(86.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(50.dp)
                )
            }
            IconButton(onClick = onSkipNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(56.dp))
            }
        }

        Spacer(modifier = Modifier.height(50.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 60.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeMute, contentDescription = null, tint = Color.White)
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color.White, CircleShape)
                    )
                },
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun SortMenuItem(
    text: String,
    order: SortOrder,
    currentOrder: SortOrder,
    onClick: (SortOrder) -> Unit
) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = { onClick(order) },
        trailingIcon = {
            if (order == currentOrder) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun SongListItem(song: Song, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.LightGray),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isSelected) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val minutesStr = if (minutes < 10) "0$minutes" else minutes.toString()
    val secondsStr = if (seconds < 10) "0$seconds" else seconds.toString()
    return "$minutesStr:$secondsStr"
}
