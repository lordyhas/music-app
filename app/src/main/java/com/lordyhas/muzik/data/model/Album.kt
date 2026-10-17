package com.lordyhas.muzik.data.model

import android.net.Uri

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val albumArtUri: Uri?,
    val songCount: Int,
    val songs: List<Song> = emptyList()
)

fun List<Song>.sortedForAlbum(): List<Song> {
    return sortedWith(
        compareBy<Song> { it.discNumber ?: 1 }
            .thenBy { if (it.trackNumber != null && it.trackNumber > 0) 0 else 1 }
            .thenBy { it.trackNumber ?: Int.MAX_VALUE }
            .thenBy { it.title.lowercase() }
    )
}

