package com.lordyhas.muzik.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.lordyhas.muzik.data.model.Playlist
import com.lordyhas.muzik.ui.components.DetailHero
import com.lordyhas.muzik.ui.components.DetailNotFound
import com.lordyhas.muzik.ui.components.DetailScaffold
import com.lordyhas.muzik.ui.components.EmptyState
import com.lordyhas.muzik.ui.components.PlaylistCollage
import com.lordyhas.muzik.ui.components.SongItem
import com.lordyhas.muzik.ui.components.pluralize
import com.lordyhas.muzik.ui.viewmodel.MusicViewModel

@OptIn(UnstableApi::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    onGoToArtist: (String) -> Unit,
    onGoToAlbum: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        onBackClick()
    }

    val playlists by viewModel.playlists.collectAsState()
    val playlist: Playlist? = playlists.find { it.id == playlistId }

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    if (playlist == null) {
        DetailNotFound(message = "Playlist not found", onBack = onBackClick, modifier = modifier)
        return
    }

    val songs = playlist.songs
    val firstArtworkUri = songs.firstOrNull { it.albumArtUri != null }?.albumArtUri
    val hasSongs = songs.isNotEmpty()

    DetailScaffold(title = playlist.name, onBack = onBackClick, modifier = modifier) {
        item(key = "hero") {
            DetailHero(
                artworkUri = firstArtworkUri,
                eyebrow = "Playlist",
                title = playlist.name,
                meta = if (hasSongs) {
                    "${pluralize(songs.size, "song")} • ${formatTotalDuration(songs.sumOf { it.duration })}"
                } else "No songs yet",
                placeholderIcon = Icons.Default.PlaylistPlay,
                cover = { coverModifier -> PlaylistCollage(playlist = playlist, modifier = coverModifier) },
                onPlay = if (hasSongs) ({ viewModel.playPlaylist(playlist) }) else null,
                onShuffle = if (hasSongs) ({
                    val shuffled = songs.shuffled()
                    viewModel.playSong(shuffled.first(), shuffled)
                }) else null
            )
        }

        if (!hasSongs) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Default.LibraryMusic,
                    title = "This playlist is empty",
                    message = "Add songs from the ⋮ menu on any track.",
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                )
            }
        } else {
            items(songs, key = { it.contentUri.toString() }) { song ->
                val isSelected = currentSong?.id == song.id
                SongItem(
                    song = song,
                    isSelected = isSelected,
                    isPlaying = isPlaying && isSelected,
                    onClick = { viewModel.playPlaylist(playlist, song) },
                    onAddToQueue = { viewModel.addToQueue(song) },
                    onRemoveFromPlaylist = { viewModel.removeSongFromPlaylist(playlist.id, song.id) },
                    onGoToArtist = { onGoToArtist(song.artist) },
                    onGoToAlbum = { onGoToAlbum(song.album) },
                    onShowDetails = { viewModel.selectSongForDetails(song) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}
