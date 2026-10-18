package com.lordyhas.muzik.data.model

import java.text.Normalizer

enum class SongSort(val label: String, val alphabetical: Boolean) {
    TITLE("Title", true),
    ARTIST("Artist", true),
    ALBUM("Album", true),
    DATE_ADDED("Recently added", false),
    DURATION("Duration", false);

    fun apply(songs: List<Song>): List<Song> = when (this) {
        TITLE -> songs.sortedBy { it.title.lowercase() }
        ARTIST -> songs.sortedWith(compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() })
        ALBUM -> songs.sortedWith(compareBy<Song> { it.album.lowercase() }.thenBy { it.discNumber ?: 1 }.thenBy { it.trackNumber ?: Int.MAX_VALUE })
        DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
        DURATION -> songs.sortedByDescending { it.duration }
    }

    /** Text the A–Z scroller indexes for [song] under this order. */
    fun indexText(song: Song): String = when (this) {
        ARTIST -> song.artist
        ALBUM -> song.album
        else -> song.title
    }
}

enum class AlbumSort(val label: String, val alphabetical: Boolean) {
    TITLE("Title", true),
    ARTIST("Artist", true),
    SONG_COUNT("Most songs", false);

    fun apply(albums: List<Album>): List<Album> = when (this) {
        TITLE -> albums.sortedBy { it.title.lowercase() }
        ARTIST -> albums.sortedWith(compareBy<Album> { it.artist.lowercase() }.thenBy { it.title.lowercase() })
        SONG_COUNT -> albums.sortedByDescending { it.songCount }
    }

    fun indexText(album: Album): String = if (this == ARTIST) album.artist else album.title
}

enum class ArtistSort(val label: String, val alphabetical: Boolean) {
    NAME("Name", true),
    SONG_COUNT("Most songs", false);

    fun apply(artists: List<Artist>): List<Artist> = when (this) {
        NAME -> artists.sortedBy { it.name.lowercase() }
        SONG_COUNT -> artists.sortedByDescending { it.songCount }
    }
}

/** A device folder that directly contains music. */
data class Folder(
    val path: String,
    val name: String,
    val songs: List<Song>
)

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList()
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
}

/** "Élodie" → "E", "2Pac" → "#": the section letter used by the A–Z scroller. */
fun indexLetter(text: String): String {
    val first = text.trim().firstOrNull() ?: return "#"
    val base = Normalizer.normalize(first.toString(), Normalizer.Form.NFD)
        .firstOrNull()
        ?.uppercaseChar()
        ?: return "#"
    return if (base in 'A'..'Z') base.toString() else "#"
}
