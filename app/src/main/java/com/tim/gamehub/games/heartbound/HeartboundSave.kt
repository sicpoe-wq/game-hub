package com.tim.gamehub.games.heartbound

interface SaveStore {
    var data: String?
    val slotCount: Int get() = 3
    var lastSlot: Int
    var settings: HbSettings
    var seenLines: Set<String>
    fun slot(i: Int): String?
    fun writeSlot(i: Int, value: String?)
}

class MemorySave(initial: String? = null) : SaveStore {
    private val slots = arrayOfNulls<String>(3)
    override var lastSlot: Int = 0
    override var settings: HbSettings = HbSettings()
    override var seenLines: Set<String> = emptySet()
    init { slots[0] = initial }
    override var data: String?
        get() = slots.getOrNull(lastSlot)
        set(v) { if (lastSlot in slots.indices) slots[lastSlot] = v }
    override fun slot(i: Int) = slots.getOrNull(i)
    override fun writeSlot(i: Int, value: String?) { if (i in slots.indices) slots[i] = value }
}

class PrefsSave(ctx: android.content.Context) : SaveStore {
    private val p = ctx.getSharedPreferences("heartbound", android.content.Context.MODE_PRIVATE)
    init { migrate() }
    private fun migrate() {
        if (p.getBoolean("migrated_v14", false)) return
        val old = p.getString("save", null)
        val ed = p.edit()
        if (old != null && p.getString("slot_0", null) == null) {
            ed.putString("slot_0", old).putInt("last_slot", 0)
        }
        ed.putBoolean("migrated_v14", true).apply()
    }
    override var lastSlot: Int
        get() = p.getInt("last_slot", 0)
        set(v) { p.edit().putInt("last_slot", v).apply() }
    override var data: String?
        get() = slot(lastSlot)
        set(v) { writeSlot(lastSlot, v) }
    override fun slot(i: Int) = p.getString("slot_$i", null)
    override fun writeSlot(i: Int, value: String?) {
        val ed = p.edit().putString("slot_$i", value)
        // keep a backup of slot 0 in the v1.3 key so a downgrade cannot wipe Tim's original file
        if (i == 0 && value != null) ed.putString("save", value)
        ed.apply()
    }
    override var settings: HbSettings
        get() = HbSettings(p.getInt("text_speed", 2), p.getBoolean("skip_seen", false), p.getBoolean("fade_to_black", true))
        set(v) { p.edit().putInt("text_speed", v.textSpeed).putBoolean("skip_seen", v.skipSeen).putBoolean("fade_to_black", v.fadeToBlack).apply() }
    override var seenLines: Set<String>
        get() = p.getString("seen_lines", "")!!.split(",").filter { it.isNotBlank() }.toSet()
        set(v) { p.edit().putString("seen_lines", v.joinToString(",")).apply() }
}

object HeartboundSave {
    private fun esc(v: String) = v.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n")
    private fun unesc(v: String) = v.replace("\\n", "\n").replace("\\|", "|").replace("\\\\", "\\")

    fun encode(s: GameState): String = buildString {
        append("v=2\n")
        append("day=${s.day}|gold=${s.gold}|phase=${s.phase}|act=${s.actionsLeft}|rooms=${s.rooms}|train=${s.training}|tavern=${s.tavern}\n")
        val p = s.player
        append("P|${esc(p.name)}|${p.look}|${p.pronouns}|${p.origin}|${p.charm}|${p.wit}|${p.courage}\n")
        s.heroes.values.forEach { h ->
            append("H|${h.id}|${if (h.recruited) 1 else 0}|${h.level}|${h.xp}|${h.injuredDays}|${h.affection}|${h.weapon ?: ""}|${h.accessory ?: ""}|${h.seenEvents.joinToString(",")}\n")
        }
        s.inventory.forEach { (k, v) -> if (v > 0) append("I|$k|$v\n") }
        s.flags.forEach { append("F|$it\n") }
        s.seenTalks.forEach { append("T|$it\n") }
        if (s.achievements.isNotEmpty()) append("A|${s.achievements.joinToString(",")}\n")
        if (s.giftsGiven > 0) append("C|${s.giftsGiven}\n")
        if (s.lovedGifts.isNotEmpty()) append("K|${s.lovedGifts.joinToString(",")}\n")
        append("W|${s.world.season}|${s.world.weather}|${s.world.route ?: ""}|${s.world.chapter}\n")
        s.anchor?.let { a ->
            append("Y|${a.day}|${a.gold}|${a.aff.entries.joinToString(",") { "${it.key}=${it.value}" }}|${a.recruited.joinToString(",")}|${a.achievements.joinToString(",")}\n")
        }
        s.report?.let { r ->
            append("Q|${r.day}|${r.questTitle ?: ""}|${r.questSuccess ?: ""}|${r.goldDelta}|${r.aff.entries.joinToString(",") { "${it.key}=${it.value}" }}|${r.leveled.joinToString(",")}|${r.injured ?: ""}|${r.recruited.joinToString(",")}|${r.newAchievements.joinToString(",")}\n")
        }
        s.log.takeLast(300).forEach { append("G|${esc(it)}\n") }
        s.lastResult?.let { r ->
            append("R|${r.questId}|${if (r.success) 1 else 0}|${r.chance}|${r.roll}|${r.gold}|${r.xp}|${r.leveled.joinToString(",")}|${r.injuredId ?: ""}|${r.injuryDays}|${r.loot ?: ""}|${esc(r.note)}\n")
        }
    }

    fun decode(raw: String): GameState {
        var day = 1; var gold = Heartbound.START_GOLD; var phase = "morning"; var act = 0
        var rooms = 0; var train = 0; var tavern = 0
        val heroes = Heartbound.heroes.associate { it.id to HeroState(it.id) }.toMutableMap()
        val inv = mutableMapOf<String, Int>()
        val flags = mutableSetOf<String>()
        val talks = mutableSetOf<String>()
        var result: QuestResult? = null
        var player = Player()
        var ach = setOf<String>()
        var gifts = 0
        var loved = setOf<String>()
        var world = WorldState()
        var anchor: DayAnchor? = null
        var report: DayReport? = null
        val log = mutableListOf<String>()
        fun mapInts(raw: String) = raw.split(",").filter { it.contains("=") }.associate { val (k, v) = it.split("=", limit = 2); k to v.toInt() }
        raw.lineSequence().forEach { line ->
            if (line.startsWith("day=")) {
                line.split("|").forEach { part ->
                    val eq = part.indexOf('='); if (eq < 0) return@forEach
                    val k = part.substring(0, eq); val v = part.substring(eq + 1)
                    when (k) {
                        "day" -> day = v.toInt()
                        "gold" -> gold = v.toInt()
                        "phase" -> phase = v
                        "act" -> act = v.toInt()
                        "rooms" -> rooms = v.toInt()
                        "train" -> train = v.toInt()
                        "tavern" -> tavern = v.toInt()
                    }
                }
            } else if (line.startsWith("P|")) {
                val p = line.split("|")
                player = Player(unesc(p[1]), p[2], p[3], p[4], p[5].toInt(), p[6].toInt(), p[7].toInt())
            } else if (line.startsWith("H|")) {
                val p = line.split("|")
                val id = p[1]
                heroes[id] = HeroState(
                    id = id, recruited = p[2] == "1", level = p[3].toInt(), xp = p[4].toInt(),
                    injuredDays = p[5].toInt(), affection = p[6].toInt(),
                    weapon = p[7].ifBlank { null }, accessory = p[8].ifBlank { null },
                    seenEvents = p.getOrElse(9) { "" }.split(",").filter { it.isNotBlank() }.toSet(),
                )
            } else if (line.startsWith("I|")) {
                val p = line.split("|"); inv[p[1]] = p[2].toInt()
            } else if (line.startsWith("F|")) flags += line.substring(2)
            else if (line.startsWith("T|")) talks += line.substring(2)
            else if (line.startsWith("A|")) ach = line.substring(2).split(",").filter { it.isNotBlank() }.toSet()
            else if (line.startsWith("C|")) gifts = line.substring(2).toInt()
            else if (line.startsWith("K|")) loved = line.substring(2).split(",").filter { it.isNotBlank() }.toSet()
            else if (line.startsWith("W|")) {
                val p = line.split("|")
                world = WorldState(p.getOrElse(1) { "spring" }, p.getOrElse(2) { "clear" }, p.getOrElse(3) { "" }.ifBlank { null }, p.getOrElse(4) { "0" }.toIntOrNull() ?: 0)
            } else if (line.startsWith("Y|")) {
                val p = line.split("|")
                anchor = DayAnchor(p[1].toInt(), p[2].toInt(), mapInts(p.getOrElse(3) { "" }), p.getOrElse(4) { "" }.split(",").filter { it.isNotBlank() }.toSet(), p.getOrElse(5) { "" }.split(",").filter { it.isNotBlank() }.toSet())
            } else if (line.startsWith("Q|")) {
                val p = line.split("|")
                val suc = p.getOrElse(3) { "" }
                report = DayReport(p[1].toInt(), p[2].ifBlank { null }, if (suc == "") null else suc.toBooleanStrictOrNull() ?: (suc == "true" || suc == "1"), p[4].toInt(), mapInts(p.getOrElse(5) { "" }), p.getOrElse(6) { "" }.split(",").filter { it.isNotBlank() }, p.getOrElse(7) { "" }.ifBlank { null }, p.getOrElse(8) { "" }.split(",").filter { it.isNotBlank() }, p.getOrElse(9) { "" }.split(",").filter { it.isNotBlank() })
            } else if (line.startsWith("G|")) log += unesc(line.substring(2))
            else if (line.startsWith("R|")) {
                val p = line.split("|")
                result = QuestResult(
                    questId = p[1], success = p[2] == "1", chance = p[3].toInt(), roll = p[4].toInt(),
                    gold = p[5].toInt(), xp = p[6].toInt(),
                    leveled = p[7].split(",").filter { it.isNotBlank() },
                    injuredId = p[8].ifBlank { null }, injuryDays = p[9].toInt(),
                    loot = p[10].ifBlank { null }, note = unesc(p.getOrElse(11) { "" }),
                )
            }
        }
        return GameState(day, gold, phase, act, rooms, train, tavern, heroes, inv, flags, talks, result, player, ach, log, report, gifts, loved, world, anchor)
    }

    fun preview(raw: String?): SlotInfo? {
        if (raw.isNullOrBlank()) return null
        return try {
            val s = decode(raw)
            SlotInfo(0, false, s.player.name, s.day, s.gold, s.player.look, s.player.originDef().name, s.phase)
        } catch (_: Exception) { null }
    }
}
