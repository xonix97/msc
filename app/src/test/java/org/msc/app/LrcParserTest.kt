package org.msc.app

import org.junit.Assert.*
import org.junit.Test

class LrcParserTest {
    @Test fun parsesFractionsAndRepeatedTimestamps() {
        assertEquals(listOf(LyricLine(1250, "one"), LyricLine(2523, "two"), LyricLine(3000, "one")),
            LrcParser.parse("[00:01.25][00:03]one\n[00:02.523]two"))
    }
    @Test fun ignoresMalformedAndMetadataLines() {
        assertTrue(LrcParser.parse("[ar:Original fixture]\n[00:72.00]bad\n[broken]text\n[999999999999999999999:02]overflow").isEmpty())
    }
    @Test fun seekingSelectsActualPosition() {
        val lines = LrcParser.parse("[00:01]a\n[00:03]b")
        assertEquals(-1, LrcParser.activeIndex(lines, 0))
        assertEquals(1, LrcParser.activeIndex(lines, 3500))
        assertEquals(0, LrcParser.activeIndex(lines, 1000))
    }
}
