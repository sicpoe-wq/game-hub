package com.tim.gamehub
import com.tim.gamehub.games.watersort.WaterSort
import org.junit.Assert.*
import org.junit.Test
class WaterSortTest {
    @Test fun pourIntoEmptyMovesWholeTopRun() {
        val s = listOf(listOf(0, 1, 1), emptyList())
        assertEquals(listOf(listOf(0), listOf(1, 1)), WaterSort.pour(s, 0, 1))
    }
    @Test fun pourOnlyOntoMatchingTop() {
        val s = listOf(listOf(0, 1), listOf(2))
        assertNull(WaterSort.pour(s, 0, 1))
        assertEquals(listOf(listOf(0), listOf(2, 1, 1)), WaterSort.pour(listOf(listOf(0, 1), listOf(2, 1)), 0, 1))
    }
    @Test fun pourOnlyAsManyAsFit() {
        val s = listOf(listOf(1, 1, 1), listOf(0, 0, 1))
        assertEquals(listOf(listOf(1, 1), listOf(0, 0, 1, 1)), WaterSort.pour(s, 0, 1))
    }
    @Test fun cannotPourIntoFullOrSelfOrFromEmpty() {
        val s = listOf(listOf(1), listOf(1, 1, 1, 1), emptyList())
        assertNull(WaterSort.pour(s, 0, 1)); assertNull(WaterSort.pour(s, 0, 0)); assertNull(WaterSort.pour(s, 2, 0))
    }
    @Test fun winDetection() {
        assertTrue(WaterSort.isWon(listOf(listOf(1, 1, 1, 1), emptyList(), listOf(0, 0, 0, 0))))
        assertFalse(WaterSort.isWon(listOf(listOf(1, 1, 1), listOf(1))))
        assertFalse(WaterSort.isWon(listOf(listOf(1, 1, 0, 1), emptyList())))
    }
    @Test fun generatedLevelsAreSolvable() {
        for (lvl in 1..200) {
            val s = WaterSort.generate(lvl)
            val spec = WaterSort.spec(lvl)
            assertEquals(spec.tubes, s.size)
            assertEquals(2, s.count { it.isEmpty() })
            assertFalse(WaterSort.isWon(s))
            val sol = WaterSort.solve(s)
            assertNotNull("level $lvl", sol)
            var st = s
            sol!!.forEach { (f, t) -> st = WaterSort.pour(st, f, t) ?: error("illegal move in solution, level $lvl") }
            assertTrue(WaterSort.isWon(st))
        }
        assertEquals(14, WaterSort.spec(100).tubes)
    }
    @Test fun generationIsDeterministic() { assertEquals(WaterSort.generate(7), WaterSort.generate(7)) }
}
