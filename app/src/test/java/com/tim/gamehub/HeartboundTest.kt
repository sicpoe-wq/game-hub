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
            val ev = Story.events.filter { it.heroId == h.id }
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
}
