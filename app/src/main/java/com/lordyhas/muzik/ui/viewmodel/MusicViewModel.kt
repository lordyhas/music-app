package com.lordyhas.muzik.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.lordyhas.muzik.data.model.Song
import com.lordyhas.muzik.data.repository.MusicRepository
import com.lordyhas.muzik.service.MusicController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@UnstableApi
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    val musicController = MusicController(application)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _showNowPlaying = MutableStateFlow(false)
    val showNowPlaying: StateFlow<Boolean> = _showNowPlaying.asStateFlow()

    val filteredSongs: StateFlow<List<Song>> = combine(_songs, _searchQuery) { songList, query ->
        if (query.isBlank()) {
            songList
        } else {
            val lowerQuery = query.trim().lowercase()
            songList.filter {
                it.title.lowercase().contains(lowerQuery) ||
                it.artist.lowercase().contains(lowerQuery) ||
                it.album.lowercase().contains(lowerQuery)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentSong: StateFlow<Song?> = combine(_songs, musicController.currentMediaItem) { songList, mediaItem ->
        if (mediaItem == null) null
        else {
            val mediaId = mediaItem.mediaId
            songList.find { it.id.toString() == mediaId } ?: Song(
                id = mediaId.toLongOrNull() ?: 0L,
                title = mediaItem.mediaMetadata.title?.toString() ?: "Titre inconnu",
                artist = mediaItem.mediaMetadata.artist?.toString() ?: "Artiste inconnu",
                album = mediaItem.mediaMetadata.albumTitle?.toString() ?: "Album inconnu",
                albumId = 0L,
                duration = 0L,
                contentUri = android.net.Uri.EMPTY,
                albumArtUri = mediaItem.mediaMetadata.artworkUri
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val isPlaying = musicController.isPlaying
    val currentPosition = musicController.currentPosition
    val duration = musicController.duration
    val isShuffle = musicController.isShuffle
    val repeatMode = musicController.repeatMode

    fun setPermissionGranted(granted: Boolean) {
        _isPermissionGranted.value = granted
        if (granted) {
            loadSongs()
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            val loaded = repository.getLocalSongs()
            _songs.value = loaded
            _isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playSong(song: Song) {
        val currentPlaylist = filteredSongs.value.ifEmpty { _songs.value }
        val index = currentPlaylist.indexOfFirst { it.id == song.id }
        if (index != -1) {
            musicController.playSongList(currentPlaylist, index)
        } else {
            musicController.playSongList(listOf(song), 0)
        }
    }

    fun togglePlayPause() {
        if (currentSong.value == null && _songs.value.isNotEmpty()) {
            playSong(_songs.value.first())
        } else {
            musicController.togglePlayPause()
        }
    }

    fun seekTo(positionMs: Long) {
        musicController.seekTo(positionMs)
    }

    fun skipNext() {
        musicController.skipToNext()
    }

    fun skipPrevious() {
        musicController.skipToPrevious()
    }

    fun toggleShuffle() {
        musicController.toggleShuffle()
    }

    fun toggleRepeat() {
        musicController.toggleRepeat()
    }

    fun setShowNowPlaying(show: Boolean) {
        _showNowPlaying.value = show
    }

    override fun onCleared() {
        super.onCleared()
        musicController.release()
    }
}
