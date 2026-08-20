package com.lordyhas.muzik.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.lordyhas.muzik.ui.components.MiniPlayer
import com.lordyhas.muzik.ui.components.NowPlayingScreen
import com.lordyhas.muzik.ui.components.PermissionRequestView
import com.lordyhas.muzik.ui.theme.AccentPrimary
import com.lordyhas.muzik.ui.theme.BackgroundDark
import com.lordyhas.muzik.ui.theme.SurfaceCard
import com.lordyhas.muzik.ui.theme.SurfaceDark
import com.lordyhas.muzik.ui.theme.TextMuted
import com.lordyhas.muzik.ui.theme.TextPrimary
import com.lordyhas.muzik.ui.viewmodel.MusicViewModel

enum class NavigationTab(val title: String, val icon: ImageVector) {
    HOME("Accueil", Icons.Default.Home),
    TRACKS("Titres", Icons.Default.MusicNote),
    ALBUMS("Albums", Icons.Default.Album),
    ARTISTS("Artistes", Icons.Default.Person)
}

@UnstableApi
@Composable
fun MainAppScreen(
    viewModel: MusicViewModel,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val showNowPlaying by viewModel.showNowPlaying.collectAsState()
    val showQueue by viewModel.showQueue.collectAsState()
    val currentQueue by viewModel.currentQueue.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()
    val selectedSongForDetails by viewModel.selectedSongForDetails.collectAsState()

    var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }

    if (!isPermissionGranted) {
        PermissionRequestView(onRequestPermission = onRequestPermissions)
        return
    }

    Box(modifier = modifier.fillMaxSize().background(BackgroundDark)) {
        Scaffold(
            containerColor = BackgroundDark,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDark)
                ) {
                    // Mini Player anchored right above navigation bar
                    if (currentSong != null) {
                        MiniPlayer(
                            song = currentSong,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNext() },
                            onClick = { viewModel.setShowNowPlaying(true) }
                        )
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = SurfaceDark,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        NavigationTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AccentPrimary,
                                    selectedTextColor = AccentPrimary,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = SurfaceCard
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                Crossfade(
                    targetState = selectedTab,
                    label = "tab_crossfade"
                ) { tab ->
                    when (tab) {
                        NavigationTab.HOME -> HomeScreen(viewModel = viewModel)
                        NavigationTab.TRACKS -> TracksScreen(viewModel = viewModel)
                        NavigationTab.ALBUMS -> AlbumsScreen(viewModel = viewModel)
                        NavigationTab.ARTISTS -> ArtistsScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Fullscreen Now Playing Animation Sheet
        AnimatedVisibility(
            visible = showNowPlaying,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            NowPlayingScreen(
                song = currentSong,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekTo(it) },
                onSkipNext = { viewModel.skipNext() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onDismiss = { viewModel.setShowNowPlaying(false) },
                onOpenQueue = { viewModel.setShowQueue(true) }
            )
        }

        // Queue Bottom Sheet
        if (showQueue) {
            com.lordyhas.muzik.ui.components.QueueSheet(
                queue = currentQueue,
                currentIndex = currentQueueIndex,
                isPlaying = isPlaying,
                onPlayIndex = { viewModel.playQueueIndex(it) },
                onRemoveIndex = { viewModel.removeQueueItem(it) },
                onDismiss = { viewModel.setShowQueue(false) }
            )
        }

        // Song Details Dialog / Bottom Sheet
        selectedSongForDetails?.let { song ->
            com.lordyhas.muzik.ui.components.SongDetailsDialog(
                song = song,
                onDismiss = { viewModel.selectSongForDetails(null) }
            )
        }
    }
}
