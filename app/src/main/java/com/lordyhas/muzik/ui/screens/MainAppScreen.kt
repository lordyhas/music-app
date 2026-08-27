package com.lordyhas.muzik.ui.screens

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lordyhas.muzik.ui.components.AddToPlaylistSheet
import com.lordyhas.muzik.ui.components.CreatePlaylistDialog
import com.lordyhas.muzik.ui.components.MiniPlayer
import com.lordyhas.muzik.ui.components.NowPlayingScreen
import com.lordyhas.muzik.ui.components.PermissionRequestView
import com.lordyhas.muzik.ui.components.SongDetailsDialog
import com.lordyhas.muzik.ui.theme.AccentPrimary
import com.lordyhas.muzik.ui.theme.BackgroundDark
import com.lordyhas.muzik.ui.theme.SurfaceCard
import com.lordyhas.muzik.ui.theme.SurfaceDark
import com.lordyhas.muzik.ui.theme.TextMuted
import com.lordyhas.muzik.ui.viewmodel.MusicViewModel

enum class NavigationTab(val route: String, val title: String, val icon: ImageVector) {
    HOME("home", "Accueil", Icons.Default.Home),
    TRACKS("tracks", "Titres", Icons.Default.MusicNote),
    ALBUMS("albums", "Albums", Icons.Default.Album),
    ARTISTS("artists", "Artistes", Icons.Default.Person),
    PLAYLISTS("playlists", "Playlists", Icons.Default.PlaylistPlay)
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
    val currentQueue by viewModel.currentQueue.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()
    val selectedSongForDetails by viewModel.selectedSongForDetails.collectAsState()
    val songPendingAddToPlaylist by viewModel.songPendingAddToPlaylist.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (!isPermissionGranted) {
        PermissionRequestView(onRequestPermission = onRequestPermissions)
        return
    }

    val isTopLevelRoute = NavigationTab.values().any { it.route == currentRoute }

    fun navigateToAlbum(albumTitle: String) {
        val encoded = URLEncoder.encode(albumTitle, StandardCharsets.UTF_8.toString())
        navController.navigate("album/$encoded")
    }

    fun navigateToArtist(artistName: String) {
        val encoded = URLEncoder.encode(artistName, StandardCharsets.UTF_8.toString())
        navController.navigate("artist/$encoded")
    }

    fun navigateToPlaylist(playlistId: String) {
        val encoded = URLEncoder.encode(playlistId, StandardCharsets.UTF_8.toString())
        navController.navigate("playlist/$encoded")
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
                    // Mini Player anchored above navigation bar or bottom insets
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

                    // Bottom Navigation Bar (always visible)
                    NavigationBar(
                            containerColor = SurfaceDark,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        ) {
                            NavigationTab.values().forEach { tab ->
                                val isSelected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (currentRoute != tab.route) {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
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
                NavHost(
                    navController = navController,
                    startDestination = NavigationTab.HOME.route,
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut() },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                ) {
                    composable(NavigationTab.HOME.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            onAlbumClick = { title -> navigateToAlbum(title) },
                            onArtistClick = { name -> navigateToArtist(name) }
                        )
                    }

                    composable(NavigationTab.TRACKS.route) {
                        TracksScreen(
                            viewModel = viewModel,
                            onGoToArtist = { name -> navigateToArtist(name) },
                            onGoToAlbum = { title -> navigateToAlbum(title) }
                        )
                    }

                    composable(NavigationTab.ALBUMS.route) {
                        AlbumsScreen(
                            viewModel = viewModel,
                            onAlbumClick = { title -> navigateToAlbum(title) }
                        )
                    }

                    composable(NavigationTab.ARTISTS.route) {
                        ArtistsScreen(
                            viewModel = viewModel,
                            onArtistClick = { name -> navigateToArtist(name) }
                        )
                    }

                    composable(NavigationTab.PLAYLISTS.route) {
                        PlaylistsScreen(
                            viewModel = viewModel,
                            onPlaylistClick = { id -> navigateToPlaylist(id) }
                        )
                    }

                    // ─── Detail Destinations ─────────────────────────────────────────

                    composable(
                        route = "album/{albumTitle}",
                        arguments = listOf(navArgument("albumTitle") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedTitle = backStackEntry.arguments?.getString("albumTitle").orEmpty()
                        val albumTitle = URLDecoder.decode(encodedTitle, StandardCharsets.UTF_8.toString())
                        AlbumDetailScreen(
                            albumTitle = albumTitle,
                            viewModel = viewModel,
                            onBackClick = { navController.popBackStack() },
                            onGoToArtist = { name -> navigateToArtist(name) }
                        )
                    }

                    composable(
                        route = "artist/{artistName}",
                        arguments = listOf(navArgument("artistName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedName = backStackEntry.arguments?.getString("artistName").orEmpty()
                        val artistName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
                        ArtistDetailScreen(
                            artistName = artistName,
                            viewModel = viewModel,
                            onBackClick = { navController.popBackStack() },
                            onGoToAlbum = { title -> navigateToAlbum(title) }
                        )
                    }

                    composable(
                        route = "playlist/{playlistId}",
                        arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedId = backStackEntry.arguments?.getString("playlistId").orEmpty()
                        val playlistId = URLDecoder.decode(encodedId, StandardCharsets.UTF_8.toString())
                        PlaylistDetailScreen(
                            playlistId = playlistId,
                            viewModel = viewModel,
                            onBackClick = { navController.popBackStack() },
                            onGoToArtist = { name -> navigateToArtist(name) },
                            onGoToAlbum = { title -> navigateToAlbum(title) }
                        )
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
                queue = currentQueue,
                currentIndex = currentQueueIndex,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekTo(it) },
                onSkipNext = { viewModel.skipNext() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onPlayIndex = { viewModel.playQueueIndex(it) },
                onRemoveIndex = { viewModel.removeQueueItem(it) },
                onDismiss = { viewModel.setShowNowPlaying(false) }
            )
        }

        // Song Details Dialog / Bottom Sheet
        selectedSongForDetails?.let { song ->
            SongDetailsDialog(
                song = song,
                onDismiss = { viewModel.selectSongForDetails(null) }
            )
        }

        // ─── Add To Playlist Sheet ─────────────────────────────────────────────
        songPendingAddToPlaylist?.let { song ->
            AddToPlaylistSheet(
                song = song,
                playlists = playlists,
                onAddToPlaylist = { playlist ->
                    viewModel.addSongToPlaylist(playlist.id, song.id)
                    viewModel.setSongPendingAddToPlaylist(null)
                },
                onCreateNew = {
                    // Dismiss the sheet and show create dialog; song is kept pending
                    viewModel.setShowCreatePlaylistDialog(true)
                },
                onDismiss = { viewModel.setSongPendingAddToPlaylist(null) }
            )
        }

        // ─── Create Playlist Dialog (global, triggered from AddToPlaylistSheet) ─
        if (showCreatePlaylistDialog && songPendingAddToPlaylist != null) {
            CreatePlaylistDialog(
                title = "Nouvelle playlist",
                initialName = "",
                onConfirm = { name ->
                    val created = viewModel.playlists.value
                    viewModel.createPlaylist(name)
                    // After creation, automatically add the pending song
                    val newPlaylist = viewModel.playlists.value.firstOrNull { p ->
                        p.name == name.trim() && p !in created
                    }
                    newPlaylist?.let { viewModel.addSongToPlaylist(it.id, songPendingAddToPlaylist!!.id) }
                    viewModel.setShowCreatePlaylistDialog(false)
                    viewModel.setSongPendingAddToPlaylist(null)
                },
                onDismiss = {
                    viewModel.setShowCreatePlaylistDialog(false)
                    // Don't clear pending song so sheet can be reshown
                }
            )
        }
    }
}

