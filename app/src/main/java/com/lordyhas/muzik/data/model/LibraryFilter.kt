package com.lordyhas.muzik.data.model

/**
 * What the user excluded from the library: hidden folders (and everything below them), voice
 * recordings, and clips shorter than [minDurationMs]. Applied wherever songs are listed, Android
 * Auto included, so excluded audio shows up nowhere.
 */
data class LibraryFilter(
    val hiddenFolders: Set<String> = emptySet(),
    val hideRecordings: Boolean = true,
    val minDurationMs: Long = 0L
) {
    fun isFolderHidden(path: String): Boolean = hidingFolderOf(path) != null

    /** The hidden folder that hides [path]: [path] itself or one of its parents, else null. */
    fun hidingFolderOf(path: String): String? =
        if (path in hiddenFolders) path else hiddenFolders.firstOrNull { path.startsWith("$it/") }

    fun allows(song: Song): Boolean =
        !(hideRecordings && song.isRecording) &&
            // Duration 0 means MediaStore doesn't know it yet; don't hide those.
            (minDurationMs <= 0L || song.duration <= 0L || song.duration >= minDurationMs) &&
            !isFolderHidden(song.folderPath)

    companion object {
        /** Choices offered for "Skip short audio", in milliseconds (0 = off). */
        val MIN_DURATION_OPTIONS = listOf(0L, 15_000L, 30_000L, 60_000L, 120_000L)
    }
}

fun List<Song>.filteredBy(filter: LibraryFilter): List<Song> = filter(filter::allows)
