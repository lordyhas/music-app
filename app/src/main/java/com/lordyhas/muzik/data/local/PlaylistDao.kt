package com.lordyhas.muzik.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    // ─── Playlists ────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: String, name: String)

    /** Returns all playlists ordered by creation date, as a live Flow. */
    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    // ─── Playlist Songs ───────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSong(entry: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long)

    /** Returns song IDs for a playlist ordered by their position. */
    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getSongIdsForPlaylist(playlistId: String): List<Long>

    /**
     * Returns a map of playlistId → ordered list of songIds, as a live Flow.
     * Room re-emits whenever playlist_songs changes.
     */
    @Query("SELECT playlistId, songId FROM playlist_songs ORDER BY playlistId, position ASC")
    fun getAllPlaylistSongs(): Flow<List<PlaylistSongRow>>

    /** Returns the max position value for a given playlist (to append new songs). */
    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: String): Int
}

/** Lightweight projection used to build in-memory song ID lists per playlist. */
data class PlaylistSongRow(val playlistId: String, val songId: Long)
