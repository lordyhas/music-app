package com.lordyhas.muzik

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
}
