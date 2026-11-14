package com.tim.gamehub.games.heartbound

enum class HeroClass(val title: String, val atkGrowth: Int, val defGrowth: Int) {
    KNIGHT("Knight", 1, 2),
    MAGE("Mage", 2, 1),
    ROGUE("Rogue", 2, 1),
    RANGER("Ranger", 2, 1),
    CLERIC("Cleric", 1, 2),
    WARDEN("Warden", 2, 2),
}

enum class AffTier(val label: String, val min: Int, val hearts: Int) {
    STRANGER("Stranger", 0, 0),
    FRIEND("Friend", 20, 1),
    CLOSE("Close", 40, 2),
    CHERISHED("Cherished", 60, 3),
    HEARTBOUND("Heartbound", 80, 4),
}

data class HeroDef(
    val id: String,
    val name: String,
    val cls: HeroClass,
    val epithet: String,
    val personality: String,
    val backstory: String,
    val love: Set<String>,
    val like: Set<String>,
    val dislike: Set<String>,
    val baseAtk: Int,
    val baseDef: Int,
    val startLevel: Int = 1,
    val startAff: Int = 0,
    val startRecruited: Boolean = false,
    val accent: Long,
)

data class GiftDef(val id: String, val name: String, val desc: String, val cost: Int, val icon: String)
data class GearDef(val id: String, val name: String, val slot: String, val atk: Int, val def: Int, val cost: Int, val desc: String)
data class QuestDef(
    val id: String,
    val title: String,
    val desc: String,
    val place: String,          // forest / town / guild
    val difficulty: Int,
    val minParty: Int,
    val maxParty: Int,
    val gold: Int,
    val xp: Int,
    val preferred: Set<HeroClass>,
    val minDay: Int,
    val risk: Int,              // extra injury chance on a close roll
    val loot: String? = null,
    val days: Int = 1,
    val minRep: Int = 0,
    val boss: Boolean = false,
    val needFlag: String? = null,
    val once: Boolean = false,
    val rival: Boolean = false,
    val enemies: List<String> = emptyList(),
    val chain: String? = null,
)
data class UpgradeDef(val id: String, val name: String, val icon: String, val levels: List<Pair<Int, String>>)

object Heartbound {
    const val MAX_AFF = 100
    const val DATE_MIN = 40
    const val START_GOLD = 80

    val heroes = listOf(
        HeroDef(
            "kael", "Kael Voss", HeroClass.KNIGHT, "Knight of the Old Road",
            "Stoic, exacting, and more gentle than he admits. Speaks little, notices everything, and measures people by what they keep doing when no one is watching.",
            "Once a company knight of the inland holds. He refused an order to burn a village that had missed its tithe, was stripped of his colors, and walked until he found a guild hall that still needed a door guarded.",
            love = setOf("whetstone"), like = setOf("tea", "map"), dislike = setOf("brooch"),
            baseAtk = 8, baseDef = 10, startLevel = 2, startAff = 8, startRecruited = true, accent = 0xFF2C4A7A,
        ),
        HeroDef(
            "mira", "Mira Solen", HeroClass.MAGE, "Scholar of Living Starlight",
            "Curious, precise, and socially a half-step behind her own thoughts. She treats questions as gifts and silence as a kind of listening.",
            "She left the Ardent Academy after her research into 'living starlight' was sealed as unsafe. She did not stop the work — she simply took it somewhere with kinder windows.",
            love = setOf("book"), like = setOf("chart", "cake"), dislike = setOf("lockpicks"),
            baseAtk = 9, baseDef = 4, startLevel = 1, startAff = 0, accent = 0xFF6B4C8A,
        ),
        HeroDef(
            "rin", "Rin Kaito", HeroClass.ROGUE, "Shadow of the Side Streets",
            "Playful, sharp-tongued, and allergic to sincerity until he isn't. He jokes first, vanishes second, and stays longer than he planned.",
            "Raised on the alleys of Lumenreach, he survived by opening locks that were not his. He is tired of sleeping with one eye on the window and wants a place that will not throw him out.",
            love = setOf("candy"), like = setOf("lockpicks", "scarf"), dislike = setOf("hymn"),
            baseAtk = 8, baseDef = 5, startLevel = 1, startAff = 0, accent = 0xFF8A2E3A,
        ),
        HeroDef(
            "sera", "Sera Thorne", HeroClass.RANGER, "Last of the Green Watch",
            "Quiet, grounded, and fiercely loyal once she decides you are part of the trail. She says more with a look than most people do with a speech.",
            "She was the last ranger of a forest watch whose woods burned in a fire no one still explains. She walks the edges of town, trusting trees more than banners, and is learning — slowly — to trust people again.",
            love = setOf("flowers"), like = setOf("map", "tea"), dislike = setOf("perfume"),
            baseAtk = 8, baseDef = 6, startLevel = 1, startAff = 0, accent = 0xFF3D6B3A,
        ),
        HeroDef(
            "yuna", "Yuna Pell", HeroClass.CLERIC, "Keeper of Small Mercies",
            "Warm, stubborn, and quietly devout on her own terms. She believes healing is a promise, not a ledger, and will work past midnight to keep it.",
            "A temple acolyte who asked why the wards were reserved for those who could pay. When the answers ran out, she packed her herbs and found a hall that left the door unlatched.",
            love = setOf("herbs"), like = setOf("seeds", "hymn"), dislike = setOf("lockpicks"),
            baseAtk = 5, baseDef = 7, startLevel = 2, startAff = 10, startRecruited = true, accent = 0xFF2F6F6A,
        ),
        HeroDef(
            "nim", "Nim Hale", HeroClass.WARDEN, "Keeper of the Hidden Key",
            "Wary, dry, and more loyal than she dares admit. She measures a room by its doors, and a person by whether they make her stay anyway.",
            "Younger sister of Rowan Hale, the last master. She was hidden when the hall's lamps went out and walked under other names until the Covenant visitor withdrew. She has come home to see whether a hall can be a place that does not require anyone to vanish.",
            love = setOf("lantern"), like = setOf("map", "tea"), dislike = setOf("perfume"),
            baseAtk = 7, baseDef = 8, startLevel = 2, startAff = 0, accent = 0xFF3D4A5C,
        ),
    )
    val hero: Map<String, HeroDef> = heroes.associateBy { it.id }

    val gifts = listOf(
        GiftDef("tea", "Black Tea", "A tin of strong, honest leaves.", 15, "🍵"),
        GiftDef("cake", "Honey Cake", "Still warm if you walk quickly.", 20, "🍰"),
        GiftDef("whetstone", "Whetstone", "Fine grit, worn at the edges from honest use.", 25, "🪨"),
        GiftDef("book", "Rare Book", "A treatise with the margins already half-full of a previous reader's notes.", 40, "📕"),
        GiftDef("flowers", "Wildflowers", "A loose bundle of mint, daisy, and something blue.", 15, "💐"),
        GiftDef("candy", "Honey Candy", "Hard sweets that click in a pocket.", 15, "🍬"),
        GiftDef("herbs", "Herb Sachet", "Lavender, feverfew, and a thread of rosemary.", 20, "🌿"),
        GiftDef("chart", "Star Chart", "The sky over Lumenreach, inked by a patient hand.", 35, "✨"),
        GiftDef("scarf", "Red Scarf", "Soft wool, a little louder than is strictly necessary.", 30, "🧣"),
        GiftDef("map", "Old Map", "Roads that may or may not still exist.", 25, "🗺️"),
        GiftDef("lockpicks", "Lockpicks", "A discreet roll of well-oiled tools.", 25, "🗝️"),
        GiftDef("seeds", "Garden Seeds", "Labeled in a careful, hopeful script.", 15, "🌱"),
        GiftDef("brooch", "Flashy Brooch", "It catches every lantern in the room.", 30, "💎"),
        GiftDef("hymn", "Hymn Booklet", "Old verses, new binding.", 20, "📘"),
        GiftDef("perfume", "Cedar Perfume", "A sharp, fashionable scent.", 25, "🌸"),
    )
    val gift: Map<String, GiftDef> = gifts.associateBy { it.id }

    val gear = listOf(
        GearDef("iron", "Iron Blade", "weapon", 3, 0, 50, "Serviceable steel. It does not pretend to be more."),
        GearDef("silver", "Silver Edge", "weapon", 7, 0, 140, "Light, keen, and quietly expensive."),
        GearDef("charm", "Oak Charm", "accessory", 0, 2, 40, "A carved token that sits warm against the chest."),
        GearDef("amulet", "Guardian Amulet", "accessory", 0, 5, 120, "Temple-work, sold without a sermon."),
        GearDef("boots", "Swift Boots", "accessory", 3, 1, 80, "The soles are already a little scuffed. That's a good sign."),
    )
    val gearById: Map<String, GearDef> = gear.associateBy { it.id }

    val quests = listOf(
        QuestDef("cat", "The Mayor's Cat", "A very dignified cat has taken a very undignified interest in the clock tower.", "town", 12, 1, 2, 35, 25, setOf(HeroClass.ROGUE, HeroClass.RANGER), 1, 8),
        QuestDef("herbs", "Herb Run", "Feverfew and river mint along the old forest path. Watch the roots.", "forest", 12, 1, 2, 35, 25, setOf(HeroClass.CLERIC, HeroClass.RANGER), 1, 10, "herbs"),
        QuestDef("escort", "Merchant Escort", "A cloth merchant wants company as far as the east gate. Bandits have been bored lately.", "town", 20, 2, 2, 60, 40, setOf(HeroClass.KNIGHT, HeroClass.RANGER), 1, 16),
        QuestDef("goblin", "Goblin Scarecrows", "Something is stealing grain and leaving rude little hats on the fence posts.", "forest", 20, 2, 3, 65, 45, setOf(HeroClass.MAGE, HeroClass.ROGUE), 2, 18, chain = "goblin_routed"),
        QuestDef("ledger", "The Missing Ledger", "The town clerk swears the accounts walked off on their own. Someone should ask the accounts.", "town", 22, 1, 2, 70, 45, setOf(HeroClass.MAGE, HeroClass.ROGUE), 2, 12, "book", chain = "ledger_found"),
        QuestDef("wolves", "Wolf Pack", "A hungry pack has come down from the ridge. They need turning, not a slaughter if it can be helped.", "forest", 32, 2, 3, 100, 70, setOf(HeroClass.RANGER, HeroClass.KNIGHT), 3, 24),
        QuestDef("watch", "Night Watch", "The south wall is short a lantern-bearer for two nights. Pay is honest. Sleep is not.", "town", 24, 2, 2, 75, 50, setOf(HeroClass.KNIGHT, HeroClass.CLERIC), 3, 14),
        QuestDef("chapel", "Ruined Chapel", "Travelers heard singing in a chapel that has been empty for years. Yuna would like that looked into.", "forest", 34, 2, 3, 110, 80, setOf(HeroClass.CLERIC, HeroClass.MAGE), 4, 22),
        QuestDef("vault", "Vault Rumor", "A locked counting-house, a missing key, and a rumor with too many teeth.", "town", 36, 2, 3, 130, 85, setOf(HeroClass.ROGUE, HeroClass.MAGE), 4, 20, "lockpicks"),
        QuestDef("bandits", "Bandit Camp", "A camp in the deep trees has been taking more than tithes. This will not be tidy.", "forest", 45, 3, 3, 160, 110, setOf(HeroClass.KNIGHT, HeroClass.RANGER, HeroClass.ROGUE), 5, 28),
        QuestDef("festival", "Festival Hands", "Lumenreach is hanging lanterns and is short on hands, ladders, and patience.", "town", 16, 1, 3, 50, 35, setOf(HeroClass.CLERIC, HeroClass.ROGUE), 6, 8, "candy"),
        QuestDef("shadow", "A Shadow in the Woods", "Whatever burned Sera's forest may have grown a new name. Go carefully. Come home.", "forest", 50, 3, 3, 200, 140, setOf(HeroClass.RANGER, HeroClass.MAGE, HeroClass.CLERIC), 8, 32),

        QuestDef("goblin_den", "The Grain Den", "The scarecrows led back to a den packed with stolen grain and worse manners. A two-day push through the woods.", "forest", 28, 2, 3, 90, 65, setOf(HeroClass.MAGE, HeroClass.ROGUE), 4, 20, days = 2, needFlag = "goblin_routed", once = true, enemies = listOf("goblin", "goblin"), chain = "den_cleared"),
        QuestDef("goblin_king", "The Coin Crown", "The den's master has crowned himself with bent coins. End the reign.", "forest", 48, 2, 3, 180, 130, setOf(HeroClass.KNIGHT, HeroClass.MAGE, HeroClass.ROGUE), 6, 28, days = 2, minRep = 12, boss = true, needFlag = "den_cleared", once = true, enemies = listOf("goblin", "goblin_king"), chain = "king_fallen"),
        QuestDef("ledger_mercy", "A Name Forgotten", "You promised the clerk the thief could start over. Now someone has to make that true.", "town", 24, 1, 2, 80, 55, setOf(HeroClass.ROGUE, HeroClass.CLERIC), 3, 12, needFlag = "ledger_mercy", once = true, enemies = listOf("bandit"), chain = "mercy_kept"),
        QuestDef("ledger_law", "The Counting-House Warrant", "The law asks you to finish what the missing ledger started.", "town", 26, 1, 2, 85, 55, setOf(HeroClass.KNIGHT, HeroClass.ROGUE), 3, 14, needFlag = "ledger_law", once = true, enemies = listOf("bandit", "bandit"), chain = "law_kept"),
        QuestDef("vesper_contract", "Contested Contract", "Iron Vesper posted the same job on the east gate. They will not step aside politely.", "town", 28, 2, 2, 90, 60, setOf(HeroClass.KNIGHT, HeroClass.ROGUE), 3, 18, rival = true, enemies = listOf("vesper"), chain = "vesper_met"),
        QuestDef("vesper_duel", "A Duel at Dusk", "A duelist of Iron Vesper has called the hall out by name. This one will be remembered.", "town", 50, 2, 3, 200, 140, setOf(HeroClass.KNIGHT, HeroClass.RANGER), 6, 26, days = 2, minRep = 15, boss = true, rival = true, needFlag = "vesper_met", once = true, enemies = listOf("vesper"), chain = "vesper_humbled"),
        QuestDef("ridge_hunt", "The Ridge Hunt", "A second pack has come down the ridge, hungrier than the first. The town will notice who answers.", "forest", 40, 2, 3, 140, 95, setOf(HeroClass.RANGER, HeroClass.KNIGHT), 5, 24, days = 2, minRep = 10, enemies = listOf("wolf", "wolf")),
        QuestDef("covenant_envoy", "The Envoy Returns", "The visitor who promised to return when the lanterns fade has kept that promise. Meet them on the old road.", "forest", 55, 3, 3, 240, 180, setOf(HeroClass.CLERIC, HeroClass.MAGE, HeroClass.KNIGHT), 8, 32, days = 2, minRep = 20, boss = true, needFlag = "plot_ch1_done", once = true, enemies = listOf("shade", "covenant"), chain = "envoy_met"),
    )
    val quest: Map<String, QuestDef> = quests.associateBy { it.id }

    val upgrades = listOf(
        UpgradeDef("rooms", "Members' Rooms", "🛏️", listOf(
            120 to "A real bed and a door that shuts. Injuries fade a day faster.",
            200 to "Sunlit rooms. Recovery is twice as sure.",
            320 to "The best rooms in Lumenreach. Exhaustion rarely lingers.",
        )),
        UpgradeDef("training", "Training Hall", "🗡️", listOf(
            150 to "A yard with posts and marks. Parties fight 6% harder.",
            250 to "Weighted dummies and a proper ring. Parties fight 12% harder.",
            400 to "A hall that other guilds would envy. Parties fight 18% harder.",
        )),
        UpgradeDef("tavern", "The Hearth", "🍺", listOf(
            140 to "A barrel and a long table. One extra evening conversation.",
            220 to "A proper taproom. Evening talks land a little warmer.",
            350 to "Lanterns, a small stage, and a reputation. Affection comes easier.",
        )),
        UpgradeDef("forge", "The Forge", "🔥", listOf(
            160 to "A hearth and an anvil. Enchanting is unlocked, simply.",
            260 to "A proper smithy. Enchants take more kindly, and cost less.",
            400 to "A hall other guilds would borrow. Enchants bite deeper.",
        )),
        UpgradeDef("library", "The Library", "📚", listOf(
            150 to "Shelves and a quiet table. The hall thinks a little clearer.",
            240 to "A catalog, and evening talks that land with more care.",
            360 to "A collection Lumenreach would envy. Rare work becomes possible.",
        )),
        UpgradeDef("garden", "The Garden", "🌱", listOf(
            130 to "A plot by the wall. Herbs and flowers come in with the morning.",
            210 to "Beds and a rain barrel. The garden is generous.",
            330 to "A small orchard of useful things. Ribbons, herbs, and enough to share.",
        )),
        UpgradeDef("stables", "The Stables", "🐴", listOf(
            150 to "Two stalls and an honest roof. Multi-day roads run a day shorter.",
            240 to "A proper yard. Parties leave fresher and come home sooner.",
            360 to "Horses the town notices. The road rarely argues.",
        )),
    )

    fun xpToNext(level: Int) = 40 + level * 30

    fun tier(aff: Int) = AffTier.entries.last { aff >= it.min }

    fun giftDelta(hero: HeroDef, giftId: String): Int {
        Life.specialDelta(hero, giftId)?.let { return it }
        return when (giftId) {
            in hero.love -> 12
            in hero.like -> 7
            in hero.dislike -> -2
            else -> 3
        }
    }

    val synergies = listOf(
        setOf(HeroClass.KNIGHT, HeroClass.CLERIC) to 0.12,
        setOf(HeroClass.MAGE, HeroClass.ROGUE) to 0.12,
        setOf(HeroClass.RANGER, HeroClass.ROGUE) to 0.10,
        setOf(HeroClass.MAGE, HeroClass.CLERIC) to 0.10,
        setOf(HeroClass.KNIGHT, HeroClass.RANGER) to 0.08,
    )
}
