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
    @Test fun heartbound_dialogue() = hb { jumpDialogue(Story.eventById("kael_e1")!!, "event", line = 3) }
    @Test fun heartbound_profile() = hb { openProfile("sera") }

    @Test fun heartbound_create() = p.snapshot {
        val c = HeartboundController(MemorySave(), instantText = true)
        c.draft = Player("Tim", "d", "he", "soldier", 1, 0, 2)
        c.jumpScreen(HbScreen.Create)
        HeartboundGame({}, controller = c, instantText = true)
    }
    @Test fun heartbound_slots() = p.snapshot {
        val store = MemorySave()
        HeartboundController(store, instantText = true).newGame(Player("Tim", "d", "he", "soldier"), 0)
        val c = HeartboundController(store, instantText = true)
        c.openSlots("load")
        HeartboundGame({}, controller = c, instantText = true)
    }
    @Test fun heartbound_journal() = hb { openJournal() }
    @Test fun heartbound_summary() = hb {
        jumpHome(state.copy(phase = "evening", report = DayReport(5, "Herb Run", true, 35, mapOf("kael" to 2, "yuna" to 2), listOf("kael"), null, listOf("mira"), listOf("first_job", "a_guest"))))
        jumpScreen(HbScreen.Summary)
    }
    @Test fun heartbound_evening() = hb { jumpHome(state.copy(phase = "evening", actionsLeft = 2)) }
    @Test fun heartbound_dialogue_bond() = hb {
        jumpHome(state.copy(phase = "evening", heroes = state.heroes + ("kael" to state.h("kael").copy(affection = 62))))
        jumpDialogue(Story.eventById("kael_e3")!!, "event", line = 1)
    }
    @Test fun heartbound_settings() = hb { openSettings() }
    @Test fun heartbound_festival() = hb { jumpHome(state.copy(day = 6, phase = "evening", actionsLeft = 2)) }
    @Test fun heartbound_companion() = hb {
        jumpHome(state.copy(day = 6, phase = "evening", actionsLeft = 2))
        openCompanionPick()
    }
    @Test fun heartbound_plot() = hb { jumpDialogue(Arc.plot.first(), "plot", line = 2); dismissCg(); toast = null }
    @Test fun heartbound_confess() = hb {
        jumpHome(state.copy(phase = "evening", heroes = state.heroes + ("kael" to state.h("kael").copy(affection = 85))))
        jumpDialogue(Story.eventById("kael_confess")!!, "event", line = 3)
    }
    @Test fun heartbound_care() = hb {
        jumpHome(state.copy(phase = "evening", actionsLeft = 1, heroes = state.heroes + ("kael" to state.h("kael").copy(injuredDays = 2))))
        jumpDialogue(Arc.careFor("kael"), "care", line = 1)
    }
    @Test fun heartbound_fade() = hb { jumpFade(Story.eventById("kael_confess")!!) }

    @Test fun heartbound_battle() = hb {
        jumpHome(state.copy(heroes = state.heroes + ("mira" to state.h("mira").copy(recruited = true, affection = 45, level = 3))
            + ("kael" to state.h("kael").copy(affection = 50)) + ("yuna" to state.h("yuna").copy(affection = 50))))
        jumpBattle("goblin_king", listOf("kael", "yuna", "mira"), auto = false)
    }
    @Test fun heartbound_travel() = hb {
        jumpHome(state.copy(phase = "evening", expedition = Expedition("ridge_hunt", listOf("kael", "yuna"), 1, 2)))
        jumpDialogue(Travel.preview(), "travel", line = 1)
    }

    @Test fun heartbound_hall() = hb { openHall() }
    @Test fun heartbound_town() = hb { openTown() }
    @Test fun heartbound_merchant() = hb {
        jumpHome(state.copy(day = 9, phase = "morning", gold = 200))
        openMerchant()
    }
    @Test fun heartbound_visit() = hb {
        jumpHome(state.copy(phase = "evening", actionsLeft = 2, heroes = state.heroes + ("kael" to state.h("kael").copy(affection = 62))))
        jumpDialogue(Life.visitScene("kael", 62), "visit", line = 1)
    }
    @Test fun heartbound_nim() = hb {
        jumpHome(state.copy(day = 10, phase = "morning", world = state.world.copy(chapter = 1),
            heroes = state.heroes + ("nim" to state.h("nim").copy(recruited = true, affection = 24, level = 2))))
        jumpDialogue(Life.romance.first(), "event", line = 1)
    }


    @Test fun heartbound_cg() = hb {
        jumpCg("kael_confess")
    }
    @Test fun heartbound_gallery() = hb {
        jumpHome(state.copy(gallery = setOf("kael_confess", "mira_confess", "plot_letter", "ending_hall")))
        openGallery()
    }

}
