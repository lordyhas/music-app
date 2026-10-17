package com.lordyhas.muzik.data.repository

import android.content.Context
import com.lordyhas.muzik.data.local.LikedSongEntity
import com.lordyhas.muzik.data.local.MuzikDatabase
import com.lordyhas.muzik.data.local.PlayStatEntity
import kotlinx.coroutines.flow.Flow

/** Likes and listening history, backed by Room. */
class LibraryRepository(context: Context) {

    private val dao = MuzikDatabase.getInstance(context).libraryDao()

    fun getLikedSongs(): Flow<List<LikedSongEntity>> = dao.getLikedSongs()

    fun getPlayStats(): Flow<List<PlayStatEntity>> = dao.getPlayStats()

    suspend fun setLiked(songId: Long, liked: Boolean) {
        if (liked) dao.like(LikedSongEntity(songId, System.currentTimeMillis())) else dao.unlike(songId)
    }

    suspend fun recordPlay(songId: Long, playedAt: Long = System.currentTimeMillis()) {
        dao.ensurePlayStat(songId)
        dao.incrementPlayCount(songId, playedAt)
    }

    /** One-time import of the old SharedPreferences "recently played" list (most recent first). */
    suspend fun importRecentHistory(songIdsMostRecentFirst: List<Long>) {
        val now = System.currentTimeMillis()
        dao.insertPlayStats(
            songIdsMostRecentFirst.mapIndexed { i, id -> PlayStatEntity(id, playCount = 1, lastPlayedAt = now - i * 1000L) }
        )
    }
}
