package com.lordyhas.muzik.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.lordyhas.muzik.ui.components.CreatePlaylistDialog
import com.lordyhas.muzik.ui.components.EmptyState
import com.lordyhas.muzik.ui.components.LoadingState
import com.lordyhas.muzik.ui.components.PlaylistCard
import com.lordyhas.muzik.ui.components.ScreenHeader
import com.lordyhas.muzik.ui.components.pluralize
import com.lordyhas.muzik.ui.theme.AccentPrimary
import com.lordyhas.muzik.ui.theme.BackgroundDark
import com.lordyhas.muzik.ui.viewmodel.MusicViewModel

@UnstableApi
@Composable
fun PlaylistsScreen(
    viewModel: MusicViewModel,
    onPlaylistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val showCreateDialog by viewModel.showCreatePlaylistDialog.collectAsState()
    val songPendingAddToPlaylist by viewModel.songPendingAddToPlaylist.collectAsState()
    val playlistPendingRename by viewModel.playlistPendingRename.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            ScreenHeader(title = "Playlists", subtitle = pluralize(playlists.size, "playlist"))

            when {
                isLoading -> LoadingState()
                playlists.isEmpty() -> EmptyState(
                    icon = Icons.Default.PlaylistPlay,
                    title = "No playlists yet",
                    message = "Tap \"New playlist\" to create your first one."
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.id) },
                            onRename = { viewModel.setPlaylistPendingRename(playlist) },
                            onDelete = { viewModel.deletePlaylist(playlist.id) }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { viewModel.setShowCreatePlaylistDialog(true) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            shape = CircleShape,
            containerColor = AccentPrimary,
            contentColor = Color.White,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("New playlist") }
        )
    }

    // The global dialog in MainAppScreen handles creation from the add-to-playlist sheet
    if (showCreateDialog && songPendingAddToPlaylist == null) {
        CreatePlaylistDialog(
            title = "New playlist",
            initialName = "",
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                viewModel.setShowCreatePlaylistDialog(false)
            },
            onDismiss = { viewModel.setShowCreatePlaylistDialog(false) }
        )
    }

    playlistPendingRename?.let { playlist ->
        CreatePlaylistDialog(
            title = "Rename playlist",
            initialName = playlist.name,
            confirmLabel = "Save",
            onConfirm = { newName ->
                viewModel.renamePlaylist(playlist.id, newName)
                viewModel.setPlaylistPendingRename(null)
            },
            onDismiss = { viewModel.setPlaylistPendingRename(null) }
        )
    }
}
