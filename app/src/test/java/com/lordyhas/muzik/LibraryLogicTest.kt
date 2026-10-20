package com.lordyhas.muzik

import android.net.FakeUri
import com.lordyhas.muzik.data.model.LibraryFilter
import com.lordyhas.muzik.data.model.Song
import com.lordyhas.muzik.data.model.filteredBy
import com.lordyhas.muzik.data.model.groupIntoAlbums
import com.lordyhas.muzik.data.model.mergeEditedOrder
import com.lordyhas.muzik.data.model.indexLetter
import com.lordyhas.muzik.data.repository.parseLyrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryLogicTest {

    @Test
    fun `lrc lyrics are parsed into sorted timed lines`() {
        val lyrics = parseLyrics(
            """
            [ar:Someone]
            [00:12.50]Second line
            [00:01.00]First line
            [01:02.123]Third line
            """.trimIndent(),
            source = "test"
        )!!

        assertTrue(lyrics.synced)
        assertEquals(listOf(1_000L, 12_500L, 62_123L), lyrics.lines.map { it.timeMs })
        assertEquals("First line", lyrics.lines.first().text)
    }

    @Test
    fun `repeated timestamps on one line produce one entry each`() {
        val lyrics = parseLyrics("[00:05.00][00:30.00]Chorus\n[00:10.00]Verse", source = "test")!!
        assertEquals(listOf("Chorus", "Verse", "Chorus"), lyrics.lines.map { it.text })
    }

    @Test
    fun `offset tag shifts every timestamp`() {
        val lyrics = parseLyrics("[offset:500]\n[00:02.00]A\n[00:04.00]B", source = "test")!!
        assertEquals(listOf(1_500L, 3_500L), lyrics.lines.map { it.timeMs })
    }

    @Test
    fun `text without timestamps is plain lyrics`() {
        val lyrics = parseLyrics("Hello\nWorld", source = "test")!!
        assertFalse(lyrics.synced)
        assertEquals(listOf("Hello", "World"), lyrics.lines.map { it.text })
    }

    @Test
    fun `blank text has no lyrics`() {
        assertNull(parseLyrics("   \n ", source = "test"))
    }

    @Test
    fun `index letters normalize accents and group non letters`() {
        assertEquals("E", indexLetter("Élodie"))
        assertEquals("A", indexLetter("  abba"))
        assertEquals("#", indexLetter("2Pac"))
        assertEquals("#", indexLetter(""))
    }

    private fun song(
        id: Long,
        album: String,
        albumId: Long,
        artist: String = "Artist",
        track: Int? = null,
        disc: Int? = null,
        folder: String = "/storage/emulated/0/Music",
        duration: Long = 180_000L,
        isRecording: Boolean = false
    ) = Song(
        id = id,
        title = "Song $id",
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration,
        contentUri = FakeUri("content://media/$id"),
        trackNumber = track,
        discNumber = disc,
        folderPath = folder,
        isRecording = isRecording
    )

    @Test
    fun `albums with the same title but different ids stay separate`() {
        val albums = listOf(
            song(1, "Greatest Hits", albumId = 10, artist = "Queen"),
            song(2, "Greatest Hits", albumId = 20, artist = "ABBA"),
            song(3, "Greatest Hits", albumId = 10, artist = "Queen")
        ).groupIntoAlbums()

        assertEquals(2, albums.size)
        // Same title, so ordered by artist
        assertEquals(listOf("ABBA", "Queen"), albums.map { it.artist })
        assertEquals(listOf(20L, 10L), albums.map { it.id })
        assertEquals(listOf(2L), albums[0].songs.map { it.id })
        assertEquals(listOf(1L, 3L), albums[1].songs.map { it.id })
    }

    @Test
    fun `album tracks are ordered by disc then track`() {
        val album = listOf(
            song(1, "Double", albumId = 5, track = 2, disc = 2),
            song(2, "Double", albumId = 5, track = 1, disc = 2),
            song(3, "Double", albumId = 5, track = 1, disc = 1)
        ).groupIntoAlbums().single()

        assertEquals(listOf(3L, 2L, 1L), album.songs.map { it.id })
        assertEquals(3, album.songCount)
    }

    @Test
    fun `album artist is the most frequent track artist`() {
        val album = listOf(
            song(1, "Record", albumId = 7, artist = "Main feat. Guest"),
            song(2, "Record", albumId = 7, artist = "Main"),
            song(3, "Record", albumId = 7, artist = "Main")
        ).groupIntoAlbums().single()

        assertEquals("Main", album.artist)
    }

    @Test
    fun `albums are sorted by title ignoring case`() {
        val titles = listOf(
            song(1, "zebra", albumId = 1),
            song(2, "Apple", albumId = 2),
            song(3, "mango", albumId = 3)
        ).groupIntoAlbums().map { it.title }

        assertEquals(listOf("Apple", "mango", "zebra"), titles)
    }

    @Test
    fun `hidden folder hides its songs and its subfolders but not look-alike names`() {
        val filter = LibraryFilter(hiddenFolders = setOf("/sdcard/Recordings"))
        val kept = listOf(
            song(1, "A", albumId = 1, folder = "/sdcard/Recordings"),
            song(2, "A", albumId = 1, folder = "/sdcard/Recordings/Calls"),
            song(3, "A", albumId = 1, folder = "/sdcard/Recordings Old"),
            song(4, "A", albumId = 1, folder = "/sdcard/Music")
        ).filteredBy(filter)

        assertEquals(listOf(3L, 4L), kept.map { it.id })
    }

    @Test
    fun `short clips and recordings are filtered but unknown durations are kept`() {
        val filter = LibraryFilter(hideRecordings = true, minDurationMs = 30_000L)
        val kept = listOf(
            song(1, "A", albumId = 1, duration = 5_000L),
            song(2, "A", albumId = 1, duration = 0L),
            song(3, "A", albumId = 1, isRecording = true),
            song(4, "A", albumId = 1)
        ).filteredBy(filter)

        assertEquals(listOf(2L, 4L), kept.map { it.id })
        val recording = song(5, "A", albumId = 1, isRecording = true)
        assertEquals(listOf(5L), listOf(recording).filteredBy(LibraryFilter(hideRecordings = false)).map { it.id })
    }

    @Test
    fun `playlist edit applies new order and drops removed songs`() {
        val order = mergeEditedOrder(
            allIds = listOf(1L, 2L, 3L, 4L),
            shownIds = setOf(1L, 2L, 3L, 4L),
            keptIds = listOf(4L, 1L, 3L) // 2 removed, 4 moved to the top
        )
        assertEquals(listOf(4L, 1L, 3L), order)
    }

    @Test
    fun `songs not shown in the editor keep their slots`() {
        // Song 3 is missing from the device (or in a hidden folder), so the editor never showed it
        val order = mergeEditedOrder(
            allIds = listOf(1L, 2L, 3L, 4L, 5L),
            shownIds = setOf(1L, 2L, 4L, 5L),
            keptIds = listOf(5L, 1L) // 2 and 4 removed, 5 moved first
        )
        assertEquals(listOf(5L, 1L, 3L), order)
    }
}
