package com.tim.gamehub
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.tim.gamehub.games.wordsearch.WordSearchGame
import com.tim.gamehub.games.heartbound.*
import com.tim.gamehub.games.watersort.*
import org.junit.Rule
import org.junit.Test
class ScreensTest {
    @get:Rule val p = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, theme = "android:Theme.Material.Light.NoActionBar")
    private fun shot(c: @androidx.compose.runtime.Composable () -> Unit) = p.snapshot {
        MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF5B4BDB), background = Color(0xFFF5F4FB))) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { c() } } }
    @Test fun hub() = shot { HubScreen {} }
    @Test fun wordsearch() = shot { WordSearchGame({}) }
    private fun ws(c: WaterSortController) = p.snapshot { WaterSortGame({}, controller = c) }
    @Test fun watersort_level1() = ws(WaterSortController(MemoryProgress(), 1))
    @Test fun watersort_level12_selected() = ws(WaterSortController(MemoryProgress(), 12).apply {
        WaterSort.solve(tubes)!!.take(9).forEach { (f, t) -> tap(f); tap(t) }
        tap(tubes.indexOfFirst { it.isNotEmpty() && !WaterSort.isSolvedTube(it) })
    })
    @Test fun watersort_level30() = ws(WaterSortController(MemoryProgress(), 30))
    @Test fun watersort_win() = ws(WaterSortController(MemoryProgress(), 5).apply { WaterSort.solve(tubes)!!.forEach { (f, t) -> tap(f); tap(t) } })


    private fun hb(build: HeartboundController.() -> Unit) = p.snapshot {
        val c = HeartboundController(MemorySave(), instantText = true).apply {
            jumpHome(HeartboundLogic.newGame().copy(gold = 160, day = 5, tavern = 1, rooms = 1, training = 1, phase = "morning",
                heroes = HeartboundLogic.newGame().heroes.mapValues { (_, h) ->
                    when (h.id) {
                        "kael" -> h.copy(affection = 24, seenEvents = setOf("kael_e1"), level = 3)
                        "yuna" -> h.copy(affection = 18, level = 3)
                        "mira" -> h.copy(recruited = true, affection = 12, level = 2)
                        "sera" -> h.copy(recruited = true, affection = 8)
                        else -> h
                    }
                }))
            build()
        }
        HeartboundGame({}, controller = c, instantText = true)
    }
    @Test fun heartbound_home() = hb { }
    @Test fun heartbound_board() = hb { openBoard() }
    @Test fun heartbound_dialogue() = hb { jumpDialogue(Story.eventById("kael_e1")!!, "event", line = 2) }
    @Test fun heartbound_profile() = hb { openProfile("sera") }
}
