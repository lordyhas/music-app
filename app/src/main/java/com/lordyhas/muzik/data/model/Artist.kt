package com.lordyhas.muzik.data.model

import android.net.Uri

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val songs: List<Song> = emptyList(),
    val artworkUri: Uri? = null
)
