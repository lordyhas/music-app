package com.lordyhas.muzik.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A song the user hearted. [songId] is the MediaStore id. */
@Entity(tableName = "liked_songs")
data class LikedSongEntity(
    @PrimaryKey val songId: Long,
    val likedAt: Long
)

/** Listening history per song, used for "Recently played", "Most played" and "Rediscover". */
@Entity(tableName = "play_stats")
data class PlayStatEntity(
    @PrimaryKey val songId: Long,
    val playCount: Int,
    val lastPlayedAt: Long
)

@Dao
interface LibraryDao {

    // ─── Likes ────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM liked_songs ORDER BY likedAt DESC")
    fun getLikedSongs(): Flow<List<LikedSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun like(entity: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE songId = :songId")
    suspend fun unlike(songId: Long)

    // ─── Play stats ───────────────────────────────────────────────────────────

    @Query("SELECT * FROM play_stats")
    fun getPlayStats(): Flow<List<PlayStatEntity>>

    @Query("INSERT OR IGNORE INTO play_stats (songId, playCount, lastPlayedAt) VALUES (:songId, 0, 0)")
    suspend fun ensurePlayStat(songId: Long)

    @Query("UPDATE play_stats SET playCount = playCount + 1, lastPlayedAt = :playedAt WHERE songId = :songId")
    suspend fun incrementPlayCount(songId: Long, playedAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlayStats(stats: List<PlayStatEntity>)
}
