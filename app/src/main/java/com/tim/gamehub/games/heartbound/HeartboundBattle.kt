package com.tim.gamehub.games.heartbound

import kotlin.random.Random

data class SkillDef(
    val id: String,
    val name: String,
    val power: Int,
    val kind: String,
    val target: String,
    val desc: String,
    val combo: Boolean = false,
)

data class EnemyDef(
    val id: String,
    val name: String,
    val art: String,
    val hp: Int,
    val atk: Int,
    val def: Int,
    val skills: List<String>,
    val boss: Boolean = false,
)

data class ComboDef(
    val id: String,
    val name: String,
    val classes: Set<HeroClass>,
    val minAff: Int,
    val skillId: String,
)

data class Fighter(
    val id: String,
    val name: String,
    val isHero: Boolean,
    val heroId: String? = null,
    val enemyId: String? = null,
    val cls: HeroClass? = null,
    val art: String? = null,
    val hp: Int,
    val maxHp: Int,
    val atk: Int,
    val def: Int,
    val defending: Boolean = false,
    val stunned: Boolean = false,
    val bless: Int = 0,
)

data class BattleState(
    val questId: String,
    val party: List<String>,
    val fighters: List<Fighter>,
    val currentId: String,
    val round: Int = 1,
    val log: List<String> = emptyList(),
    val auto: Boolean = false,
    val finished: Boolean = false,
    val victory: Boolean? = null,
    val flashId: String? = null,
    val flashKind: String? = null,
    val usedCombo: Boolean = false,
    val anim: Int = 0,
)

object Battle {
    val skills = listOf(
        SkillDef("strike", "Strike", 6, "attack", "enemy", "A measured blow."),
        SkillDef("shield", "Shield", 0, "guard", "self", "Raise a guard and halve the next hit."),
        SkillDef("wall", "Shield Wall", 0, "guard", "allAllies", "The party braces together."),
        SkillDef("bolt", "Arcane Bolt", 8, "attack", "enemy", "A bright, precise lance of light."),
        SkillDef("starflare", "Starflare", 5, "attack", "allEnemies", "Living starlight fans across the field."),
        SkillDef("mend", "Mend", 14, "heal", "ally", "Knit a wound with careful light."),
        SkillDef("stab", "Stab", 7, "attack", "enemy", "Find the gap and take it."),
        SkillDef("feint", "Feint", 4, "stun_attack", "enemy", "A lie in the footwork. They miss a turn."),
        SkillDef("smoke", "Smoke", 0, "guard", "self", "Vanish a moment and take the next hit softer."),
        SkillDef("shot", "True Shot", 7, "attack", "enemy", "One arrow, no wasted breath."),
        SkillDef("pin", "Pin", 5, "stun_attack", "enemy", "Nail a foot to the ground."),
        SkillDef("volley", "Volley", 4, "attack", "allEnemies", "A rain of shafts."),
        SkillDef("smite", "Smite", 6, "attack", "enemy", "A merciful, unyielding blow."),
        SkillDef("heal", "Heal", 18, "heal", "ally", "A promise kept in light."),
        SkillDef("bless", "Bless", 6, "buff", "ally", "A ward and a little warmth."),
        SkillDef("bite", "Bite", 5, "attack", "enemy", "Teeth and intent."),
        SkillDef("cleave", "Cleave", 6, "attack", "allEnemies", "A wide, ugly arc."),
        SkillDef("slash", "Slash", 6, "attack", "enemy", "Steel, quickly."),
        SkillDef("tithe", "Tithe of Light", 7, "attack", "allEnemies", "The lantern drinks."),
        SkillDef("scratch", "Scratch", 3, "attack", "enemy", "Dignity, interrupted."),
        SkillDef("oath", "Oath of Mercy", 16, "heal", "allAllies", "Knight and cleric, one vow.", combo = true),
        SkillDef("starcut", "Starlight Cut", 16, "attack", "enemy", "A mage's light on a rogue's blade.", combo = true),
        SkillDef("shadows", "Twin Shadows", 7, "stun_attack", "allEnemies", "Two silhouettes, one strike.", combo = true),
        SkillDef("living", "Living Light", 8, "heal_attack", "all", "Heal the party and scorch the rest.", combo = true),
        SkillDef("hold", "Hold the Line", 5, "guard_attack", "all", "Brace, then answer.", combo = true),
    ).associateBy { it.id }

    val enemies = listOf(
        EnemyDef("goblin", "Grain Goblin", "enemy_goblin", 36, 8, 4, listOf("strike")),
        EnemyDef("wolf", "Ridge Wolf", "enemy_wolf", 44, 10, 5, listOf("bite")),
        EnemyDef("bandit", "Road Bandit", "enemy_bandit", 40, 9, 5, listOf("slash")),
        EnemyDef("shade", "Ash Shade", "enemy_shade", 48, 11, 6, listOf("strike", "tithe")),
        EnemyDef("vesper", "Iron Vesper Duelist", "enemy_vesper", 72, 13, 8, listOf("slash", "feint")),
        EnemyDef("goblin_king", "The Coin Crown", "enemy_goblin_king", 96, 14, 8, listOf("cleave", "strike"), boss = true),
        EnemyDef("covenant", "Covenant Envoy", "enemy_covenant", 110, 15, 9, listOf("tithe", "strike"), boss = true),
        EnemyDef("cat", "The Mayor's Cat", "enemy_cat", 26, 6, 3, listOf("scratch")),
    ).associateBy { it.id }

    val combos = listOf(
        ComboDef("oath", "Oath of Mercy", setOf(HeroClass.KNIGHT, HeroClass.CLERIC), 40, "oath"),
        ComboDef("starcut", "Starlight Cut", setOf(HeroClass.MAGE, HeroClass.ROGUE), 40, "starcut"),
        ComboDef("shadows", "Twin Shadows", setOf(HeroClass.RANGER, HeroClass.ROGUE), 40, "shadows"),
        ComboDef("living", "Living Light", setOf(HeroClass.MAGE, HeroClass.CLERIC), 40, "living"),
        ComboDef("hold", "Hold the Line", setOf(HeroClass.KNIGHT, HeroClass.RANGER), 40, "hold"),
    )

    fun classSkills(cls: HeroClass) = when (cls) {
        HeroClass.KNIGHT -> listOf("strike", "shield", "wall")
        HeroClass.MAGE -> listOf("bolt", "starflare", "mend")
        HeroClass.RANGER -> listOf("shot", "pin", "volley")
        HeroClass.ROGUE -> listOf("stab", "feint", "smoke")
        HeroClass.CLERIC -> listOf("smite", "heal", "bless")
    }.map { skills.getValue(it) }

    fun maxHp(h: HeroState) = 22 + h.defn() * 3 + h.level * 5

    fun defaultEnemyIds(quest: QuestDef): List<String> = quest.enemies.ifEmpty {
        when (quest.id) {
            "cat" -> listOf("cat")
            "herbs" -> listOf("wolf")
            "escort" -> listOf("bandit")
            "goblin" -> listOf("goblin")
            "ledger" -> listOf("bandit")
            "wolves" -> listOf("wolf", "wolf")
            "watch" -> listOf("bandit")
            "chapel" -> listOf("shade")
            "vault" -> listOf("bandit", "bandit")
            "bandits" -> listOf("bandit", "bandit")
            "festival" -> listOf("bandit")
            "shadow" -> listOf("shade", "shade")
            else -> listOf(if (quest.place == "forest") "goblin" else "bandit")
        }
    }

    fun start(state: GameState, party: List<String>, quest: QuestDef, auto: Boolean = false): BattleState {
        val train = 1.0 + state.training * 0.06
        val haste = state.expedition?.flags?.contains("travel_haste") == true
        val weary = state.expedition?.flags?.contains("travel_weary") == true
        val heroes = party.map { id ->
            val h = state.h(id)
            val hp = (maxHp(h) * (if (weary) 0.9 else 1.0)).toInt().coerceAtLeast(8)
            Fighter(
                id = id, name = h.def.name.split(" ").first(), isHero = true, heroId = id,
                cls = h.def.cls, art = id, hp = hp, maxHp = hp,
                atk = (h.atk() * train).toInt() + if (haste) 2 else 0, def = h.defn(),
            )
        }
        val counts = mutableMapOf<String, Int>()
        val foes = defaultEnemyIds(quest).map { eid ->
            val d = enemies.getValue(eid)
            val n = (counts[eid] ?: 0) + 1
            counts[eid] = n
            val unique = if (n == 1 && defaultEnemyIds(quest).count { it == eid } == 1) eid else "$eid#$n"
            Fighter(
                id = unique, name = d.name, isHero = false, enemyId = eid, art = d.art,
                hp = d.hp, maxHp = d.hp, atk = d.atk, def = d.def,
            )
        }
        val fighters = heroes + foes
        return BattleState(
            questId = quest.id, party = party, fighters = fighters,
            currentId = heroes.first().id, auto = auto,
            log = listOf("The fight is joined."),
        )
    }

    fun current(b: BattleState) = b.fighters.firstOrNull { it.id == b.currentId }

    fun living(b: BattleState, heroes: Boolean) = b.fighters.filter { it.isHero == heroes && it.hp > 0 }

    fun availableSkills(state: GameState, b: BattleState): List<SkillDef> {
        val actor = current(b) ?: return emptyList()
        if (!actor.isHero || actor.cls == null) {
            val eid = actor.enemyId ?: return emptyList()
            return (enemies[eid]?.skills ?: listOf("strike")).mapNotNull { skills[it] }
        }
        val basic = classSkills(actor.cls)
        val combo = if (b.usedCombo) emptyList() else combos.filter { def ->
            val members = b.fighters.filter { it.isHero && it.hp > 0 && it.cls in def.classes }
            def.classes.all { c -> members.any { it.cls == c } } &&
                def.classes.all { c ->
                    val id = members.first { it.cls == c }.heroId!!
                    state.h(id).aff() >= def.minAff
                }
        }.map { skills.getValue(it.skillId) }
        return basic + combo
    }

    fun comboReady(state: GameState, b: BattleState) = availableSkills(state, b).filter { it.combo }

    fun damage(actor: Fighter, target: Fighter, skill: SkillDef): Int {
        val atk = actor.atk + skill.power + if (actor.bless > 0) 4 else 0
        val raw = (atk - target.def / 2).coerceAtLeast(1)
        return if (target.defending) (raw + 1) / 2 else raw
    }

    fun targetsFor(b: BattleState, actor: Fighter, skill: SkillDef, chosen: String?): List<Fighter> {
        val allies = living(b, actor.isHero)
        val foes = living(b, !actor.isHero)
        return when (skill.target) {
            "self" -> listOf(actor)
            "allAllies" -> allies
            "allEnemies" -> foes
            "all" -> allies + foes
            "ally" -> listOf(allies.firstOrNull { it.id == chosen } ?: allies.minByOrNull { it.hp } ?: actor)
            else -> listOf(foes.firstOrNull { it.id == chosen } ?: foes.minByOrNull { it.hp } ?: return emptyList())
        }
    }

    private fun write(fighters: List<Fighter>, id: String, fn: (Fighter) -> Fighter) =
        fighters.map { if (it.id == id) fn(it) else it }

    fun act(b: BattleState, skill: SkillDef, targetId: String?, state: GameState? = null): BattleState {
        if (b.finished) return b
        var actor = current(b) ?: return b.copy(finished = true, victory = false)
        var fighters = b.fighters
        if (actor.stunned) {
            fighters = write(fighters, actor.id) { it.copy(stunned = false, defending = false) }
            return conclude(b.copy(fighters = fighters, log = b.log + "${actor.name} is caught off-balance and misses the turn.", flashId = actor.id, flashKind = "stun", anim = b.anim + 1), usedCombo = b.usedCombo)
        }
        // drop last turn's guard as this action begins, then tick bless
        fighters = write(fighters, actor.id) { it.copy(defending = false, bless = (it.bless - if (it.bless > 0) 1 else 0).coerceAtLeast(0)) }
        actor = fighters.first { it.id == actor.id }
        val targets = targetsFor(b.copy(fighters = fighters), actor, skill, targetId)
        var log = "${actor.name} uses ${skill.name}."
        var flashId = targets.firstOrNull()?.id ?: actor.id
        var flashKind = "hit"
        when (skill.kind) {
            "attack", "stun_attack" -> targets.forEach { t ->
                val dmg = damage(actor, t, skill)
                fighters = write(fighters, t.id) { it.copy(hp = (it.hp - dmg).coerceAtLeast(0), stunned = it.stunned || skill.kind == "stun_attack") }
                log = "${actor.name} uses ${skill.name} on ${t.name} for $dmg."
                flashId = t.id; flashKind = "hit"
            }
            "heal" -> targets.forEach { t ->
                val amt = skill.power + actor.atk / 3
                fighters = write(fighters, t.id) { it.copy(hp = (it.hp + amt).coerceAtMost(it.maxHp)) }
                log = "${actor.name} heals ${t.name} for $amt."
                flashId = t.id; flashKind = "heal"
            }
            "guard" -> {
                targets.forEach { t -> fighters = write(fighters, t.id) { it.copy(defending = true) } }
                log = "${actor.name} raises a guard."
                flashId = actor.id; flashKind = "guard"
            }
            "buff" -> targets.forEach { t ->
                val amt = 6
                fighters = write(fighters, t.id) { it.copy(bless = 2, hp = (it.hp + amt).coerceAtMost(it.maxHp)) }
                log = "${actor.name} blesses ${t.name}."
                flashId = t.id; flashKind = "heal"
            }
            "heal_attack" -> {
                living(b.copy(fighters = fighters), actor.isHero).forEach { t ->
                    val amt = skill.power + 6
                    fighters = write(fighters, t.id) { it.copy(hp = (it.hp + amt).coerceAtMost(it.maxHp)) }
                }
                living(b.copy(fighters = fighters), !actor.isHero).forEach { t ->
                    val dmg = damage(actor, t, skill)
                    fighters = write(fighters, t.id) { it.copy(hp = (it.hp - dmg).coerceAtLeast(0)) }
                    flashId = t.id
                }
                log = "${actor.name} calls ${skill.name}."
                flashKind = "heal"
            }
            "guard_attack" -> {
                living(b.copy(fighters = fighters), actor.isHero).forEach { t ->
                    fighters = write(fighters, t.id) { it.copy(defending = true) }
                }
                living(b.copy(fighters = fighters), !actor.isHero).forEach { t ->
                    val dmg = damage(actor, t, skill)
                    fighters = write(fighters, t.id) { it.copy(hp = (it.hp - dmg).coerceAtLeast(0)) }
                    flashId = t.id
                }
                log = "${actor.name} holds the line."
                flashKind = "guard"
            }
        }
        return conclude(
            b.copy(fighters = fighters, log = (b.log + log).takeLast(8), flashId = flashId, flashKind = flashKind, anim = b.anim + 1),
            usedCombo = b.usedCombo || skill.combo,
        )
    }

    private fun conclude(b: BattleState, usedCombo: Boolean): BattleState {
        val heroes = living(b, true)
        val foes = living(b, false)
        val base = b.copy(usedCombo = usedCombo)
        return when {
            foes.isEmpty() -> base.copy(finished = true, victory = true, log = (base.log + "The field is yours.").takeLast(8))
            heroes.isEmpty() -> base.copy(finished = true, victory = false, log = (base.log + "The party is driven back.").takeLast(8))
            else -> advance(base)
        }
    }

    private fun advance(b: BattleState): BattleState {
        val order = b.fighters.filter { it.hp > 0 }
        if (order.isEmpty()) return b.copy(finished = true, victory = false)
        val idx = order.indexOfFirst { it.id == b.currentId }.let { if (it < 0) 0 else it }
        val next = (idx + 1) % order.size
        val round = if (next == 0) b.round + 1 else b.round
        return b.copy(currentId = order[next].id, round = round)
    }

    fun autoPick(state: GameState, b: BattleState): Pair<SkillDef, String?> {
        val actor = current(b) ?: return skills.getValue("strike") to null
        val kit = availableSkills(state, b)
        val allies = living(b, actor.isHero)
        val foes = living(b, !actor.isHero)
        val hurt = allies.filter { it.hp * 5 <= it.maxHp * 2 }.minByOrNull { it.hp }
        val heal = kit.firstOrNull { it.kind == "heal" || it.target == "allAllies" && it.kind == "heal" }
        if (hurt != null && heal != null) return heal to if (heal.target == "allAllies") null else hurt.id
        val combo = kit.firstOrNull { it.combo }
        if (combo != null && foes.isNotEmpty()) return combo to foes.minByOrNull { it.hp }!!.id
        val atk = kit.filter { it.kind == "attack" || it.kind == "stun_attack" || it.kind == "heal_attack" || it.kind == "guard_attack" }
        val best = atk.maxByOrNull { it.power } ?: kit.firstOrNull() ?: skills.getValue("strike")
        return best to foes.minByOrNull { it.hp }?.id
    }

    fun autoStep(state: GameState, b: BattleState): BattleState {
        if (b.finished) return b
        val (sk, tgt) = autoPick(state, b)
        return act(b, sk, tgt, state)
    }

    fun runAuto(state: GameState, party: List<String>, quest: QuestDef, maxSteps: Int = 120): BattleState {
        var b = start(state, party, quest, auto = true)
        var n = 0
        while (!b.finished && n++ < maxSteps) b = autoStep(state, b)
        return b
    }
}

object Travel {
    fun pick(state: GameState, party: List<String>, quest: QuestDef, rng: Random): Scene {
        val hid = party.firstOrNull { state.heroes[it]?.recruited == true } ?: party.firstOrNull()
        val speaker = hid?.let { Heartbound.hero[it]?.name } ?: "A companion"
        val pool = listOf(fork(hid, speaker), traveler(hid, speaker), cache(hid, speaker), camp(hid, speaker), scout(hid, speaker), weather(hid, speaker))
        return pool[rng.nextInt(pool.size)]
    }

    fun preview(): Scene = camp("kael", "Kael")

    fun pendingBranch(state: GameState): Scene? {
        if ("ledger_found" !in state.flags) return null
        if ("ledger_mercy" in state.flags || "ledger_law" in state.flags) return null
        return ledgerChoice
    }

    val ledgerChoice = sc(
        "ledger_choice", null, "town",
        N("The clerk turns the recovered book in both hands, then looks at you."),
        N("\"There's a name in the margin. A young thief. I can send it to the watch, or I can forget I can read.\""),
        title = "The Name in the Margin",
        choices = listOf(
            Choice("Let them start over. Forget the name.", flag = "ledger_mercy", next = listOf(N("The clerk closes the book. \"Then I never saw it.\""))),
            Choice("The law is the law. Send it to the watch.", flag = "ledger_law", next = listOf(N("The clerk nods, already reaching for a warrant slip."))),
        ),
    )

    private fun fork(hid: String?, speaker: String) = sc(
        "travel_fork", hid, "forest",
        N("The road splits under a leaning oak. One path is shorter, and badly kept. The other is honest and long."),
        L(speaker, "Your call, Guildmaster. I know which one I'd rather sleep on."),
        title = "A Fork in the Road",
        choices = listOf(
            Choice("Take the short path. Courage will have to do.", flag = "travel_haste", next = listOf(N("The shortcut is rougher than it looks, but you make the time."), L(speaker, "We'll arrive sharp. Or at least sooner.")), stat = "courage", statMin = 2),
            Choice("Take the short path anyway.", flag = "travel_haste", next = listOf(N("You cut through the brambles and pick burrs out of your coat for an hour."))),
            Choice("Keep to the honest road.", next = listOf(N("You take the longer way. No one is sorry."))),
        ),
    )

    private fun traveler(hid: String?, speaker: String) = sc(
        "travel_mercy", hid, "forest",
        N("A traveler sits against a milestone, one boot off, trying to look as if they meant to rest."),
        L(speaker, "We could help. We could also keep our hours."),
        title = "Someone on the Milestone",
        choices = listOf(
            Choice("Stop and bind the ankle.", 2, "travel_help", next = listOf(N("You spend an hour and a clean bandage. The traveler presses a grateful hand to yours."))),
            Choice("Point them toward the next well and press on.", next = listOf(N("They nod, already reaching for the boot. The road does not wait."))),
        ),
    )

    private fun cache(hid: String?, speaker: String) = sc(
        "travel_cache", hid, "forest",
        N("A mossy hollow holds a cracked strongbox someone was in too much of a hurry to hide well."),
        L(speaker, "If we don't take it, the next rain will."),
        title = "A Forgotten Cache",
        choices = listOf(
            Choice("Open it. Carefully.", flag = "travel_cache", next = listOf(N("A handful of coins and a dry map. The hall can use both."))),
            Choice("Leave it. Bad luck likes loose lids.", next = listOf(N("You cover it back with moss and walk on."))),
        ),
    )

    fun camp(hid: String?, speaker: String) = sc(
        "travel_camp", hid, "forest",
        N("You make a small fire off the path. The trees hold the smoke, and for a little while the work is only the work of staying warm."),
        L(speaker, "I forget, sometimes, that the road can be kind.", "smile"),
        title = "A Fire off the Path",
        choices = listOf(
            Choice("Sit with them until the fire burns low.", 3, next = listOf(L(speaker, "Don't make a habit of looking at me like that. I'll start expecting it.", "shy"))),
            Choice("Keep the watch and let them sleep.", 1, next = listOf(L(speaker, "Wake me for the second half. I mean that."))),
        ),
    )

    private fun scout(hid: String?, speaker: String) = sc(
        "travel_vesper", hid, "town",
        N("Two figures in black-and-brass stand in the road as if they had rented it. The iron sunburst of Iron Vesper sits on one cloak."),
        L(speaker, "Rivals. They'll want the same contract, or a look at our faces."),
        title = "Iron Vesper on the Road",
        choices = listOf(
            Choice("Talk. There is no law against courtesy.", flag = "travel_vesper_talk", next = listOf(N("They give you a measuring smile and step aside. \"Another time, hall.\""))),
            Choice("Leave the road and go around.", flag = "travel_vesper_hid", next = listOf(N("You cut through a copse and come out ahead of them, unseen, or nearly."))),
        ),
    )

    private fun weather(hid: String?, speaker: String) = sc(
        "travel_weather", hid, "forest",
        N("The sky opens without asking. The path becomes a trough, and every step is a negotiation."),
        L(speaker, "We'll be damp when we arrive. I can live with that. I would prefer not to be late as well."),
        title = "A Hard Rain",
        choices = listOf(
            Choice("Push through and take the weather.", flag = "travel_weary", next = listOf(N("You arrive wet, a little poorer in patience, and on time."))),
            Choice("Wait the worst of it under the pines.", next = listOf(N("The rain eases. You lose an hour and keep your feet."))),
        ),
    )
}
