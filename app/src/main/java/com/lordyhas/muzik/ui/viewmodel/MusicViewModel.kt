package com.lordyhas.muzik.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.lordyhas.muzik.data.model.Album
import com.lordyhas.muzik.data.model.Artist
import com.lordyhas.muzik.data.model.Song
import com.lordyhas.muzik.data.repository.MusicRepository
import com.lordyhas.muzik.service.MusicController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@UnstableApi
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("muzik_prefs", Context.MODE_PRIVATE)
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

    private val _recentSongIds = MutableStateFlow<List<Long>>(loadRecentSongIds())

    // Detail sheets
    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist.asStateFlow()

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

    val albums: StateFlow<List<Album>> = _songs.map { songList ->
        songList.groupBy { it.album }
            .map { (albumTitle, albumSongs) ->
                val first = albumSongs.first()
                Album(
                    id = first.albumId,
                    title = albumTitle,
                    artist = first.artist,
                    albumArtUri = albumSongs.firstOrNull { it.albumArtUri != null }?.albumArtUri,
                    songCount = albumSongs.size,
                    songs = albumSongs
                )
            }
            .sortedBy { it.title.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val artists: StateFlow<List<Artist>> = _songs.map { songList ->
        songList.groupBy { it.artist }
            .map { (artistName, artistSongs) ->
                val albumCount = artistSongs.map { it.album }.distinct().size
                Artist(
                    name = artistName,
                    songCount = artistSongs.size,
                    albumCount = albumCount,
                    songs = artistSongs,
                    artworkUri = artistSongs.firstOrNull { it.albumArtUri != null }?.albumArtUri
                )
            }
            .sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentlyPlayedSongs: StateFlow<List<Song>> = combine(_songs, _recentSongIds) { songList, recentIds ->
        val songMap = songList.associateBy { it.id }
        recentIds.mapNotNull { songMap[it] }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentlyPlayedAlbums: StateFlow<List<Album>> = combine(albums, recentlyPlayedSongs) { allAlbums, recentSongs ->
        val recentAlbumNames = recentSongs.map { it.album }.distinct()
        val albumMap = allAlbums.associateBy { it.title }
        val recents = recentAlbumNames.mapNotNull { albumMap[it] }
        if (recents.isNotEmpty()) recents else allAlbums.take(6)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentlyPlayedArtists: StateFlow<List<Artist>> = combine(artists, recentlyPlayedSongs) { allArtists, recentSongs ->
        val recentArtistNames = recentSongs.map { it.artist }.distinct()
        val artistMap = allArtists.associateBy { it.name }
        val recents = recentArtistNames.mapNotNull { artistMap[it] }
        if (recents.isNotEmpty()) recents else allArtists.take(8)
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

    fun playSong(song: Song, playlist: List<Song>? = null) {
        val currentPlaylist = playlist ?: filteredSongs.value.ifEmpty { _songs.value }
        val index = currentPlaylist.indexOfFirst { it.id == song.id }
        if (index != -1) {
            musicController.playSongList(currentPlaylist, index)
        } else {
            musicController.playSongList(listOf(song), 0)
        }
        addToRecentHistory(song.id)
    }

    fun playAlbum(album: Album, startSong: Song? = null) {
        if (album.songs.isNotEmpty()) {
            val targetSong = startSong ?: album.songs.first()
            playSong(targetSong, album.songs)
        }
    }

    fun playArtist(artist: Artist, startSong: Song? = null) {
        if (artist.songs.isNotEmpty()) {
            val targetSong = startSong ?: artist.songs.first()
            playSong(targetSong, artist.songs)
        }
    }

    fun playRandom() {
        val all = _songs.value
        if (all.isNotEmpty()) {
            val shuffled = all.shuffled()
            musicController.playSongList(shuffled, 0)
            addToRecentHistory(shuffled.first().id)
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

    fun selectAlbum(album: Album?) {
        _selectedAlbum.value = album
    }

    fun selectArtist(artist: Artist?) {
        _selectedArtist.value = artist
    }

    private fun addToRecentHistory(songId: Long) {
        val current = _recentSongIds.value.toMutableList()
        current.remove(songId)
        current.add(0, songId)
        val trimmed = current.take(30)
        _recentSongIds.value = trimmed
        saveRecentSongIds(trimmed)
    }

    private fun loadRecentSongIds(): List<Long> {
        val raw = prefs.getString("recent_song_ids", null) ?: return emptyList()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    private fun saveRecentSongIds(ids: List<Long>) {
        val raw = ids.joinToString(",")
        prefs.edit().putString("recent_song_ids", raw).apply()
    }

    override fun onCleared() {
        super.onCleared()
        musicController.release()
    }
}
