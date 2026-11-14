package com.tim.gamehub.games.heartbound

data class GroupEvent(val id: String, val minDay: Int, val title: String, val blurb: String, val bg: String)

object Arc {
    val groupsCalendar = listOf(
        GroupEvent("festival", 6, "Festival of Lanterns", "Lumenreach is hanging lanterns over the square. The town will be gold until midnight. Who walks it with you?", "festival"),
        GroupEvent("beach", 11, "Shore Day", "The lake has gone still and warm. Anyone with sense is leaving the board for a few hours. Who do you take to the water?", "beach"),
        GroupEvent("harvest", 16, "Harvest Dance", "The hall has been cleared, garlanded, and slightly over-lanterned. There will be music. There will be a dance, if you want one.", "harvest"),
    )

    val romance: List<Scene> = listOf(
        // —— Confessions (80) ——
        sc("kael_confess", "kael", "guild",
            N("You find him in the yard after the last lamp has been lit, the sword on the bench, his hands empty for once."),
            L("Kael", "I have been a soldier, a exile, and a door. I thought that would be enough of a life."),
            L("Kael", "It isn't. Not since you started walking through that door as if it were yours — and as if I could be, too."),
            L("Kael", "I am not good at this. I'm saying it anyway, {name}. I love you. If that is more than you wanted from your knight, tell me now, and I will still stand the watch.", "shy"),
            title = "The Watch, Spoken Aloud", req = 80,
            choices = listOf(
                Choice("I love you. Stay. That's an order I mean as a request.", 10, "kael_confess", listOf(
                    L("Kael", "Then I'll take the request.", "smile"),
                    L("Kael", "Come here. I have wanted to say your name without a title on it."),
                    F("The door of the yard office closes. The hall keeps its own counsel, and so do you."),
                ), needFreeRoute = true),
                Choice("You already have my trust. Let me give you the rest of it.", 10, "kael_confess", listOf(
                    L("Kael", "I'll be careful with it.", "shy"),
                    F("The lantern burns itself down. Morning will come when it comes."),
                ), needFreeRoute = true),
                Choice("I care for you deeply. I need the rest of the words to come slower.", 6, next = listOf(
                    L("Kael", "Then I'll wait. I am very good at watches."),
                    L("Kael", "Thank you — for not leaving me standing here with all of that unsaid."),
                )),
            ),
        ),
        sc("mira_confess", "mira", "town",
            N("The clock-tower stair is narrow. Mira has brought the little orrery and, for once, no notes."),
            L("Mira", "I have a hypothesis I have been refusing to write down, because writing it would make it real, and real things can be wrong."),
            L("Mira", "The hypothesis is that I am in love with you, {name}. The evidence has been piling up on every table I meant to keep clear.", "shy"),
            L("Mira", "If the hypothesis is unwelcome, I will put it back in a drawer and we will still map the sky. I just... wanted you to see the work."),
            title = "A Hypothesis, Spoken", req = 80,
            choices = listOf(
                Choice("It's welcome. I love you. You can write it down.", 10, "mira_confess", listOf(
                    L("Mira", "Oh.", "wow"),
                    L("Mira", "I'll need a better pen.", "smile"),
                    F("The orrery ticks in the dark. The rest of the night does not need an audience."),
                ), needFreeRoute = true),
                Choice("I've been hoping you would say it first. I love you too.", 10, "mira_confess", listOf(
                    L("Mira", "That's statistically very kind of the universe.", "shy"),
                    F("The stairwell keeps the quiet. Morning can wait."),
                ), needFreeRoute = true),
                Choice("You're important to me. I'm not ready to name it yet.", 6, next = listOf(
                    L("Mira", "Then I'll leave the hypothesis open. That's allowed, in good science."),
                    L("Mira", "Thank you for not laughing. I would have survived it. I prefer this."),
                )),
            ),
        ),
        sc("rin_confess", "rin", "tavern",
            N("The tavern has closed around the two of you. Rin is turning a coin over and over, as if it might become a braver object."),
            L("Rin", "I had a whole speech. It was funny, and it had an escape hatch. I lost it somewhere between the door and this table."),
            L("Rin", "I love you. There. No lockpick, no joke, no window. If you want me to take it back I will make a very elegant exit and you will still have a rogue in the morning.", "shy"),
            L("Rin", "I'd rather not take it back, {name}."),
            title = "No Escape Hatch", req = 80,
            choices = listOf(
                Choice("Don't take it back. I love you. Stay at the table.", 10, "rin_confess", listOf(
                    L("Rin", "I can do that.", "smile"),
                    L("Rin", "For the record, this is the least stylish I have ever been, and I don't mind."),
                    F("The last lantern is left to itself. Some things are not written down."),
                ), needFreeRoute = true),
                Choice("Come here. You don't have to make it a performance.", 10, "rin_confess", listOf(
                    L("Rin", "Good. I'm out of material.", "shy"),
                    F("The coin stops turning. The night does not need a witness."),
                ), needFreeRoute = true),
                Choice("I want you in my life. I'm not ready to call it love tonight.", 6, next = listOf(
                    L("Rin", "That's honest. I can work with honest."),
                    L("Rin", "I'll still be here in the morning. Windows are overrated."),
                )),
            ),
        ),
        sc("sera_confess", "sera", "forest",
            N("The unmarked path opens on the little shrine and the sapling, ribbon faded greener than you remember."),
            L("Sera", "I used to think belonging was a kind of trap. Then the hall happened. Then you happened, walking my trails as if you had always been allowed."),
            L("Sera", "I love you, {name}. I don't have a prettier way to say it. The woods don't either.", "shy"),
            L("Sera", "If you don't want that from me, I'll still show you the fox with the limp. I just needed the air to hear it once."),
            title = "The Unmarked Path", req = 80,
            choices = listOf(
                Choice("I want it. I love you. I'm not going anywhere.", 10, "sera_confess", listOf(
                    L("Sera", "Good.", "smile"),
                    L("Sera", "Then sit. The trees can listen. They've been waiting longer than we have."),
                    F("The ribbon stirs. The rest of the evening is a quiet the woods already know how to keep."),
                ), needFreeRoute = true),
                Choice("I've been walking this way on purpose. I love you too.", 10, "sera_confess", listOf(
                    L("Sera", "Then we are both bad at being lost.", "shy"),
                    F("The path home can wait. Nothing here needs an audience."),
                ), needFreeRoute = true),
                Choice("You matter to me. I need time before I say the same words.", 6, next = listOf(
                    L("Sera", "Time I understand. Seasons do too."),
                    L("Sera", "I'll be on the trail. You know how to find it."),
                )),
            ),
        ),
        sc("yuna_confess", "yuna", "guild",
            N("The seedlings have grown a handspan. Yuna has two cups again, and a look that has been rehearsed and then abandoned."),
            L("Yuna", "I have spent my life being careful with blessings, and reckless with my hours. I would like, just once, to be reckless with a truth."),
            L("Yuna", "I love you, {name}. Not as a guildmaster, and not as a patient, and not as a duty. As you.", "shy"),
            L("Yuna", "If that is too much, we will still have tea, and I will still keep the door unlatched. I simply wanted you to hear it from me, not from a prayer I was too shy to finish."),
            title = "A Blessing, Unlatched", req = 80,
            choices = listOf(
                Choice("It isn't too much. I love you. I'm staying.", 10, "yuna_confess", listOf(
                    L("Yuna", "Then the tea can wait.", "smile"),
                    L("Yuna", "Come sit. I have wanted to hold your hand without pretending it was an accident."),
                    F("The cups cool on the step. The hall is kind enough not to look."),
                ), needFreeRoute = true),
                Choice("I've loved you longer than I admitted. I'm glad you said it.", 10, "yuna_confess", listOf(
                    L("Yuna", "Oh, good. I was very brave and also slightly terrified.", "shy"),
                    F("The garden keeps the rest. Morning can find you when it wishes."),
                ), needFreeRoute = true),
                Choice("You have my heart in pieces. I'm not ready to name the whole of it.", 6, next = listOf(
                    L("Yuna", "Pieces I can keep. I am patient with growing things."),
                    L("Yuna", "Drink. That's still a blessing, even if we leave the larger word for later."),
                )),
            ),
        ),

        // —— Proposals (90, after confess, route only) ——
        sc("kael_propose", "kael", "town",
            N("He stops you at the cutler's stall he always pretends not to favor, a small fold of cloth in his glove."),
            L("Kael", "I had a ring made. It is not court steel. It is only honest."),
            L("Kael", "Stay with me, {name}. Not as my commander. As the person I come home to. If you'll have a knight without a banner, I am already yours.", "shy"),
            title = "An Honest Ring", req = 90, needFlag = "kael_confess", routeOnly = true,
            choices = listOf(
                Choice("Yes. I'll have you. Put it on.", 8, "kael_vow", listOf(
                    L("Kael", "Then it's done.", "smile"),
                    F("The square goes on without you. The rest of the evening belongs to no one else."),
                )),
                Choice("Yes. I don't need a ring to know, but I want it anyway.", 8, "kael_vow", listOf(
                    L("Kael", "Then we'll have both.", "shy"),
                    F("Some promises are quieter than a market, and stronger."),
                )),
            ),
        ),
        sc("mira_propose", "mira", "town",
            N("On the clock-tower landing she offers not a ring at first, but a page — your name written beside hers in a careful, slightly shaking hand."),
            L("Mira", "I can do a ring if you prefer jewelry to documentation. I have both. I find I wanted the paper first, because I am still myself."),
            L("Mira", "Build a life with me, {name}. A room with a window, work that doesn't hide, and a future I don't have to footnote.", "shy"),
            title = "A Future, Footnoted", req = 90, needFlag = "mira_confess", routeOnly = true,
            choices = listOf(
                Choice("Yes. Write it in ink. I'll wear the ring too.", 8, "mira_vow", listOf(
                    L("Mira", "Excellent methodology.", "smile"),
                    F("The clock forgets to be loud. The night does not need an audience."),
                )),
                Choice("Yes. I'm already in the margin. Put me in the body of the work.", 8, "mira_vow", listOf(
                    L("Mira", "You are the work.", "shy"),
                    F("The orrery keeps the time. You keep each other."),
                )),
            ),
        ),
        sc("rin_propose", "rin", "tavern",
            N("He slides a thin silver band across the table with two fingers, as if it might bolt."),
            L("Rin", "I didn't steal it. I need that on the record. I paid, and I was very annoying about the size."),
            L("Rin", "Be mine, officially, in the boring, wonderful way. No last jobs. No windows. Just you, {name}, and a rogue who finally wants to be found.", "shy"),
            title = "Found On Purpose", req = 90, needFlag = "rin_confess", routeOnly = true,
            choices = listOf(
                Choice("Yes. I'll be yours. You're already mine.", 8, "rin_vow", listOf(
                    L("Rin", "Good. I practiced this and still almost fled.", "smile"),
                    F("The coin, the ring, the lantern — all of it can wait outside the quiet."),
                )),
                Choice("Yes. Come home with me. We'll tell the hall tomorrow.", 8, "rin_vow", listOf(
                    L("Rin", "Tomorrow. I like tomorrow on you.", "shy"),
                    F("The tavern keeps its last light. The rest is not written down."),
                )),
            ),
        ),
        sc("sera_propose", "sera", "forest",
            N("At the shrine she ties a new green ribbon beside the old one, then takes something small from her pocket — a ring of polished wood and a single set stone."),
            L("Sera", "It isn't court metal. It came from a branch that survived the fire. I thought that mattered."),
            L("Sera", "Walk with me for the rest of the trail, {name}. Be the person I come back to. I'll be the person who always finds the path home.", "shy"),
            title = "A Ribbon and a Ring", req = 90, needFlag = "sera_confess", routeOnly = true,
            choices = listOf(
                Choice("Yes. I'll walk it with you. All of it.", 8, "sera_vow", listOf(
                    L("Sera", "Then we're bound. In the good way.", "smile"),
                    F("The woods close around a promise they are old enough to keep."),
                )),
                Choice("Yes. Tie it properly. I'm not going back alone.", 8, "sera_vow", listOf(
                    L("Sera", "Neither am I.", "shy"),
                    F("The shrine holds the quiet. Nothing else is required."),
                )),
            ),
        ),
        sc("yuna_propose", "yuna", "guild",
            N("She has laid a small ring in the empty saucer, as if it were a second cup you were always meant to notice."),
            L("Yuna", "I asked no temple for permission. I only asked myself, and I was surprisingly firm."),
            L("Yuna", "Make a life with me, {name}. Let me be the person who keeps the kettle, and the door, and you. I will be very serious about all three.", "shy"),
            title = "The Second Cup", req = 90, needFlag = "yuna_confess", routeOnly = true,
            choices = listOf(
                Choice("Yes. I'll make that life with you.", 8, "yuna_vow", listOf(
                    L("Yuna", "Then we are promised. I'll try not to cry into the tea.", "smile"),
                    F("The garden, the step, the hall — all of it looks the other way."),
                )),
                Choice("Yes. Put it on. I'm already home.", 8, "yuna_vow", listOf(
                    L("Yuna", "Good. I hoped you were.", "shy"),
                    F("The cups are forgotten. The night does not need a witness."),
                )),
            ),
        ),

        // —— Endings (100, after vow + chapter 1) ——
        sc("kael_ending", "kael", "guild",
            N("The Covenant has withdrawn, for now. Kael stands the evening watch anyway, then makes room on the wall for you."),
            L("Kael", "They will come back. So will we. That's the work."),
            L("Kael", "I used to think a life was a post you died at. I would rather build one with you, {name}, and be inconveniently alive in it.", "smile"),
            L("Kael", "Whatever comes for this hall will find a door, a knight, and the person I chose. I can live with those odds."),
            title = "The Door We Keep", req = 100, needFlag = "kael_vow", needChapter = 1, routeOnly = true,
            choices = listOf(
                Choice("Then we'll keep it together. That's my ending, and my beginning.", 5, "kael_ending", listOf(
                    L("Kael", "Good.", "smile"),
                    L("Kael", "Come inside. The watch can do without us for one night."),
                    F("The hall is still standing. So are you. The rest is yours."),
                )),
            ),
        ),
        sc("mira_ending", "mira", "town",
            N("From the clock tower the town looks newly mended and not quite safe. Mira's orrery is ticking in time with something stubborn in the sky."),
            L("Mira", "The living light is still in the air. They didn't take all of it. I intend to write a better chart than the one they tried to bury."),
            L("Mira", "I also intend to do it with you in the room, {name}. That's the part of the future I will not renegotiate.", "smile"),
            L("Mira", "If the Covenant returns, they will find a scholar who is no longer alone, and a hypothesis that has already been proven."),
            title = "A Chart of Our Own", req = 100, needFlag = "mira_vow", needChapter = 1, routeOnly = true,
            choices = listOf(
                Choice("Then we'll write it together. That's the life I want.", 5, "mira_ending", listOf(
                    L("Mira", "Excellent. I'll fetch the ink.", "smile"),
                    F("The sky keeps its secrets. You keep each other. That's enough of an ending for now."),
                )),
            ),
        ),
        sc("rin_ending", "rin", "tavern",
            N("Rin has both boots on the floor, for once, and a look that has decided not to run."),
            L("Rin", "I thought, when the Covenant showed up, that I would want a window. I wanted the door. That's new."),
            L("Rin", "So here's my official, boring, wonderful plan, {name}: I stay. We keep the hall. I steal nothing except time with you, which I intend to do constantly.", "smile"),
            L("Rin", "If they come back, they can argue with a rogue who finally has something he will not leave."),
            title = "A Life Without a Window", req = 100, needFlag = "rin_vow", needChapter = 1, routeOnly = true,
            choices = listOf(
                Choice("Stay. That's the only plan I need.", 5, "rin_ending", listOf(
                    L("Rin", "Then we're stuck with each other. I'm delighted.", "smile"),
                    F("The tavern closes around a future no one needs to break into."),
                )),
            ),
        ),
        sc("sera_ending", "sera", "forest",
            N("New growth has come in along the burned edge. Sera stands in it as if she is counting every leaf."),
            L("Sera", "The woods will take years. So will we. I don't mind the work if you're on the path."),
            L("Sera", "The Covenant can come back for the light. They will find a ranger who has already chosen her home, and the person she chose it with.", "smile"),
            L("Sera", "That's you, {name}. In case the trees were unclear."),
            title = "The Path Home", req = 100, needFlag = "sera_vow", needChapter = 1, routeOnly = true,
            choices = listOf(
                Choice("I'm on the path. I'm not leaving it.", 5, "sera_ending", listOf(
                    L("Sera", "Good. Then we'll walk it in every season.", "smile"),
                    F("The woods keep the ending. It is not a finish so much as a way to go on."),
                )),
            ),
        ),
        sc("yuna_ending", "yuna", "guild",
            N("The garden is thicker now. Yuna has left the door unlatched, on purpose, and is waiting on the step as if she always was."),
            L("Yuna", "The wards will need watching. The sick will still come. I am not tired of that, and I am not tired of you."),
            L("Yuna", "Whatever the Covenant intends, they will have to come through a hall that knows how to keep a promise, and a person I have already promised myself to.", "smile"),
            L("Yuna", "Stay, {name}. Not just tonight. For the life we said we would make."),
            title = "The Door Unlatched", req = 100, needFlag = "yuna_vow", needChapter = 1, routeOnly = true,
            choices = listOf(
                Choice("I'm staying. This is the life I wanted.", 5, "yuna_ending", listOf(
                    L("Yuna", "Then come inside. I'll put the kettle on, and the rest can be ours.", "smile"),
                    F("The hall holds. The ending is only a kinder beginning."),
                )),
            ),
        ),
    )

    val plot: List<Scene> = listOf(
        sc("plot_letter", null, "guild",
            N("On a quiet morning the ledger's last page does not sit flush. Behind it, in a thinner hand, a letter has been hidden and then almost taken back."),
            N("It is signed Rowan Hale — the last master of this hall — and it is not a goodbye so much as a warning left for whoever was stubborn enough to take the key."),
            N("\"If you are reading this, the Ash Covenant did not find it first. They have been taking a tithe of living light from the holds, the woods, and the temple wards. I refused to collect it. I was not the first.\""),
            L("Kael", "Tithe of light. I was ordered to burn a village that would not pay one. I thought it was grain. I may have been a fool.", "grim"),
            L("Yuna", "The wards failed the year before I left the temple. We were told it was thrift. I did not believe them then. I believe this even less kindly.", "grim"),
            N("Rowan's last line is smaller. \"They will come for the hall. Keep the key. Keep each other. I have gone to follow the light they stole. If I can come back, I will.\""),
            title = "A Letter in the Ledger",
            choices = listOf(
                Choice("We keep the key. We find out what Rowan found.", 0, "plot_letter", listOf(
                    L("Kael", "Then the board is not only cats and grain anymore."),
                    L("Yuna", "I'll ask at the temple. Carefully. Some doors listen."),
                )),
                Choice("If they come for the hall, they'll find it occupied.", 0, "plot_letter", listOf(
                    L("Kael", "Good. I prefer a warning I can stand in front of."),
                    L("Yuna", "And I prefer a hall that is not surprised twice."),
                )),
            ),
        ),
        sc("plot_inquiry", null, "chapel",
            N("The ruined chapel in the east woods still holds a little light that does not belong to the moon. Dust hangs in it like gold that forgot how to fall."),
            L("Yuna", "This was a mercy-house once. The wards here should have gone dark years ago. They didn't. They were... emptied.", "grim"),
            L("Kael", "Tracks. Soft boots, more than one person, not a pilgrim's gait. They were here recently, and they were looking for something they didn't find.", "grim"),
            N("On the broken altar, scratched thin as a secret, is a mark like a lantern with its flame inverted. The same mark is half-hidden on Rowan's letter, if you know to look."),
            L("Yuna", "If Mira were here she would have a name for this. I have only a feeling, and it is not a kind one."),
            title = "The Emptied Chapel", needFlag = "plot_letter",
            choices = listOf(
                Choice("We follow the tracks before they fade.", 0, "plot_inquiry", listOf(
                    L("Kael", "North edge of the old watch. Sera's woods, if she'll stand it."),
                    L("Yuna", "I'll pack the herbs. If the light is being taken, someone nearby is paying for it."),
                )),
                Choice("We copy the mark and take it home. No one waits here alone.", 0, "plot_inquiry", listOf(
                    L("Yuna", "Yes. This place has given enough."),
                    L("Kael", "Agreed. We come back with more than two pairs of eyes."),
                )),
            ),
        ),
        sc("plot_woods", null, "forest",
            N("At the burned edge of the old watch, the new growth is wrong — too pale, as if the green were being asked to work without its share of the sun."),
            L("Sera", "This is where my woods ended. I thought it was fire. Fire does not leave the light looking hungry.", "grim"),
            L("Kael", "The same inverted lantern, cut into a stump. They've been harvesting here for years. The fire was a cleanup, or a cover.", "grim"),
            N("Something faint still moves in the pale leaves, a shimmer like breath on glass. It leans toward the guild key at your throat as if it recognizes a door."),
            L("Yuna", "Rowan said keep the key. I think the key is not only for the hall. I think it is a seal.", "wow"),
            title = "A Hungry Light", needFlag = "plot_inquiry",
            choices = listOf(
                Choice("Then the hall is a lock they haven't picked. We hold it.", 0, "plot_woods", listOf(
                    L("Sera", "I'll walk the edge every dawn. If they come through my trees again, I want to see their faces."),
                    L("Kael", "And I want the door barred by more than habit."),
                )),
                Choice("If the key is a seal, we learn how it works before they do.", 0, "plot_woods", listOf(
                    L("Yuna", "I'll ask Mira to look, if she's with us. If not, I'll look myself. I am tired of being told not to."),
                    L("Kael", "We do it together. Rowan went alone. Look where that left the letter."),
                )),
            ),
        ),
        sc("plot_climax", null, "guild",
            N("They do not knock. A figure in an unmarked grey cloak stands in the hall at last light, hands open, as if courtesy could make a demand look like a visit."),
            N("The inverted lantern is stitched small at the throat. When they speak, the lamps in the room lean, just slightly, toward the door."),
            L("Kael", "You're in the wrong hall.", "grim"),
            N("\"We are in the only hall that still holds a seal,\" the visitor says. \"Rowan Hale took something that belongs to the balance of this region. Return the key, and the tithe can be... renegotiated. Refuse, and Lumenreach will learn what a winter without light feels like.\""),
            L("Yuna", "You will not bargain with a threat in this doorway. Not while I can still stand in it.", "grim"),
            N("The key is warm against your chest. The visitor's gaze finds it, and for a moment the fire on the hearth thins, as if considering leaving."),
            title = "A Visitor at Last Light", needFlag = "plot_woods",
            choices = listOf(
                Choice("The key stays. This hall is not a tithe. Leave.", 0, "plot_ch1_done", listOf(
                    N("Kael steps to your shoulder. Yuna does not move from the door. If the others are in the hall, they have made a wall of ordinary people who will not be moved."),
                    N("The visitor smiles with no warmth at all. \"Then we will return when the lanterns fade. Keep your key, Guildmaster. See how heavy it becomes.\""),
                    N("They are gone as if the threshold had never held them. The fire comes back. The hall is still a hall."),
                    L("Kael", "They'll come again. We'll be here.", "grim"),
                    L("Yuna", "Then we keep the door, and each other, and whatever Rowan hid in that key. That is enough for tonight. It is not the end."),
                )),
                Choice("You already took enough. You will not take this house.", 0, "plot_ch1_done", listOf(
                    N("The lamps steady, as if the hall itself had chosen a side. The visitor looks at the key, then at you, and seems to file a different kind of report."),
                    N("\"A delay, then. Not a refusal the Ash Covenant will remember kindly.\" They go. The night remains yours."),
                    L("Yuna", "A delay we will use. I'll write to anyone who still remembers Rowan kindly."),
                    L("Kael", "And I'll see the watch doubled. We held. That's the first page. Not the last."),
                )),
            ),
        ),
    )

    val care: List<Scene> = listOf(
        sc("care_kael", "kael", "guild",
            N("Kael has been made to sit, which he is treating as a more serious injury than the actual one. The bandage is already neat. His patience is not."),
            L("Kael", "You don't have to hover. I've had worse. I have also, apparently, a guildmaster with opinions about sitting.", "grim"),
            title = "A Clean Bandage",
            choices = listOf(
                Choice("I'm not hovering. I'm keeping you company. There's a difference.", 5, next = listOf(L("Kael", "Hnh. Then sit. If you're going to be stubborn, be stubborn where I can see you.", "smile"))),
                Choice("Let someone look after you for once. That's an order I mean kindly.", 6, next = listOf(L("Kael", "Kind orders are a dangerous innovation.", "shy"), L("Kael", "...Thank you. I'll keep the sitting."))),
            ),
        ),
        sc("care_mira", "mira", "guild",
            N("Mira has a book open on the wrong page and a bruise she is trying to classify rather than feel."),
            L("Mira", "If I hold still, the data should improve. That's the theory. The theory is annoying.", "grim"),
            title = "Hold Still",
            choices = listOf(
                Choice("Forget the data. Drink this, and let me sit with you.", 5, next = listOf(L("Mira", "That's a very unscientific intervention.", "smile"), L("Mira", "I'll allow it."))),
                Choice("You don't have to be a hypothesis right now. You can just be tired.", 6, next = listOf(L("Mira", "Oh.", "shy"), L("Mira", "That's... surprisingly helpful. Stay anyway."))),
            ),
        ),
        sc("care_rin", "rin", "guild",
            N("Rin is on a bench he swore he didn't need, one arm bound, putting on a show of being bored."),
            L("Rin", "If you came to say I told you so, get it over with. If you came for any other reason, I might be embarrassingly grateful.", "grim"),
            title = "A Bench He Didn't Need",
            choices = listOf(
                Choice("I came to sit. You can skip the performance.", 5, next = listOf(L("Rin", "Ugh. Kindness. Don't tell the alleys.", "shy"), L("Rin", "...Stay a bit. The bench is big enough."))),
                Choice("You scared me. I'm allowed to say that.", 6, next = listOf(L("Rin", "That's illegal.", "shy"), L("Rin", "Say it quieter. Or don't. I'm listening either way.", "smile"))),
            ),
        ),
        sc("care_sera", "sera", "guild",
            N("Sera has submitted to a clean dressing and is watching the window as if the trees might come in and finish the argument."),
            L("Sera", "I hate indoor recovering. The walls don't do anything.", "grim"),
            title = "Indoor Recovering",
            choices = listOf(
                Choice("Then I'll be something the walls can't be. I'm here.", 5, next = listOf(L("Sera", "That's a fair substitute.", "smile"), L("Sera", "Talk if you want. Or don't. The quiet is better with you in it."))),
                Choice("You got everyone home. Let us get you the rest of the way.", 6, next = listOf(L("Sera", "I can live with that.", "shy"), L("Sera", "Sit. If I have to be indoors, I won't do it alone."))),
            ),
        ),
        sc("care_yuna", "yuna", "guild",
            N("Yuna has been made, at last, to be the patient. She looks offended by the concept and also very tired."),
            L("Yuna", "This is a terrible reversal. I am much better at the other chair.", "grim"),
            title = "The Other Chair",
            choices = listOf(
                Choice("Then I'll take your chair for an evening. Rest.", 5, next = listOf(L("Yuna", "You're going to be insufferably good at this.", "smile"), L("Yuna", "Don't stop."))),
                Choice("You heal everyone else. Let me look after you.", 6, next = listOf(L("Yuna", "That's a blessing I wasn't going to ask for.", "shy"), L("Yuna", "I'm asking now. Stay."))),
            ),
        ),
    )

    val jealous: List<Scene> = emptyList() // built by jealousy()
    val rival: List<Scene> = listOf(
        sc("rival_kael_rin", "kael", "tavern",
            N("You walk in on a conversation that has already found its edge. Kael is standing. Rin is smiling with too many teeth."),
            L("Rin", "I was only saying the guildmaster has a type, and that type might not be a man who irons his own cloak."),
            L("Kael", "And I was only saying some people should speak less in rooms they are lucky to be allowed in.", "grim"),
            title = "A Conversation with an Edge",
            choices = listOf(
                Choice("Enough. You're both needed here. Don't make me choose a side I don't want.", 3, "rival_kael_rin", listOf(
                    L("Rin", "...Fine. I like the hall more than I like winning that particular fight."),
                    L("Kael", "I'll stand down. I was... sharper than the remark deserved."),
                    L("Yuna", "Good. I already put out extra cups. You're both staying for them."),
                )),
                Choice("If this is about me, say it like adults, not like rivals in a yard.", 4, "rival_kael_rin", listOf(
                    L("Rin", "Adults. Tragic."),
                    L("Kael", "He's not wrong, which I resent."),
                    L("Rin", "Truce. Temporary. Charming. I'll even mean it."),
                )),
            ),
        ),
    )

    fun groupScene(eventId: String, heroId: String): Scene {
        val ev = groupsCalendar.first { it.id == eventId }
        return when (eventId to heroId) {
            "festival" to "kael" -> sc("festival_kael", "kael", "festival",
                N("The square is a river of lanterns. Kael offers his arm as if it were a formal report."),
                L("Kael", "I don't dance. I can walk with you until the last light goes up, and mean it.", "smile"),
                title = ev.title,
                choices = listOf(
                    Choice("Then walk with me. That's the dance I wanted.", 8, next = listOf(L("Kael", "Good.", "shy"), F("The lanterns burn themselves out. The square does not need to see the rest."))),
                    Choice("We'll stand under the lights and let the town be loud for us.", 7, next = listOf(L("Kael", "I can do that. I find I don't mind the noise when you're in it.", "smile"))),
                ))
            "festival" to "mira" -> sc("festival_mira", "mira", "festival",
                N("Mira is counting lanterns and losing the count every time one is reflected in the fountain."),
                L("Mira", "There is no scientifically useful reason to hang this much fire over a town. I approve completely.", "wow"),
                title = ev.title,
                choices = listOf(
                    Choice("Stay and lose the count with me.", 8, next = listOf(L("Mira", "A worthy experiment.", "shy"), F("The lanterns go on without a ledger. So do you."))),
                    Choice("Tell me which light is your favorite, and we'll stand under it.", 7, next = listOf(L("Mira", "That one. No — that one. This is why I don't do favorites in public.", "smile"))),
                ))
            "festival" to "rin" -> sc("festival_rin", "rin", "festival",
                N("Rin has already acquired a paper lantern he swears was being given away. It probably was. Eventually."),
                L("Rin", "Don't look at me like that. I paid. Mostly. Come on, the good roof is this way.", "smile"),
                title = ev.title,
                choices = listOf(
                    Choice("I'll follow you. Don't you dare vanish at the top.", 8, next = listOf(L("Rin", "Not tonight. Tonight I'm extremely findable.", "shy"), F("The lanterns blur. The town keeps its own secrets."))),
                    Choice("Hold that light properly and walk beside me, not ahead.", 7, next = listOf(L("Rin", "Beside. I can try a new formation.", "smile"))),
                ))
            "festival" to "sera" -> sc("festival_sera", "sera", "festival",
                N("Sera stands at the edge of the square where the lantern light frays into the dark, not quite in the crowd, not quite out of it."),
                L("Sera", "It's a lot of people. It's also... pretty. I can admit pretty.", "smile"),
                title = ev.title,
                choices = listOf(
                    Choice("We can stay at the edge. I like it here too.", 8, next = listOf(L("Sera", "Good. The air is better, and so is the company.", "shy"), F("The lights go on without you. The edge of the square is enough."))),
                    Choice("One walk through the middle, then I'll bring you back to the quiet.", 7, next = listOf(L("Sera", "One walk. That's a brave offer. I'll take it.", "smile"))),
                ))
            "festival" to "yuna" -> sc("festival_yuna", "yuna", "festival",
                N("Yuna has a spare lantern for a stall that ran short, because of course she does, and her other hand is free for yours."),
                L("Yuna", "I love a town that remembers to be kind in public. Come on. I want you to see the children's lights.", "smile"),
                title = ev.title,
                choices = listOf(
                    Choice("I'll go wherever you're going. That's the festival I wanted.", 8, next = listOf(L("Yuna", "Then we're already doing it right.", "shy"), F("The last lanterns thin. The night does not need an audience."))),
                    Choice("Take my hand and show me your favorite corner.", 7, next = listOf(L("Yuna", "This way. I saved it.", "smile"))),
                ))
            "beach" to "kael" -> sc("beach_kael", "kael", "beach",
                N("Kael has taken off his boots with the solemnity of a campaign decision. The lake is very still."),
                L("Kael", "I can't remember the last time I stood somewhere with no wall at my back. It's... not nothing.", "smile"),
                title = ev.title,
                choices = listOf(
                    Choice("Then stay a while. I'll be your wall if you need one.", 8, next = listOf(L("Kael", "That's a better offer than a wall.", "shy"), F("The light thins over the water. The shore keeps the rest."))),
                    Choice("We can just watch the town from here. No armor required.", 7, next = listOf(L("Kael", "Hnh. I'll try the unarmored hour.", "smile"))),
                ))
            "beach" to "mira" -> sc("beach_mira", "mira", "beach",
                N("Mira has her hem in one hand and a theory about the way light sits on water in the other."),
                L("Mira", "It's the same sky as the clock tower, only closer to the ground. I like it better this way.", "smile"),
                title = ev.title,
                choices = listOf(
                    Choice("Sit with me until the gold goes. That's the whole plan.", 8, next = listOf(L("Mira", "A plan with no footnotes. Revolutionary.", "shy"), F("The lake keeps the evening. Nothing else needs writing down."))),
                    Choice("Tell me what the light is doing. I'll listen.", 7, next = listOf(L("Mira", "Don't start me unless you mean it. I mean it.", "smile"))),
                ))
            "beach" to "rin" -> sc("beach_rin", "rin", "beach",
                N("Rin skips a stone that does not skip, then another that does, then looks far too pleased."),
                L("Rin", "I am very talented. Also slightly damp. If you laugh, I will require compensation.", "smile"),
                title = ev.title,
                choices = listOf(
                    Choice("Compensation later. Stay and get more damp with me.", 8, next = listOf(L("Rin", "That's a dangerous contract. I'll sign it.", "shy"), F("The water goes copper, then quiet. The shore does not tell."))),
                    Choice("I'll skip one too. If I win, you walk back at my pace.", 7, next = listOf(L("Rin", "I never said I was good at losing. Fine. Watch this.", "smile"))),
                ))
            "beach" to "sera" -> sc("beach_sera", "sera", "beach",
                N("Sera has found the place where the grass gives up and the sand begins, and looks more at home than she does in any square."),
                L("Sera", "I forget, sometimes, that the woods end in water. It's a good ending. Or a good start.", "smile"),
                title = ev.title,
                choices = listOf(
                    Choice("Let's stay until the town lights come on across the lake.", 8, next = listOf(L("Sera", "Yes. I want that with you.", "shy"), F("The shore holds the hour. It does not need a witness."))),
                    Choice("Show me the quiet path back when you're ready. Not before.", 7, next = listOf(L("Sera", "Not before. That's the right kind of order.", "smile"))),
                ))
            "beach" to "yuna" -> sc("beach_yuna", "yuna", "beach",
                N("Yuna has her shoes in one hand and a little bundle of lakeside mint in the other, because she cannot help herself."),
                L("Yuna", "Don't look at the mint. Look at the water. I am on holiday. Officially. Almost.", "smile"),
                title = ev.title,
                choices = listOf(
                    Choice("Holiday it is. Sit. The mint can wait.", 8, next = listOf(L("Yuna", "You are a bad influence of the very best kind.", "shy"), F("The lake takes the gold away slowly. You let it."))),
                    Choice("We'll take the long way back, and you can name every plant you like.", 7, next = listOf(L("Yuna", "Careful. I will.", "smile"))),
                ))
            "harvest" to "kael" -> sc("harvest_kael", "kael", "harvest",
                N("The hall has been turned into a dance it was not built for. Kael has been cornered by the idea of music and is bearing it bravely."),
                L("Kael", "If you ask me to dance I will say yes, and then I will have to be taught. I am warning you now.", "shy"),
                title = ev.title,
                choices = listOf(
                    Choice("I'm asking. I'll teach you. It's only a hall, not a battlefield.", 8, next = listOf(L("Kael", "Then I'll follow your lead. Once.", "smile"), F("The music goes on without a lesson plan. The hall looks the other way."))),
                    Choice("We can stand by the fire and call it a dance if anyone asks.", 7, next = listOf(L("Kael", "A reasonable compromise. I'll take it.", "smile"))),
                ))
            "harvest" to "mira" -> sc("harvest_mira", "mira", "harvest",
                N("Mira is watching the dancers as if they were a problem set she would like, very much, to be good at."),
                L("Mira", "I understand the time signature. I do not yet understand my feet. This is embarrassing and also slightly wonderful.", "wow"),
                title = ev.title,
                choices = listOf(
                    Choice("Then take my hand. We'll invent a version that works.", 8, next = listOf(L("Mira", "A private theorem. I accept.", "shy"), F("The garlands blur. The hall keeps the rest of the evening."))),
                    Choice("We can watch one song and dance the next. No rush.", 7, next = listOf(L("Mira", "A controlled trial. Yes. Stay close.", "smile"))),
                ))
            "harvest" to "rin" -> sc("harvest_rin", "rin", "harvest",
                N("Rin can actually dance, which he demonstrates twice before pretending he can't."),
                L("Rin", "I learned in rooms with worse lighting and worse company. This is an upgrade. Don't tell anyone I said something that sincere.", "smile"),
                title = ev.title,
                choices = listOf(
                    Choice("Dance with me, then, and be as sincere as you can stand.", 8, next = listOf(L("Rin", "That's a low bar I intend to trip over.", "shy"), F("The hall turns. The night does not take notes."))),
                    Choice("One dance, then we steal cake from the side table.", 7, next = listOf(L("Rin", "A perfect crime. I'll lead.", "smile"))),
                ))
            "harvest" to "sera" -> sc("harvest_sera", "sera", "harvest",
                N("Sera has a sprig of wheat tucked in her braid, as if the hall had briefly become a field she could trust."),
                L("Sera", "I can dance. I just don't usually do it under a roof. If you're asking, I'm out of excuses.", "shy"),
                title = ev.title,
                choices = listOf(
                    Choice("I'm asking. Come here.", 8, next = listOf(L("Sera", "All right. Don't let go if I forget the count.", "smile"), F("The fire and the garlands keep the time. Nothing else is required."))),
                    Choice("We can stay near the door, where the air is honest.", 7, next = listOf(L("Sera", "That's my kind of dance floor. Stay anyway.", "smile"))),
                ))
            "harvest" to "yuna" -> sc("harvest_yuna", "yuna", "harvest",
                N("Yuna has been claimed by two different conversations and escaped both, somehow, with two cups and a clear path to you."),
                L("Yuna", "I have been waiting all evening to be here and not needed by anyone except you. That's a small miracle. Dance with me.", "smile"),
                title = ev.title,
                choices = listOf(
                    Choice("Yes. I've been waiting too.", 8, next = listOf(L("Yuna", "Good. Then we're both off duty.", "shy"), F("The hall is kind enough to look after itself for a while."))),
                    Choice("One dance, then we'll sit by the fire and let the rest of them whirl.", 7, next = listOf(L("Yuna", "A perfect harvest. Come on.", "smile"))),
                ))
            else -> sc("group_fallback", heroId, ev.bg, N("The evening is kinder with company."), title = ev.title)
        }
    }

    fun careFor(heroId: String) = care.first { it.heroId == heroId }

    fun jealousy(jealousId: String, ofId: String): Scene {
        val a = Heartbound.hero.getValue(jealousId).name
        val first = a.split(" ").first()
        val other = Heartbound.hero.getValue(ofId).name.split(" ").first()
        val bg = if (jealousId == "sera") "forest" else if (jealousId == "rin") "tavern" else "guild"
        val lines = when (jealousId) {
            "kael" -> listOf(
                N("Kael is oiling a blade that does not need oiling. He does not look at you at once."),
                L("Kael", "You were with $other a long time this evening. I'm not owed an account. I am, apparently, still a man who notices."),
                L("Kael", "If I am making a fool of myself, tell me. I'd rather be a fool with facts.", "grim"),
            )
            "mira" -> listOf(
                N("Mira is at the long table, writing something she then crosses out, then writing it again."),
                L("Mira", "This is not a paper. This is me failing to be graceful about $other, and about you, and about the part of my chest that has opinions."),
                L("Mira", "I don't own your evenings. I only... noticed where they went.", "shy"),
            )
            "rin" -> listOf(
                N("Rin is on the tavern windowsill, which is not a seat, and he knows it."),
                L("Rin", "So. $other. That's a look I recognize, and I hate that I recognize it."),
                L("Rin", "I'm not going to start a fight in your hall. I am going to be annoying about this for at least one minute, and then you can tell me to stop.", "grim"),
            )
            "sera" -> listOf(
                N("Sera is on the porch, hood down, as if the trees had asked her a question she didn't like."),
                L("Sera", "I saw you with $other. I'm not owed the story. I wanted, stupidly, to be the one you told it to anyway."),
                L("Sera", "If that's too much, I'll go back to the path. I needed to say it once.", "grim"),
            )
            else -> listOf(
                N("Yuna has two cups, as usual, and a quieter face than the kettle deserves."),
                L("Yuna", "I'm not angry. I want that said first. I am... a little sore, in the way a person gets when they watch a door they love open for someone else."),
                L("Yuna", "You were with $other. I hope it was kind. I also hope you have a minute left for me.", "shy"),
            )
        }
        return sc("jealous_${jealousId}_of_$ofId", jealousId, bg, *lines.toTypedArray(),
            title = "A Minute of Honesty",
            choices = listOf(
                Choice("I'm sorry I left you standing with that. You matter to me.", 6, next = listOf(L(first, "That's all I needed, I think. Thank you for not pretending you didn't hear me.", "smile"))),
                Choice("I care for you. I also won't lie about where I was.", 5, next = listOf(L(first, "Good. I can live with honesty. I would have hated a kind lie.", "grim"))),
                Choice("If I made you feel second, that wasn't what I meant. I'm here now.", 6, next = listOf(L(first, "Then I'll take now. It's a start I can stand on.", "shy"))),
            ),
        )
    }

    fun pendingPlot(state: GameState): Scene? {
        if (state.phase != "morning") return null
        val day = state.day
        return when {
            "plot_letter" !in state.flags && day >= 3 -> plot[0]
            "plot_letter" in state.flags && "plot_inquiry" !in state.flags && day >= 5 -> plot[1]
            "plot_inquiry" in state.flags && "plot_woods" !in state.flags && day >= 7 -> plotWoodsFor(state)
            "plot_woods" in state.flags && "plot_ch1_done" !in state.flags && day >= 9 -> plot[3]
            else -> null
        }
    }

    private fun plotWoodsFor(state: GameState): Scene {
        val base = plot[2]
        if (state.h("sera").recruited) return base
        val lines = base.lines.map {
            if (it.speaker == "Sera") it.copy(speaker = "Kael", text = "This is the edge of the old watch. I thought it was fire that ended it. Fire does not leave the light looking hungry.")
            else it
        }
        return base.copy(lines = lines)
    }

    fun pendingGroup(state: GameState): GroupEvent? {
        if (state.phase != "evening") return null
        return groupsCalendar.firstOrNull { state.day >= it.minDay && "event_${it.id}" !in state.flags }
    }

    fun pendingRival(state: GameState): Scene? {
        if (state.phase != "evening") return null
        if ("rival_kael_rin" in state.flags) return null
        if (state.day < 5) return null
        val kael = state.h("kael"); val rin = state.h("rin")
        if (!kael.recruited || !rin.recruited) return null
        if (kael.affection < 20 || rin.affection < 20) return null
        return rival.first()
    }
}
