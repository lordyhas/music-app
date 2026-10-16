package com.lordyhas.muzik.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lordyhas.muzik.data.model.Song
import com.lordyhas.muzik.ui.theme.AccentPrimary
import com.lordyhas.muzik.ui.theme.AccentSecondary
import com.lordyhas.muzik.ui.theme.BackgroundDark
import com.lordyhas.muzik.ui.theme.SurfaceCard
import com.lordyhas.muzik.ui.theme.SurfaceDark
import com.lordyhas.muzik.ui.theme.SurfaceElevated
import com.lordyhas.muzik.ui.theme.TextMuted
import com.lordyhas.muzik.ui.theme.TextPrimary
import com.lordyhas.muzik.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun NowPlayingScreen(
    song: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isShuffle: Boolean,
    repeatMode: Int,
    queue: List<Song>,
    currentIndex: Int,
    userQueueCount: Int,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onPlayIndex: (Int) -> Unit,
    onRemoveIndex: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    // Handle back button / gesture: if on queue page, return to player page; if on player, dismiss now playing
    BackHandler(enabled = true) {
        if (pagerState.currentPage > 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        } else {
            onDismiss()
        }
    }

    // Slider drag state
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val actualProgress = if (duration > 0) {
        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val sliderProgress = if (isDragging) dragProgress else actualProgress

    // Breathing scale for album art when playing
    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        animationSpec = tween(400),
        label = "art_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SurfaceDark, BackgroundDark, Color(0xFF09080E))
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* consume clicks */ }
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                // ═══════════════════════════════════════════════════════════════
                // PAGE 0 — Full Player
                // ═══════════════════════════════════════════════════════════════
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // ── Top Bar ──────────────────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceCard)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown, "Fermer",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Text(
                                text = "EN LECTURE",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AccentPrimary
                            )

                            // Spacer to balance the row
                            Box(modifier = Modifier.size(44.dp))
                        }

                        // ── Album Art ────────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .aspectRatio(1f)
                                .scale(artScale)
                                .shadow(
                                    elevation = 28.dp,
                                    shape = RoundedCornerShape(22.dp),
                                    spotColor = AccentPrimary.copy(alpha = 0.4f),
                                    ambientColor = AccentSecondary.copy(alpha = 0.3f)
                                )
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(SurfaceElevated, Color(0xFF2B2244), Color(0xFF1E1530))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (song.albumArtUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(song.albumArtUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Pochette d'album",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.MusicNote, null,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(96.dp)
                                )
                            }
                        }

                        // ── Title & Artist ───────────────────────────────────
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${song.artist} • ${song.album}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        // ── Progress Slider ──────────────────────────────────
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ThinSlider(
                                value = sliderProgress,
                                onValueChange = {
                                    isDragging = true
                                    dragProgress = it
                                },
                                onValueChangeFinished = {
                                    onSeekTo((dragProgress * duration).toLong())
                                    isDragging = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val displayed = if (isDragging) {
                                    (dragProgress * duration).toLong()
                                } else currentPosition

                                Text(
                                    text = formatTime(displayed),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = formatTime(duration),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        // ── Playback Controls ────────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(
                                onClick = onToggleShuffle,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Shuffle, "Aléatoire",
                                    tint = if (isShuffle) AccentPrimary else TextMuted,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            IconButton(
                                onClick = onSkipPrevious,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Icon(
                                    Icons.Default.SkipPrevious, "Précédent",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .shadow(16.dp, CircleShape, spotColor = AccentPrimary)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(AccentPrimary, AccentSecondary)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = onTogglePlayPause,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        if (isPlaying) "Pause" else "Lecture",
                                        tint = Color.White,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onSkipNext,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Icon(
                                    Icons.Default.SkipNext, "Suivant",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            IconButton(
                                onClick = onToggleRepeat,
                                modifier = Modifier.size(48.dp)
                            ) {
                                val (icon, tint) = when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> Pair(Icons.Default.RepeatOne, AccentPrimary)
                                    Player.REPEAT_MODE_ALL -> Pair(Icons.Default.Repeat, AccentPrimary)
                                    else -> Pair(Icons.Default.Repeat, TextMuted)
                                }
                                Icon(
                                    icon, "Répétition",
                                    tint = tint,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "File d'attente",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                //modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown, "Swipe",
                                tint = TextSecondary,
                                modifier = Modifier.size(40.dp).padding(bottom = 16.dp)
                            )
                            Box(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // PAGE 1 — Queue with compact mini-player header
                // ═══════════════════════════════════════════════════════════════
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(SurfaceDark, BackgroundDark, Color(0xFF09080E))
                                )
                            )
                            .statusBarsPadding()
                    ) {
                        // ── Compact Mini-Player Header ───────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark.copy(alpha = 0.95f))
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini cover in the top-left corner
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .shadow(
                                        4.dp, RoundedCornerShape(10.dp),
                                        spotColor = AccentPrimary.copy(alpha = 0.3f)
                                    )
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                if (song.albumArtUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(song.albumArtUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.MusicNote, null,
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Title & Artist
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Play/Pause
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceCard)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (isPlaying) "Pause" else "Play",
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Box(modifier = Modifier.width(8.dp))

                            Spacer(modifier = Modifier.width(8.dp))

                            // Next
                            IconButton(
                                onClick = onSkipNext,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceCard)
                            ) {
                                Icon(
                                    Icons.Default.SkipNext, "next",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }


                        }

                        // ── Queue Track List ─────────────────────────────────
                        if (queue.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Queue is empty",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextMuted
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 48.dp)
                            ) {
                                // 1. En cours de lecture
                                if (currentIndex in queue.indices) {
                                    item {
                                        Text(
                                            text = "EN COURS DE LECTURE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                letterSpacing = 1.5.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = AccentPrimary,
                                            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 8.dp)
                                        )
                                        QueueSongRow(
                                            song = queue[currentIndex],
                                            isCurrent = true,
                                            isPlaying = isPlaying,
                                            onClick = { onPlayIndex(currentIndex) },
                                            onRemove = null,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // 2. File d'attente utilisateur
                                if (userQueueCount > 0 && currentIndex + 1 < queue.size) {
                                    item {
                                        Text(
                                            text = "FILE D'ATTENTE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                letterSpacing = 1.5.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 8.dp)
                                        )
                                    }
                                    
                                    val uqStart = currentIndex + 1
                                    val uqEnd = (uqStart + userQueueCount).coerceAtMost(queue.size)
                                    
                                    items(
                                        count = uqEnd - uqStart,
                                        key = { i -> "${uqStart + i}_${queue[uqStart + i].id}" }
                                    ) { i ->
                                        val absoluteIndex = uqStart + i
                                        QueueSongRow(
                                            song = queue[absoluteIndex],
                                            isCurrent = false,
                                            isPlaying = false,
                                            onClick = { onPlayIndex(absoluteIndex) },
                                            onRemove = { onRemoveIndex(absoluteIndex) },
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                
                                // 3. À suivre (Reste de l'album/playlist)
                                val contextStart = currentIndex + 1 + userQueueCount
                                if (contextStart < queue.size) {
                                    item {
                                        Text(
                                            text = "À SUIVRE : " + (song?.album?.ifEmpty { "Bibliothèque" } ?: "Bibliothèque").uppercase(Locale.getDefault()),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                letterSpacing = 1.5.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = TextSecondary,
                                            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 8.dp)
                                        )
                                    }
                                    
                                    items(
                                        count = queue.size - contextStart,
                                        key = { i -> "${contextStart + i}_${queue[contextStart + i].id}" }
                                    ) { i ->
                                        val absoluteIndex = contextStart + i
                                        QueueSongRow(
                                            song = queue[absoluteIndex],
                                            isCurrent = false,
                                            isPlaying = false,
                                            onClick = { onPlayIndex(absoluteIndex) },
                                            onRemove = { onRemoveIndex(absoluteIndex) },
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Queue Song Row ─────────────────────────────────────────────────────────

@Composable
private fun QueueSongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) SurfaceElevated else SurfaceCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(listOf(SurfaceElevated, Color(0xFF2B2244)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(
                    Icons.Default.MusicNote, null,
                    tint = if (isCurrent) AccentPrimary else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Equalizer, null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title & Artist + Duration
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = if (isCurrent) AccentPrimary else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${song.artist} • ${song.formattedDuration()}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = if (isCurrent) AccentSecondary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.Close, "Retirer de la file",
                    tint = TextMuted,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
