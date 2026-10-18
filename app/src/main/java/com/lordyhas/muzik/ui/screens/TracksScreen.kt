package com.lordyhas.muzik.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.lordyhas.muzik.data.model.SongSort
import com.lordyhas.muzik.data.model.indexLetter
import com.lordyhas.muzik.ui.components.AlphabetFastScroller
import com.lordyhas.muzik.ui.components.CircleIconButton
import com.lordyhas.muzik.ui.components.SortMenuButton
import com.lordyhas.muzik.ui.components.EmptyState
import com.lordyhas.muzik.ui.components.LoadingState
import com.lordyhas.muzik.ui.components.MuzikSearchField
import com.lordyhas.muzik.ui.components.ScreenHeader
import com.lordyhas.muzik.ui.components.SongItem
import com.lordyhas.muzik.ui.components.pluralize
import com.lordyhas.muzik.ui.theme.AccentPrimary
import com.lordyhas.muzik.ui.theme.BackgroundDark
import com.lordyhas.muzik.ui.theme.TextSecondary
import com.lordyhas.muzik.ui.viewmodel.MusicViewModel

@UnstableApi
@Composable
fun TracksScreen(
    viewModel: MusicViewModel,
    onGoToArtist: (String) -> Unit,
    onGoToAlbum: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.filteredSongs.collectAsState()
    val totalSongs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val likedIds by viewModel.likedSongIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val songSort by viewModel.songSort.collectAsState()
    val listState = rememberLazyListState()

    BackHandler(enabled = searchQuery.isNotBlank()) {
        viewModel.updateSearchQuery("")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(
            title = "Songs",
            subtitle = pluralize(totalSongs.size, "song") + " in your library"
        ) {
            if (totalSongs.isNotEmpty()) {
                CircleIconButton(
                    icon = Icons.Default.Shuffle,
                    contentDescription = "Shuffle all",
                    onClick = { viewModel.playRandom() },
                    tint = AccentPrimary
                )
            }
            SortMenuButton(
                options = SongSort.entries,
                selected = songSort,
                label = { it.label },
                onSelect = { viewModel.setSongSort(it) }
            )
            CircleIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Refresh library",
                onClick = { viewModel.loadSongs() },
                tint = TextSecondary
            )
        }

        MuzikSearchField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = "Search songs, artists, albums"
        )

        when {
            isLoading -> LoadingState()
            songs.isEmpty() && searchQuery.isNotBlank() -> EmptyState(
                icon = Icons.Default.SearchOff,
                title = "No results for \"$searchQuery\"",
                message = "Try a different title, artist or album."
            )
            songs.isEmpty() -> EmptyState(
                icon = Icons.Default.MusicNote,
                title = "No music found",
                message = "Add audio files to your device, then tap refresh."
            )
            else -> Box(modifier = Modifier.fillMaxSize()) {
            val scroller = showScroller(songSort.alphabetical, songs.size, searchQuery)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(end = if (scroller) 18.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                items(songs, key = { it.contentUri.toString() }) { song ->
                    val isSelected = currentSong?.id == song.id
                    SongItem(
                        song = song,
                        isSelected = isSelected,
                        isPlaying = isPlaying && isSelected,
                        onClick = { viewModel.playSong(song) },
                        onAddToQueue = { viewModel.addToQueue(song) },
                        isLiked = song.id in likedIds,
                        onToggleLike = { viewModel.toggleLike(song) },
                        onAddToPlaylist = { viewModel.setSongPendingAddToPlaylist(song) },
                        onGoToArtist = { onGoToArtist(song.artist) },
                        onGoToAlbum = { onGoToAlbum(song.albumId) },
                        onShowDetails = { viewModel.selectSongForDetails(song) }
                    )
                }
            }
            if (scroller) {
                AlphabetFastScroller(
                    letters = remember(songs, songSort) { songs.map { indexLetter(songSort.indexText(it)) } },
                    onScrollToIndex = { listState.scrollToItem(it) },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            }
        }
    }
}

/** The A–Z scroller only makes sense for long, alphabetically sorted, unfiltered lists. */
internal fun showScroller(alphabetical: Boolean, itemCount: Int, query: String) =
    alphabetical && itemCount > 30 && query.isBlank()
