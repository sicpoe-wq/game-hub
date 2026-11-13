package com.tim.gamehub.games.heartbound

interface SaveStore { var data: String? }
class MemorySave(override var data: String? = null) : SaveStore

object HeartboundSave {
    fun encode(s: GameState): String = buildString {
        fun esc(v: String) = v.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n")
        append("v=1\n")
        append("day=${s.day}|gold=${s.gold}|phase=${s.phase}|act=${s.actionsLeft}|rooms=${s.rooms}|train=${s.training}|tavern=${s.tavern}\n")
        s.heroes.values.forEach { h ->
            append("H|${h.id}|${if (h.recruited) 1 else 0}|${h.level}|${h.xp}|${h.injuredDays}|${h.affection}|${h.weapon ?: ""}|${h.accessory ?: ""}|${h.seenEvents.joinToString(",")}\n")
        }
        s.inventory.forEach { (k, v) -> if (v > 0) append("I|$k|$v\n") }
        s.flags.forEach { append("F|$it\n") }
        s.seenTalks.forEach { append("T|$it\n") }
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
            } else if (line.startsWith("H|")) {
                val p = line.split("|")
                val id = p[1]
                heroes[id] = HeroState(
                    id = id,
                    recruited = p[2] == "1",
                    level = p[3].toInt(),
                    xp = p[4].toInt(),
                    injuredDays = p[5].toInt(),
                    affection = p[6].toInt(),
                    weapon = p[7].ifBlank { null },
                    accessory = p[8].ifBlank { null },
                    seenEvents = p.getOrElse(9) { "" }.split(",").filter { it.isNotBlank() }.toSet(),
                )
            } else if (line.startsWith("I|")) {
                val p = line.split("|"); inv[p[1]] = p[2].toInt()
            } else if (line.startsWith("F|")) flags += line.substring(2)
            else if (line.startsWith("T|")) talks += line.substring(2)
            else if (line.startsWith("R|")) {
                val p = line.split("|")
                fun unesc(v: String) = v.replace("\\n", "\n").replace("\\|", "|").replace("\\\\", "\\")
                result = QuestResult(
                    questId = p[1], success = p[2] == "1", chance = p[3].toInt(), roll = p[4].toInt(),
                    gold = p[5].toInt(), xp = p[6].toInt(),
                    leveled = p[7].split(",").filter { it.isNotBlank() },
                    injuredId = p[8].ifBlank { null }, injuryDays = p[9].toInt(),
                    loot = p[10].ifBlank { null }, note = unesc(p.getOrElse(11) { "" }),
                )
            }
        }
        return GameState(day, gold, phase, act, rooms, train, tavern, heroes, inv, flags, talks, result)
    }
}

class PrefsSave(ctx: android.content.Context) : SaveStore {
    private val p = ctx.getSharedPreferences("heartbound", android.content.Context.MODE_PRIVATE)
    override var data: String?
        get() = p.getString("save", null)
        set(v) { p.edit().putString("save", v).apply() }
}
