package com.lordyhas.muzik.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table linking playlists to songs.
 * [position] stores the order of the song within the playlist.
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE   // Deleting a playlist removes all its songs
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistSongEntity(
    val playlistId: String,
    val songId: Long,
    val position: Int
)
