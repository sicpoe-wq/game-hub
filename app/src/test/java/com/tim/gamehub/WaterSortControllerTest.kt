package com.tim.gamehub
import com.tim.gamehub.games.watersort.*
import org.junit.Assert.*
import org.junit.Test

class WaterSortControllerTest {
    private fun WaterSortController.solveLevel() {
        WaterSort.solve(tubes)!!.forEach { (f, t) -> tap(f); assertTrue(tap(t)) }
        assertTrue(won)
    }
    private fun WaterSortController.firstMove() = WaterSort.solve(tubes)!!.first()

    @Test fun winThenNextLevelAcceptsPours() {
        val store = MemoryProgress()
        val c = WaterSortController(store, 1)
        c.solveLevel()
        assertEquals(2, store.unlocked)
        assertFalse(c.tap(0))          // input ignored while won
        c.nextLevel()
        assertEquals(2, c.level); assertEquals(2, store.current)
        assertFalse(c.won); assertNull(c.selected); assertEquals(0, c.moves); assertFalse(c.canUndo); assertFalse(c.extraUsed)
        val (f, t) = c.firstMove()
        c.tap(f); assertTrue(c.tap(t)); assertEquals(1, c.moves)
    }

    @Test fun restartResetsEverything() {
        val c = WaterSortController(MemoryProgress(), 4)
        val start = c.tubes
        val (f, t) = c.firstMove()
        c.tap(f); c.tap(t); c.addTube(); c.tap(0)
        c.restart()
        assertEquals(start, c.tubes); assertEquals(0, c.moves); assertNull(c.selected); assertFalse(c.extraUsed); assertFalse(c.canUndo)
        c.tap(f); assertTrue(c.tap(t))
    }

    @Test fun restartAfterWinAndLevelPicker() {
        val c = WaterSortController(MemoryProgress(), 1)
        c.solveLevel(); c.restart(); assertFalse(c.won); c.solveLevel()
        c.load(1); assertFalse(c.won)
        c.load(2); val (f, t) = c.firstMove(); c.tap(f); assertTrue(c.tap(t))
    }

    @Test fun undoAndExtraTube() {
        val c = WaterSortController(MemoryProgress(), 3)
        val start = c.tubes
        c.addTube(); assertEquals(start.size + 1, c.tubes.size); c.addTube(); assertEquals(start.size + 1, c.tubes.size)
        c.undo(); assertEquals(start, c.tubes)
        val (f, t) = c.firstMove(); c.tap(f); c.tap(t); assertEquals(1, c.moves)
        c.undo(); assertEquals(start, c.tubes); assertEquals(0, c.moves)
    }
}
