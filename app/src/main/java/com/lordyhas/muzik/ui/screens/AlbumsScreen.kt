package com.lordyhas.muzik.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.lordyhas.muzik.ui.components.AlbumCard
import com.lordyhas.muzik.ui.components.EmptyState
import com.lordyhas.muzik.ui.components.LoadingState
import com.lordyhas.muzik.ui.components.MuzikSearchField
import com.lordyhas.muzik.ui.components.ScreenHeader
import com.lordyhas.muzik.ui.components.pluralize
import com.lordyhas.muzik.ui.theme.BackgroundDark
import com.lordyhas.muzik.ui.viewmodel.MusicViewModel

@UnstableApi
@Composable
fun AlbumsScreen(
    viewModel: MusicViewModel,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val albums by viewModel.albums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = searchQuery.isNotBlank()) {
        searchQuery = ""
    }

    val filteredAlbums = if (searchQuery.isBlank()) {
        albums
    } else {
        val query = searchQuery.trim().lowercase()
        albums.filter { it.title.lowercase().contains(query) || it.artist.lowercase().contains(query) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(title = "Albums", subtitle = pluralize(albums.size, "album"))

        MuzikSearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search albums or artists"
        )

        when {
            isLoading -> LoadingState()
            filteredAlbums.isEmpty() && searchQuery.isNotBlank() -> EmptyState(
                icon = Icons.Default.SearchOff,
                title = "No albums match \"$searchQuery\""
            )
            filteredAlbums.isEmpty() -> EmptyState(
                icon = Icons.Default.Album,
                title = "No albums yet",
                message = "Albums appear here once music is on your device."
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                items(filteredAlbums, key = { it.title + it.id }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = { onAlbumClick(album.title) }
                    )
                }
            }
        }
    }
}
