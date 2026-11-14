package com.tim.gamehub.games.heartbound

data class PronounSet(val id: String, val label: String, val they: String, val them: String, val their: String, val theirs: String, val themself: String, val are: String, val have: String) {
    companion object {
        val ALL = listOf(
            PronounSet("they", "they / them", "they", "them", "their", "theirs", "themself", "are", "have"),
            PronounSet("she", "she / her", "she", "her", "her", "hers", "herself", "is", "has"),
            PronounSet("he", "he / him", "he", "him", "his", "his", "himself", "is", "has"),
        )
        fun of(id: String) = ALL.firstOrNull { it.id == id } ?: ALL.first()
    }
}

data class LookDef(val id: String, val label: String, val blurb: String)
data class OriginDef(val id: String, val name: String, val blurb: String, val charm: Int, val wit: Int, val courage: Int)

data class Player(
    val name: String = "Alex",
    val look: String = "a",
    val pronouns: String = "they",
    val origin: String = "wanderer",
    val charm: Int = 1,
    val wit: Int = 1,
    val courage: Int = 1,
) {
    fun pr() = PronounSet.of(pronouns)
    fun originDef() = Content.origins.first { it.id == origin }
    fun lookDef() = Content.looks.firstOrNull { it.id == look } ?: Content.looks.first()
    fun stat(id: String) = when (id) { "charm" -> charm; "wit" -> wit; "courage" -> courage; else -> 0 }
    fun gain(id: String) = when (id) {
        "charm" -> copy(charm = (charm + 1).coerceAtMost(5))
        "wit" -> copy(wit = (wit + 1).coerceAtMost(5))
        "courage" -> copy(courage = (courage + 1).coerceAtMost(5))
        else -> this
    }
}

data class Achievement(val id: String, val title: String, val desc: String)
data class HbSettings(val textSpeed: Int = 2, val skipSeen: Boolean = false, val fadeToBlack: Boolean = true) {
    /** 0 = instant, 1 = fast, 2 = normal, 3 = slow */
    fun delayMs() = when (textSpeed) { 0 -> 0; 1 -> 8; 2 -> 16; else -> 28 }
    fun speedLabel() = when (textSpeed) { 0 -> "Instant"; 1 -> "Fast"; 2 -> "Normal"; else -> "Slow" }
}

data class DayAnchor(val day: Int, val gold: Int, val aff: Map<String, Int>, val recruited: Set<String>, val achievements: Set<String>)
data class DayReport(
    val day: Int,
    val questTitle: String?,
    val questSuccess: Boolean?,
    val goldDelta: Int,
    val aff: Map<String, Int>,
    val leveled: List<String>,
    val injured: String?,
    val recruited: List<String>,
    val newAchievements: List<String>,
)

/** Hooks later stages can hang off without rewriting the save format. */
data class WorldState(
    val season: String = "spring",
    val weather: String = "clear",
    val route: String? = null,
    val chapter: Int = 0,
)

data class SlotInfo(val index: Int, val empty: Boolean, val name: String = "", val day: Int = 0, val gold: Int = 0, val look: String = "a", val origin: String = "", val phase: String = "")

object Content {
    val looks = listOf(
        LookDef("a", "Look A", "Short chestnut hair, a calm face, the burgundy coat of the hall."),
        LookDef("b", "Look B", "Long dark hair, a steady smile, the same guildmaster's coat."),
        LookDef("c", "Look C", "Ash-blond hair and a quieter kind of presence."),
        LookDef("d", "Look D", "Dark braids, a gold ring, and the same burgundy coat."),
    )
    val origins = listOf(
        OriginDef("wanderer", "The Path", "You arrived with a pack, a key, and no house behind you. The road taught you a little of everything.", 1, 1, 1),
        OriginDef("soldier", "The Banner", "You served a hold and learned when an order should be refused. The habit of courage came with you.", 1, 0, 2),
        OriginDef("scholar", "The Archive", "You were trained among books and arguments. You notice the sentence other people skip.", 1, 2, 0),
        OriginDef("merchant", "The River Road", "You bargained along the water and learned how a kind word opens more doors than a blade.", 2, 1, 0),
    )
    val achievements = listOf(
        Achievement("first_dawn", "First Dawn", "Begin life as guildmaster."),
        Achievement("first_job", "First Job", "Complete a quest successfully."),
        Achievement("came_home", "Came Home Short", "Return from a quest that did not go as planned."),
        Achievement("a_guest", "A Guest at the Door", "Recruit a new hero."),
        Achievement("full_hall", "A Full Hall", "Recruit every hero."),
        Achievement("friend", "Someone to Trust", "Reach Friend with anyone."),
        Achievement("close", "Close Company", "Reach Close with anyone."),
        Achievement("cherished", "Cherished", "Reach Cherished with anyone."),
        Achievement("heartbound", "Heartbound", "Reach the Heartbound bond."),
        Achievement("loved_gift", "Exactly Right", "Give a gift someone truly loves."),
        Achievement("patron", "Local Patron", "Buy something from the market."),
        Achievement("mason", "Raising the Hall", "Improve a wing of the guild."),
        Achievement("week", "A Week in Lumenreach", "See the seventh morning."),
        Achievement("named", "A Name on the Door", "The hall knows what to call you."),
        Achievement("promised", "A Promise Made", "Choose an exclusive route."),
        Achievement("a_vow", "A Vow", "Accept a proposal."),
        Achievement("an_ending", "A Life Chosen", "Reach a partner's ending."),
        Achievement("lanterns", "Under the Lanterns", "Share the Festival of Lanterns."),
        Achievement("shore", "A Day at the Shore", "Spend Shore Day with a companion."),
        Achievement("harvest_dance", "The Harvest Dance", "Share the harvest dance."),
        Achievement("a_light_kept", "A Light Kept", "Finish the first chapter of Lumenreach's story."),
        Achievement("bedside", "Bedside", "Sit with an injured hero."),
    )
    val achById = achievements.associateBy { it.id }

    fun giftHint(hero: HeroDef, aff: Int): String = when {
        aff >= AffTier.FRIEND.min ->
            "Loves ${hero.love.joinToString { Heartbound.gift[it]?.name ?: it }}. Likes ${hero.like.joinToString { Heartbound.gift[it]?.name ?: it }}."
        aff >= 8 -> vagueHint(hero)
        else -> "Spend an evening with them. People reveal themselves slowly."
    }

    fun vagueHint(hero: HeroDef) = when (hero.id) {
        "kael" -> "He keeps a stone in his kit for the edge of a blade. Practical gifts land."
        "mira" -> "She lights up at paper, ink, and anything that maps the sky."
        "rin" -> "Sweets, and tools that open doors. He pretends not to care."
        "sera" -> "Things that grew, or that keep a trail honest."
        "yuna" -> "Herbs, seeds, and anything that heals without a ledger."
        else -> "Pay attention to what their hands go to."
    }
}

object Voice {
    fun apply(text: String, p: Player): String {
        val n = p.name.ifBlank { "Guildmaster" }
        val pr = p.pr()
        return text
            .replace("{name}", n)
            .replace("{Name}", n)
            .replace("{They}", pr.they.replaceFirstChar { it.titlecase() })
            .replace("{Them}", pr.them.replaceFirstChar { it.titlecase() })
            .replace("{Their}", pr.their.replaceFirstChar { it.titlecase() })
            .replace("{they}", pr.they)
            .replace("{them}", pr.them)
            .replace("{their}", pr.their)
            .replace("{theirs}", pr.theirs)
            .replace("{themself}", pr.themself)
            .replace("{are}", pr.are)
            .replace("{have}", pr.have)
    }
}

object Ach {
    fun evaluate(s: GameState): Set<String> {
        val have = s.achievements.toMutableSet()
        fun add(id: String) { have += id }
        add("first_dawn")
        if (s.player.name.isNotBlank() && s.player.name != "Guildmaster") add("named")
        if (s.flags.any { it.startsWith("quest_") } || s.lastResult?.success == true) {
            /* completed at least a resolve; success tracked below */
        }
        val maxAff = s.heroes.values.maxOfOrNull { it.affection } ?: 0
        if (maxAff >= 20) add("friend")
        if (maxAff >= 40) add("close")
        if (maxAff >= 60) add("cherished")
        if (maxAff >= 80) add("heartbound")
        if (s.heroes.values.count { it.recruited } >= 3) add("a_guest")
        if (s.heroes.values.all { it.recruited }) add("full_hall")
        if (s.lovedGifts.isNotEmpty()) add("loved_gift")
        if (s.rooms + s.training + s.tavern > 0) add("mason")
        if (s.day >= 7) add("week")
        if ("bought" in s.flags) add("patron")
        if ("won_quest" in s.flags) add("first_job")
        if ("lost_quest" in s.flags) add("came_home")
        if (s.world.route != null || s.flags.any { it.endsWith("_confess") }) add("promised")
        if (s.flags.any { it.endsWith("_vow") }) add("a_vow")
        if (s.flags.any { it.endsWith("_ending") && it != "plot_inquiry" }) add("an_ending")
        if ("event_festival" in s.flags) add("lanterns")
        if ("event_beach" in s.flags) add("shore")
        if ("event_harvest" in s.flags) add("harvest_dance")
        if ("plot_ch1_done" in s.flags || s.world.chapter >= 1) add("a_light_kept")
        if (s.flags.any { it.startsWith("cared_") }) add("bedside")
        return have
    }
}
