package com.lordyhas.muzik.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.lordyhas.muzik.data.model.LibraryFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide [LibraryFilter], persisted in SharedPreferences. The UI changes it and the playback
 * service reads it for the Android Auto browse tree, like [com.lordyhas.muzik.service.PlaybackSettings].
 */
object LibraryFilterStore {

    private const val PREFS = "muzik_library"
    private const val KEY_HIDDEN_FOLDERS = "hidden_folders"
    private const val KEY_HIDE_RECORDINGS = "hide_recordings"
    private const val KEY_MIN_DURATION = "min_duration_ms"

    private var prefs: SharedPreferences? = null

    private val _filter = MutableStateFlow(LibraryFilter())
    val filter: StateFlow<LibraryFilter> = _filter.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _filter.value = LibraryFilter(
            hiddenFolders = p.getStringSet(KEY_HIDDEN_FOLDERS, null).orEmpty().toSet(),
            hideRecordings = p.getBoolean(KEY_HIDE_RECORDINGS, true),
            minDurationMs = p.getLong(KEY_MIN_DURATION, 0L)
        )
    }

    fun setFolderHidden(path: String, hidden: Boolean) = update {
        it.copy(hiddenFolders = if (hidden) it.hiddenFolders + path else it.hiddenFolders - path)
    }

    fun setHideRecordings(hide: Boolean) = update { it.copy(hideRecordings = hide) }

    fun setMinDuration(ms: Long) = update { it.copy(minDurationMs = ms) }

    private fun update(transform: (LibraryFilter) -> LibraryFilter) {
        val next = transform(_filter.value)
        _filter.value = next
        prefs?.edit()
            ?.putStringSet(KEY_HIDDEN_FOLDERS, HashSet(next.hiddenFolders))
            ?.putBoolean(KEY_HIDE_RECORDINGS, next.hideRecordings)
            ?.putLong(KEY_MIN_DURATION, next.minDurationMs)
            ?.apply()
    }
}
