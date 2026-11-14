package com.tim.gamehub.games.heartbound

data class HeroState(
    val id: String,
    val recruited: Boolean = false,
    val level: Int = 1,
    val xp: Int = 0,
    val injuredDays: Int = 0,
    val affection: Int = 0,
    val seenEvents: Set<String> = emptySet(),
    val weapon: String? = null,
    val accessory: String? = null,
    val enchantAtk: Int = 0,
    val enchantDef: Int = 0,
) {
    val def get() = Heartbound.hero.getValue(id)
    val ready get() = recruited && injuredDays <= 0
    fun atk(): Int {
        val g = Heartbound.gearById
        return def.baseAtk + (level - 1) * def.cls.atkGrowth +
            (weapon?.let { g[it]?.atk } ?: 0) + (accessory?.let { g[it]?.atk } ?: 0) + enchantAtk
    }
    fun defn(): Int {
        val g = Heartbound.gearById
        return def.baseDef + (level - 1) * def.cls.defGrowth +
            (weapon?.let { g[it]?.def } ?: 0) + (accessory?.let { g[it]?.def } ?: 0) + enchantDef
    }
    fun power(): Double = atk() + defn() * 0.5
    fun aff() = affection.coerceIn(0, Heartbound.MAX_AFF)
    fun tier() = Heartbound.tier(aff())
}

data class Expedition(
    val questId: String,
    val party: List<String>,
    val daysLeft: Int,
    val totalDays: Int,
    val eventsDone: Int = 0,
    val flags: Set<String> = emptySet(),
)

data class GameState(
    val day: Int = 1,
    val gold: Int = Heartbound.START_GOLD,
    val phase: String = "morning",          // morning | evening
    val actionsLeft: Int = 0,
    val rooms: Int = 0,
    val training: Int = 0,
    val tavern: Int = 0,
    val heroes: Map<String, HeroState> = emptyMap(),
    val inventory: Map<String, Int> = emptyMap(),
    val flags: Set<String> = emptySet(),
    val seenTalks: Set<String> = emptySet(),
    val lastResult: QuestResult? = null,
    val player: Player = Player(),
    val achievements: Set<String> = emptySet(),
    val log: List<String> = emptyList(),
    val report: DayReport? = null,
    val giftsGiven: Int = 0,
    val lovedGifts: Set<String> = emptySet(),
    val world: WorldState = WorldState(),
    val anchor: DayAnchor? = null,
    val reputation: Int = 0,
    val expedition: Expedition? = null,
    val forge: Int = 0,
    val library: Int = 0,
    val garden: Int = 0,
    val stables: Int = 0,
    val staff: Set<String> = emptySet(),
    val decor: Set<String> = emptySet(),
    val roomDecor: Map<String, Set<String>> = emptyMap(),
    val gallery: Set<String> = emptySet(),
) {
    fun h(id: String) = heroes.getValue(id)
    fun recruited() = Heartbound.heroes.map { h(it.id) }.filter { it.recruited }
    fun ready() = recruited().filter { it.ready }
    fun onRoad(id: String) = expedition?.party?.contains(id) == true
    fun hallReady() = ready().filter { !onRoad(it.id) }
    fun inv(id: String) = inventory[id] ?: 0
    fun has(flag: String) = flag in flags
    fun eveningCap() = (if (tavern > 0) 2 else 1) + (if ("keeper" in staff) 1 else 0)
    fun wing(id: String) = when (id) {
        "rooms" -> rooms; "training" -> training; "tavern" -> tavern
        "forge" -> forge; "library" -> library; "garden" -> garden; "stables" -> stables
        else -> 0
    }
}

data class QuestResult(
    val questId: String,
    val success: Boolean,
    val chance: Int,
    val roll: Int,
    val gold: Int,
    val xp: Int,
    val leveled: List<String>,
    val injuredId: String?,
    val injuryDays: Int,
    val loot: String?,
    val note: String,
)

object HeartboundLogic {
    fun newGame(player: Player = Player()): GameState {
        val p = player.originDef().let { player.copy(charm = it.charm, wit = it.wit, courage = it.courage) }
        val heroes = Heartbound.heroes.associate { d ->
            d.id to HeroState(d.id, recruited = d.startRecruited, level = d.startLevel, affection = d.startAff)
        }
        val s = GameState(heroes = heroes, inventory = mapOf("tea" to 1), player = p)
        return s.copy(achievements = Ach.evaluate(s), anchor = snapshot(s))
    }

    fun snapshot(s: GameState) = DayAnchor(
        day = s.day, gold = s.gold,
        aff = s.heroes.mapValues { it.value.affection },
        recruited = s.heroes.filter { it.value.recruited }.keys,
        achievements = s.achievements,
    )

    fun summarize(before: DayAnchor, after: GameState): DayReport {
        val aff = after.heroes.mapValues { it.value.affection - (before.aff[it.key] ?: it.value.affection) }.filter { it.value != 0 }
        val rec = after.heroes.filter { it.value.recruited && it.key !in before.recruited }.keys.toList()
        val newAch = (after.achievements - before.achievements).toList()
        val r = after.lastResult
        return DayReport(
            day = before.day, questTitle = r?.questId?.let { Heartbound.quest[it]?.title },
            questSuccess = r?.success, goldDelta = after.gold - before.gold, aff = aff,
            leveled = r?.leveled ?: emptyList(), injured = r?.injuredId, recruited = rec, newAchievements = newAch,
        )
    }

    fun partyScore(state: GameState, party: List<String>, quest: QuestDef): Double {
        val members = party.map { state.h(it) }
        var power = members.sumOf { it.power() }
        val classes = members.map { it.def.cls }.toSet()
        var syn = 1.0
        Heartbound.synergies.forEach { (need, bonus) -> if (classes.containsAll(need)) syn += bonus }
        syn += 0.08 * quest.preferred.count { it in classes }
        val bond = 1.0 + members.map { it.affection }.average().let { if (it.isNaN()) 0.0 else it } / 400.0
        val train = 1.0 + state.training * 0.06
        return power * syn * bond * train
    }

    fun successChance(state: GameState, party: List<String>, quest: QuestDef): Int {
        val score = partyScore(state, party, quest)
        return (55 + (score - quest.difficulty) * 3).toInt().coerceIn(8, 95)
    }

    fun addXp(h: HeroState, amount: Int): Pair<HeroState, Boolean> {
        var xp = h.xp + amount
        var lv = h.level
        var up = false
        while (xp >= Heartbound.xpToNext(lv)) {
            xp -= Heartbound.xpToNext(lv)
            lv += 1
            up = true
        }
        return h.copy(level = lv, xp = xp) to up
    }

    fun resolve(state: GameState, party: List<String>, quest: QuestDef, roll: Int): Pair<GameState, QuestResult> {
        val chance = successChance(state, party, quest)
        val success = roll < chance
        val close = kotlin.math.abs(roll - chance) <= 12
        val hasCleric = party.any { state.h(it).def.cls == HeroClass.CLERIC }
        var injured: String? = null
        var days = 0
        val injureChance = when {
            success && !close -> 0
            success && close -> quest.risk / 2
            else -> quest.risk + 20
        } - if (hasCleric) 12 else 0
        if (injureChance > 0 && (roll + quest.risk) % 100 < injureChance.coerceAtLeast(0)) {
            injured = party[(roll + party.size) % party.size]
            days = (if (success) 1 else 2) - (state.rooms + 1) / 2
            if (days < 1) days = 1
            if (state.rooms >= 3) days = 1
        }
        val gold = if (success) quest.gold else quest.gold / 5
        val xp = if (success) quest.xp else quest.xp / 4
        val loot = if (success && quest.loot != null && roll % 3 == 0) quest.loot else null
        val leveled = mutableListOf<String>()
        var heroes = state.heroes
        party.forEach { id ->
            var h = heroes.getValue(id)
            val (next, up) = addXp(h, xp)
            h = next
            if (up) leveled += id
            if (id == injured) h = h.copy(injuredDays = days)
            // a little warmth for coming home
            val bond = if (success) 2 else 1
            h = h.copy(affection = (h.affection + bond).coerceIn(0, Heartbound.MAX_AFF))
            heroes = heroes + (id to h)
        }
        var inv = state.inventory
        if (loot != null) inv = inv + (loot to (inv[loot] ?: 0) + 1)
        val note = buildString {
            append(if (success) "The party returns with the job done." else "The party returns empty-handed, but together.")
            if (injured != null) append(" ${Heartbound.hero.getValue(injured).name} was hurt and needs $days day${if (days == 1) "" else "s"} to recover.")
            if (loot != null) append(" Found: ${Heartbound.gift[loot]?.name ?: Heartbound.gearById[loot]?.name ?: loot}.")
        }
        val result = QuestResult(quest.id, success, chance, roll, gold, xp, leveled, injured, days, loot, note)
        return applyRewards(state, party, quest, result, heroes, inv, gold, success) to result
    }

    fun turnIn(state: GameState): GameState {
        val heroes = state.heroes.mapValues { (_, h) ->
            if (h.injuredDays > 0) {
                val extra = (if (state.rooms >= 2 && h.injuredDays > 1) 1 else 0) + (if ("grounds" in state.staff) 1 else 0)
                h.copy(injuredDays = (h.injuredDays - 1 - extra).coerceAtLeast(0))
            } else h
        }
        val exp = state.expedition?.let { if (it.daysLeft > 0) it.copy(daysLeft = it.daysLeft - 1) else it }
        val newDay = state.day + 1
        val season = Life.seasonOf(newDay)
        val weather = Life.weatherFor(season, kotlin.random.Random(newDay * 17 + state.gold + state.reputation))
        var next = state.copy(
            day = newDay,
            phase = "morning",
            actionsLeft = 0,
            heroes = heroes,
            lastResult = null,
            expedition = exp,
            world = state.world.copy(season = season, weather = weather),
        )
        next = Life.harvest(next)
        return next
    }


    fun availableQuests(state: GameState) = Heartbound.quests.filter { q ->
        q.minDay <= state.day &&
            q.minRep <= state.reputation &&
            (q.needFlag == null || state.has(q.needFlag)) &&
            (!q.once || "won_${q.id}" !in state.flags)
    }

    fun repGain(quest: QuestDef, success: Boolean): Int {
        if (!success) return 0
        return when {
            quest.boss && quest.rival -> 10
            quest.boss -> 8
            quest.rival -> 5
            else -> 3
        }
    }

    fun applyRewards(
        state: GameState,
        party: List<String>,
        quest: QuestDef,
        result: QuestResult,
        heroes: Map<String, HeroState>,
        inv: Map<String, Int>,
        gold: Int,
        success: Boolean,
    ): GameState {
        var flags = state.flags + "quest_${quest.id}" + (if (quest.place == "forest") "did_forest" else "did_town") + (if (success) "won_quest" else "lost_quest")
        if (success) {
            flags = flags + "won_${quest.id}"
            if (quest.chain != null) flags = flags + quest.chain
            if (quest.rival) flags = flags + "beat_vesper"
        }
        val pay = gold + if ("scout" in state.staff && (quest.id == "escort" || quest.rival) && success) gold / 10 else 0
        return state.copy(
            gold = state.gold + pay,
            heroes = heroes,
            inventory = inv,
            flags = flags,
            lastResult = result,
            phase = "evening",
            actionsLeft = state.eveningCap(),
            reputation = state.reputation + repGain(quest, success),
            expedition = null,
        )
    }

    fun settleBattle(state: GameState, battle: BattleState): Pair<GameState, QuestResult> {
        val quest = Heartbound.quest.getValue(battle.questId)
        val party = battle.party
        val success = battle.victory == true
        val chance = successChance(state, party, quest)
        val ko = battle.fighters.filter { it.isHero && it.hp <= 0 }.mapNotNull { it.heroId }
        val injured = ko.firstOrNull()
        var days = 0
        if (injured != null) {
            days = (if (success) 1 else 2) - (state.rooms + 1) / 2
            if (days < 1) days = 1
            if (state.rooms >= 3) days = 1
        }
        val gold = if (success) quest.gold else quest.gold / 5
        val xp = if (success) quest.xp else quest.xp / 4
        val remain = battle.fighters.filter { !it.isHero }.sumOf { it.hp }
        val loot = if (success && quest.loot != null && (remain + battle.round) % 3 == 0) quest.loot else null
        val leveled = mutableListOf<String>()
        var heroes = state.heroes
        party.forEach { id ->
            var h = heroes.getValue(id)
            val (next, up) = addXp(h, xp)
            h = next
            if (up) leveled += id
            if (id == injured) h = h.copy(injuredDays = days)
            val bond = if (success) 2 else 1
            h = h.copy(affection = (h.affection + bond).coerceIn(0, Heartbound.MAX_AFF))
            heroes = heroes + (id to h)
        }
        var inv = state.inventory
        if (loot != null) inv = inv + (loot to (inv[loot] ?: 0) + 1)
        val note = buildString {
            append(if (success) "The party returns with the job done." else "The party returns empty-handed, but together.")
            if (injured != null) append(" ${Heartbound.hero.getValue(injured).name} was hurt and needs $days day${if (days == 1) "" else "s"} to recover.")
            if (loot != null) append(" Found: ${Heartbound.gift[loot]?.name ?: Heartbound.gearById[loot]?.name ?: loot}.")
            val rg = repGain(quest, success)
            if (rg > 0) append(" The hall's name travels a little farther (+$rg reputation).")
        }
        val result = QuestResult(quest.id, success, chance, if (success) 0 else 99, gold, xp, leveled, injured, days, loot, note)
        return applyRewards(state, party, quest, result, heroes, inv, gold, success) to result
    }

    fun addAff(h: HeroState, delta: Int) = h.copy(affection = (h.affection + delta).coerceIn(0, Heartbound.MAX_AFF))

    fun spendInv(state: GameState, id: String, n: Int = 1): GameState? {
        val have = state.inv(id)
        if (have < n) return null
        val next = have - n
        val inv = if (next <= 0) state.inventory - id else state.inventory + (id to next)
        return state.copy(inventory = inv)
    }

    fun addInv(state: GameState, id: String, n: Int = 1) =
        state.copy(inventory = state.inventory + (id to state.inv(id) + n))

    fun eveningBonus(state: GameState) = (if (state.tavern >= 2) 1 else 0) + (if (state.tavern >= 3) 1 else 0) + (if ("cook" in state.staff) 1 else 0) + (if (state.library >= 2) 1 else 0)
}
