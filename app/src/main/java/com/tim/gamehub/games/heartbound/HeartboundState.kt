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
) {
    val def get() = Heartbound.hero.getValue(id)
    val ready get() = recruited && injuredDays <= 0
    fun atk(): Int {
        val g = Heartbound.gearById
        return def.baseAtk + (level - 1) * def.cls.atkGrowth +
            (weapon?.let { g[it]?.atk } ?: 0) + (accessory?.let { g[it]?.atk } ?: 0)
    }
    fun defn(): Int {
        val g = Heartbound.gearById
        return def.baseDef + (level - 1) * def.cls.defGrowth +
            (weapon?.let { g[it]?.def } ?: 0) + (accessory?.let { g[it]?.def } ?: 0)
    }
    fun power(): Double = atk() + defn() * 0.5
    fun aff() = affection.coerceIn(0, Heartbound.MAX_AFF)
    fun tier() = Heartbound.tier(aff())
}

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
) {
    fun h(id: String) = heroes.getValue(id)
    fun recruited() = Heartbound.heroes.map { h(it.id) }.filter { it.recruited }
    fun ready() = recruited().filter { it.ready }
    fun inv(id: String) = inventory[id] ?: 0
    fun has(flag: String) = flag in flags
    fun eveningCap() = if (tavern > 0) 2 else 1
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
    fun newGame(): GameState = GameState(
        heroes = Heartbound.heroes.associate { d ->
            d.id to HeroState(d.id, recruited = d.startRecruited, level = d.startLevel, affection = d.startAff)
        },
        inventory = mapOf("tea" to 1),
    )

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
        val next = state.copy(
            gold = state.gold + gold,
            heroes = heroes,
            inventory = inv,
            flags = state.flags + "quest_${quest.id}" + if (quest.place == "forest") "did_forest" else "did_town",
            lastResult = result,
            phase = "evening",
            actionsLeft = state.eveningCap(),
        )
        return next to result
    }

    fun turnIn(state: GameState): GameState {
        val heroes = state.heroes.mapValues { (_, h) ->
            if (h.injuredDays > 0) h.copy(injuredDays = (h.injuredDays - 1 - if (state.rooms >= 2 && h.injuredDays > 1) 1 else 0).coerceAtLeast(0)) else h
        }
        return state.copy(
            day = state.day + 1,
            phase = "morning",
            actionsLeft = 0,
            heroes = heroes,
            lastResult = null,
        )
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

    fun eveningBonus(state: GameState) = (if (state.tavern >= 2) 1 else 0) + (if (state.tavern >= 3) 1 else 0)
}
