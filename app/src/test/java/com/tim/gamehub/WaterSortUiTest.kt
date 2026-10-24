package com.tim.gamehub
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tim.gamehub.games.watersort.*
import com.tim.gamehub.games.wordsearch.Placed
import com.tim.gamehub.games.wordsearch.Puzzle
import com.tim.gamehub.games.wordsearch.WordSearchGame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** End-to-end UI regressions: taps must work after Next level / Restart / level picker / Play again. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class WaterSortUiTest {
    @get:Rule val rule = createComposeRule()

    private fun click(tag: String) = rule.onNodeWithTag(tag).performClick()
    private fun pour(f: Int, t: Int) { click("tube$f"); click("tube$t") }
    private fun solve(level: Int) = WaterSort.solve(WaterSort.generate(level))!!.forEach { (f, t) -> pour(f, t) }
    private fun first(level: Int) = WaterSort.solve(WaterSort.generate(level))!!.first()
    private fun moves(n: Int) = rule.onNodeWithTag("moves").assertTextEquals("Moves $n")

    @Test fun nextLevelAcceptsTaps() {
        val c = WaterSortController(MemoryProgress(), 1)
        rule.setContent { MaterialTheme { WaterSortGame({}, controller = c) } }
        solve(1)
        rule.onNodeWithTag("win_overlay").assertExists()
        click("btn_next")
        rule.onNodeWithTag("win_overlay").assertDoesNotExist()
        rule.onNodeWithText("Level 2 ▾").assertExists(); moves(0)
        val (f, t) = first(2); pour(f, t); moves(1)

    }

    @Test fun restartAndLevelPickerAcceptTaps() {
        val c = WaterSortController(MemoryProgress(), 1)
        rule.setContent { MaterialTheme { WaterSortGame({}, controller = c) } }
        val (f, t) = first(1)
        pour(f, t); moves(1)
        click("btn_restart"); moves(0)
        pour(f, t); moves(1)
        click("btn_restart"); solve(1); click("btn_next")
        click("btn_levels"); rule.onNodeWithTag("level_1").performClick()
        rule.onNodeWithText("Level 1 ▾").assertExists(); moves(0)
        pour(f, t); moves(1)
        click("btn_levels"); rule.onNodeWithTag("level_2").performClick()
        val (f2, t2) = first(2); pour(f2, t2); moves(1)
    }

    @Test fun wordSearchPlayAgainAcceptsSelection() {
        var puzzle: Puzzle? = null
        rule.setContent { MaterialTheme { WordSearchGame({}, onPuzzle = { puzzle = it }) } }
        fun drag(w: Placed) {
            val n = puzzle!!.size
            rule.onNodeWithTag("ws_grid").performTouchInput {
                val cell = width.toFloat() / n
                fun at(r: Int, c: Int) = Offset(c * cell + cell / 2, r * cell + cell / 2)
                val a = w.cells.first(); val b = w.cells.last()
                swipe(at(a.r, a.c), at(b.r, b.c), 300)
            }
        }
        val p1 = puzzle!!
        p1.words.forEachIndexed { i, w -> drag(w); rule.onNodeWithTag("ws_count").assertTextEquals("Words (${i + 1}/${p1.words.size})") }
        rule.onNodeWithText("Play again").assertExists().performClick()
        rule.waitForIdle()
        val p2 = puzzle!!
        assert(p2 !== p1)
        rule.onNodeWithTag("ws_count").assertTextEquals("Words (0/${p2.words.size})")
        drag(p2.words.first())
        rule.onNodeWithTag("ws_count").assertTextEquals("Words (1/${p2.words.size})")
    }
}
