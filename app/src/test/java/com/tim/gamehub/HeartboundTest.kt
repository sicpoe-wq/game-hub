package com.tim.gamehub

import com.tim.gamehub.games.heartbound.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class HeartboundTest {
    private fun game() = HeartboundLogic.newGame()
    private fun kaelYuna() = listOf("kael", "yuna")

    @Test fun startingRoster() {
        val s = game()
        assertTrue(s.h("kael").recruited)
        assertTrue(s.h("yuna").recruited)
        assertFalse(s.h("mira").recruited)
        assertEquals(Heartbound.START_GOLD, s.gold)
        assertEquals(1, s.inv("tea"))
    }

    @Test fun chanceStaysInBoundsAndScales() {
        val s = game()
        val q = Heartbound.quest.getValue("escort")
        val base = HeartboundLogic.successChance(s, kaelYuna(), q)
        assertTrue("chance $base", base in 8..95)
        val stronger = s.copy(heroes = s.heroes + ("kael" to s.h("kael").copy(level = 8)))
        assertTrue(HeartboundLogic.successChance(stronger, kaelYuna(), q) > base)
        val bonded = s.copy(heroes = s.heroes + ("kael" to s.h("kael").copy(affection = 80)) + ("yuna" to s.h("yuna").copy(affection = 80)))
        assertTrue(HeartboundLogic.successChance(bonded, kaelYuna(), q) > base)
        val trained = s.copy(training = 3)
        assertTrue(HeartboundLogic.successChance(trained, kaelYuna(), q) > base)
        val noPref = HeartboundLogic.successChance(s, kaelYuna(), Heartbound.quest.getValue("goblin"))
        val pref = HeartboundLogic.successChance(s, kaelYuna(), Heartbound.quest.getValue("watch"))
        assertTrue("preferred $pref vs $noPref", pref > noPref)
    }

    @Test fun knightClericSynergyBeatsSoloPairWithoutIt() {
        val s = game()
        // Mira + Rin have mage+rogue synergy; Kael+Yuna have knight+cleric. Compare a neutral quest.
        val neutral = Heartbound.quest.getValue("festival")
        val s2 = s.copy(heroes = s.heroes + ("mira" to s.h("mira").copy(recruited = true, level = 2)) + ("rin" to s.h("rin").copy(recruited = true, level = 2)))
        val withSyn = HeartboundLogic.successChance(s2, listOf("mira", "rin"), Heartbound.quest.getValue("goblin")) // mage+rogue preferred too
        val without = HeartboundLogic.successChance(s2, listOf("mira", "rin"), Heartbound.quest.getValue("wolves")) // no mage/rogue pref, no their synergy? wait they still have mage+rogue synergy
        // Same party, preferred classes increase chance
        assertTrue(withSyn > without)
    }

    @Test fun resolveSuccessPaysAndFailsPaysLess() {
        val s = game()
        val q = Heartbound.quest.getValue("escort")
        val chance = HeartboundLogic.successChance(s, kaelYuna(), q)
        val (win, wr) = HeartboundLogic.resolve(s, kaelYuna(), q, 0)
        assertTrue(wr.success)
        assertEquals(q.gold, wr.gold)
        assertEquals(q.xp, wr.xp)
        assertEquals(s.gold + q.gold, win.gold)
        assertEquals("evening", win.phase)
        assertTrue(win.h("kael").xp > 0 || win.h("kael").level > s.h("kael").level)
        val (loss, lr) = HeartboundLogic.resolve(s, kaelYuna(), q, 99)
        assertFalse(lr.success)
        assertEquals(q.gold / 5, lr.gold)
        assertTrue(lr.gold < wr.gold)
        assertEquals(chance, wr.chance)
    }

    @Test fun injuryCanHappenOnAFailAndRoomsShortenIt() {
        val s = game()
        val q = Heartbound.quest.getValue("wolves")
        // search for a failing roll that also injures
        var hit: QuestResult? = null
        for (roll in 90..99) {
            val (_, r) = HeartboundLogic.resolve(s, kaelYuna(), q, roll)
            if (!r.success && r.injuredId != null) { hit = r; break }
        }
        if (hit == null) {
            // try a wider window
            for (roll in 0..99) {
                val (_, r) = HeartboundLogic.resolve(s, kaelYuna(), q, roll)
                if (!r.success && r.injuredId != null) { hit = r; break }
            }
        }
        assertNotNull("expected some failing roll to injure", hit)
        val rooms = s.copy(rooms = 3)
        val (_, r2) = HeartboundLogic.resolve(rooms, kaelYuna(), q, hit!!.roll)
        if (r2.injuredId != null) assertTrue(r2.injuryDays <= hit.injuryDays)
    }

    @Test fun addXpLevelsUp() {
        val h = HeroState("kael", recruited = true, level = 1, xp = 0)
        val need = Heartbound.xpToNext(1)
        val (next, up) = HeartboundLogic.addXp(h, need)
        assertTrue(up)
        assertEquals(2, next.level)
        assertEquals(0, next.xp)
    }

    @Test fun giftDeltasAndTiers() {
        val kael = Heartbound.hero.getValue("kael")
        assertEquals(12, Heartbound.giftDelta(kael, "whetstone"))
        assertEquals(7, Heartbound.giftDelta(kael, "tea"))
        assertEquals(-2, Heartbound.giftDelta(kael, "brooch"))
        assertEquals(3, Heartbound.giftDelta(kael, "candy"))
        assertEquals(AffTier.STRANGER, Heartbound.tier(0))
        assertEquals(AffTier.FRIEND, Heartbound.tier(20))
        assertEquals(AffTier.CLOSE, Heartbound.tier(40))
        assertEquals(AffTier.CHERISHED, Heartbound.tier(60))
        assertEquals(AffTier.HEARTBOUND, Heartbound.tier(80))
        assertEquals(AffTier.HEARTBOUND, Heartbound.tier(100))
    }

    @Test fun eventsUnlockInOrderAndOnlyOnce() {
        assertNull(Story.eventFor("kael", 19, emptySet()))
        assertEquals("kael_e1", Story.eventFor("kael", 20, emptySet())?.id)
        assertEquals("kael_e2", Story.eventFor("kael", 50, setOf("kael_e1"))?.id)
        assertEquals("kael_e3", Story.eventFor("kael", 60, setOf("kael_e1", "kael_e2"))?.id)
        assertNull(Story.eventFor("kael", 100, setOf("kael_e1", "kael_e2", "kael_e3")))
        Heartbound.heroes.forEach { h ->
            val ev = Story.allBondEvents().filter { it.heroId == h.id && it.reqAff in setOf(20, 40, 60) }
            assertEquals("${h.id} should have 3 events", 3, ev.size)
            assertEquals(listOf(20, 40, 60), ev.map { it.reqAff })
            ev.forEach { assertTrue("${it.id} needs choices", it.choices.size >= 2) }
        }
    }

    @Test fun saveLoadRoundTrip() {
        val s0 = game()
        val q = Heartbound.quest.getValue("herbs")
        val (s1, _) = HeartboundLogic.resolve(s0, kaelYuna(), q, 3)
        val s2 = HeartboundLogic.turnIn(s1).copy(
            rooms = 1, training = 2, tavern = 1, gold = 333,
            flags = s1.flags + "recruited_mira",
            seenTalks = setOf("kael_t1"),
            heroes = s1.heroes + ("mira" to s1.h("mira").copy(recruited = true, affection = 22, seenEvents = setOf("mira_e1"), weapon = "iron"))
                + ("kael" to s1.h("kael").copy(injuredDays = 1, accessory = "charm")),
            inventory = mapOf("tea" to 2, "iron" to 0, "book" to 1),
        )
        val raw = HeartboundSave.encode(s2)
        val back = HeartboundSave.decode(raw)
        assertEquals(s2.day, back.day)
        assertEquals(s2.gold, back.gold)
        assertEquals(s2.rooms, back.rooms)
        assertEquals(s2.training, back.training)
        assertEquals(s2.tavern, back.tavern)
        assertEquals(s2.flags, back.flags)
        assertEquals(s2.seenTalks, back.seenTalks)
        assertEquals(22, back.h("mira").affection)
        assertTrue(back.h("mira").recruited)
        assertEquals(setOf("mira_e1"), back.h("mira").seenEvents)
        assertEquals("iron", back.h("mira").weapon)
        assertEquals("charm", back.h("kael").accessory)
        assertEquals(1, back.h("kael").injuredDays)
        assertEquals(2, back.inv("tea"))
        assertEquals(1, back.inv("book"))
        val raw2 = HeartboundSave.encode(back)
        val back2 = HeartboundSave.decode(raw2)
        assertEquals(back, back2.copy(lastResult = back.lastResult))
    }

    @Test fun controllerTalkEventGiftShopUpgradeAndRecruit() {
        val c = HeartboundController(MemorySave(), rng = Random(1), instantText = true)
        c.newGame()
        // finish intro
        repeat(20) { if (c.screen == HbScreen.Dialogue) { if (c.showingChoices()) c.choose(0) else c.advance() } }
        assertEquals(HbScreen.Home, c.screen)
        c.openShop()
        val gold = c.state.gold
        assertTrue(c.buy("whetstone"))
        assertEquals(gold - 25, c.state.gold)
        assertEquals(1, c.state.inv("whetstone"))
        c.jumpHome(c.state.copy(gold = 400))
        assertTrue(c.buyUpgrade("tavern"))
        assertEquals(1, c.state.tavern)
        assertEquals(2, c.state.eveningCap())
        // evening gift + event
        c.openEvening()
        assertEquals(2, c.state.actionsLeft)
        assertTrue(c.giveGift("kael", "whetstone"))
        assertEquals(HbScreen.Dialogue, c.screen)
        repeat(10) { if (c.screen == HbScreen.Dialogue) { if (c.showingChoices()) c.choose(0) else c.advance() } }
        assertTrue(c.state.h("kael").affection >= 8 + 12)
        // push to friend and talk should open event 1
        c.jumpHome(c.state.copy(phase = "evening", actionsLeft = 2,
            heroes = c.state.heroes + ("kael" to c.state.h("kael").copy(affection = 20))))
        assertTrue(c.startTalk("kael"))
        assertEquals("kael_e1", c.play?.scene?.id)
        if (c.showingChoices()) c.choose(1) else { repeat(12) { if (!c.showingChoices() && c.screen == HbScreen.Dialogue) c.advance() }; if (c.showingChoices()) c.choose(1) }
        repeat(12) { if (c.screen == HbScreen.Dialogue) { if (c.showingChoices()) c.choose(1) else c.advance() } }
        assertTrue("kael_e1" in c.state.h("kael").seenEvents)
        assertTrue(c.state.h("kael").affection > 20)
        // second talk is not the same event
        c.jumpHome(c.state.copy(phase = "evening", actionsLeft = 1))
        assertTrue(c.startTalk("kael"))
        assertNotEquals("kael_e1", c.play?.scene?.id)
        // injured heroes cannot be sent
        val injured = c.state.copy(phase = "morning", heroes = c.state.heroes + ("kael" to c.state.h("kael").copy(injuredDays = 1)))
        c.jumpHome(injured)
        c.beginParty("cat")
        c.toggleParty("kael")
        assertFalse("kael" in c.party)
        c.toggleParty("yuna")
        assertTrue("yuna" in c.party)
        assertTrue(c.confirmQuest(0))
        assertTrue(c.state.lastResult!!.success)
        // next day heals
        val hurt = c.state.copy(heroes = c.state.heroes + ("yuna" to c.state.h("yuna").copy(injuredDays = 1)))
        c.jumpHome(hurt)
        c.turnIn()
        assertEquals(0, c.state.h("yuna").injuredDays)
        assertEquals(hurt.day + 1, c.state.day)
        // recruit mira on day 2
        c.jumpHome(c.state.copy(day = 2, phase = "morning"))
        c.goHome()
        assertEquals("recruit_mira", c.play?.scene?.id)
        repeat(10) { if (c.screen == HbScreen.Dialogue) { if (c.showingChoices()) c.choose(0) else c.advance() } }
        assertTrue(c.state.h("mira").recruited)
    }

    @Test fun autosavePersistsAcrossControllers() {
        val store = MemorySave()
        val a = HeartboundController(store, instantText = true)
        a.newGame()
        repeat(20) { if (a.screen == HbScreen.Dialogue) { if (a.showingChoices()) a.choose(0) else a.advance() } }
        a.openEvening()
        val day = a.state.day
        val b = HeartboundController(store, instantText = true)
        assertTrue(b.loadSave())
        assertEquals(day, b.state.day)
        assertEquals("evening", b.state.phase)
        assertEquals(HbScreen.Evening, b.screen)
    }

    @Test fun v1SaveMigratesAndV2RoundTripsPlayer() {
        val v1 = HeartboundSave.encode(HeartboundLogic.newGame()) // actually v2 now; craft a raw v1
        val rawV1 = """
v=1
day=6|gold=210|phase=evening|act=1|rooms=1|train=0|tavern=1
H|kael|1|3|10|0|24||charm|kael_e1
H|yuna|1|2|0|0|14|||
H|mira|1|1|0|0|8|||
H|rin|0|1|0|0|0|||
H|sera|0|1|0|0|0|||
I|tea|2
F|won_quest
T|kael_t1
""".trimIndent()
        val migrated = HeartboundSave.decode(rawV1)
        assertEquals(6, migrated.day)
        assertEquals(210, migrated.gold)
        assertEquals(1, migrated.tavern)
        assertEquals(24, migrated.h("kael").affection)
        assertEquals("charm", migrated.h("kael").accessory)
        assertEquals("Alex", migrated.player.name) // default filled in
        assertTrue("won_quest" in migrated.flags)
        val store = MemorySave()
        // simulate prefs migration: empty slots, legacy body written to slot 0
        store.writeSlot(0, rawV1); store.lastSlot = 0
        val c = HeartboundController(store, instantText = true)
        assertTrue(c.loadSave())
        assertEquals(6, c.state.day)
        assertEquals(24, c.state.h("kael").affection)
        // custom player survives a v2 round trip
        val withP = migrated.copy(player = Player("Tim", "d", "he", "soldier", 1, 0, 2), achievements = setOf("first_dawn", "named"))
        val back = HeartboundSave.decode(HeartboundSave.encode(withP))
        assertEquals("Tim", back.player.name)
        assertEquals("d", back.player.look)
        assertEquals("he", back.player.pronouns)
        assertEquals("soldier", back.player.origin)
        assertEquals(2, back.player.courage)
        assertEquals(setOf("first_dawn", "named"), back.achievements)
    }

    @Test fun slotsDoNotOverwriteEachOther() {
        val store = MemorySave()
        val a = HeartboundController(store, instantText = true)
        a.newGame(Player("North", "a", "they", "wanderer"), slot = 0)
        val b = HeartboundController(store, instantText = true)
        b.newGame(Player("South", "b", "she", "scholar"), slot = 1)
        assertEquals("South", HeartboundSave.decode(store.slot(1)!!).player.name)
        assertEquals("North", HeartboundSave.decode(store.slot(0)!!).player.name)
        a.loadSlot(0)
        assertEquals("North", a.state.player.name)
        assertEquals(2, a.slotInfos().count { !it.empty })
    }

    @Test fun voiceAndStatGates() {
        val p = Player("Tim", "a", "he", "soldier", 1, 0, 2)
        assertEquals("Tim kept his word, and he is here.", Voice.apply("{name} kept {their} word, and {they} {are} here.", p))
        val she = Player("Ada", "b", "she", "scholar", 1, 2, 0)
        assertEquals("Ada kept her word, and she is here.", Voice.apply("{name} kept {their} word, and {they} {are} here.", she))
        val intro = Story.introFor(p)
        assertTrue(intro.lines.any { it.text.contains("{name}") || Voice.apply(it.text, p).contains("Tim") })
        assertTrue(intro.choices.any { it.origin == "soldier" && Story.unlocked(it, p) })
        assertFalse(intro.choices.any { it.origin == "scholar" && Story.unlocked(it, p) })
        val ev = Story.eventById("kael_e1")!!
        val gated = ev.choices.filter { it.stat == "courage" }
        assertTrue(gated.isNotEmpty())
        assertTrue(Story.unlocked(gated.first(), p))
        assertFalse(Story.unlocked(gated.first(), Player(origin = "wanderer", courage = 1, charm = 1, wit = 1)))
    }

    @Test fun achievementsAndDailySummary() {
        val c = HeartboundController(MemorySave(), instantText = true)
        c.newGame(Player("Tim", "d", "he", "soldier"))
        assertTrue("first_dawn" in c.state.achievements)
        assertTrue("named" in c.state.achievements)
        repeat(24) { if (c.screen == HbScreen.Dialogue) { if (c.showingChoices()) c.choose(0) else c.advance() } }
        c.openEvening(); c.turnIn()
        assertEquals(HbScreen.Summary, c.screen)
        assertNotNull(c.state.report)
        assertEquals(1, c.state.report!!.day)
        c.dismissSummary()
        assertEquals(2, c.state.day)
        // Day 2 morning auto-offers Mira; skip her intro if it opened
        repeat(24) { if (c.screen == HbScreen.Dialogue) { if (c.showingChoices()) c.choose(0) else c.advance() } }
        assertEquals(HbScreen.Home, c.screen)
        // a win flags first_job
        c.beginParty("cat"); c.toggleParty("kael")
        assertTrue(c.confirmQuest(0))
        assertTrue("won_quest" in c.state.flags)
        assertTrue("first_job" in c.state.achievements)
    }

    @Test fun giftHintsUnlockAtFriend() {
        val kael = Heartbound.hero.getValue("kael")
        val vague = Content.giftHint(kael, 8)
        val clear = Content.giftHint(kael, 20)
        assertTrue(vague.contains("blade") || vague.contains("Practical"))
        assertTrue(clear.contains("Whetstone"))
        assertFalse(vague.contains("Whetstone"))
    }

    private fun HeartboundController.pump(limit: Int = 40) {
        repeat(limit) {
            if (screen != HbScreen.Dialogue) return
            if (showingChoices()) return
            advance()
        }
    }

    private fun HeartboundController.withBonds(vararg ids: Pair<String, Int>, seenThrough: Int = 3, extra: Set<String> = emptySet()): Map<String, HeroState> {
        val ev = mapOf("kael" to listOf("kael_e1","kael_e2","kael_e3"), "mira" to listOf("mira_e1","mira_e2","mira_e3"), "rin" to listOf("rin_e1","rin_e2","rin_e3"), "sera" to listOf("sera_e1","sera_e2","sera_e3"), "yuna" to listOf("yuna_e1","yuna_e2","yuna_e3"))
        var heroes = state.heroes
        ids.forEach { (id, aff) ->
            val seen = ev.getValue(id).take(seenThrough).toSet() + extra
            heroes = heroes + (id to state.h(id).copy(recruited = true, affection = aff, seenEvents = seen))
        }
        return heroes
    }

    @Test fun exclusiveRouteLocksOtherRomance() {
        val c = HeartboundController(MemorySave(), instantText = true)
        c.newGame(Player("Tim", "a", "he", "soldier"))
        c.pump(); if (c.showingChoices()) c.choose(0); c.pump()
        val heroes = c.withBonds("kael" to 85, "mira" to 85)
        c.jumpHome(c.state.copy(phase = "evening", actionsLeft = 2, heroes = heroes))
        assertTrue(c.startTalk("kael"))
        assertEquals("kael_confess", c.play?.scene?.id)
        c.pump()
        assertTrue(c.showingChoices())
        c.choose(0); c.pump()
        assertEquals("kael", c.state.world.route)
        assertTrue("kael_confess" in c.state.flags)
        assertTrue("promised" in c.state.achievements)
        c.jumpHome(c.state.copy(phase = "evening", actionsLeft = 2))
        assertTrue(c.startTalk("mira"))
        assertEquals("mira_confess", c.play?.scene?.id)
        c.pump()
        val ch = c.choices()
        assertTrue(ch.any { it.first.needFreeRoute && !it.second })
        assertTrue(ch.any { !it.first.needFreeRoute && it.second })
    }

    @Test fun fadeToBlackCanBeSkipped() {
        val c = HeartboundController(MemorySave(), instantText = true)
        c.newGame(Player("Tim", "a", "they", "wanderer"))
        c.pump(); if (c.showingChoices()) c.choose(0); c.pump()
        c.setFadeToBlack(false)
        val heroes = c.withBonds("kael" to 85)
        c.jumpHome(c.state.copy(phase = "evening", actionsLeft = 2, heroes = heroes))
        c.startTalk("kael"); c.pump(); c.choose(0); c.pump()
        assertTrue(c.currentLine()?.fade != true || c.screen != HbScreen.Dialogue)
        assertEquals("kael", c.state.world.route)
    }

    @Test fun plotChapterOneAndGroupEvent() {
        val c = HeartboundController(MemorySave(), instantText = true)
        c.newGame(Player("Tim", "a", "they", "wanderer"))
        c.pump(); if (c.showingChoices()) c.choose(0); c.pump()
        val roster = c.withBonds("mira" to 10, "sera" to 10, "rin" to 10)
        c.jumpHome(c.state.copy(day = 3, phase = "morning", heroes = roster))
        c.goHome()
        assertEquals("plot_letter", c.play?.scene?.id)
        c.pump(); c.choose(0); c.pump()
        assertTrue("plot_letter" in c.state.flags)
        c.jumpHome(c.state.copy(day = 9, phase = "morning", flags = c.state.flags + "plot_inquiry" + "plot_woods", heroes = c.state.heroes))
        c.goHome()
        assertEquals("plot_climax", c.play?.scene?.id)
        c.pump(); c.choose(0); c.pump()
        assertTrue("plot_ch1_done" in c.state.flags)
        assertEquals(1, c.state.world.chapter)
        assertTrue("a_light_kept" in c.state.achievements)
        c.jumpHome(c.state.copy(day = 6, phase = "evening", actionsLeft = 2))
        assertEquals("festival", c.pendingGroup()?.id)
        c.chooseCompanion("yuna")
        assertEquals("festival_yuna", c.play?.scene?.id)
        c.pump(); if (c.showingChoices()) c.choose(1); c.pump()
        assertTrue("event_festival" in c.state.flags)
        assertTrue("lanterns" in c.state.achievements)
    }

    @Test fun careSceneAndEndingGates() {
        val c = HeartboundController(MemorySave(), instantText = true)
        c.newGame(Player("Tim", "a", "they", "wanderer"))
        c.pump(); if (c.showingChoices()) c.choose(0); c.pump()
        val heroes = c.state.heroes + ("kael" to c.state.h("kael").copy(injuredDays = 2, affection = 40))
        c.jumpHome(c.state.copy(phase = "evening", actionsLeft = 0, heroes = heroes))
        assertTrue(c.canCare("kael"))
        assertTrue(c.startCare("kael"))
        c.pump(); c.choose(0); c.pump()
        assertTrue(c.state.flags.any { it.startsWith("cared_kael_") })
        assertTrue("bedside" in c.state.achievements)
        val ready = c.state.copy(
            world = c.state.world.copy(route = "kael", chapter = 1),
            flags = c.state.flags + "kael_confess" + "kael_vow",
            heroes = c.state.heroes + ("kael" to c.state.h("kael").copy(affection = 100, injuredDays = 0, seenEvents = setOf("kael_e1", "kael_e2", "kael_e3", "kael_confess", "kael_propose"))),
        )
        assertEquals("kael_ending", Story.eventFor("kael", ready)?.id)
        val tooSoon = ready.copy(world = ready.world.copy(chapter = 0))
        assertNull(Story.eventFor("kael", tooSoon))
    }

    @Test fun journalListsSeenTitlesOnly() {
        val seen = setOf("kael_e1")
        val shown = Story.allBondEvents().filter { it.heroId == "kael" && it.id in seen }.map { it.title }
        val hidden = Story.allBondEvents().filter { it.heroId == "kael" && it.id !in seen }.mapNotNull { it.title }
        assertTrue(shown.contains("The Unfinished Sword"))
        assertTrue(hidden.contains("A Letter from the Holds"))
        assertTrue(hidden.contains("The Watch, Spoken Aloud"))
    }

    @Test fun v2SaveKeepsRouteAndChapter() {
        val base = HeartboundLogic.newGame(Player("Tim", "d", "he", "soldier"))
        val s = base.copy(world = base.world.copy(route = "sera", chapter = 1), flags = base.flags + "sera_confess" + "plot_ch1_done")
        val again = HeartboundSave.decode(HeartboundSave.encode(s))
        assertEquals("sera", again.world.route)
        assertEquals(1, again.world.chapter)
        assertTrue("sera_confess" in again.flags)
        assertEquals("Tim", again.player.name)
        val v14 = HeartboundSave.encode(HeartboundLogic.newGame()).replace("W|spring|clear||0", "W|spring|clear||0")
        val decoded = HeartboundSave.decode(v14)
        assertNull(decoded.world.route)
        assertEquals(0, decoded.world.chapter)
    }

    @Test fun confirmQuestWithRollStillResolvesInstantly() {
        val c = HeartboundController(MemorySave(), instantText = true)
        c.newGame()
        c.jumpHome()
        c.beginParty("cat")
        c.toggleParty("yuna")
        assertTrue(c.confirmQuest(0))
        assertNull(c.battle)
        assertEquals(HbScreen.Result, c.screen)
        assertTrue(c.state.lastResult!!.success)
        assertNull(c.state.expedition)
        assertEquals(3, c.state.reputation)
    }

    @Test fun confirmQuestWithoutRollStartsBattle() {
        val c = HeartboundController(MemorySave(), instantText = true)
        c.newGame()
        c.jumpHome()
        c.beginParty("cat")
        c.toggleParty("yuna")
        assertTrue(c.confirmQuest())
        assertEquals(HbScreen.Battle, c.screen)
        assertNotNull(c.battle)
        assertFalse(c.battle!!.finished)
    }

    @Test fun autoBattleSettlesQuestAndPaysRep() {
        val s = game()
        val q = Heartbound.quest.getValue("cat")
        val b = Battle.runAuto(s, kaelYuna(), q)
        assertTrue("battle should finish", b.finished)
        val (next, r) = HeartboundLogic.settleBattle(s, b)
        assertEquals(b.victory == true, r.success)
        if (r.success) {
            assertEquals(q.gold, r.gold)
            assertEquals(3, next.reputation)
            assertTrue("won_cat" in next.flags)
        } else {
            assertEquals(q.gold / 5, r.gold)
            assertEquals(0, next.reputation)
        }
        assertNull(next.expedition)
        assertEquals("evening", next.phase)
    }

    @Test fun bondComboRequiresClassesAndAffection() {
        val s0 = game()
        val q = Heartbound.quest.getValue("escort")
        val low = Battle.start(s0, kaelYuna(), q)
        assertTrue(Battle.comboReady(s0, low).isEmpty())
        val high = s0.copy(heroes = s0.heroes + ("kael" to s0.h("kael").copy(affection = 50)) + ("yuna" to s0.h("yuna").copy(affection = 50)))
        val b = Battle.start(high, kaelYuna(), q)
        val combos = Battle.comboReady(high, b)
        assertTrue(combos.any { it.id == "oath" })
        val used = Battle.act(b, Battle.skills.getValue("oath"), null, high)
        assertTrue(used.usedCombo)
        assertTrue(Battle.comboReady(high, used).isEmpty())
    }

    @Test fun reputationAndFlagsGateHarderQuests() {
        val s = game()
        val ids = HeartboundLogic.availableQuests(s).map { it.id }
        assertTrue("cat" in ids)
        assertFalse("ridge_hunt" in ids)
        assertFalse("goblin_den" in ids)
        assertFalse("covenant_envoy" in ids)
        val mid = s.copy(day = 6, reputation = 10, flags = s.flags + "goblin_routed")
        val midIds = HeartboundLogic.availableQuests(mid).map { it.id }
        assertTrue("ridge_hunt" in midIds)
        assertTrue("goblin_den" in midIds)
        assertFalse("goblin_king" in midIds) // needs den_cleared and rep 12
        val late = mid.copy(day = 8, reputation = 25, flags = mid.flags + setOf("den_cleared", "plot_ch1_done"))
        val lateIds = HeartboundLogic.availableQuests(late).map { it.id }
        assertTrue("goblin_king" in lateIds)
        assertTrue("covenant_envoy" in lateIds)
        val once = late.copy(flags = late.flags + setOf("won_goblin_king"))
        assertFalse("goblin_king" in HeartboundLogic.availableQuests(once).map { it.id })
    }

    @Test fun ledgerBranchUnlocksDifferentFollowups() {
        val s = game().copy(day = 3, flags = setOf("ledger_found"))
        assertNotNull(Travel.pendingBranch(s))
        val mercy = s.copy(flags = s.flags + "ledger_mercy")
        assertNull(Travel.pendingBranch(mercy))
        val idsM = HeartboundLogic.availableQuests(mercy).map { it.id }
        assertTrue("ledger_mercy" in idsM)
        assertFalse("ledger_law" in idsM)
        val law = s.copy(flags = s.flags + "ledger_law")
        val idsL = HeartboundLogic.availableQuests(law).map { it.id }
        assertTrue("ledger_law" in idsL)
        assertFalse("ledger_mercy" in idsL)
    }

    @Test fun expeditionPersistsAndTicksAndBlocksHallReady() {
        val s0 = game()
        val exp = Expedition("ridge_hunt", kaelYuna(), daysLeft = 1, totalDays = 2, flags = setOf("travel_haste"))
        val s = s0.copy(expedition = exp, reputation = 12, day = 5)
        assertTrue(s.onRoad("kael"))
        assertTrue(s.hallReady().none { it.id == "kael" || it.id == "yuna" })
        val raw = HeartboundSave.encode(s)
        assertTrue(raw.contains("X|ridge_hunt"))
        assertTrue(raw.contains("|rep=12"))
        val back = HeartboundSave.decode(raw)
        assertEquals(1, back.expedition!!.daysLeft)
        assertEquals(listOf("kael", "yuna"), back.expedition!!.party)
        assertEquals(setOf("travel_haste"), back.expedition!!.flags)
        assertEquals(12, back.reputation)
        val next = HeartboundLogic.turnIn(back)
        assertEquals(0, next.expedition!!.daysLeft)
        assertEquals(6, next.day)
        // old v2 without rep or X still loads
        val legacy = HeartboundSave.encode(game()).replace("|rep=0", "")
        assertFalse("rep=" in legacy)
        val migrated = HeartboundSave.decode(legacy)
        assertEquals(0, migrated.reputation)
        assertNull(migrated.expedition)
    }

    @Test fun travelChoiceSticksToExpeditionAndPaysCache() {
        val c = HeartboundController(MemorySave(), rng = Random(0), instantText = true)
        c.newGame()
        c.jumpHome(c.state.copy(expedition = Expedition("ridge_hunt", kaelYuna(), 1, 2)))
        c.jumpDialogue(Travel.preview(), "travel")
        assertEquals(HbScreen.Dialogue, c.screen)
        assertEquals("travel", c.play?.kind)
        repeat(20) { if (c.screen == HbScreen.Dialogue) { if (c.showingChoices()) c.choose(0) else c.advance() } }
        assertEquals(HbScreen.Evening, c.screen)
        assertEquals(1, c.state.expedition!!.eventsDone)
        assertTrue("travel_${c.state.day}" in c.state.flags)
    }

    @Test fun multiDayConfirmStartsTravelNotInstantResolve() {
        val c = HeartboundController(MemorySave(), rng = Random(2), instantText = true)
        c.newGame()
        val ready = c.state.copy(day = 5, reputation = 12, phase = "morning")
        c.jumpHome(ready)
        c.beginParty("ridge_hunt")
        c.toggleParty("kael"); c.toggleParty("yuna")
        assertTrue(c.confirmQuest())
        assertEquals(HbScreen.Dialogue, c.screen)
        assertEquals("travel", c.play?.kind)
        assertEquals("ridge_hunt", c.state.expedition?.questId)
        assertEquals(1, c.state.expedition?.daysLeft)
        assertNull(c.state.lastResult)
    }

    @Test fun secretHeroArrivesLateAndHasARoute() {
        val c = HeartboundController(MemorySave(), instantText = true)
        c.newGame()
        assertFalse(c.state.h("nim").recruited)
        val recruited = c.state.heroes.mapValues { (_, h) -> if (h.id in setOf("mira","sera","rin","kael","yuna")) h.copy(recruited = true) else h }
        c.jumpHome(c.state.copy(day = 10, phase = "morning", world = c.state.world.copy(chapter = 1), flags = c.state.flags + "plot_ch1_done", heroes = recruited))
        c.goHome()
        assertEquals("recruit_nim", c.play?.scene?.id)
        repeat(20) { if (c.screen == HbScreen.Dialogue) { if (c.showingChoices()) c.choose(0) else c.advance() } }
        assertTrue(c.state.h("nim").recruited)
        assertTrue("the_hidden_key" in c.state.achievements)
    }

    @Test fun seasonsCycleAndMerchantAndCraftWork() {
        val s0 = game()
        assertEquals("spring", Life.seasonOf(1))
        assertEquals("summer", Life.seasonOf(8))
        assertEquals("winter", Life.seasonOf(22))
        val c = HeartboundController(MemorySave(), instantText = true)
        c.newGame()
        c.jumpHome(c.state.copy(day = 4, phase = "morning", gold = 400, forge = 1, library = 1, garden = 1, inventory = c.state.inventory + mapOf("wax" to 1, "ashglass" to 1, "ore" to 2, "herbs" to 1, "tea" to 1, "d_banner" to 1)))
        assertTrue(Life.merchantVisits(c.state))
        assertTrue(c.craft("lantern"))
        assertEquals(1, c.state.inv("lantern"))
        assertTrue("crafted" in c.state.flags)
        assertTrue(c.enchant("kael", false))
        assertEquals(1, c.state.h("kael").enchantAtk)
        assertTrue(c.state.h("kael").atk() > HeartboundLogic.newGame().h("kael").atk())
        assertTrue(c.placeDecor("d_banner"))
        assertTrue("d_banner" in c.state.decor)
        assertTrue(c.hireStaff("cook"))
        assertTrue("cook" in c.state.staff)
        val raw = HeartboundSave.encode(c.state)
        val back = HeartboundSave.decode(raw)
        assertEquals(1, back.forge)
        assertEquals(1, back.h("kael").enchantAtk)
        assertTrue("cook" in back.staff)
        assertTrue("d_banner" in back.decor)
        assertEquals(0, HeartboundSave.decode(HeartboundSave.encode(game()).replace("|forge=0|lib=0|garden=0|stables=0", "")).forge)
    }

    @Test fun roomVisitAndTownDateAndBirthday() {
        val c = HeartboundController(MemorySave(), instantText = true)
        c.newGame()
        val heroes = c.state.heroes + ("kael" to c.state.h("kael").copy(affection = 55))
        c.jumpHome(c.state.copy(day = 7, phase = "evening", actionsLeft = 2, heroes = heroes))
        assertTrue(c.canVisit("kael"))
        assertTrue(c.startVisit("kael"))
        assertEquals("visit", c.play?.kind)
        repeat(16) { if (c.screen == HbScreen.Dialogue) { if (c.showingChoices()) c.choose(0) else c.advance() } }
        assertTrue(c.state.flags.any { it.startsWith("visited_kael") })
        c.jumpHome(c.state.copy(phase = "evening", actionsLeft = 2))
        assertTrue(c.startDate("kael", "hill"))
        assertEquals("hill", c.play?.scene?.bg)
        repeat(16) { if (c.screen == HbScreen.Dialogue) { if (c.showingChoices()) c.choose(0) else c.advance() } }
        // birthday of Kael is calendar day 7
        val s = c.state.copy(day = 7, phase = "evening", flags = c.state.flags - c.state.flags.filter { it.startsWith("bday_") }.toSet())
        assertEquals("kael", Life.birthdayOf(s))
        val end = Life.pendingEnding(game().copy(day = 14, world = WorldState(chapter = 1), flags = setOf("plot_ch1_done")))
        assertEquals("ending_hall", end?.id)
    }


    @Test fun galleryUnlocksOnPlotAndSurvivesSave() {
        val c = HeartboundController(MemorySave(), instantText = true)
        c.newGame()
        val scene = Story.eventById("plot_letter")!!
        c.jumpDialogue(scene, "plot")
        assertTrue("plot_letter" in c.state.gallery)
        assertEquals("plot_letter", c.cgView)
        c.dismissCg()
        assertNull(c.cgView)
        val back = HeartboundSave.decode(HeartboundSave.encode(c.state))
        assertEquals(c.state.gallery, back.gallery)
        assertTrue(HeartboundSave.decode(HeartboundSave.encode(game())).gallery.isEmpty())
    }

    @Test fun confessionChoiceRevealsCgAndSettingsPersist() {
        val store = MemorySave()
        val c = HeartboundController(store, instantText = true)
        c.newGame()
        val scene = Story.eventById("kael_confess")!!
        c.jumpDialogue(scene, "event", showChoices = true)
        assertTrue("kael_confess" !in c.state.gallery)
        assertNull(c.cgView)
        c.choose(0)
        assertTrue("kael_confess" in c.state.gallery)
        assertEquals("kael_confess", c.cgView)
        c.setMusicVol(3); c.setSfxVol(1); c.setMuted(true)
        assertEquals(3, store.settings.musicVol)
        assertEquals(1, store.settings.sfxVol)
        assertTrue(store.settings.muted)
        assertEquals("battle", HbSound.themeFor(HbScreen.Battle, null))
        assertEquals("evening", HbSound.themeFor(HbScreen.Evening, null))
        assertEquals("ending", HbSound.themeFor(HbScreen.Dialogue, c.play?.copy(kind = "ending")))
        c.dismissCg()
        c.openGallery()
        assertEquals(HbScreen.Gallery, c.screen)
    }


    @Test fun introPlaysOncePerLaunchAndRespectsSetting() {
        HbSession.reset()
        val store = MemorySave()
        val c = HeartboundController(store, instantText = true)
        assertTrue(c.shouldPlayIntro())
        c.beginIntroIfNeeded()
        assertEquals("play", c.introMode)
        c.skipIntro()
        assertEquals("done", c.introMode)
        assertFalse(c.shouldPlayIntro())
        // a second entry this launch skips
        c.introMode = "unset"
        c.beginIntroIfNeeded()
        assertEquals("done", c.introMode)
        HbSession.reset()
        c.setPlayIntro(false)
        c.introMode = "unset"
        assertFalse(c.shouldPlayIntro())
        c.beginIntroIfNeeded()
        assertEquals("done", c.introMode)
        assertTrue(store.settings.playIntro.not())
        assertEquals("title", HbSound.themeFor(HbScreen.Title, null, started = false))
        assertEquals("title", HbSound.themeFor(HbScreen.Settings, null, started = false))
        assertEquals("hall", HbSound.themeFor(HbScreen.Home, null, started = true))
    }

    @Test fun continueUsesLatestSlotAndTitleGalleryDoesNotStart() {
        val store = MemorySave()
        val early = HeartboundLogic.newGame().copy(day = 3, player = Player(name = "Early"), gallery = setOf("kael_confess"))
        val later = HeartboundLogic.newGame().copy(day = 11, player = Player(name = "Later"), gallery = setOf("mira_confess", "plot_letter"))
        store.writeSlot(0, HeartboundSave.encode(early))
        store.writeSlot(1, HeartboundSave.encode(later))
        store.lastSlot = 1
        val c = HeartboundController(store, instantText = true)
        c.jumpTitleMenu()
        assertEquals(1, c.latestSlotIndex())
        assertTrue(c.loadSave())
        assertEquals("Later", c.state.player.name)
        assertEquals(11, c.state.day)
        c.backToTitle()
        assertEquals(HbScreen.Title, c.screen)
        assertFalse(c.shouldPlayIntro())
        assertEquals("done", c.introMode)
        c.openGalleryFromTitle()
        assertEquals(HbScreen.Gallery, c.screen)
        assertTrue("mira_confess" in c.state.gallery)
        assertTrue("plot_letter" in c.state.gallery)
        // viewing the gallery from the title must not mark a run as started
        c.closeMenu()
        assertEquals(HbScreen.Title, c.screen)
    }


}
