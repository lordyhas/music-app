package com.lordyhas.muzik.data.model

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
    val createdAt: Long,
    val songs: List<Song> = emptyList()
)

/**
 * Song order after an edit in which only [shownIds] were on screen and [keptIds] is what is left of
 * them, in their new order. Songs that weren't shown (missing from the device, or in hidden
 * folders) keep their slots; the slots of removed songs are dropped.
 */
fun mergeEditedOrder(allIds: List<Long>, shownIds: Set<Long>, keptIds: List<Long>): List<Long> {
    val kept = keptIds.iterator()
    return allIds.mapNotNull { id ->
        when {
            id !in shownIds -> id
            kept.hasNext() -> kept.next()
            else -> null
        }
    }
}
