package com.tim.gamehub.games.heartbound

import kotlin.random.Random

data class StaffDef(val id: String, val name: String, val role: String, val cost: Int, val perk: String)
data class RecipeDef(val id: String, val name: String, val desc: String, val inputs: List<Pair<String, Int>>, val output: String, val outN: Int = 1, val needForge: Int = 0, val needGarden: Int = 0, val needLibrary: Int = 0)
data class CatalogItem(val id: String, val name: String, val desc: String, val cost: Int, val kind: String, val icon: String = "•", val seasons: Set<String> = emptySet())
data class PlaceDef(val id: String, val name: String, val blurb: String, val night: Boolean = false)

object Life {
    val places = listOf(
        PlaceDef("cafe", "The East-Wall Cafe", "Warm wood, honest tea, and a window that pretends not to listen."),
        PlaceDef("lake", "The Quiet Shore", "A pier, a still lake, and enough sky to think in."),
        PlaceDef("market", "Lantern Market", "Awnings, fruit, and the pleasant work of choosing nothing urgent."),
        PlaceDef("hill", "Stargazing Hill", "A stone lookout and a sky that remembers every lantern below.", night = true),
    )
    val place = places.associateBy { it.id }

    val staff = listOf(
        StaffDef("cook", "Bram the Cook", "Kitchen", 80, "Evening conversations land a little warmer."),
        StaffDef("quartermaster", "Osa the Quartermaster", "Stores", 100, "Shop prices are kinder by about a fifth."),
        StaffDef("grounds", "Tess the Groundskeeper", "Yard", 90, "The garden yields even in lean weeks, and injuries ease a little faster."),
        StaffDef("scout", "Jori the Scout", "Road", 110, "The traveling merchant calls more often, and escorts pay a little more."),
        StaffDef("keeper", "Old Penn", "Hall", 120, "One extra evening conversation, if you will use it kindly."),
    )
    val staffBy = staff.associateBy { it.id }

    val materials = listOf(
        CatalogItem("ore", "River Ore", "A knuckle of dark metal, still cold.", 22, "mat", "⛏"),
        CatalogItem("pelt", "Ridge Pelt", "Soft, and honestly earned.", 20, "mat", "🦌"),
        CatalogItem("wax", "Hive Wax", "It smells faintly of summer even in winter.", 12, "mat", "🕯"),
        CatalogItem("ribbon", "Quiet Ribbon", "A length of good cloth, waiting for a reason.", 14, "mat", "🎀"),
        CatalogItem("ashglass", "Ashglass", "A shard that holds a little unkind light.", 28, "mat", "🔮"),
    )
    val decor = listOf(
        CatalogItem("d_banner", "Hall Banner", "Burgundy cloth for the rafters.", 35, "decor", "🚩"),
        CatalogItem("d_rug", "Hearth Rug", "It makes the stones less stern.", 30, "decor", "🟫"),
        CatalogItem("d_plant", "Potted Fern", "A green that does not ask for much.", 20, "decor", "🌿"),
        CatalogItem("d_lanterns", "Extra Lanterns", "The hall looks lived-in after dusk.", 32, "decor", "🏮"),
        CatalogItem("d_painting", "A Small Painting", "A view of the east road, unsigned.", 40, "decor", "🖼"),
    )
    val roomItems = listOf(
        CatalogItem("r_quilt", "Spare Quilt", "Heavier than it looks. That is the point.", 24, "room", "🛏"),
        CatalogItem("r_flowers", "Window Flowers", "A little color for a private sill.", 16, "room", "🌸"),
        CatalogItem("r_lamp", "Desk Lamp", "A kinder light to read by.", 22, "room", "💡"),
        CatalogItem("r_keepsake", "Keepsake Shelf", "A place for things that are not inventory.", 26, "room", "📦"),
    )
    val rareGifts = listOf(
        GiftDef("lantern", "Pocket Lantern", "A small iron lantern, honestly made.", 45, "🪔"),
        GiftDef("pendant", "Starlight Pendant", "A sliver of glass that remembers the sky.", 55, "🌙"),
        GiftDef("cloak", "Winter Cloak", "Wool and patience, for the long cold.", 50, "🧥"),
        GiftDef("vow_ribbon", "Vow Ribbon", "A length of cloth meant to be tied and kept.", 40, "🎗️"),
        GiftDef("lake_stone", "Lake-Smooth Stone", "Worn kind by a great deal of water.", 25, "🪨"),
        GiftDef("flower_crown", "Spring Crown", "A loose crown of whatever is blooming.", 22, "🌼"),
        GiftDef("iced_tea", "Iced Tea", "The kettle's summer cousin.", 16, "🧋"),
        GiftDef("harvest_loaf", "Harvest Loaf", "Still warm if you walk quickly.", 20, "🍞"),
    )
    val catalog: Map<String, CatalogItem> = (materials + decor + roomItems).associateBy { it.id }
    val allGifts: Map<String, GiftDef> = (Heartbound.gifts + rareGifts).associateBy { it.id }

    val recipes = listOf(
        RecipeDef("salve", "Feverfew Salve", "A reliable kindness for sore hands.", listOf("herbs" to 1, "tea" to 1), "salve"),
        RecipeDef("bouquet", "Bound Bouquet", "Flowers, and a ribbon that means you stayed to tie it.", listOf("flowers" to 1, "ribbon" to 1), "bouquet"),
        RecipeDef("lantern", "Pocket Lantern", "Wax, ashglass, and a careful hand at the forge.", listOf("wax" to 1, "ashglass" to 1), "lantern", needForge = 1),
        RecipeDef("pendant", "Starlight Pendant", "Library notes and a shard of sky-minded glass.", listOf("ashglass" to 1, "chart" to 1), "pendant", needLibrary = 1),
        RecipeDef("cloak", "Winter Cloak", "A pelt and patience.", listOf("pelt" to 1, "scarf" to 1), "cloak"),
        RecipeDef("vow_ribbon", "Vow Ribbon", "A ribbon you have already decided the meaning of.", listOf("ribbon" to 1, "hymn" to 1), "vow_ribbon"),
        RecipeDef("lake_stone", "Lake-Smooth Stone", "You cannot craft a lake. You can polish what it left you.", listOf("ore" to 1), "lake_stone"),
        RecipeDef("oil", "Lantern Oil", "For the hall, and for anyone who walks after dark.", listOf("wax" to 1, "herbs" to 1), "oil", needGarden = 1),
    )
    val craftedGift = mapOf(
        "salve" to GiftDef("salve", "Feverfew Salve", "A reliable kindness for sore hands.", 0, "🧴"),
        "bouquet" to GiftDef("bouquet", "Bound Bouquet", "Tied as if it mattered, because it did.", 0, "💐"),
        "oil" to GiftDef("oil", "Lantern Oil", "A small bottle, carefully stoppered.", 0, "🪔"),
    )

    fun specialDelta(hero: HeroDef, giftId: String): Int? = when {
        hero.id == "nim" && giftId == "lantern" -> 12
        hero.id == "mira" && giftId == "pendant" -> 12
        hero.id == "sera" && giftId == "lake_stone" -> 12
        hero.id == "kael" && giftId == "cloak" -> 12
        hero.id == "yuna" && giftId in setOf("salve", "oil") -> 12
        hero.id == "rin" && giftId == "vow_ribbon" -> 12
        hero.id == "sera" && giftId == "flower_crown" -> 10
        hero.id == "yuna" && giftId == "harvest_loaf" -> 8
        hero.id == "mira" && giftId == "iced_tea" -> 8
        giftId in hero.love || giftId in hero.like || giftId in hero.dislike -> null
        giftId in setOf("lantern", "pendant", "cloak", "vow_ribbon", "lake_stone", "flower_crown", "iced_tea", "harvest_loaf", "bouquet", "salve", "oil") -> 7
        else -> null
    }

    fun giftDef(id: String) = allGifts[id] ?: craftedGift[id] ?: Heartbound.gift[id]

    val birthdays = mapOf("yuna" to 4, "kael" to 7, "mira" to 12, "rin" to 18, "sera" to 21, "nim" to 25)
    fun calendarDay(day: Int) = ((day - 1) % 28) + 1
    fun seasonOf(day: Int) = listOf("spring", "summer", "autumn", "winter")[((day - 1) / 7) % 4]
    fun weatherFor(season: String, rng: Random) = when (season) {
        "winter" -> listOf("snow", "snow", "clear", "clear", "storm")[rng.nextInt(5)]
        "autumn" -> listOf("clear", "breeze", "rain", "clear")[rng.nextInt(4)]
        "summer" -> listOf("clear", "clear", "clear", "breeze", "rain")[rng.nextInt(5)]
        else -> listOf("clear", "clear", "rain", "breeze")[rng.nextInt(4)]
    }
    fun weatherLabel(w: String) = when (w) { "snow" -> "snow"; "rain" -> "rain"; "breeze" -> "a light breeze"; "storm" -> "a hard storm"; else -> "clear skies" }
    fun seasonLabel(s: String) = s.replaceFirstChar { it.titlecase() }

    fun shopPrice(base: Int, state: GameState) = if ("quartermaster" in state.staff) ((base * 85) + 99) / 100 else base

    fun merchantVisits(state: GameState): Boolean {
        if (state.phase != "morning") return false
        val scout = "scout" in state.staff
        return state.day % 5 == 4 || (scout && state.day % 3 == 0)
    }

    fun merchantStock(state: GameState): List<CatalogItem> {
        val season = state.world.season
        val rot = (state.day / 5)
        val mats = materials.shuffled(Random(rot + 17)).take(3)
        val dec = (decor + roomItems).shuffled(Random(rot + 4)).take(2)
        val seasonal = seasonalItems().filter { it.seasons.isEmpty() || season in it.seasons }
        val rares = listOf(
            CatalogItem("pendant", "Starlight Pendant", "A sliver of glass that remembers the sky.", 55, "gift", "🌙", setOf("summer", "autumn")),
            CatalogItem("cloak", "Winter Cloak", "Wool and patience, for the long cold.", 50, "gift", "🧥", setOf("winter", "autumn")),
            CatalogItem("flower_crown", "Spring Crown", "A loose crown of whatever is blooming.", 22, "gift", "🌼", setOf("spring")),
            CatalogItem("iced_tea", "Iced Tea", "The kettle's summer cousin.", 16, "gift", "🧋", setOf("summer")),
            CatalogItem("harvest_loaf", "Harvest Loaf", "Still warm if you walk quickly.", 20, "gift", "🍞", setOf("autumn")),
            CatalogItem("lantern", "Pocket Lantern", "A small iron lantern, honestly made.", 45, "gift", "🪔"),
            CatalogItem("vow_ribbon", "Vow Ribbon", "A length of cloth meant to be tied and kept.", 40, "gift", "🎗️"),
            CatalogItem("lake_stone", "Lake-Smooth Stone", "Worn kind by a great deal of water.", 25, "gift", "🪨"),
        ).filter { it.seasons.isEmpty() || season in it.seasons }.shuffled(Random(rot + 9)).take(2)
        return mats + dec + rares + seasonal.filter { it.id !in rares.map { r -> r.id } }.take(1)
    }

    fun seasonalItems() = listOf(
        CatalogItem("flower_crown", "Spring Crown", "A loose crown of whatever is blooming.", 22, "gift", "🌼", setOf("spring")),
        CatalogItem("iced_tea", "Iced Tea", "The kettle's summer cousin.", 16, "gift", "🧋", setOf("summer")),
        CatalogItem("harvest_loaf", "Harvest Loaf", "Still warm if you walk quickly.", 20, "gift", "🍞", setOf("autumn")),
        CatalogItem("cloak", "Winter Cloak", "Wool and patience, for the long cold.", 50, "gift", "🧥", setOf("winter")),
    )

    fun harvest(state: GameState): GameState {
        if (state.garden <= 0 && "grounds" !in state.staff) return state
        val season = seasonOf(state.day)
        val yield = mutableListOf<String>()
        if (season != "winter") yield += if (season == "spring" || state.garden >= 2) "flowers" else "herbs"
        if (state.garden >= 1 || "grounds" in state.staff) yield += if (season == "winter") "seeds" else "herbs"
        if (state.garden >= 3) yield += "ribbon"
        var s = state
        yield.forEach { s = HeartboundLogic.addInv(s, it) }
        return if (yield.isEmpty()) s else s.copy(flags = s.flags + "harvested_${state.day}")
    }

    fun birthdayOf(state: GameState): String? {
        val cal = calendarDay(state.day)
        return birthdays.entries.firstOrNull { (id, d) ->
            d == cal && state.heroes[id]?.recruited == true && "bday_${id}_${state.day}" !in state.flags
        }?.key
    }

    fun pendingEnding(state: GameState): Scene? {
        val flags = state.flags
        return when {
            "envoy_met" in flags && "vesper_humbled" in flags && "ending_iron_light" !in flags && state.day >= 10 -> endingIron
            "envoy_met" in flags && "ending_lanterns" !in flags && state.day >= 9 -> endingLanterns
            (state.world.chapter >= 1 || "plot_ch1_done" in flags) && state.day >= 14 && "ending_hall" !in flags -> endingHall
            state.world.route == null && state.day >= 20 && (state.heroes.values.maxOfOrNull { it.affection } ?: 0) >= 60 && "ending_quiet" !in flags -> endingQuiet
            else -> null
        }
    }

    val recruitNim = sc(
        "recruit_nim", "nim", "guild",
        N("On a late morning the door opens as if the key had been waiting for a hand that already knew it."),
        L("Nim", "You can put the kettle down. I am not here to take the hall from you."),
        L("Nim", "I'm Nim Hale. Rowan was my brother. I was hidden when the last lamps went out, and I have been walking under other names since."),
        L("Nim", "The visitor withdrew. I felt it from the road. If this hall is going to keep a light, I would rather keep it from the inside.", "grim"),
        title = "A Name the Ledger Still Holds",
        choices = listOf(
            Choice("The door is yours, if you want a room and a name that stays.", 5, "recruited_nim", listOf(L("Nim", "A name that stays. That is a rarer offer than you think.", "smile"), L("Yuna", "I'll air the east room. And put an extra cup out.")), true),
            Choice("If you can keep a lantern, you're family enough for this house.", 4, "recruited_nim", listOf(L("Nim", "Family is a large word. I will try the smaller ones first. Work. Morning. Stay.", "smile")), true),
            Choice("Give me a day to ready a place that deserves you.", 0, next = listOf(L("Nim", "I can wait. I have practiced waiting."))),
        ),
    )

    val romance = listOf(
        sc("nim_e1", "nim", "guild",
            N("You find Nim in the archive, a finger on a name that has been crossed out and written back in."),
            L("Nim", "I almost told you a different name, the first morning. Habit. The road teaches you to arrive as someone disposable."),
            L("Nim", "Nim is mine. Hale is a weight I am still deciding how to carry. Thank you for not making me perform it.", "smile"),
            title = "A Name Kept", req = 20,
            choices = listOf(
                Choice("Keep whatever name lets you sleep.", 3, next = listOf(L("Nim", "That is a kinder sentence than it sounds.", "smile"))),
                Choice("Hale is welcome here. So is Nim.", 4, next = listOf(L("Nim", "Careful. I might start believing the hall is allowed to have a future.", "shy"))),
            ),
        ),
        sc("nim_e2", "nim", "forest",
            N("She walks the tree line as if it were a street she used to live on."),
            L("Nim", "They hid me in a chapel first, then a barn, then a town that asked no questions. I learned to love doors that opened quietly."),
            L("Nim", "I am trying to learn the other kind. The kind you walk through in daylight, and are still allowed back.", "shy"),
            title = "Doors That Open Quietly", req = 40,
            choices = listOf(
                Choice("This door will be here in the morning.", 4, next = listOf(L("Nim", "Then I will practice coming back.", "smile"))),
                Choice("You don't have to earn the daylight.", 5, next = listOf(L("Nim", "That may be the first untrue-sounding true thing anyone has said to me in years.", "shy"))),
            ),
        ),
        sc("nim_e3", "nim", "chapel",
            N("The emptied chapel is quieter than it was. Nim stands where a visitor once withdrew."),
            L("Nim", "Rowan hid a letter and a key and a sister. I used to be angry that the letter came first. I am less angry now."),
            L("Nim", "If you keep this hall, keep it as a place that does not require anyone to vanish in order to be safe. That is all I wanted the Hale name to mean.", "grim"),
            title = "What the Name Meant", req = 60,
            choices = listOf(
                Choice("Then we'll keep it that way.", 5, "nim_romance", listOf(L("Nim", "We. I will try getting used to that word too.", "smile"))),
                Choice("You don't have to carry the name alone.", 6, "nim_romance", listOf(L("Nim", "I have been waiting, very quietly, for someone to say that.", "shy"))),
            ),
        ),
        sc("nim_confess", "nim", "guild",
            N("The hall is late and almost empty. Nim's lantern pin catches the last of the fire."),
            L("Nim", "I should be careful. I have left too many places the moment they became dear."),
            L("Nim", "I am not leaving this one. If you want a future that includes a woman who is still learning how to stay, I would like it to include me.", "shy"),
            title = "Learning How to Stay", req = 80, needFlag = "nim_romance",
            choices = listOf(
                Choice("Stay. I want the future that has you in it.", 6, "nim_confess", listOf(L("Nim", "Then I will practice, every morning.", "smile"), N("She does not vanish. The fire does not mind.")), needFreeRoute = true),
                Choice("I care for you. I need time before I promise a route.", 2, next = listOf(L("Nim", "Time I understand. I will not pack. That is already a kind of promise.", "smile"))),
            ),
        ),
        sc("nim_propose", "nim", "hill",
            N("The hill holds its breath. Lumenreach is a scatter of patient lights."),
            L("Nim", "I do not have a ring. I have a lantern, a stubborn name, and the first quiet year I have wanted to keep."),
            L("Nim", "If you will have me — not as a secret, and not as a debt — I will light whatever road we walk.", "shy"),
            title = "A Light We Keep", req = 90, needFlag = "nim_confess", routeOnly = true,
            choices = listOf(
                Choice("Yes. We'll keep it together.", 8, "nim_vow", listOf(L("Nim", "Then come here, before I remember how to be careful.", "smile"), F("The hill keeps its own counsel. The lights below go on being lights."))),
                Choice("I want that. Let me find the right words tomorrow.", 3, next = listOf(L("Nim", "Tomorrow, then. I can wait one more day. I have waited longer for worse reasons."))),
            ),
        ),
        sc("nim_ending", "nim", "guild",
            N("Years do not announce themselves. The hall is simply still here, and so is she."),
            L("Nim", "I used to measure a life by how lightly I could leave it. Now I measure it by the cups that are left out on purpose.", "smile"),
            L("Nim", "If this is the ending of one story, let it be the beginning of a very ordinary, very stubborn happiness. I want that with you.", "shy"),
            title = "A House That Stays", req = 100, needFlag = "nim_vow", needChapter = 1, routeOnly = true,
            choices = listOf(
                Choice("Then we'll be ordinary, and stubborn, and here.", 0, "nim_ending", listOf(L("Nim", "Good. I'll put the kettle on. Stay where I can see you.", "smile"), F("The door of the east room closes, gently. The hall, as always, keeps its own counsel."))),
            ),
        ),
    )

    val nimTalks = listOf(
        sc("nim_t1", "nim", "guild", L("Nim", "I still startle when someone says my name as if they expected me to be there. It is a good startling.", "smile")),
        sc("nim_t2", "nim", "town", L("Nim", "The market does not know I was gone. I find that comforting, and a little rude. Markets have the right idea.")),
    )
    val nimDate = sc(
        "nim_date", "nim", "town",
        N("Nim walks as if the town might still be a place she has to apologize to."),
        L("Nim", "I keep waiting for someone to ask me to leave. It is a poor habit. You are not helping it, which I think I like.", "smile"),
        choices = listOf(
            Choice("Stay as long as the lanterns are lit.", 4, next = listOf(L("Nim", "Then we may be here a while.", "shy"))),
            Choice("I'll walk you back when you're ready.", 3, next = listOf(L("Nim", "I may not be ready. I may still want the walk.", "smile"))),
        ),
    )

    fun withPlace(base: Scene, place: String): Scene {
        val intro = N(when (place) {
            "cafe" -> "You take a corner table at the east-wall cafe. The herbs in the rafters tick softly in the heat."
            "lake" -> "The lake holds the last of the light. A pier board creaks, and then is kind enough to be quiet."
            "market" -> "The market is closing in a good mood, which is the only mood a market should close in."
            "hill" -> "The hill is cold in a honest way. Lumenreach lies below like a handful of kept promises."
            "room" -> "The room is small, and theirs, and warmer for you being asked in."
            else -> "You take the long way through town, because the long way is the point."
        })
        return base.copy(id = "${base.id}_$place", bg = place, lines = listOf(intro) + base.lines)
    }

    fun visitScene(heroId: String, aff: Int): Scene {
        val d = Heartbound.hero.getValue(heroId)
        val close = aff >= 60
        val lines = when (heroId) {
            "kael" -> listOf(N("Kael's room is as exact as his kit. A whetstone sits out, as if it were a guest."), L("Kael", if (close) "You can sit. I find I sleep better when the room has had a witness." else "It's a small room. I didn't invite many people into the last one.", if (close) "smile" else "idle"))
            "mira" -> listOf(N("Notes have colonized the desk and, politely, the floor."), L("Mira", if (close) "Don't step on the star chart. Sit on it, if you must. I can copy a person more easily than a sky." else "I should warn you that the chair is also a bookshelf.", if (close) "shy" else "smile"))
            "rin" -> listOf(N("Rin's window is unlatched. He looks briefly guilty, then not."), L("Rin", if (close) "If you're here, I don't have to keep one eye on the latch. That's new. I like new." else "Don't inspect the locks. They're decorative. Mostly.", if (close) "shy" else "smile"))
            "sera" -> listOf(N("A feather, a map, a little dirt that no one has asked her to sweep."), L("Sera", if (close) "I don't talk much in here. You can stay anyway. That's the point of a room like this." else "It's quieter than the trees. I'm still deciding if that's a virtue.", if (close) "smile" else "idle"))
            "yuna" -> listOf(N("The kettle has followed her upstairs, which is either charming or a fire hazard."), L("Yuna", if (close) "Sit. I'll pour. This is the part of the evening I used to do alone, and I find I don't miss the alone." else "Careful — the floorboard by the window sings. I haven't the heart to fix it.", if (close) "smile" else "idle"))
            else -> listOf(N("Nim's room is almost bare, except for a lantern already lit, as if she had hoped you would come."), L("Nim", if (close) "I left the door unlatched. I am practicing. Stay until the oil runs low." else "It is not much of a room yet. It is mine. That is still new.", if (close) "shy" else "smile"))
        }
        val fade = if (close) listOf(F("The lantern is turned down. The hall, two floors below, keeps its own counsel, and so do you.")) else emptyList()
        return sc("visit_${heroId}", heroId, "room", *lines.toTypedArray(), title = "A Private Hour", choices = listOf(
            Choice("I'll stay a while.", if (close) 4 else 3, next = lines.takeLast(1) + fade),
            Choice("Thank you for trusting me with the door.", 3, next = listOf(L(d.name, "Don't make it rare, and I won't have to be brave about it every time.", "smile"))),
        ))
    }

    fun birthdayScene(heroId: String): Scene {
        val d = Heartbound.hero.getValue(heroId)
        return sc(
            "bday_$heroId", heroId, "tavern",
            N("Someone has put a small cake on the long table, and is trying not to look proud of it."),
            L(d.name, "I don't make a habit of the day. I will, however, make a habit of this hall — and of you, if you keep looking at me like that.", "shy"),
            title = "A Day That Comes Back",
            choices = listOf(
                Choice("Happy birthday. I'm glad you're here.", 5, next = listOf(L(d.name, "That is a very decent sentence. I'll keep it.", "smile"))),
                Choice("I got you something, if you'll have it later.", 4, next = listOf(L(d.name, "Later, then. Don't let me forget I am allowed to be glad.", "smile"))),
            ),
        )
    }

    val endingHall = sc(
        "ending_hall", null, "guild",
        N("The hall has survived another season. The ledger is less frightened than it was."),
        N("You stand in the doorway and understand, without drama, that this is no longer a temporary post."),
        L("Yuna", "The kettle still sings. That's all the omen I need.", "smile"),
        L("Kael", "We've made a hall that can be left to someone, someday. Not soon, if I have a say.", "smile"),
        title = "The Hall Endures",
        choices = listOf(Choice("Then we'll keep the door unlatched.", 0, "ending_hall", next = listOf(N("The work goes on. That, it turns out, is a kind of ending, and a kind of beginning.")))),
    )
    val endingLanterns = sc(
        "ending_lanterns", null, "chapel",
        N("The envoy is gone. The inverted lanterns are only lanterns again."),
        L("Sera", "The woods will take years. I can live with years, if the dark has a name we already faced.", "grim"),
        L("Mira", "Living light, unharvested. I would like to write that down as a result, not a rumor.", "smile"),
        title = "Lanterns Against the Dark",
        choices = listOf(Choice("We'll be here when the dark learns new manners.", 0, "ending_lanterns", next = listOf(N("Lumenreach keeps its lamps. For now, that is enough, and also everything.")))),
    )
    val endingIron = sc(
        "ending_iron_light", null, "town",
        N("Iron Vesper has learned the hall's name, and the Covenant has learned it is not easy to take."),
        L("Rin", "I like a town that can tell two kinds of trouble to wait their turn.", "smile"),
        L("Kael", "Rivals on one road, a darker thing on the other. We'll walk both. That's the work.", "idle"),
        title = "Iron and Light",
        choices = listOf(Choice("Then the hall will be known for more than surviving.", 0, "ending_iron_light", next = listOf(N("A reputation is a kind of lantern. You choose what it is hung for.")))),
    )
    val endingQuiet = sc(
        "ending_quiet", null, "guild",
        N("No vow has been spoken. The hall does not seem to mind. The cups still come out in pairs."),
        L("Yuna", "Not every life needs a ceremony. Some of them need a kettle, and a chair left for whoever sits down.", "smile"),
        title = "A Quiet Life",
        choices = listOf(Choice("This is enough. This is a life.", 0, "ending_quiet", next = listOf(N("The evening holds. That is the whole plot, and it is a good one.")))),
    )

    fun rareGiftScene(hero: HeroDef, gift: GiftDef, delta: Int): Scene? {
        if (gift.id !in rareGifts.map { it.id } + craftedGift.keys) return null
        val line = when {
            hero.id == "nim" && gift.id == "lantern" -> L("Nim", "You — this is a small light, and it is exactly the size of the promise I can hold in a hand. How did you know?", "wow")
            hero.id == "mira" && gift.id == "pendant" -> L("Mira", "Starlight, portable. I am going to be scientifically embarrassing about this.", "wow")
            hero.id == "sera" && gift.id == "lake_stone" -> L("Sera", "You picked a quiet thing. I trust people who do that.", "smile")
            hero.id == "kael" && gift.id == "cloak" -> L("Kael", "Warm, and not showy. I'll wear it on the night watch. I'll think of the hall. Of you.", "shy")
            hero.id == "yuna" && gift.id in setOf("salve", "harvest_loaf", "oil") -> L("Yuna", "You made this for use, not for display. That's a love language I can bless without a book.", "smile")
            hero.id == "rin" && gift.id == "vow_ribbon" -> L("Rin", "Careful. I might start acting like someone who can be tied to a place.", "shy")
            delta >= 12 -> L(hero.name, "This isn't a stall trinket. You thought about me in particular. I will not pretend I didn't notice.", "shy")
            else -> return null
        }
        return sc("rare_${hero.id}_${gift.id}", hero.id, "guild", N("You offer ${hero.name} the ${gift.name.lowercase()}."), line, L(hero.name, "I'll keep this where I can reach it.", "smile"))
    }

    val allExtraScenes: List<Scene> get() = romance + nimTalks + listOf(nimDate, recruitNim, endingHall, endingLanterns, endingIron, endingQuiet)
}
