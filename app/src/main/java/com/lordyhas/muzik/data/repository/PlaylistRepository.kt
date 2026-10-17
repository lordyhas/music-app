package com.lordyhas.muzik.data.repository

import android.content.Context
import com.lordyhas.muzik.data.local.MuzikDatabase
import com.lordyhas.muzik.data.local.PlaylistEntity
import com.lordyhas.muzik.data.local.PlaylistSongEntity
import com.lordyhas.muzik.data.local.PlaylistSongRow
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository for playlist CRUD operations backed by Room.
 *
 * All write operations are suspend functions (run on a coroutine).
 * Read operations return Flow<> so the UI auto-updates when the DB changes.
 */
class PlaylistRepository(context: Context) {

    private val db = MuzikDatabase.getInstance(context)
    private val dao = db.playlistDao()

    /** Live stream of all playlists (metadata only, no songs resolved yet). */
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = dao.getAllPlaylists()

    /** Live stream of all (playlistId, songId) rows — used to build songIds per playlist. */
    fun getAllPlaylistSongs(): Flow<List<PlaylistSongRow>> = dao.getAllPlaylistSongs()

    suspend fun createPlaylist(name: String): PlaylistEntity {
        val entity = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            createdAt = System.currentTimeMillis()
        )
        dao.insertPlaylist(entity)
        return entity
    }

    suspend fun deletePlaylist(id: String) {
        dao.deletePlaylist(id)
        // Cascade delete in FK removes playlist_songs rows automatically
    }

    suspend fun renamePlaylist(id: String, newName: String) {
        dao.renamePlaylist(id, newName.trim())
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: Long) {
        val nextPosition = dao.getMaxPosition(playlistId) + 1
        dao.insertPlaylistSong(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                position = nextPosition
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        dao.removeSongFromPlaylist(playlistId, songId)
    }

    /** Rewrites song positions so the playlist follows [orderedSongIds]. */
    suspend fun reorder(playlistId: String, orderedSongIds: List<Long>) {
        db.withTransaction {
            orderedSongIds.forEachIndexed { index, songId ->
                dao.updatePosition(playlistId, songId, index)
            }
        }
    }
}
