package org.discnest.app

data class LyricLine(val timeMs: Long, val text: String)

/** Parses multiple timestamps per line; ignores metadata and malformed timestamps. */
object LrcParser {
    private val timestamp = Regex("""\[(\d+):(\d{2})(?:[.:](\d{1,3}))?]""")
    fun parse(input: String): List<LyricLine> = input.lineSequence().flatMap { line ->
        val matches = timestamp.findAll(line).toList()
        val text = line.substring(matches.lastOrNull()?.range?.last?.plus(1) ?: line.length).trim()
        matches.mapNotNull { match ->
            val minute = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val second = match.groupValues[2].toLong()
            val fraction = match.groupValues[3].padEnd(3, '0').toLongOrNull() ?: 0L
            if (second >= 60 || minute > (Long.MAX_VALUE - 59999) / 60000) null
            else LyricLine(minute * 60000 + second * 1000 + fraction, text)
        }.asSequence()
    }.sortedBy { it.timeMs }.toList()

    fun activeIndex(lines: List<LyricLine>, positionMs: Long): Int =
        lines.indexOfLast { it.timeMs <= positionMs }
}
