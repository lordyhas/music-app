package com.lordyhas.muzik.data.model

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
    val createdAt: Long,
    val songs: List<Song> = emptyList()
)
