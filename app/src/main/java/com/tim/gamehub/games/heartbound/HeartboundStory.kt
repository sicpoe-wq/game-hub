package com.tim.gamehub.games.heartbound

data class Line(val speaker: String, val text: String, val expr: String = "idle", val fade: Boolean = false)
data class Choice(val text: String, val delta: Int = 0, val flag: String? = null, val next: List<Line> = emptyList(), val recruit: Boolean = false, val stat: String? = null, val statMin: Int = 0, val origin: String? = null, val gain: String? = null, val needFreeRoute: Boolean = false, val needRoute: String? = null)
data class Scene(
    val id: String,
    val heroId: String?,
    val bg: String,
    val title: String? = null,
    val reqAff: Int = 0,
    val lines: List<Line>,
    val choices: List<Choice> = emptyList(),
    val needFlag: String? = null,
    val needChapter: Int = 0,
    val routeOnly: Boolean = false,
)

internal fun L(speaker: String, text: String, expr: String = "idle") = Line(speaker, text, expr)
internal fun N(text: String) = Line("", text)
internal fun F(text: String) = Line("", text, fade = true)
internal fun sc(
    id: String, hero: String?, bg: String, vararg lines: Line,
    title: String? = null, req: Int = 0, choices: List<Choice> = emptyList(),
    needFlag: String? = null, needChapter: Int = 0, routeOnly: Boolean = false,
) = Scene(id, hero, bg, title, req, lines.toList(), choices, needFlag, needChapter, routeOnly)

object Story {
    val intro = sc(
        "intro", null, "guild",
        N("The key is heavier than it looks. Lumenreach's guild hall smells of old smoke, beeswax, and a life that almost ended."),
        N("The last master left a ledger, a debt, and two people who refused to leave with the furniture."),
        L("Kael", "You're the new guildmaster, then. I'll take the measure of you the usual way — by what you do, not by the title on the door."),
        L("Yuna", "Don't let him frighten you. The kettle's on, the rooms are aired, and we have been hoping someone would come."),
        L("Yuna", "We're smaller than we were. But the hall is still a hall, if you want it to be."),
        choices = listOf(
            Choice("I'll earn the title. Starting today.", 0, next = listOf(L("Kael", "Good. I can work with that."), L("Yuna", "Then welcome home, {name}.", "smile"))),
            Choice("I'm just glad the two of you stayed.", 0, next = listOf(L("Yuna", "That is a very kind thing to say on a first morning."), L("Kael", "...We'll see that you don't regret the company."))),
            Choice("A kettle sounds like the right kind of beginning.", 0, next = listOf(L("Yuna", "I was hoping you'd say that."), L("Kael", "Drink first. Then the board. The work will not drink itself."))),
        ),
    )

    val recruitMira = sc(
        "recruit_mira", "mira", "guild",
        N("On the second morning a woman in travel-worn scholar's robes is already inside, as if the door were a theory she had solved."),
        L("Mira", "Ah. You would be the new master. I'm Mira Solen. I left the Ardent Academy with most of my notes and almost none of their permission."),
        L("Mira", "I need a table, a window, and a place that will not confiscate a hypothesis. I can work. I can also set small things on fire, but only on purpose, lately."),
        title = "A Scholar at the Door",
        choices = listOf(
            Choice("We have a table. The window you'll have to share.", 4, "recruited_mira", listOf(L("Mira", "Sharing is a kind of data. I accept."), L("Yuna", "I'll put extra honey out. Scholars forget to eat.")), true),
            Choice("If you can keep the hall standing, you're in.", 3, "recruited_mira", listOf(L("Mira", "A reasonable condition. I find architecture very motivating.")), true),
            Choice("Give me a day to ready a room.", 0, next = listOf(L("Mira", "Of course. I'll be at the fountain. The light there is honest."))),
        ),
    )

    val recruitSera = sc(
        "recruit_sera", "sera", "forest",
        N("A ranger is waiting at the tree line as if she grew there, a hawk feather tied in her braid."),
        L("Sera", "You're the ones using the east path. You didn't wreck the undergrowth. That's rarer than you'd think."),
        L("Sera", "I'm Sera Thorne. I used to watch these woods when there were more of them. If your guild is going to keep walking my trails, I'd rather walk them with you."),
        title = "The Watch at the Trees",
        choices = listOf(
            Choice("We'd be lucky to have you.", 4, "recruited_sera", listOf(L("Sera", "Luck is a weather. I'll bring skill."), N("She nods once, as if a pact has been marked on a map.")), true),
            Choice("The woods could use a friend in the hall.", 5, "recruited_sera", listOf(L("Sera", "Careful. That almost sounded like belonging."), L("Sera", "...All right. I'll try the door.")), true),
            Choice("Not today — but I won't forget you.", 0, next = listOf(L("Sera", "I'll be here. The trees aren't going anywhere. Neither, for now, am I."))),
        ),
    )

    val recruitRin = sc(
        "recruit_rin", "rin", "town",
        N("A young man is perched on the guild's lower window like he pays rent there. He does not."),
        L("Rin", "You look understaffed. I'm Rin. I open things, I find things, I occasionally return things. Relatively honest, depending on the week."),
        L("Rin", "I work cheap. I stay cheaper if the soup is decent and nobody asks where I learned to climb."),
        title = "A Window Applicant",
        choices = listOf(
            Choice("Come in the door next time. You're hired.", 4, "recruited_rin", listOf(L("Rin", "The door. How formal of you."), L("Rin", "All right, Guildmaster. I'll try it your way.")), true),
            Choice("Soup we have. Trust you'll have to earn.", 3, "recruited_rin", listOf(L("Rin", "That's the first honest offer I've had all month. I'll take it.")), true),
            Choice("Give me a day to think it over.", 0, next = listOf(L("Rin", "Sure. I'll be around. I'm extremely good at being around."))),
        ),
    )

    val events: List<Scene> = listOf(
        // —— Kael ——
        sc("kael_e1", "kael", "guild", N("You find Kael in the yard before dawn, oiling a blade that already shines."),
            L("Kael", "This was my company's sword. They let me keep the steel when they took the colors."),
            L("Kael", "I was ordered to put a village to the torch over a missed tithe. I said no. It was the first time I had ever said it, and the last day I wore their cloak."),
            L("Kael", "I don't tell that story often. I'm not sure why I am telling it now.", "grim"),
            title = "The Unfinished Sword", req = 20,
            choices = listOf(
                Choice("Because you wanted someone here to know you.", 6, next = listOf(L("Kael", "That's a bold reading.", "shy"), L("Kael", "...It isn't a wrong one.", "smile"))),
                Choice("You did the right thing. The sword knows it, even if the company didn't.", 7, next = listOf(L("Kael", "Hnh."), N("He looks at the steel a long time, then at you, and some of the iron in his shoulders eases."))),
                Choice("Sit. You don't have to finish the story today.", 5, next = listOf(L("Kael", "I think I already did."), L("Kael", "Thank you — for the sitting, not the silence. The silence I can do alone."))),
                Choice("I know what it costs to refuse an order. I did it too.", 8, next = listOf(L("Kael", "Then we are rarer company than I thought, {name}.", "wow"), L("Kael", "I'm glad it was you who came through that door.", "smile")), stat = "courage", statMin = 2, origin = null, gain = "courage"),
            ),
        ),
        sc("kael_e2", "kael", "guild", N("A sealed letter has arrived for Kael. The wax is the color of an old authority. He has not opened it."),
            L("Kael", "They want a word. Or a warning. Or an apology I will not be giving."),
            L("Kael", "I have a life here now. I did not expect that to become a reason."),
            title = "A Letter from the Holds", req = 40,
            choices = listOf(
                Choice("You don't owe them an answer.", 6, "kael_no_letter", listOf(L("Kael", "No. I don't."), L("Kael", "I'll keep the seal unbroken. Some doors are better left as doors."))),
                Choice("If you want to answer, I'll go with you.", 8, "kael_answer_letter", listOf(L("Kael", "You would ride all that way for a letter I haven't opened."), L("Kael", "Keep a day free, then. Not yet. But... keep it."))),
                Choice("Burn it. You're already home.", 5, "kael_burn_letter", listOf(L("Kael", "That's dramatic."), N("He feeds the letter to the hall fire anyway, and does not look away until the wax is gone."), L("Kael", "Home. You make it sound simple."))),
            ),
        ),
        sc("kael_e3", "kael", "guild", N("You find him on the hall roof, a lantern unlit at his knee, the town a scatter of gold below."),
            L("Kael", "I sleep easier knowing you're under this roof. That's an unprofessional thing to say. I'm saying it anyway.", "shy"),
            L("Kael", "If you asked me to leave tomorrow, I would go. If you asked me to stay, I would do that more gladly.", "smile"),
            title = "The Night Watch", req = 60,
            choices = listOf(
                Choice("Stay. And take my hand while you do it.", 10, "kael_romance", listOf(N("His glove comes off. His hand is warm, and a little unsteady, which he will deny later."), L("Kael", "All right. I'm here.", "shy"))),
                Choice("I rely on you too. More than I planned to.", 9, "kael_romance", listOf(L("Kael", "Then we are both bad at planning."), L("Kael", "I find I don't mind."))),
                Choice("Watch the town with me a while. That's enough for tonight.", 7, next = listOf(L("Kael", "It's a good town."), N("He does not move away. The lantern stays unlit, and the quiet does all the rest of the work."))),
            ),
        ),
        // —— Mira ——
        sc("mira_e1", "mira", "guild", N("Mira has fallen asleep at the long table, cheek on an open book, a spill of silver hair across a diagram of the sky."),
            N("When she wakes she looks first at the page, then at you, then at the page again, as if one of you might be a variable."),
            L("Mira", "I was mapping the constellations over Lumenreach. They sit a fraction west of the academy charts. Either the sky has moved, or the academy was slightly wrong, which is the more exciting option."),
            L("Mira", "You didn't have to stay. But I'm... glad the first face I saw was a kind one.", "shy"),
            title = "Asleep on the Theorems", req = 20,
            choices = listOf(
                Choice("Teach me the names. I'll bring the tea.", 7, next = listOf(L("Mira", "Tea and astronomy. That's a very civilized hypothesis."), L("Mira", "We'll start with the easy ones. I suspect you'll be better at this than you think."))),
                Choice("I brought a blanket. The stars can wait an hour.", 6, next = listOf(L("Mira", "That's — oh."), N("She accepts the blanket with both hands, as if it were a rare instrument."), L("Mira", "An hour, then. Don't let me negotiate for two."))),
                Choice("You look less alarming when you're not mid-experiment.", 4, next = listOf(L("Mira", "I shall add that to my notes. 'Appears less alarming when horizontal.'"), L("Mira", "Thank you for waking me kindly."))),
                Choice("If the academy charts are wrong, we should write better ones. I'll help.", 8, next = listOf(L("Mira", "You said 'we.' That's my favorite pronoun, in this context.", "wow"), L("Mira", "Bring ink tomorrow, {name}.", "smile")), stat = "wit", statMin = 2, gain = "wit"),
            ),
        ),
        sc("mira_e2", "mira", "guild", N("A meek plume of violet smoke is curling from under the storeroom door. Mira is already apologizing to the doorframe."),
            L("Mira", "It was supposed to be a contained demonstration of residual starlight. It was contained. Then it was slightly less contained."),
            L("Mira", "I can clean it. I can also, if you prefer, go back to the fountain and be a problem somewhere with better ventilation."),
            title = "Smoke in the Storeroom", req = 40,
            choices = listOf(
                Choice("I'll help you clean. Failure is just data.", 8, "mira_data", listOf(L("Mira", "That is the correct sentence. Almost nobody says the correct sentence."), N("Together you open the windows. The smoke smells like rain on hot stone."))),
                Choice("Are you hurt? The hall can take a scorch. You can't.", 7, "mira_care", listOf(L("Mira", "I'm not hurt. That's the first thing you asked."), L("Mira", "I will put that in a margin, where I keep the important things."))),
                Choice("Next time, tell me before you test the laws of nature indoors.", 4, next = listOf(L("Mira", "A fair protocol. I'll write it above the workbench."), L("Mira", "In large letters."))),
            ),
        ),
        sc("mira_e3", "mira", "town", N("She leads you to the clock-tower hill after dusk, a palm-sized orrery ticking softly in her hands."),
            L("Mira", "I made this after I left. The academy would have called it sentimental. I call it a way of keeping the sky close."),
            L("Mira", "I never stopped looking up. It's just nicer, it turns out, with someone else in the frame."),
            title = "The Little Orrery", req = 60,
            choices = listOf(
                Choice("Then I'll keep standing here, if you'll have me.", 10, "mira_romance", listOf(L("Mira", "I have run the numbers."), L("Mira", "The numbers are very much in favor of you staying.", "shy"))),
                Choice("What do you wish for, when you look up?", 8, "mira_romance", listOf(L("Mira", "A room with a window. Work that doesn't have to hide. And —"), L("Mira", "Someone who asks me questions like that."))),
                Choice("It's a beautiful sky. Thank you for sharing it.", 7, next = listOf(L("Mira", "Sharing is a kind of data."), N("She smiles at the old joke, and does not step away from your shoulder."))),
            ),
        ),
        // —— Rin ——
        sc("rin_e1", "rin", "town", N("You catch Rin slipping a coin-purse back through an open market window. He looks like a cat who has been found doing charity."),
            L("Rin", "Don't make that face. I only borrowed it to prove I could. The baker is careless and lucky I was in a good mood."),
            L("Rin", "What? If I wanted to keep it, you would not have seen me."),
            title = "A Purse Returned", req = 20,
            choices = listOf(
                Choice("That was decent of you. You can admit it.", 7, next = listOf(L("Rin", "Ugh. Decency. Don't tell the alleys."), L("Rin", "...Thanks, I guess, for not assuming the worst first."))),
                Choice("You wanted me to see you do it.", 6, next = listOf(L("Rin", "That's a dangerous theory."), L("Rin", "Maybe I did. Maybe I wanted the new master to know I can be more than a window."))),
                Choice("Next time, skip the theft and just walk her the purse.", 4, next = listOf(L("Rin", "Where's the theatre in that?"), L("Rin", "Fine. Once. For you."))),
                Choice("You wanted a witness who would think well of you. I do.", 8, next = listOf(L("Rin", "That's illegal.", "shy"), L("Rin", "Say it quieter next time. Or don't.", "smile")), stat = "charm", statMin = 2, gain = "charm"),
            ),
        ),
        sc("rin_e2", "rin", "tavern", N("A stranger leaves the tavern as you arrive. Rin's smile is the wrong shape."),
            L("Rin", "Old crew. They want me for one last job. A counting-house, a lock I already know, a split I don't trust."),
            L("Rin", "I told them I'd think about it. I am thinking about it. That's the problem."),
            title = "One Last Job", req = 40,
            choices = listOf(
                Choice("I trust you to walk away from it.", 7, "rin_refused", listOf(L("Rin", "That's a lot of trust to hand a former thief over a half-finished drink."), L("Rin", "All right. I'll send them a no. A charming no. A no with style."))),
                Choice("If you go, I'm coming as backup.", 8, "rin_backup", listOf(L("Rin", "You'd get in so much trouble."), L("Rin", "Which is why I'm not going. I like this hall. I like... this. More than a split."))),
                Choice("You're not doing it. I won't let them use you.", 3, "rin_forbid", listOf(L("Rin", "Careful. I don't like being owned, even by people I —"), L("Rin", "Skip it. I won't do the job. But ask me next time, don't tell me."))),
            ),
        ),
        sc("rin_e3", "rin", "tavern", N("The tavern is closed. Rin is still here, boots on a chair he will deny putting there, a last lantern burning low."),
            L("Rin", "I've never stayed anywhere this long. Don't make it a big thing."),
            L("Rin", "It's just... the soup is decent. The lock on my door is one I set myself. And you keep looking at me like I'm someone worth keeping around."),
            title = "After Hours", req = 60,
            choices = listOf(
                Choice("It's a home if you want it. I want you to stay.", 10, "rin_romance", listOf(L("Rin", "That's a big thing."), N("He looks at the lantern instead of you, then gives up and looks at you anyway."), L("Rin", "All right. I'll stay. Don't get weird about it. Or do. I might not mind.", "shy"))),
                Choice("Bump his shoulder, lightly, and let that be the promise.", 8, "rin_romance", listOf(L("Rin", "Wow. Very eloquent."), L("Rin", "I'm keeping that, just so you know."))),
                Choice("You are worth keeping around. I'll say it as many times as you need.", 9, "rin_romance", listOf(L("Rin", "Once is already going to live in my head rent-free."), L("Rin", "Say it again tomorrow anyway."))),
            ),
        ),
        // —— Sera ——
        sc("sera_e1", "sera", "guild", N("Sera sets a bundle of wild mint on the table without preface, as if the herbs had arrived on their own."),
            L("Sera", "The hall smells like old smoke. This helps. Hang it by the hearth."),
            L("Sera", "I used to do this for the watch tower. It's a habit. You can call it a gift if you need a word for it."),
            title = "Mint for the Hall", req = 20,
            choices = listOf(
                Choice("Thank you. I'll hang it myself.", 6, next = listOf(L("Sera", "Good."), N("She almost smiles. The mint already makes the air feel less like a leftover fire."))),
                Choice("Will you tell me about the fire, someday?", 5, next = listOf(L("Sera", "Someday is a door I keep closed."), L("Sera", "...Not locked, though. Don't push tonight. Just don't go far."))),
                Choice("Stay for tea. Gifts should be sat with.", 7, next = listOf(L("Sera", "That's a rule I haven't heard."), L("Sera", "All right. I'll sit. Don't expect a speech with the tea."))),
            ),
        ),
        sc("sera_e2", "sera", "guild", N("A storm leans on the hall. Sera is on the porch in it, hood down, rain in her braid, watching the trees toss."),
            L("Sera", "The night the woods went, it sounded like this at first. Just weather. Then it wasn't."),
            L("Sera", "I couldn't get everyone out. I still walk as if I can make the path shorter by wanting it."),
            title = "Rain on the Porch", req = 40,
            choices = listOf(
                Choice("Bring a cloak out and stand with her in it.", 8, "sera_cloak", listOf(N("She lets you set the cloak on her shoulders. She does not come inside yet, but she is no longer alone in the weather."), L("Sera", "You didn't tell me to come in. That's why I might."))),
                Choice("I'm listening. For as long as it takes.", 8, "sera_listen", listOf(L("Sera", "It may take a while."), L("Sera", "That's all right. You're a patient kind of trail."))),
                Choice("You're not there anymore. You're here. That's allowed.", 7, next = listOf(L("Sera", "Allowed."), N("She tests the word, then nods, once, as if marking a new path on a private map."))),
            ),
        ),
        sc("sera_e3", "sera", "forest", N("She takes you off the marked path to a little stone shrine and a sapling tied with a faded green ribbon."),
            L("Sera", "The watch tower stood here. I planted this the spring after. I didn't think I'd show anyone."),
            L("Sera", "I still don't know if the woods will forgive us. I know I wanted you to see what I'm trying to keep.", "shy"),
            title = "The Sapling", req = 60,
            choices = listOf(
                Choice("Then we'll keep it together. I'm not going anywhere.", 10, "sera_romance", listOf(L("Sera", "That's a vow, where I come from."), L("Sera", "I'll hold you to it. Gladly."))),
                Choice("Take her hand and stand with the sapling a while.", 10, "sera_romance", listOf(N("Her palm is calloused and careful. A bird starts up somewhere in the new leaves."), L("Sera", "All right. This is a good place to be quiet with someone."))),
                Choice("It's a brave tree. Like the person who planted it.", 7, next = listOf(L("Sera", "Don't make me famous for a sapling."), L("Sera", "...Thank you. For coming this far."))),
            ),
        ),
        // —— Yuna ——
        sc("yuna_e1", "yuna", "guild", N("Behind the hall, Yuna is kneeling in a rectangle of turned earth, a row of seedlings already labeled in a careful hand."),
            L("Yuna", "The temple had marble. I like this better. Prayers with dirt under the nails feel like they know where they're going."),
            L("Yuna", "You can stand there and look kindly at the mint, or you can come down here and be useful. Both are welcome."),
            title = "Dirt Under the Nails", req = 20,
            choices = listOf(
                Choice("Kneel and help. Show me which ones are the mint.", 7, next = listOf(L("Yuna", "That one. And that one. And — carefully — that one, it's shy."), L("Yuna", "You're a natural. Or you are very good at following a voice you trust."))),
                Choice("Why did you really leave the temple?", 5, next = listOf(L("Yuna", "Because they healed the rich first and called it order."), L("Yuna", "I still believe in the work. I just believe it belongs to anyone who walks in cold."))),
                Choice("Your patience is one of the better things in this hall.", 6, next = listOf(L("Yuna", "Careful. I'll start blushing at vegetables."), L("Yuna", "Thank you. It's easier to be patient when someone notices."))),
            ),
        ),
        sc("yuna_e2", "yuna", "guild", N("A traveler is asleep on the hall bench, fever-flushed. Yuna's sleeves are pushed up, a basin of herb-water steaming beside her."),
            L("Yuna", "The temple turned him away. Not enough coin, not enough daylight. I told him we don't keep those ledgers here."),
            L("Yuna", "I'll be up. You don't have to be. I wanted you to know, in case you wondered where the kettle went."),
            title = "The Night Patient", req = 40,
            choices = listOf(
                Choice("I'm staying. Tell me how to help.", 8, "yuna_helped", listOf(L("Yuna", "Fresh water. A clean cloth. And sit where he can see a second face if he wakes."), L("Yuna", "I forgot what it felt like, not doing this alone."))),
                Choice("I'll fetch supplies. Make a list.", 7, "yuna_supplies", listOf(L("Yuna", "Willow, clean linen, a little honey. And something for you to eat, while you're being kind."), L("Yuna", "Go gently. I'll be here."))),
                Choice("You should rest. I'll take a watch.", 5, next = listOf(L("Yuna", "I won't sleep while he's like this. But I will sit down, if you'll sit too."), L("Yuna", "A compromise. My favorite kind of prayer."))),
            ),
        ),
        sc("yuna_e3", "yuna", "town", N("The day leans gold over the square. Yuna sits on the guild steps with two cups, as if she knew you would come."),
            L("Yuna", "I was afraid that leaving the temple meant leaving my purpose. It didn't. It just moved house."),
            L("Yuna", "I think purpose looks a little like this: a door we leave unlatched, a garden that needs water, and a person I am glad to see at the end of a long day."),
            title = "Sunset on the Steps", req = 60,
            choices = listOf(
                Choice("If I'm that person, then I'm exactly where I should be.", 10, "yuna_romance", listOf(L("Yuna", "You are.", "shy"), N("She sets her cup down so she can hold your hand properly, like a blessing she has decided not to be shy about."))),
                Choice("Take her hand. Let the sunset do some of the talking.", 10, "yuna_romance", listOf(L("Yuna", "Oh."), L("Yuna", "Yes. This is also a kind of prayer."))),
                Choice("I'm glad you chose this hall. I'm glad you chose us.", 8, next = listOf(L("Yuna", "Us is a lovely word. I'll keep it."), L("Yuna", "Drink your tea before it cools, Guildmaster. Some blessings are practical."))),
            ),
        ),
    )

    val talks: List<Scene> = listOf(
        sc("kael_t1", "kael", "guild", L("Kael", "The strap on this pauldron is thinning. I'll stitch it tonight."), L("Kael", "Did you need something, or are you inspecting the inspector?"),
            choices = listOf(
                Choice("I wanted to see how you're settling in.", 4, next = listOf(L("Kael", "That's considerate. I'll be fine. But thank you."))),
                Choice("Just making sure my knight hasn't rusted.", 5, next = listOf(L("Kael", "Hnh. Still sharp enough, Guildmaster."))),
            )),
        sc("kael_t2", "kael", "guild", L("Kael", "You skip meals when the board is busy. I noticed."), L("Kael", "There's bread. I didn't bake it, so it's safe."),
            choices = listOf(
                Choice("Then I'll eat, if you'll sit too.", 5, next = listOf(L("Kael", "A reasonable bargain. Move over."))),
                Choice("You noticed that?", 4, next = listOf(L("Kael", "I notice most things. It's a professional failing."))),
            )),
        sc("kael_t3", "kael", "town", L("Kael", "The square is loud today. I don't hate it. I just prefer a wall at my back."),
            choices = listOf(
                Choice("I'll be the wall for a minute.", 5, next = listOf(L("Kael", "That's... not what I meant. It's fine. Stay."))),
                Choice("Want to walk the quiet lane instead?", 4, next = listOf(L("Kael", "Yes. Thank you for not making me ask."))),
            )),
        sc("mira_t1", "mira", "guild", L("Mira", "If you see a chalk circle in the east corridor, that's not a trap. It's a reminder. Mostly."),
            choices = listOf(
                Choice("Define 'mostly.'", 4, next = listOf(L("Mira", "It has a ninety-four percent chance of being a reminder. I like those odds."))),
                Choice("I'll walk around it and trust you.", 5, next = listOf(L("Mira", "Trust is very scientific, actually. Thank you."))),
            )),
        sc("mira_t2", "mira", "town", L("Mira", "The bookshop acquired a treatise with the wrong date on the spine. I may have raised my voice. A little."),
            choices = listOf(
                Choice("I'll go back with you. We can be civil and terrifying.", 5, next = listOf(L("Mira", "Civil and terrifying. I would like that on a seal."))),
                Choice("I wish I'd seen that.", 4, next = listOf(L("Mira", "I was very dignified. The bookseller may remember it differently."))),
            )),
        sc("mira_t3", "mira", "guild", L("Mira", "You have ink on your wrist. Unless that's a new fashion in guildmasters, in which case I support it."),
            choices = listOf(
                Choice("It's from your notes. I was reading them.", 5, next = listOf(L("Mira", "Oh. That's — you can. I mean. Please do."))),
                Choice("Help me get it off?", 4, next = listOf(L("Mira", "Hold still. I have a solvent that is only slightly too interesting."))),
            )),
        sc("rin_t1", "rin", "tavern", L("Rin", "If anyone asks, I won this drink fairly. If they ask twice, I won it charmingly."),
            choices = listOf(
                Choice("Deal me in. I'll keep an eye on your sleeves.", 5, next = listOf(L("Rin", "Rude. Correct. Sit down."))),
                Choice("Charming is doing a lot of work in that sentence.", 4, next = listOf(L("Rin", "It's my best quality. After my sleeves."))),
            )),
        sc("rin_t2", "rin", "guild", L("Rin", "I fixed the catch on the storeroom window. You're welcome. Please do not ask how I knew it was broken."),
            choices = listOf(
                Choice("Thank you. I was going to ask you to look at it anyway.", 5, next = listOf(L("Rin", "See? Destined. Or nosy. Fine line."))),
                Choice("I'm choosing not to ask. Enjoy that gift.", 4, next = listOf(L("Rin", "Wow. Growth. For both of us."))),
            )),
        sc("rin_t3", "rin", "town", L("Rin", "Don't look now, but the baker just hid her tips box. My reputation is inescapable and also a little exaggerated."),
            choices = listOf(
                Choice("We'll buy something. Leave a little extra.", 5, next = listOf(L("Rin", "You're making me look like a reformed character. I hate that I like it."))),
                Choice("Wave at her. Commit to the bit.", 3, next = listOf(L("Rin", "You're a menace. Fine. I'm waving. This is humiliating."))),
            )),
        sc("sera_t1", "sera", "forest", L("Sera", "The east path is flowering early. I thought you might want to see it before the town does."),
            choices = listOf(
                Choice("Show me. I'll walk where you walk.", 5, next = listOf(L("Sera", "Keep to the left of the roots. That's all the lesson for today."))),
                Choice("You thought of me, out here?", 5, next = listOf(L("Sera", "Don't look so surprised. It's a small thought. It fit in my pocket."))),
            )),
        sc("sera_t2", "sera", "guild", L("Sera", "Your stance when you lift the firewood is going to hurt you in ten years. Here. Like this."),
            choices = listOf(
                Choice("Like this?", 4, next = listOf(L("Sera", "Better. We'll make a ranger of you yet. A slow one."))),
                Choice("Are you always this gently bossy?", 5, next = listOf(L("Sera", "Only with people I intend to keep."))),
            )),
        sc("sera_t3", "sera", "forest", L("Sera", "If we are quiet, a fox will cross the creek. She has a limp. I don't hunt her. Neither will you."),
            choices = listOf(
                Choice("We'll wait. I can be quiet.", 5, next = listOf(N("You wait. The fox does come, and Sera's face eases in a way you file away for later."))),
                Choice("You know all their names, don't you?", 4, next = listOf(L("Sera", "Not their names. Their habits. That's closer to knowing."))),
            )),
        sc("yuna_t1", "yuna", "guild", L("Yuna", "The kettle sings if you let it, you know. I like to think that's the hall talking."),
            choices = listOf(
                Choice("Then let's listen a minute.", 4, next = listOf(L("Yuna", "A very wise use of a guildmaster's time."))),
                Choice("What is it saying today?", 5, next = listOf(L("Yuna", "That you work too hard, and that the tea is ready. It's a very practical hall."))),
            )),
        sc("yuna_t2", "yuna", "town", L("Yuna", "The herb-seller tried to charge me twice for feverfew. I smiled at him until he remembered his sums."),
            choices = listOf(
                Choice("Remind me never to haggle with you.", 5, next = listOf(L("Yuna", "I only use the smile for justice. And occasionally for cake."))),
                Choice("Need a menacing guildmaster at your elbow next time?", 4, next = listOf(L("Yuna", "You can come. But I do the smiling. You may hold the basket."))),
            )),
        sc("yuna_t3", "yuna", "guild", L("Yuna", "You look tired. Sit. This is not a request dressed up as kindness. It is a request."),
            choices = listOf(
                Choice("Yes, healer.", 5, next = listOf(L("Yuna", "Good. I've already poured. You're worse at resting than Kael, and that's a feat."))),
                Choice("Only if you sit as well.", 5, next = listOf(L("Yuna", "A negotiation. Fine. I'll sit. Don't tell the mint I took a break."))),
            )),
    )

    val dates: List<Scene> = listOf(
        sc("kael_date", "kael", "town", N("Kael pretends he is only in the square to inspect a cutler's stall. He has already inspected it twice."),
            L("Kael", "The balance on that short sword is honest. So is this tea shop, if you still have time."),
            title = "Tea After Steel",
            choices = listOf(
                Choice("I have time. I saved it for you.", 9, next = listOf(L("Kael", "Then I'll try to be worth the hour."), N("He pays before you can, and looks faintly pleased with himself."))),
                Choice("Lead on, knight.", 7, next = listOf(L("Kael", "It's not a battlefield."), L("Kael", "All right. It's a little like one. Follow me."))),
            )),
        sc("mira_date", "mira", "town", N("Mira has a key she absolutely should not have, and the clock tower's upper stair smells of dust and possibility."),
            L("Mira", "The stars will be out in twenty minutes. I checked. Twice. I wanted this to go well."),
            title = "The Clock Tower",
            choices = listOf(
                Choice("It already is.", 9, next = listOf(L("Mira", "Oh."), L("Mira", "I'll still check the stars. But I'll believe you too."))),
                Choice("Twenty minutes of you talking about the sky sounds perfect.", 8, next = listOf(L("Mira", "Careful. I can do an hour. I have visual aids."))),
            )),
        sc("rin_date", "rin", "tavern", N("Rin deals a slow hand in the corner and does not, for once, hide a card in his sleeve."),
            L("Rin", "Tonight I'm playing fair. It's very stressful. You should compliment me frequently."),
            title = "Honest Cards",
            choices = listOf(
                Choice("You're doing beautifully. Even without cheating.", 8, next = listOf(L("Rin", "I'm going to need a moment. That was illegal."), L("Rin", "Say it again after I lose."))),
                Choice("If you win fairly, I'll buy the next round.", 7, next = listOf(L("Rin", "Motivation. I love motivation. Deal."))),
            )),
        sc("sera_date", "sera", "forest", N("She takes the long path on purpose. Light falls through the leaves in slow coins of gold."),
            L("Sera", "I don't bring people this way. It isn't secret. It's just mine. Today it can be ours."),
            title = "The Long Way Home",
            choices = listOf(
                Choice("Then I'll walk it carefully.", 9, next = listOf(L("Sera", "You already are."), N("She matches her pace to yours and does not hurry the creek."))),
                Choice("Thank you for trusting me with it.", 8, next = listOf(L("Sera", "Thank you for not asking me to talk over it."))),
            )),
        sc("yuna_date", "yuna", "town", N("Yuna moves through the market like a blessing in comfortable shoes, naming plants as if they were old friends."),
            L("Yuna", "This is my favorite hour in town. Everyone is hungry for something gentle. I thought you might be, too."),
            title = "The Herb Market",
            choices = listOf(
                Choice("I am. I'm glad it's with you.", 9, next = listOf(L("Yuna", "Then we'll take the long way back, and I'll buy the honey cakes."), L("Yuna", "Don't argue. It's a healing."))),
                Choice("Tell me every name. I want to learn your hour.", 8, next = listOf(L("Yuna", "You are going to regret the thoroughness of that request."), L("Yuna", "Or, if you are very unlucky, you won't."))),
            )),
    )

    fun allBondEvents(): List<Scene> = events + Arc.romance + Life.romance

    fun eventFor(heroId: String, aff: Int, seen: Set<String>) =
        events.filter { it.heroId == heroId && aff >= it.reqAff && it.id !in seen }.minByOrNull { it.reqAff }

    fun eventFor(heroId: String, state: GameState): Scene? {
        val aff = state.h(heroId).affection
        val seen = state.h(heroId).seenEvents
        return allBondEvents().filter { it.heroId == heroId && aff >= it.reqAff && it.id !in seen && available(it, state) }
            .minByOrNull { it.reqAff }
    }

    fun available(scene: Scene, s: GameState): Boolean {
        if (scene.needFlag != null && scene.needFlag !in s.flags) return false
        if (s.world.chapter < scene.needChapter) return false
        if (scene.routeOnly && s.world.route != scene.heroId) return false
        return true
    }

    fun talksFor(heroId: String) = (talks + Life.nimTalks).filter { it.heroId == heroId }
    fun baseDate(heroId: String) = dates.firstOrNull { it.heroId == heroId } ?: Life.nimDate
    fun dateFor(heroId: String, place: String = "town") = Life.withPlace(baseDate(heroId), place)
    fun eventById(id: String): Scene? {
        (allBondEvents() + Arc.plot + Arc.care + Arc.rival + Life.allExtraScenes).firstOrNull { it.id == id }?.let { return it }
        if (id.startsWith("visit_") && id.removePrefix("visit_") in Heartbound.hero) return Life.visitScene(id.removePrefix("visit_"), 50)
        if (id.startsWith("bday_") && id.removePrefix("bday_") in Heartbound.hero) return Life.birthdayScene(id.removePrefix("bday_"))
        Arc.groupsCalendar.forEach { ev ->
            Heartbound.heroes.forEach { h ->
                if (id == "${ev.id}_${h.id}") return Arc.groupScene(ev.id, h.id)
            }
        }
        return null
    }

    fun giftLines(hero: HeroDef, gift: GiftDef, delta: Int): Scene {
        val (line, close) = when {
            delta >= 12 -> L(hero.name, "You — this is exactly right. How did you know?") to L(hero.name, "I'll keep this where I can see it.")
            delta >= 7 -> L(hero.name, "Oh. I actually like this. That's either good taste or good listening.") to L(hero.name, "Thank you. Truly.")
            delta < 0 -> L(hero.name, "Ah. I... appreciate the thought. Maybe not the thing itself.") to L(hero.name, "Don't look so grim. The thought still counts. A little.")
            else -> L(hero.name, "That's kind of you. I'll find a place for it.") to L(hero.name, "Thank you, Guildmaster.")
        }
        return sc("gift_${hero.id}_${gift.id}", hero.id, "guild", N("You offer ${hero.name} the ${gift.name.lowercase()}."), line, close)
    }

    fun recruitScene(id: String) = when (id) {
        "mira" -> recruitMira
        "sera" -> recruitSera
        "rin" -> recruitRin
        "nim" -> Life.recruitNim
        else -> recruitMira
    }

    fun introFor(p: Player): Scene {
        val extra = when (p.origin) {
            "soldier" -> listOf(L("Kael", "You carry yourself like someone who has already said no to a bad order. I can work with that, {name}.", "smile"))
            "scholar" -> listOf(L("Yuna", "A scholar's hands. The hall has missed a good library, {name}.", "smile"))
            "merchant" -> listOf(L("Yuna", "You look like someone who has talked a stall down to a fair price. We'll need that.", "smile"))
            else -> listOf(L("Yuna", "A traveler's pack, and a key. That's as honest a beginning as any, {name}.", "smile"))
        }
        val base = intro
        val originChoice = when (p.origin) {
            "soldier" -> Choice("I left a banner. I won't waste a hall.", 0, next = listOf(L("Kael", "Then we understand each other.", "smile")), origin = "soldier")
            "scholar" -> Choice("I read the ledger on the way in. We'll put the books in order.", 0, next = listOf(L("Yuna", "I was hoping you'd say that.", "smile")), origin = "scholar", stat = "wit", statMin = 2)
            "merchant" -> Choice("A hall is a kind of shop. I'll keep it solvent, and kind.", 0, next = listOf(L("Yuna", "Solvent and kind. That's a prayer I can say.", "smile")), origin = "merchant")
            else -> Choice("I don't have a house behind me. I'd like this to be one.", 0, next = listOf(L("Yuna", "Then it will be, {name}.", "smile")), origin = "wanderer")
        }
        return base.copy(lines = base.lines + extra, choices = base.choices + originChoice)
    }

    fun unlocked(c: Choice, p: Player): Boolean {
        if (c.stat != null && p.stat(c.stat) < c.statMin) return false
        if (c.origin != null && p.origin != c.origin) return false
        return true
    }

    fun unlocked(c: Choice, s: GameState, heroId: String? = null): Boolean {
        if (!unlocked(c, s.player)) return false
        if (c.needFreeRoute && s.world.route != null && s.world.route != heroId) return false
        if (c.needRoute != null && s.world.route != c.needRoute) return false
        return true
    }
}
