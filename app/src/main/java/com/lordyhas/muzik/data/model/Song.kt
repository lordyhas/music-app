package com.lordyhas.muzik.data.model

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.util.Locale
import java.util.concurrent.TimeUnit

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val contentUri: Uri,
    val albumArtUri: Uri? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null
) {
    fun formattedDuration(): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun toMediaItem(): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setDisplayTitle(title)

        trackNumber?.let { metadataBuilder.setTrackNumber(it) }
        discNumber?.let { metadataBuilder.setDiscNumber(it) }

        albumArtUri?.let {
            metadataBuilder.setArtworkUri(it)
        }

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }
}
