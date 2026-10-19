package com.tim.gamehub
import com.tim.gamehub.games.wordsearch.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random
class GeneratorTest {
    @Test fun allPuzzlesValid() {
        for (d in Difficulty.entries) for (t in THEMES.keys) repeat(20) { s ->
            val p = Generator.generate(t, d, Random(s))
            assertEquals(d.size, p.size); assertEquals(d.words, p.words.size)
            p.words.forEach { w -> assertEquals(w.word, w.cells.map { p.grid[it.r][it.c] }.joinToString(""))
                assertEquals(w, Generator.match(Generator.line(w.cells.last(), w.cells.first())!!, p.words)) }
        }
    }
    @Test fun lineRejectsBent() { assertNull(Generator.line(Cell(0,0), Cell(1,2))) }
}
