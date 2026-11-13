package com.tim.gamehub.games.heartbound

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

sealed class HbScreen {
    data object Title : HbScreen()
    data object Home : HbScreen()
    data object Board : HbScreen()
    data object Party : HbScreen()
    data object Result : HbScreen()
    data object Evening : HbScreen()
    data object Shop : HbScreen()
    data object Upgrades : HbScreen()
    data class Profile(val heroId: String) : HbScreen()
    data class Gift(val heroId: String) : HbScreen()
    data object Dialogue : HbScreen()
}

data class ScenePlay(
    val scene: Scene,
    val kind: String,
    val heroId: String?,
    val lineIndex: Int = 0,
    val followup: List<Line> = emptyList(),
    val followIndex: Int = 0,
    val awaitingChoice: Boolean = false,
    val pendingDelta: Int = 0,
    val pendingFlag: String? = null,
    val pendingRecruit: Boolean = false,
)

class HeartboundController(
    val store: SaveStore,
    var rng: Random = Random.Default,
    val instantText: Boolean = false,
) {
    var state by mutableStateOf(HeartboundLogic.newGame())
        private set
    var screen by mutableStateOf<HbScreen>(HbScreen.Title)
        private set
    var play by mutableStateOf<ScenePlay?>(null)
        private set
    var questId by mutableStateOf<String?>(null)
        private set
    var party by mutableStateOf(listOf<String>())
        private set
    var toast by mutableStateOf<String?>(null)
    var started by mutableStateOf(false)
        private set

    val hasSave get() = store.data != null

    private fun commit(next: GameState) {
        state = next
        store.data = HeartboundSave.encode(next)
    }

    fun loadSave(): Boolean {
        val raw = store.data ?: return false
        return try {
            state = HeartboundSave.decode(raw)
            started = true
            screen = when (state.phase) {
                "evening" -> if (state.lastResult != null) HbScreen.Result else HbScreen.Evening
                else -> HbScreen.Home
            }
            play = null
            true
        } catch (_: Exception) { false }
    }

    fun newGame() {
        commit(HeartboundLogic.newGame())
        started = true
        openScene(Story.intro, "intro", null)
    }

    fun backToTitle() {
        screen = HbScreen.Title
        play = null
    }

    fun goHome() {
        play = null
        screen = if (state.phase == "evening") HbScreen.Evening else HbScreen.Home
        maybeOfferRecruit()
    }

    private fun maybeOfferRecruit() {
        if (screen != HbScreen.Home || state.phase != "morning") return
        val offer = pendingRecruit() ?: return
        openScene(offer, "recruit", offer.heroId)
    }

    fun pendingRecruit(): Scene? {
        if (state.phase != "morning") return null
        val mira = state.h("mira")
        val sera = state.h("sera")
        val rin = state.h("rin")
        return when {
            !mira.recruited && state.day >= 2 && "offered_mira_${state.day}" !in state.flags -> Story.recruitMira
            !sera.recruited && (state.day >= 3 && (state.has("did_forest") || state.day >= 4)) && "offered_sera_${state.day}" !in state.flags -> Story.recruitSera
            !rin.recruited && state.day >= 4 && "offered_rin_${state.day}" !in state.flags -> Story.recruitRin
            else -> null
        }
    }

    fun openBoard() { screen = HbScreen.Board }
    fun openShop() { screen = HbScreen.Shop }
    fun openUpgrades() { screen = HbScreen.Upgrades }
    fun openProfile(id: String) { screen = HbScreen.Profile(id) }
    fun openGift(id: String) { screen = HbScreen.Gift(id) }
    fun openEvening() {
        val next = if (state.phase == "morning") state.copy(phase = "evening", actionsLeft = state.eveningCap()) else state
        commit(next)
        screen = HbScreen.Evening
    }

    fun availableQuests(): List<QuestDef> = Heartbound.quests.filter { it.minDay <= state.day }

    fun beginParty(id: String) {
        questId = id
        party = emptyList()
        screen = HbScreen.Party
    }

    fun toggleParty(id: String) {
        val q = Heartbound.quest[questId] ?: return
        if (!state.h(id).ready) return
        party = if (id in party) party - id else if (party.size < q.maxParty) party + id else party
    }

    fun chance(): Int? {
        val q = Heartbound.quest[questId] ?: return null
        if (party.size !in q.minParty..q.maxParty) return null
        return HeartboundLogic.successChance(state, party, q)
    }

    fun confirmQuest(roll: Int? = null): Boolean {
        val q = Heartbound.quest[questId] ?: return false
        if (party.size !in q.minParty..q.maxParty) return false
        if (party.any { !state.h(it).ready }) return false
        val r = roll ?: rng.nextInt(100)
        val (next, _) = HeartboundLogic.resolve(state, party, q, r)
        commit(next)
        screen = HbScreen.Result
        return true
    }

    fun dismissResult() {
        commit(state.copy(lastResult = null))
        screen = HbScreen.Evening
    }

    fun buy(id: String): Boolean {
        val gift = Heartbound.gift[id]; val gear = Heartbound.gearById[id]
        val cost = gift?.cost ?: gear?.cost ?: return false
        val name = gift?.name ?: gear?.name ?: return false
        if (state.gold < cost) return false
        commit(HeartboundLogic.addInv(state.copy(gold = state.gold - cost), id))
        toast = "Bought $name."
        return true
    }

    fun buyUpgrade(id: String): Boolean {
        val def = Heartbound.upgrades.firstOrNull { it.id == id } ?: return false
        val level = when (id) { "rooms" -> state.rooms; "training" -> state.training; else -> state.tavern }
        if (level >= def.levels.size) return false
        val cost = def.levels[level].first
        if (state.gold < cost) return false
        val next = when (id) {
            "rooms" -> state.copy(gold = state.gold - cost, rooms = level + 1)
            "training" -> state.copy(gold = state.gold - cost, training = level + 1)
            else -> state.copy(gold = state.gold - cost, tavern = level + 1)
        }
        commit(next)
        toast = "${def.name} improved."
        return true
    }

    fun equip(heroId: String, gearId: String): Boolean {
        val gear = Heartbound.gearById[gearId] ?: return false
        if (state.inv(gearId) <= 0) return false
        var s = state
        val h = s.h(heroId)
        val old = if (gear.slot == "weapon") h.weapon else h.accessory
        if (old != null) s = HeartboundLogic.addInv(s, old)
        s = HeartboundLogic.spendInv(s, gearId) ?: return false
        val nh = if (gear.slot == "weapon") h.copy(weapon = gearId) else h.copy(accessory = gearId)
        commit(s.copy(heroes = s.heroes + (heroId to nh)))
        toast = "Equipped ${gear.name}."
        return true
    }

    fun giveGift(heroId: String, giftId: String): Boolean {
        if (state.actionsLeft <= 0) return false
        if (state.inv(giftId) <= 0) return false
        val def = Heartbound.hero.getValue(heroId)
        val gift = Heartbound.gift[giftId] ?: return false
        val delta = Heartbound.giftDelta(def, giftId) + HeartboundLogic.eveningBonus(state)
        var s = HeartboundLogic.spendInv(state, giftId) ?: return false
        s = s.copy(
            heroes = s.heroes + (heroId to HeartboundLogic.addAff(s.h(heroId), delta)),
            actionsLeft = s.actionsLeft - 1,
        )
        commit(s)
        openScene(Story.giftLines(def, gift, delta), "gift", heroId)
        return true
    }

    fun startTalk(heroId: String): Boolean {
        if (state.actionsLeft <= 0 || !state.h(heroId).recruited) return false
        val ev = Story.eventFor(heroId, state.h(heroId).affection, state.h(heroId).seenEvents)
        if (ev != null) {
            openScene(ev, "event", heroId)
            return true
        }
        val options = Story.talksFor(heroId)
        val unseen = options.filter { it.id !in state.seenTalks }
        val pick = (unseen.ifEmpty { options }).let { it[rng.nextInt(it.size)] }
        openScene(pick, "talk", heroId)
        return true
    }

    fun startDate(heroId: String): Boolean {
        if (state.actionsLeft <= 0 || state.h(heroId).affection < Heartbound.DATE_MIN) return false
        openScene(Story.dateFor(heroId), "date", heroId)
        return true
    }

    private fun openScene(scene: Scene, kind: String, heroId: String?) {
        play = ScenePlay(scene, kind, heroId)
        screen = HbScreen.Dialogue
    }

    fun currentLine(): Line? {
        val p = play ?: return null
        return if (p.followup.isNotEmpty()) p.followup.getOrNull(p.followIndex) else p.scene.lines.getOrNull(p.lineIndex)
    }

    fun hasMoreLines(): Boolean {
        val p = play ?: return false
        return if (p.followup.isNotEmpty()) p.followIndex < p.followup.lastIndex
        else p.lineIndex < p.scene.lines.lastIndex
    }

    fun showingChoices(): Boolean = play?.awaitingChoice == true

    fun advance() {
        val p = play ?: return
        if (p.awaitingChoice) return
        if (p.followup.isNotEmpty()) {
            if (p.followIndex < p.followup.lastIndex) play = p.copy(followIndex = p.followIndex + 1)
            else finishScene()
            return
        }
        if (p.lineIndex < p.scene.lines.lastIndex) play = p.copy(lineIndex = p.lineIndex + 1)
        else if (p.scene.choices.isNotEmpty()) play = p.copy(awaitingChoice = true)
        else finishScene()
    }

    fun choose(index: Int) {
        val p = play ?: return
        val c = p.scene.choices.getOrNull(index) ?: return
        val n = p.copy(awaitingChoice = false, pendingDelta = c.delta, pendingFlag = c.flag, pendingRecruit = c.recruit, followup = c.next, followIndex = 0)
        if (c.next.isNotEmpty()) play = n else { play = n; finishScene() }
    }

    fun finishScene() {
        val p = play ?: return
        var s = state
        val hid = p.heroId
        if (hid != null && p.pendingDelta != 0 && s.heroes[hid] != null) {
            s = s.copy(heroes = s.heroes + (hid to HeartboundLogic.addAff(s.h(hid), p.pendingDelta)))
        }
        if (p.pendingFlag != null) s = s.copy(flags = s.flags + p.pendingFlag!!)
        if (p.pendingRecruit && hid != null) {
            s = s.copy(heroes = s.heroes + (hid to s.h(hid).copy(recruited = true, affection = maxOf(s.h(hid).affection, 6))))
        }
        if (p.kind == "recruit" && hid != null) s = s.copy(flags = s.flags + "offered_${hid}_${s.day}")
        when (p.kind) {
            "event" -> if (hid != null) {
                val h = s.h(hid)
                s = s.copy(heroes = s.heroes + (hid to h.copy(seenEvents = h.seenEvents + p.scene.id)), actionsLeft = (s.actionsLeft - 1).coerceAtLeast(0))
            }
            "talk", "date" -> {
                s = s.copy(seenTalks = s.seenTalks + p.scene.id, actionsLeft = (s.actionsLeft - 1).coerceAtLeast(0))
            }
            "gift" -> { /* action already spent */ }
            "intro" -> { }
            "recruit" -> { }
        }
        commit(s)
        play = null
        screen = when (p.kind) {
            "intro", "recruit" -> HbScreen.Home
            else -> HbScreen.Evening
        }
        if (p.kind == "recruit") maybeOfferRecruit()
    }

    fun turnIn() {
        commit(HeartboundLogic.turnIn(state))
        screen = HbScreen.Home
        maybeOfferRecruit()
    }

    // Test / screenshot helpers
    fun jumpHome(s: GameState = state) {
        started = true
        commit(s)
        play = null
        screen = if (s.phase == "evening") HbScreen.Evening else HbScreen.Home
    }

    fun jumpDialogue(scene: Scene, kind: String = "event", line: Int = 0, showChoices: Boolean = false) {
        started = true
        openScene(scene, kind, scene.heroId)
        play = play?.copy(lineIndex = line.coerceIn(0, scene.lines.lastIndex), awaitingChoice = showChoices)
        screen = HbScreen.Dialogue
    }
}
