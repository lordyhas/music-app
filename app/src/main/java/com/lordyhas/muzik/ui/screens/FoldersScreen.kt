package com.lordyhas.muzik.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.lordyhas.muzik.data.model.Folder
import com.lordyhas.muzik.ui.components.CircleIconButton
import com.lordyhas.muzik.ui.components.DetailHero
import com.lordyhas.muzik.ui.components.DetailNotFound
import com.lordyhas.muzik.ui.components.DetailScaffold
import com.lordyhas.muzik.ui.components.EmptyState
import com.lordyhas.muzik.ui.components.ScreenHeader
import com.lordyhas.muzik.ui.components.SongItem
import com.lordyhas.muzik.ui.components.pluralize
import com.lordyhas.muzik.ui.theme.AccentPrimary
import com.lordyhas.muzik.ui.theme.BackgroundDark
import com.lordyhas.muzik.ui.theme.TextMuted
import com.lordyhas.muzik.ui.theme.TextPrimary
import com.lordyhas.muzik.ui.theme.TextSecondary
import com.lordyhas.muzik.ui.viewmodel.MusicViewModel

@UnstableApi
@Composable
fun FoldersScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    onFolderClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) { onBackClick() }
    val folders by viewModel.folders.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBackClick
            )
        }
        ScreenHeader(title = "Folders", subtitle = pluralize(folders.size, "folder"))

        if (folders.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Folder,
                title = "No folders found",
                message = "Folders containing music will show up here."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                items(folders, key = { it.path }) { folder ->
                    FolderRow(folder = folder, onClick = { onFolderClick(folder.path) })
                }
            }
        }
    }
}

@Composable
private fun FolderRow(folder: Folder, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentPrimary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(folder.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "${pluralize(folder.songs.size, "song")} • ${displayPath(folder.path)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(22.dp))
    }
}

@UnstableApi
@Composable
fun FolderDetailScreen(
    folderPath: String,
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    onGoToArtist: (String) -> Unit,
    onGoToAlbum: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) { onBackClick() }

    val folders by viewModel.folders.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val folder = folders.find { it.path == folderPath }

    if (folder == null) {
        DetailNotFound(message = "Folder not found", onBack = onBackClick, modifier = modifier)
        return
    }

    val songs = folder.songs
    DetailScaffold(title = folder.name, onBack = onBackClick, modifier = modifier) {
        item(key = "hero") {
            DetailHero(
                artworkUri = songs.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                eyebrow = "Folder",
                title = folder.name,
                subtitle = displayPath(folder.path),
                meta = "${pluralize(songs.size, "song")} • ${formatTotalDuration(songs.sumOf { it.duration })}",
                placeholderIcon = Icons.Default.Folder,
                onPlay = { viewModel.playSong(songs.first(), songs) },
                onShuffle = {
                    val shuffled = songs.shuffled()
                    viewModel.playSong(shuffled.first(), shuffled)
                }
            )
        }
        items(songs, key = { it.contentUri.toString() }) { song ->
            val isSelected = currentSong?.id == song.id
            SongItem(
                song = song,
                isSelected = isSelected,
                isPlaying = isPlaying && isSelected,
                onClick = { viewModel.playSong(song, songs) },
                onAddToQueue = { viewModel.addToQueue(song) },
                onAddToPlaylist = { viewModel.setSongPendingAddToPlaylist(song) },
                onGoToArtist = { onGoToArtist(song.artist) },
                onGoToAlbum = { onGoToAlbum(song.album) },
                onShowDetails = { viewModel.selectSongForDetails(song) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

/** "/storage/emulated/0/Music/Rock" → "Internal storage/Music/Rock" */
internal fun displayPath(path: String): String =
    path.replace(Regex("^/storage/emulated/\\d+"), "Internal storage")
        .replace(Regex("^/storage/([^/]+)"), "SD card")
