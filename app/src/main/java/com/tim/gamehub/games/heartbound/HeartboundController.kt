package com.tim.gamehub.games.heartbound

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

object HbSession {
    var introPlayed: Boolean = false
    fun reset() { introPlayed = false }
}

object IntroTiming {
    const val DURATION = 9000
    const val TOWN = 700
    const val HALL = 2000
    const val LANTERNS = 1400
    const val HEROES = 3400
    const val LOGO = 5600
}

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
    data object Create : HbScreen()
    data object Slots : HbScreen()
    data object Settings : HbScreen()
    data object Journal : HbScreen()
    data object Trophies : HbScreen()
    data object Summary : HbScreen()
    data object Log : HbScreen()
    data object CompanionPick : HbScreen()
    data object Battle : HbScreen()
    data object Hall : HbScreen()
    data object Town : HbScreen()
    data object Craft : HbScreen()
    data object Merchant : HbScreen()
    data class DateMap(val heroId: String) : HbScreen()
    data object Gallery : HbScreen()
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
    val pendingGain: String? = null,
    val cgOffered: Boolean = false,
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
    var draft by mutableStateOf(Player())
    var slotMode by mutableStateOf("load") // load | new
    var returnTo by mutableStateOf<HbScreen>(HbScreen.Title)
    var settings by mutableStateOf(store.settings)
    var lastFocus by mutableStateOf<String?>(null)
    var battle by mutableStateOf<BattleState?>(null)
    var cgView by mutableStateOf<String?>(null)
    var introMode by mutableStateOf("unset") // unset | play | done
    var introFreezeMs by mutableStateOf<Int?>(null)


    val hasSave get() = (0 until store.slotCount).any { store.slot(it) != null }

    fun slotInfos(): List<SlotInfo> = (0 until store.slotCount).map { i ->
        HeartboundSave.preview(store.slot(i))?.copy(index = i) ?: SlotInfo(i, true)
    }

    private fun commit(next: GameState, toastNew: Boolean = true) {
        val ev = Ach.evaluate(next)
        val fresh = ev - next.achievements
        val s = next.copy(achievements = ev)
        state = s
        store.data = HeartboundSave.encode(s)
        if (toastNew && fresh.isNotEmpty()) {
            val a = Content.achById[fresh.first()]
            if (a != null) toast = "Achievement: ${a.title}"
            HbSound.sfx("ach", settings)
        }
    }

    fun loadSave(): Boolean = latestSlotIndex()?.let { loadSlot(it) } ?: false

    fun loadSlot(i: Int): Boolean {
        val raw = store.slot(i) ?: return false
        return try {
            store.lastSlot = i
            state = HeartboundSave.decode(raw)
            if (state.anchor == null) state = state.copy(anchor = HeartboundLogic.snapshot(state))
            started = true
            play = null
            screen = when {
                state.report != null -> HbScreen.Summary
                state.phase == "evening" -> if (state.lastResult != null && state.actionsLeft == state.eveningCap()) HbScreen.Result else HbScreen.Evening
                else -> HbScreen.Home
            }
            true
        } catch (_: Exception) { false }
    }

    fun deleteSlot(i: Int) {
        store.writeSlot(i, null)
        if (store.lastSlot == i) started = false
    }

    fun openSlots(mode: String) { slotMode = mode; screen = HbScreen.Slots }
    fun openCreate(slot: Int) { store.lastSlot = slot; draft = Player(); screen = HbScreen.Create }
    fun openSettings() { returnTo = screen; screen = HbScreen.Settings }
    fun openJournal() { returnTo = screen; screen = HbScreen.Journal }
    fun openTrophies() { returnTo = screen; screen = HbScreen.Trophies }
    fun openLog() { returnTo = screen; screen = HbScreen.Log }
    fun closeMenu() { screen = returnTo }

    fun setSpeed(n: Int) { settings = settings.copy(textSpeed = n.coerceIn(0, 3)); store.settings = settings }
    fun setSkipSeen(v: Boolean) { settings = settings.copy(skipSeen = v); store.settings = settings }
    fun setFadeToBlack(v: Boolean) { settings = settings.copy(fadeToBlack = v); store.settings = settings }
    fun setAutoBattle(v: Boolean) {
        settings = settings.copy(autoBattle = v)
        store.settings = settings
        battle = battle?.copy(auto = v)
    }
    fun setMusicVol(n: Int) { settings = settings.copy(musicVol = n.coerceIn(0, 10)); store.settings = settings }
    fun setSfxVol(n: Int) { settings = settings.copy(sfxVol = n.coerceIn(0, 10)); store.settings = settings }
    fun setMuted(v: Boolean) { settings = settings.copy(muted = v); store.settings = settings }
    fun setPlayIntro(v: Boolean) { settings = settings.copy(playIntro = v); store.settings = settings }
    fun openGallery() { returnTo = screen; screen = HbScreen.Gallery }
    fun latestSlotIndex(): Int? {
        val occupied = (0 until store.slotCount).filter { store.slot(it) != null }
        if (occupied.isEmpty()) return null
        if (store.lastSlot in occupied) return store.lastSlot
        return occupied.maxByOrNull { i -> HeartboundSave.preview(store.slot(i))?.day ?: -1 }
    }
    fun openGalleryFromTitle() {
        latestSlotIndex()?.let { i ->
            val raw = store.slot(i) ?: return@let
            try { state = HeartboundSave.decode(raw) } catch (_: Exception) {}
        }
        returnTo = HbScreen.Title
        screen = HbScreen.Gallery
    }
    fun beginIntroIfNeeded() {
        if (introMode != "unset") return
        introMode = if (settings.playIntro && !HbSession.introPlayed) "play" else "done"
        if (introMode == "done") HbSession.introPlayed = true
    }
    fun skipIntro() {
        introMode = "done"
        introFreezeMs = null
        HbSession.introPlayed = true
    }
    fun jumpIntro(ms: Int) {
        introMode = "play"
        introFreezeMs = ms.coerceAtLeast(0)
        screen = HbScreen.Title
        started = false
    }
    fun jumpTitleMenu() {
        introMode = "done"
        introFreezeMs = null
        HbSession.introPlayed = true
        screen = HbScreen.Title
    }
    fun shouldPlayIntro(): Boolean = settings.playIntro && !HbSession.introPlayed && introMode != "done"
    fun openCg(id: String) { if (id in state.gallery) cgView = id }
    fun dismissCg() { cgView = null }
    fun offerCg(show: Boolean = true) {
        val p = play ?: return
        val def = Gallery.byScene[p.scene.id] ?: return
        if (def.id !in state.gallery) commit(state.copy(gallery = state.gallery + def.id), toastNew = true)
        if (show && !p.cgOffered) {
            cgView = def.id
            play = p.copy(cgOffered = true)
        }
    }
    fun jumpCg(id: String) {
        started = true
        if (id !in state.gallery) commit(state.copy(gallery = state.gallery + id), toastNew = false)
        cgView = id
        if (screen == HbScreen.Title) screen = HbScreen.Home
    }
    fun markSeen(id: String) { store.seenLines = store.seenLines + id }
    fun isSeen(id: String) = id in store.seenLines
    fun lineId(): String? {
        val p = play ?: return null
        val tag = if (p.followup.isNotEmpty()) "f${p.followIndex}" else "${p.lineIndex}"
        return "${p.scene.id}#$tag"
    }

    fun newGame(player: Player = draft, slot: Int = store.lastSlot) {
        store.lastSlot = slot
        commit(HeartboundLogic.newGame(player), toastNew = false)
        started = true
        openScene(Story.introFor(state.player), "intro", null)
    }

    fun backToTitle() {
        skipIntro()
        screen = HbScreen.Title
        play = null
    }

    fun goHome() {
        play = null
        screen = if (state.phase == "evening") HbScreen.Evening else HbScreen.Home
        maybeOfferMorning()
    }

    private fun maybeOfferMorning() {
        if (screen != HbScreen.Home || state.phase != "morning") return
        pendingRecruit()?.let { openScene(it, "recruit", it.heroId); return }
        Arc.pendingPlot(state)?.let { openScene(it, "plot", it.heroId); return }
        if (state.expedition != null && state.expedition!!.daysLeft > 0) offerTravel()
    }

    private fun maybeOfferRecruit() = maybeOfferMorning()

    fun pendingRecruit(): Scene? {
        if (state.phase != "morning") return null
        val mira = state.h("mira"); val sera = state.h("sera"); val rin = state.h("rin")
        return when {
            !mira.recruited && state.day >= 2 && "offered_mira_${state.day}" !in state.flags -> Story.recruitMira
            !sera.recruited && (state.day >= 3 && (state.has("did_forest") || state.day >= 4)) && "offered_sera_${state.day}" !in state.flags -> Story.recruitSera
            !rin.recruited && state.day >= 4 && "offered_rin_${state.day}" !in state.flags -> Story.recruitRin
            !state.h("nim").recruited && state.day >= 10 && (state.has("plot_ch1_done") || state.world.chapter >= 1 || state.day >= 16) && "offered_nim_${state.day}" !in state.flags -> Life.recruitNim
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
        maybeOfferEvening()
    }

    fun availableQuests(): List<QuestDef> = HeartboundLogic.availableQuests(state)

    fun beginParty(id: String) {
        if (state.expedition != null) {
            toast = "A party is already in the field."
            return
        }
        questId = id; party = emptyList(); screen = HbScreen.Party
    }
    fun toggleParty(id: String) {
        val q = Heartbound.quest[questId] ?: return
        if (!state.h(id).ready || state.onRoad(id)) return
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
        if (party.any { !state.h(it).ready || state.onRoad(it) }) return false
        if (roll != null) {
            val (next, _) = HeartboundLogic.resolve(state, party, q, roll)
            commit(next)
            screen = HbScreen.Result
            return true
        }
        val shortcut = if (state.stables >= 3) 2 else if (state.stables >= 1) 1 else 0
        val exp = Expedition(q.id, party, daysLeft = (q.days - 1 - shortcut).coerceAtLeast(0), totalDays = q.days)
        commit(state.copy(expedition = exp), toastNew = false)
        return if (q.days >= 2) { offerTravel(); true } else { startBattle(); true }
    }

    fun startBattle() {
        val ex = state.expedition
        val q = Heartbound.quest[ex?.questId ?: questId] ?: return
        val p = ex?.party ?: party
        questId = q.id
        party = p
        battle = Battle.start(state, p, q, auto = settings.autoBattle)
        screen = HbScreen.Battle
    }

    fun joinFight() {
        val ex = state.expedition ?: return
        if (ex.daysLeft > 0) return
        questId = ex.questId
        party = ex.party
        startBattle()
    }

    fun autoBattleStep() {
        val b = battle ?: return
        if (b.finished) { finishBattle(); return }
        battle = Battle.autoStep(state, b)
        sfxBattle(b, battle)
    }

    fun playerAct(skillId: String, targetId: String?) {
        val b = battle ?: return
        if (b.finished || b.auto) return
        val skill = Battle.skills[skillId] ?: return
        battle = Battle.act(b, skill, targetId, state)
        sfxBattle(b, battle)
    }

    private fun sfxBattle(before: BattleState, after: BattleState?) {
        if (after == null) return
        val prev = before.fighters.associate { it.id to it.hp }
        val healed = after.fighters.any { (prev[it.id] ?: it.hp) < it.hp }
        val hurt = after.fighters.any { (prev[it.id] ?: it.hp) > it.hp }
        when {
            healed -> HbSound.sfx("heal", settings)
            hurt -> HbSound.sfx("hit", settings)
        }
    }

    fun toggleBattleAuto() { setAutoBattle(!(battle?.auto ?: settings.autoBattle)) }

    fun retreatBattle() {
        val b = battle ?: return
        val lost = b.copy(finished = true, victory = false, log = b.log + "You call the retreat.")
        battle = lost
        finishBattle()
    }

    fun finishBattle() {
        val b = battle ?: return
        if (!b.finished) return
        var (next, _) = HeartboundLogic.settleBattle(state, b)
        if (b.usedCombo) next = next.copy(flags = next.flags + "used_combo")
        battle = null
        commit(next)
        if (next.lastResult?.leveled?.isNotEmpty() == true) HbSound.sfx("level", settings)
        screen = HbScreen.Result
    }

    fun offerTravel() {
        val ex = state.expedition ?: return
        if (ex.daysLeft <= 0) return
        if ("travel_${state.day}" in state.flags) {
            if (state.phase != "evening") commit(state.copy(phase = "evening", actionsLeft = state.eveningCap()), toastNew = false)
            screen = HbScreen.Evening
            return
        }
        val q = Heartbound.quest[ex.questId] ?: return
        if (state.phase != "evening") commit(state.copy(phase = "evening", actionsLeft = state.eveningCap()), toastNew = false)
        val scene = Travel.pick(state, ex.party, q, rng)
        openScene(scene, "travel", scene.heroId)
    }

    fun dismissResult() {
        screen = HbScreen.Evening
        Travel.pendingBranch(state)?.let { openScene(it, "branch", null); return }
        maybeOfferEvening()
    }

    private fun maybeOfferEvening() {
        if (pendingGroup() != null) return
        if (state.heroes.values.any { it.injuredDays > 0 && canCare(it.id) }) return
        Life.birthdayOf(state)?.let { openScene(Life.birthdayScene(it), "birthday", it); return }
        Life.pendingEnding(state)?.let { openScene(it, "ending", null); return }
        val scn = pendingRival() ?: return
        openScene(scn, "rival", scn.heroId)
    }

    fun buy(id: String, merchant: Boolean = false): Boolean {
        val gift = Life.giftDef(id); val gear = Heartbound.gearById[id]; val cat = Life.catalog[id]
        val cost0 = gift?.cost ?: gear?.cost ?: cat?.cost ?: return false
        val cost = Life.shopPrice(cost0, state)
        val name = gift?.name ?: gear?.name ?: cat?.name ?: return false
        if (state.gold < cost) return false
        var flags = state.flags + "bought"
        if (merchant) flags = flags + "merchant"
        commit(HeartboundLogic.addInv(state.copy(gold = state.gold - cost, flags = flags), id))
        toast = "Bought $name."
        return true
    }
    fun buyUpgrade(id: String): Boolean {
        val def = Heartbound.upgrades.firstOrNull { it.id == id } ?: return false
        val level = state.wing(id)
        if (level >= def.levels.size) return false
        val cost = def.levels[level].first
        if (state.gold < cost) return false
        val next = when (id) {
            "rooms" -> state.copy(gold = state.gold - cost, rooms = level + 1)
            "training" -> state.copy(gold = state.gold - cost, training = level + 1)
            "tavern" -> state.copy(gold = state.gold - cost, tavern = level + 1)
            "forge" -> state.copy(gold = state.gold - cost, forge = level + 1)
            "library" -> state.copy(gold = state.gold - cost, library = level + 1)
            "garden" -> state.copy(gold = state.gold - cost, garden = level + 1)
            "stables" -> state.copy(gold = state.gold - cost, stables = level + 1)
            else -> return false
        }
        commit(next)
        toast = "${def.name} improved."
        return true
    }
    fun hireStaff(id: String): Boolean {
        val d = Life.staffBy[id] ?: return false
        if (id in state.staff) return false
        if (state.gold < d.cost) return false
        commit(state.copy(gold = state.gold - d.cost, staff = state.staff + id, flags = state.flags + "hired_$id"))
        toast = "${d.name} has joined the hall."
        return true
    }
    fun placeDecor(id: String, room: String? = null): Boolean {
        if (state.inv(id) <= 0) return false
        var s = HeartboundLogic.spendInv(state, id) ?: return false
        s = if (room == null) s.copy(decor = s.decor + id)
        else s.copy(roomDecor = s.roomDecor + (room to (s.roomDecor[room] ?: emptySet()) + id))
        commit(s)
        toast = "Placed."
        return true
    }
    fun craft(id: String): Boolean {
        val r = Life.recipes.firstOrNull { it.id == id } ?: return false
        if (state.forge < r.needForge || state.garden < r.needGarden || state.library < r.needLibrary) {
            toast = "The hall isn't ready for that work yet."; return false
        }
        if (r.inputs.any { state.inv(it.first) < it.second }) { toast = "You're short on materials."; return false }
        var s = state
        r.inputs.forEach { (i, n) -> s = HeartboundLogic.spendInv(s, i, n) ?: return false }
        s = HeartboundLogic.addInv(s, r.output, r.outN).copy(flags = s.flags + "crafted")
        commit(s)
        toast = "Crafted ${r.name}."
        return true
    }
    fun enchant(heroId: String, defn: Boolean = false): Boolean {
        if (state.forge <= 0) { toast = "The forge isn't ready."; return false }
        val cost = if (state.forge >= 2) 30 else 40
        if (state.gold < cost || state.inv("ore") < 1) { toast = "You need coin and ore."; return false }
        val h = state.h(heroId)
        val cap = state.forge
        if ((if (defn) h.enchantDef else h.enchantAtk) >= cap) { toast = "That's as far as this forge will take it."; return false }
        var s = HeartboundLogic.spendInv(state, "ore") ?: return false
        val nh = if (defn) h.copy(enchantDef = h.enchantDef + 1) else h.copy(enchantAtk = h.enchantAtk + 1)
        commit(s.copy(gold = s.gold - cost, heroes = s.heroes + (heroId to nh), flags = s.flags + "enchanted"))
        toast = "The ${if (defn) "guard" else "edge"} takes a little more light."
        return true
    }
    fun openHall() { returnTo = screen; screen = HbScreen.Hall }
    fun openTown() { returnTo = screen; screen = HbScreen.Town }
    fun openCraft() { returnTo = screen; screen = HbScreen.Craft }
    fun openMerchant() { returnTo = screen; screen = HbScreen.Merchant }
    fun openDateMap(heroId: String) { if (state.actionsLeft > 0 && state.h(heroId).affection >= Heartbound.DATE_MIN) screen = HbScreen.DateMap(heroId) }

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
        val gift = Life.giftDef(giftId) ?: return false
        val delta = Heartbound.giftDelta(def, giftId) + HeartboundLogic.eveningBonus(state)
        var s = HeartboundLogic.spendInv(state, giftId) ?: return false
        val loved = if (giftId in def.love) s.lovedGifts + "$heroId:$giftId" else s.lovedGifts
        s = s.copy(
            heroes = s.heroes + (heroId to HeartboundLogic.addAff(s.h(heroId), delta)),
            actionsLeft = s.actionsLeft - 1,
            giftsGiven = s.giftsGiven + 1,
            lovedGifts = loved,
        )
        commit(s)
        lastFocus = heroId
        HbSound.sfx("gift", settings)
        openScene(Life.rareGiftScene(def, gift, delta) ?: Story.giftLines(def, gift, delta), "gift", heroId)
        return true
    }

    fun startTalk(heroId: String): Boolean {
        if (state.actionsLeft <= 0 || !state.h(heroId).recruited) return false
        lastFocus = heroId
        val ev = Story.eventFor(heroId, state)
        if (ev != null) { openScene(ev, "event", heroId); return true }
        val options = Story.talksFor(heroId)
        val unseen = options.filter { it.id !in state.seenTalks }
        val pick = (unseen.ifEmpty { options }).let { it[rng.nextInt(it.size)] }
        openScene(pick, "talk", heroId)
        return true
    }
    fun startDate(heroId: String, place: String = "town"): Boolean {
        if (state.actionsLeft <= 0 || state.h(heroId).affection < Heartbound.DATE_MIN) return false
        lastFocus = heroId
        openScene(Story.dateFor(heroId, place), "date", heroId)
        return true
    }
    fun canVisit(heroId: String) = state.actionsLeft > 0 && state.h(heroId).recruited && state.h(heroId).affection >= 40 && "visited_${heroId}_${state.day}" !in state.flags
    fun startVisit(heroId: String): Boolean {
        if (!canVisit(heroId)) return false
        lastFocus = heroId
        openScene(Life.visitScene(heroId, state.h(heroId).affection), "visit", heroId)
        return true
    }

    fun canCare(heroId: String) =
        state.h(heroId).recruited && state.h(heroId).injuredDays > 0 && "cared_${heroId}_${state.day}" !in state.flags

    fun startCare(heroId: String): Boolean {
        if (!canCare(heroId)) return false
        openScene(Arc.careFor(heroId), "care", heroId)
        return true
    }

    fun pendingGroup() = Arc.pendingGroup(state)
    fun openCompanionPick() { if (pendingGroup() != null) screen = HbScreen.CompanionPick }
    fun chooseCompanion(heroId: String) {
        val ev = pendingGroup() ?: return
        if (!state.h(heroId).recruited) return
        lastFocus = heroId
        openScene(Arc.groupScene(ev.id, heroId), "group", heroId)
    }

    fun pendingRival() = Arc.pendingRival(state)

    private fun openScene(scene: Scene, kind: String, heroId: String?) {
        play = ScenePlay(scene, kind, heroId)
        logCurrent()
        screen = HbScreen.Dialogue
        skipDisabledFade()
        if (Gallery.shouldShowAtStart(scene.id)) offerCg(show = true)
    }

    fun currentLine(): Line? {
        val p = play ?: return null
        val raw = if (p.followup.isNotEmpty()) p.followup.getOrNull(p.followIndex) else p.scene.lines.getOrNull(p.lineIndex)
        return raw?.copy(text = Voice.apply(raw.text, state.player))
    }
    fun hasMoreLines(): Boolean {
        val p = play ?: return false
        return if (p.followup.isNotEmpty()) p.followIndex < p.followup.lastIndex
        else p.lineIndex < p.scene.lines.lastIndex
    }
    fun showingChoices(): Boolean = play?.awaitingChoice == true
    fun choices(): List<Pair<Choice, Boolean>> {
        val p = play ?: return emptyList()
        if (!p.awaitingChoice) return emptyList()
        return p.scene.choices.map { it.copy(text = Voice.apply(it.text, state.player)) to Story.unlocked(it, state, p.heroId) }
    }

    private fun logCurrent() {
        val line = currentLine() ?: return
        val who = line.speaker.ifBlank { "—" }
        val entry = "$who: ${line.text}"
        if (state.log.lastOrNull() == entry) return
        state = state.copy(log = (state.log + entry).takeLast(300))
    }

    fun advance() {
        val p = play ?: return
        if (p.awaitingChoice) return
        lineId()?.let { markSeen(it) }
        if (p.followup.isNotEmpty()) {
            if (p.followIndex < p.followup.lastIndex) { play = p.copy(followIndex = p.followIndex + 1); logCurrent(); skipDisabledFade() }
            else finishScene()
            return
        }
        if (p.lineIndex < p.scene.lines.lastIndex) { play = p.copy(lineIndex = p.lineIndex + 1); logCurrent(); skipDisabledFade() }
        else if (p.scene.choices.isNotEmpty()) play = p.copy(awaitingChoice = true)
        else finishScene()
    }

    /** When fade-to-black is off, skip cutaway lines entirely. */
    private fun skipDisabledFade() {
        if (settings.fadeToBlack) return
        var guard = 0
        while (guard++ < 16 && play != null && currentLine()?.fade == true && !showingChoices()) {
            val p = play ?: return
            if (p.followup.isNotEmpty()) {
                if (p.followIndex < p.followup.lastIndex) { play = p.copy(followIndex = p.followIndex + 1); logCurrent() }
                else { finishScene(); return }
            } else if (p.lineIndex < p.scene.lines.lastIndex) {
                play = p.copy(lineIndex = p.lineIndex + 1); logCurrent()
            } else if (p.scene.choices.isNotEmpty()) {
                play = p.copy(awaitingChoice = true)
            } else {
                finishScene(); return
            }
        }
    }

    /** Jump through already-seen lines when skip-seen is on, stopping at new text or choices. */
    fun skipSeenLines() {
        if (!settings.skipSeen && !instantText) return
        var guard = 0
        while (guard++ < 40 && play != null && !showingChoices()) {
            val id = lineId() ?: break
            val last = !hasMoreLines()
            if (!isSeen(id) && !instantText) break
            if (last) { advance(); break }
            advance()
        }
    }

    fun choose(index: Int) {
        val p = play ?: return
        val c = p.scene.choices.getOrNull(index) ?: return
        if (!Story.unlocked(c, state.player)) return
        val n = p.copy(awaitingChoice = false, pendingDelta = c.delta, pendingFlag = c.flag, pendingRecruit = c.recruit, pendingGain = c.gain, followup = c.next, followIndex = 0)
        if (c.next.isNotEmpty()) { play = n; logCurrent(); skipDisabledFade() } else { play = n; finishScene(); return }
        offerCg(show = true)
    }

    fun finishScene() {
        val p = play ?: return
        offerCg(show = false)
        var s = state
        val hid = p.heroId
        if (hid != null && p.pendingDelta != 0 && s.heroes[hid] != null) {
            s = s.copy(heroes = s.heroes + (hid to HeartboundLogic.addAff(s.h(hid), p.pendingDelta)))
        }
        if (p.pendingFlag != null) {
            val f = p.pendingFlag!!
            s = s.copy(flags = s.flags + f)
            if (f.endsWith("_confess") && s.world.route == null) {
                s = s.copy(world = s.world.copy(route = f.removeSuffix("_confess")))
            }
            if (f == "plot_ch1_done") s = s.copy(world = s.world.copy(chapter = maxOf(s.world.chapter, 1)))
        }
        if (p.pendingGain != null) s = s.copy(player = s.player.gain(p.pendingGain!!))
        if (p.pendingRecruit && hid != null) {
            s = s.copy(heroes = s.heroes + (hid to s.h(hid).copy(recruited = true, affection = maxOf(s.h(hid).affection, 6))))
        }
        if (p.kind == "recruit" && hid != null) s = s.copy(flags = s.flags + "offered_${hid}_${s.day}")
        if (p.kind == "group") {
            val evId = Arc.groupsCalendar.firstOrNull { p.scene.id.startsWith(it.id + "_") }?.id
            if (evId != null) s = s.copy(flags = s.flags + "event_$evId")
        }
        if (p.kind == "care" && hid != null) s = s.copy(flags = s.flags + "cared_${hid}_${s.day}")
        if (p.kind == "jealousy") s = s.copy(flags = s.flags + p.scene.id + "jealous_day_${s.day}")
        if (p.kind == "rival") s = s.copy(flags = s.flags + (p.pendingFlag ?: p.scene.id))
        when (p.kind) {
            "event" -> if (hid != null) {
                val h = s.h(hid)
                s = s.copy(heroes = s.heroes + (hid to h.copy(seenEvents = h.seenEvents + p.scene.id)), actionsLeft = (s.actionsLeft - 1).coerceAtLeast(0))
            }
            "talk", "date" -> s = s.copy(seenTalks = s.seenTalks + p.scene.id, actionsLeft = (s.actionsLeft - 1).coerceAtLeast(0))
        }
        if (p.kind == "travel") {
            val flag = p.pendingFlag
            val exp = s.expedition
            if (exp != null) {
                var flags = exp.flags
                if (flag != null) flags = flags + flag
                s = s.copy(expedition = exp.copy(flags = flags, eventsDone = exp.eventsDone + 1), flags = s.flags + "travel_${s.day}" + setOfNotNull(flag))
            } else {
                s = s.copy(flags = s.flags + "travel_${s.day}")
            }
            if ("travel_cache" in s.flags && "paid_travel_cache" !in s.flags) {
                s = s.copy(gold = s.gold + 20, flags = s.flags + "paid_travel_cache")
            }
            s = s.copy(phase = "evening", actionsLeft = s.eveningCap())
            commit(s)
            play = null
            screen = HbScreen.Evening
            return
        }
        if (p.kind == "branch") {
            commit(s)
            play = null
            screen = HbScreen.Evening
            return
        }
        if (p.kind == "visit" && hid != null) {
            s = s.copy(flags = s.flags + "visited_${hid}_${s.day}", actionsLeft = (s.actionsLeft - 1).coerceAtLeast(0))
            commit(s); play = null; screen = HbScreen.Evening
            maybeOfferJealousy()
            return
        }
        if (p.kind == "birthday" && hid != null) {
            s = s.copy(flags = s.flags + "bday_${hid}_${s.day}")
            commit(s); play = null; screen = HbScreen.Evening
            return
        }
        if (p.kind == "ending") {
            commit(s); play = null; screen = HbScreen.Evening
            return
        }
        commit(s)
        play = null
        screen = when (p.kind) {
            "intro", "recruit", "plot" -> HbScreen.Home
            else -> HbScreen.Evening
        }
        if (p.kind == "recruit" || p.kind == "plot") maybeOfferMorning()
        if (p.kind in setOf("event", "date", "gift", "group") && screen == HbScreen.Evening) maybeOfferJealousy()
    }

    private fun maybeOfferJealousy() {
        if (state.phase != "evening" || screen != HbScreen.Evening) return
        if ("jealous_day_${state.day}" in state.flags) return
        val a = lastFocus ?: return
        val rival = state.recruited().filter { it.id != a && it.affection >= 40 }.maxByOrNull { it.affection } ?: return
        val id = "jealous_${rival.id}_of_$a"
        if (id in state.flags) return
        val scn = Arc.jealousy(rival.id, a)
        commit(state.copy(flags = state.flags + id + "jealous_day_${state.day}"), toastNew = false)
        openScene(scn, "jealousy", rival.id)
    }

    fun turnIn() {
        val endOfDay = state
        val anchor = endOfDay.anchor ?: HeartboundLogic.snapshot(endOfDay)
        val ev = Ach.evaluate(endOfDay)
        val report = HeartboundLogic.summarize(anchor, endOfDay.copy(achievements = ev))
        val next = HeartboundLogic.turnIn(endOfDay).copy(report = report, achievements = ev, lastResult = null)
        commit(next.copy(anchor = HeartboundLogic.snapshot(next.copy(report = null))))
        state = state.copy(report = report)
        screen = HbScreen.Summary
    }

    fun dismissSummary() {
        commit(state.copy(report = null), toastNew = false)
        screen = HbScreen.Home
        maybeOfferMorning()
    }

    fun isNight(): Boolean {
        val bg = play?.scene?.bg
        if (bg in setOf("festival", "harvest", "chapel", "hill", "room")) return true
        if (bg == "beach") return false
        return state.phase == "evening" || (screen == HbScreen.Dialogue && play?.kind in setOf("talk", "date", "event", "gift", "group", "care", "jealousy", "rival", "plot", "travel", "branch", "visit", "birthday", "ending"))
    }

    fun jumpHome(s: GameState = state) {
        started = true
        commit(s, toastNew = false)
        play = null
        screen = if (s.phase == "evening") HbScreen.Evening else HbScreen.Home
    }
    fun jumpDialogue(scene: Scene, kind: String = "event", line: Int = 0, showChoices: Boolean = false) {
        started = true
        openScene(scene, kind, scene.heroId)
        play = play?.copy(lineIndex = line.coerceIn(0, scene.lines.lastIndex), awaitingChoice = showChoices)
        screen = HbScreen.Dialogue
    }
    fun jumpScreen(s: HbScreen) { screen = s }
    fun jumpBattle(questId: String = "goblin_king", partyIds: List<String> = listOf("kael", "yuna", "mira"), auto: Boolean = false) {
        started = true
        this.questId = questId
        this.party = partyIds
        battle = Battle.start(state, partyIds, Heartbound.quest.getValue(questId), auto)
        screen = HbScreen.Battle
    }
    fun jumpFade(scene: Scene) {
        val fade = scene.choices.firstOrNull { ch -> ch.next.any { it.fade } }?.next?.last { it.fade } ?: return
        started = true
        openScene(scene, "event", scene.heroId)
        play = play?.copy(followup = listOf(fade), followIndex = 0, awaitingChoice = false)
        screen = HbScreen.Dialogue
    }
}
