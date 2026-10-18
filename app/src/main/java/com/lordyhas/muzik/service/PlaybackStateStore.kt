package com.lordyhas.muzik.service

import android.content.Context
import androidx.media3.common.Player

/** Snapshot of the playback session, restored on the next launch. */
data class SavedSession(
    val songIds: List<Long>,
    val index: Int,
    val positionMs: Long,
    /** How many songs after [index] were manually queued (see MusicController's queue model). */
    val userQueueCount: Int,
    /** Unshuffled order of the album/playlist being played, to restore order when shuffle is turned off. */
    val contextIds: List<Long>,
    val shuffle: Boolean,
    val repeatMode: Int
)

/**
 * Persists the current queue and position in SharedPreferences.
 *
 * MusicController writes the full state while the UI is alive; the playback service writes
 * queue + position while playing in the background so the snapshot stays fresh.
 */
object PlaybackStateStore {

    private const val PREFS = "muzik_session"
    private const val KEY_IDS = "ids"
    private const val KEY_INDEX = "index"
    private const val KEY_POSITION = "position"
    private const val KEY_USER_QUEUE = "user_queue"
    private const val KEY_CONTEXT = "context_ids"
    private const val KEY_SHUFFLE = "shuffle"
    private const val KEY_REPEAT = "repeat"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, session: SavedSession) {
        if (session.songIds.isEmpty()) return
        prefs(context).edit()
            .putString(KEY_IDS, session.songIds.joinToString(","))
            .putInt(KEY_INDEX, session.index)
            .putLong(KEY_POSITION, session.positionMs)
            .putInt(KEY_USER_QUEUE, session.userQueueCount)
            .putString(KEY_CONTEXT, session.contextIds.joinToString(","))
            .putBoolean(KEY_SHUFFLE, session.shuffle)
            .putInt(KEY_REPEAT, session.repeatMode)
            .apply()
    }

    /**
     * Background update from the service: it only knows the player's items, so it keeps the
     * rest of the snapshot and shrinks the user queue by however many songs were advanced.
     */
    fun saveProgress(context: Context, songIds: List<Long>, index: Int, positionMs: Long) {
        if (songIds.isEmpty()) return
        val previous = load(context)
        val sameQueue = previous != null && previous.songIds == songIds
        val advanced = if (sameQueue) index - previous!!.index else 0
        save(
            context,
            SavedSession(
                songIds = songIds,
                index = index,
                positionMs = positionMs,
                userQueueCount = if (sameQueue) (previous!!.userQueueCount - advanced).coerceAtLeast(0) else 0,
                contextIds = previous?.contextIds.orEmpty(),
                shuffle = previous?.shuffle ?: false,
                repeatMode = previous?.repeatMode ?: Player.REPEAT_MODE_OFF
            )
        )
    }

    fun load(context: Context): SavedSession? {
        val p = prefs(context)
        val ids = p.getString(KEY_IDS, null).toIdList()
        if (ids.isEmpty()) return null
        return SavedSession(
            songIds = ids,
            index = p.getInt(KEY_INDEX, 0).coerceIn(0, ids.lastIndex),
            positionMs = p.getLong(KEY_POSITION, 0L).coerceAtLeast(0L),
            userQueueCount = p.getInt(KEY_USER_QUEUE, 0).coerceAtLeast(0),
            contextIds = p.getString(KEY_CONTEXT, null).toIdList(),
            shuffle = p.getBoolean(KEY_SHUFFLE, false),
            repeatMode = p.getInt(KEY_REPEAT, Player.REPEAT_MODE_OFF)
        )
    }

    private fun String?.toIdList(): List<Long> =
        this?.split(",")?.mapNotNull { it.trim().toLongOrNull() }.orEmpty()
}
