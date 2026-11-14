package com.tim.gamehub.games.heartbound

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tim.gamehub.R
import kotlinx.coroutines.delay

private val Ink = Color(0xFF2A1F1A)
private val Cream = Color(0xFFF7EFE3)
private val Burgundy = Color(0xFF8B3A42)
private val Gold = Color(0xFFD4B15A)
private val Wood = Color(0xFF5C3D2E)
private val Parchment = Color(0xFFF3E2C4)
private val Night = Color(0xCC1A1214)
private val Soft = RoundedCornerShape(16.dp)

@Composable
fun HeartboundGame(onExit: () -> Unit, controller: HeartboundController? = null, instantText: Boolean = false) {
    val ctx = LocalContext.current
    val c = remember(controller) { controller ?: HeartboundController(PrefsSave(ctx), instantText = instantText) }
    val instant = instantText || c.instantText
    BackHandler {
        when (c.screen) {
            HbScreen.Title -> onExit()
            HbScreen.Home, HbScreen.Evening -> c.backToTitle()
            HbScreen.Dialogue -> if (c.showingChoices()) { /* wait */ } else c.advance()
            HbScreen.Create, HbScreen.Slots, HbScreen.Settings, HbScreen.Journal, HbScreen.Trophies, HbScreen.Log -> c.closeMenu()
            HbScreen.Summary -> c.dismissSummary()
            else -> c.goHome()
        }
    }
    Box(Modifier.fillMaxSize().testTag("hb_root")) {
        when (val s = c.screen) {
            HbScreen.Title -> TitleScreen(c, onExit)
            HbScreen.Home -> HomeScreen(c, onExit, morning = true)
            HbScreen.Evening -> HomeScreen(c, onExit, morning = false)
            HbScreen.Board -> BoardScreen(c)
            HbScreen.Party -> PartyScreen(c)
            HbScreen.Result -> ResultScreen(c)
            HbScreen.Shop -> ShopScreen(c)
            HbScreen.Upgrades -> UpgradeScreen(c)
            is HbScreen.Profile -> ProfileScreen(c, s.heroId)
            is HbScreen.Gift -> GiftScreen(c, s.heroId)
            HbScreen.Dialogue -> DialogueScreen(c, instant)
            HbScreen.Create -> CreateScreen(c)
            HbScreen.Slots -> SlotsScreen(c)
            HbScreen.Settings -> SettingsScreen(c)
            HbScreen.Journal -> JournalScreen(c)
            HbScreen.Trophies -> TrophyScreen(c)
            HbScreen.Summary -> SummaryScreen(c)
            HbScreen.Log -> LogScreen(c)
        }
        c.toast?.let { msg ->
            LaunchedEffect(msg) { delay(1800); c.toast = null }
            Text(msg, color = Ink, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp)
                .shadow(6.dp, Soft).background(Cream, Soft).padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

@Composable private fun Bg(name: String, dim: Float = 0.18f, night: Boolean = false) {
    val res = when (name to night) {
        "tavern" to true -> R.drawable.bg_tavern_night
        "town" to true -> R.drawable.bg_town_night
        "forest" to true -> R.drawable.bg_forest_night
        "guild" to true -> R.drawable.bg_guild_night
        "tavern" to false -> R.drawable.bg_tavern
        "town" to false -> R.drawable.bg_town
        "forest" to false -> R.drawable.bg_forest
        else -> if (night) R.drawable.bg_guild_night else R.drawable.bg_guild
    }
    Image(painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    if (dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
}

@Composable private fun Portrait(id: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit, expr: String = "idle") {
    val key = if (expr in setOf("idle", "neutral", "", "default")) id else "${id}_$expr"
    val res = when (key) {
        "mira" -> R.drawable.portrait_mira
        "mira_smile" -> R.drawable.portrait_mira_smile
        "mira_shy" -> R.drawable.portrait_mira_shy
        "mira_grim" -> R.drawable.portrait_mira_grim
        "mira_wow" -> R.drawable.portrait_mira_wow
        "rin" -> R.drawable.portrait_rin
        "rin_smile" -> R.drawable.portrait_rin_smile
        "rin_shy" -> R.drawable.portrait_rin_shy
        "rin_grim" -> R.drawable.portrait_rin_grim
        "rin_wow" -> R.drawable.portrait_rin_wow
        "sera" -> R.drawable.portrait_sera
        "sera_smile" -> R.drawable.portrait_sera_smile
        "sera_shy" -> R.drawable.portrait_sera_shy
        "sera_grim" -> R.drawable.portrait_sera_grim
        "sera_wow" -> R.drawable.portrait_sera_wow
        "yuna" -> R.drawable.portrait_yuna
        "yuna_smile" -> R.drawable.portrait_yuna_smile
        "yuna_shy" -> R.drawable.portrait_yuna_shy
        "yuna_grim" -> R.drawable.portrait_yuna_grim
        "yuna_wow" -> R.drawable.portrait_yuna_wow
        "kael_smile" -> R.drawable.portrait_kael_smile
        "kael_shy" -> R.drawable.portrait_kael_shy
        "kael_grim" -> R.drawable.portrait_kael_grim
        "kael_wow" -> R.drawable.portrait_kael_wow
        "a", "look_a" -> R.drawable.look_a
        "b", "look_b" -> R.drawable.look_b
        "c", "look_c" -> R.drawable.look_c
        "d", "look_d" -> R.drawable.look_d
        else -> R.drawable.portrait_kael
    }
    Image(painterResource(res), Heartbound.hero[id]?.name ?: id, modifier, contentScale = scale)
}

@Composable private fun GoldChip(gold: Int) {
    Text("🪙 $gold", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
        modifier = Modifier.background(Cream.copy(alpha = 0.92f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp).testTag("hb_gold"))
}

@Composable private fun Pill(text: String, onClick: (() -> Unit)? = null, fill: Color = Cream, ink: Color = Ink, tag: String? = null) {
    val m = Modifier.shadow(3.dp, RoundedCornerShape(20.dp)).background(fill, RoundedCornerShape(20.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .then(if (tag != null) Modifier.testTag(tag) else Modifier)
        .padding(horizontal = 14.dp, vertical = 8.dp)
    Text(text, color = ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = m)
}

@Composable private fun HbButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, fill: Color = Burgundy, enabled: Boolean = true, tag: String? = null) {
    val bg = if (enabled) fill else Color(0xFF6B5A58)
    Text(text, color = Cream, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center,
        modifier = modifier.shadow(5.dp, Soft).background(bg, Soft)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp))
}

@Composable private fun Hearts(aff: Int, size: Int = 14) {
    val t = Heartbound.tier(aff)
    Text((0 until 4).joinToString("") { if (it < t.hearts) "❤" else "♡" }, fontSize = size.sp, color = Color(0xFFE1546A))
}

@Composable private fun TitleScreen(c: HeartboundController, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().testTag("hb_title")) {
        Bg("guild", 0.35f)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xAA140C0A), Color.Transparent, Color(0xCC140C0A)))))
        Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Text("HEARTBOUND", color = Gold, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
            Spacer(Modifier.height(8.dp))
            Text("Lead a guild. Win their hearts.", color = Cream.copy(alpha = 0.9f), fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text("The hall at Lumenreach is yours now.", color = Cream.copy(alpha = 0.7f), fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            if (c.hasSave) {
                HbButton("Continue", { c.loadSave() }, Modifier.fillMaxWidth(), Gold, tag = "hb_continue")
                Spacer(Modifier.height(10.dp))
            }
            HbButton(if (c.hasSave) "New Game" else "Begin", { c.openSlots("new") }, Modifier.fillMaxWidth(), tag = "hb_new")
            Spacer(Modifier.height(10.dp))
            if (c.hasSave) {
                HbButton("Load", { c.openSlots("load") }, Modifier.fillMaxWidth(), Wood, tag = "hb_load")
                Spacer(Modifier.height(10.dp))
            }
            Pill("Settings", onClick = { c.openSettings() }, tag = "hb_settings")
            Spacer(Modifier.height(10.dp))
            Pill("‹  Back to Hub", onClick = onExit, tag = "hb_hub")
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable private fun TopBar(c: HeartboundController, onExit: () -> Unit, title: String, back: () -> Unit = { c.goHome() }) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Pill("‹  Hub", onClick = onExit, tag = "hb_hub")
        Spacer(Modifier.width(8.dp))
        if (c.screen != HbScreen.Home && c.screen != HbScreen.Evening && c.screen != HbScreen.Title)
            Pill("Back", onClick = back)
        Spacer(Modifier.weight(1f))
        Text(title, color = Cream, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        GoldChip(c.state.gold)
    }
}

@Composable private fun HomeScreen(c: HeartboundController, onExit: () -> Unit, morning: Boolean) {
    val st = c.state
    Box(Modifier.fillMaxSize().testTag(if (morning) "hb_home" else "hb_evening")) {
        Bg("guild", if (morning) 0.22f else 0.28f, night = !morning)
        Column(Modifier.fillMaxSize()) {
            TopBar(c, onExit, if (morning) "Day ${st.day}  ·  Morning" else "Day ${st.day}  ·  Evening")
            Text(if (morning) "Morning, ${st.player.name}. The hall is awake." else "The work is done, ${st.player.name}. The evening is yours.",
                color = Cream, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp).testTag("hb_greet"))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                st.recruited().forEach { h ->
                    Column(Modifier.width(92.dp).shadow(4.dp, Soft).background(Cream.copy(alpha = 0.94f), Soft)
                        .clickable { c.openProfile(h.id) }.padding(8.dp).testTag("hb_chip_${h.id}"),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Portrait(h.id, Modifier.size(76.dp, 88.dp).clip(RoundedCornerShape(12.dp)), ContentScale.Crop)
                        Text(h.def.name.split(" ").first(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
                        Hearts(h.affection, 11)
                        if (h.injuredDays > 0) Text("hurt ${h.injuredDays}d", fontSize = 10.sp, color = Burgundy)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (morning) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HbButton("Quest Board", { c.openBoard() }, Modifier.fillMaxWidth(), tag = "hb_btn_board")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HbButton("Shop", { c.openShop() }, Modifier.weight(1f), Wood, tag = "hb_btn_shop")
                        HbButton("Upgrades", { c.openUpgrades() }, Modifier.weight(1f), Wood, tag = "hb_btn_upgrades")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pill("Journal", onClick = { c.openJournal() }, tag = "hb_btn_journal")
                        Pill("Achievements", onClick = { c.openTrophies() }, tag = "hb_btn_ach")
                        Pill("Settings", onClick = { c.openSettings() })
                    }
                    Pill("Rest today  ·  skip to evening", onClick = { c.openEvening() }, tag = "hb_btn_rest")
                }
            } else {
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (st.actionsLeft > 0) "Conversations left: ${st.actionsLeft}" else "It's late. Let them rest.",
                        color = Cream, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("hb_actions"))
                    st.recruited().forEach { h ->
                        Row(Modifier.fillMaxWidth().background(Cream.copy(alpha = 0.93f), Soft).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Portrait(h.id, Modifier.size(52.dp).clip(CircleShape), ContentScale.Crop)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(h.def.name, fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp)
                                Text("${h.def.cls.title}  ·  ${h.tier().label}", fontSize = 11.sp, color = Wood)
                            }
                            if (st.actionsLeft > 0) {
                                Text("Talk", color = Cream, modifier = Modifier.background(Burgundy, Soft).clickable { c.startTalk(h.id) }.padding(horizontal = 10.dp, vertical = 6.dp).testTag("hb_talk_${h.id}"))
                                Spacer(Modifier.width(6.dp))
                                Text("Gift", color = Ink, modifier = Modifier.background(Gold, Soft).clickable { c.openGift(h.id) }.padding(horizontal = 10.dp, vertical = 6.dp))
                                if (h.affection >= Heartbound.DATE_MIN) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("Date", color = Cream, modifier = Modifier.background(Color(0xFF6B4C8A), Soft).clickable { c.startDate(h.id) }.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    HbButton("Turn in for the night", { c.turnIn() }, Modifier.fillMaxWidth(), fill = Wood, tag = "hb_turnin")
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable private fun BoardScreen(c: HeartboundController) {
    Box(Modifier.fillMaxSize().testTag("hb_board")) {
        Bg("guild", 0.35f)
        Column(Modifier.fillMaxSize()) {
            TopBar(c, {}, "Quest Board")
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pin a job. Pick a party. Bring them home.", color = Cream, fontSize = 13.sp)
                c.availableQuests().forEach { q ->
                    val stars = when {
                        q.difficulty >= 45 -> "★★★★"
                        q.difficulty >= 32 -> "★★★"
                        q.difficulty >= 20 -> "★★"
                        else -> "★"
                    }
                    Column(Modifier.fillMaxWidth().shadow(4.dp, Soft).background(Parchment, Soft)
                        .clickable { c.beginParty(q.id) }.padding(14.dp).testTag("hb_quest_${q.id}")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(q.title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 17.sp, modifier = Modifier.weight(1f))
                            Text(stars, color = Burgundy, fontSize = 13.sp)
                        }
                        Text(q.desc, color = Ink.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("${q.place.replaceFirstChar { it.uppercase() }}  ·  Party ${q.minParty}–${q.maxParty}  ·  🪙 ${q.gold}   XP ${q.xp}" +
                            (if (q.preferred.isNotEmpty()) "  ·  Best: ${q.preferred.joinToString { it.title }}" else ""),
                            fontSize = 11.sp, color = Wood, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable private fun PartyScreen(c: HeartboundController) {
    val q = Heartbound.quest[c.questId] ?: return
    val chance = c.chance()
    Box(Modifier.fillMaxSize().testTag("hb_party")) {
        Bg(q.place, 0.4f)
        Column(Modifier.fillMaxSize()) {
            TopBar(c, {}, q.title, back = { c.openBoard() })
            Text(q.desc, color = Cream, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 18.dp))
            Text("Choose ${q.minParty} to ${q.maxParty} ready members.", color = Gold, fontSize = 13.sp, modifier = Modifier.padding(18.dp, 8.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                c.state.recruited().forEach { h ->
                    val on = h.id in c.party
                    val ok = h.ready
                    Row(Modifier.fillMaxWidth().background(if (on) Gold.copy(alpha = 0.92f) else Cream.copy(alpha = 0.92f), Soft)
                        .clickable(enabled = ok) { c.toggleParty(h.id) }.padding(10.dp).testTag("hb_party_${h.id}"),
                        verticalAlignment = Alignment.CenterVertically) {
                        Portrait(h.id, Modifier.size(52.dp).clip(CircleShape), ContentScale.Crop)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${h.def.name}  ·  Lv ${h.level} ${h.def.cls.title}", fontWeight = FontWeight.Bold, color = Ink)
                            Text("ATK ${h.atk()}   DEF ${h.defn()}" + if (h.injuredDays > 0) "   Resting ${h.injuredDays}d" else "", fontSize = 12.sp, color = Wood)
                        }
                        if (h.def.cls in q.preferred) Text("suited", fontSize = 11.sp, color = Burgundy, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (chance != null) "Success chance  $chance%" else "Pick a valid party to see the odds.",
                    color = Cream, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.testTag("hb_chance").padding(bottom = 8.dp))
                HbButton("Send party", { c.confirmQuest() }, Modifier.fillMaxWidth(), enabled = chance != null, tag = "hb_send")
            }
        }
    }
}

@Composable private fun ResultScreen(c: HeartboundController) {
    val r = c.state.lastResult ?: return
    val q = Heartbound.quest.getValue(r.questId)
    Box(Modifier.fillMaxSize().testTag("hb_result")) {
        Bg(q.place, 0.45f)
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(22.dp)).background(Cream, RoundedCornerShape(22.dp)).padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (r.success) "Quest complete" else "Came home short", color = if (r.success) Color(0xFF2E6B3A) else Burgundy,
                    fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text(q.title, color = Wood, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
                Text(r.note, color = Ink, fontSize = 15.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text("🪙  +${r.gold}     XP  +${r.xp}     Roll  ${r.roll} / ${r.chance}%", fontWeight = FontWeight.SemiBold, color = Wood)
                if (r.leveled.isNotEmpty()) Text("Level up: ${r.leveled.joinToString { Heartbound.hero.getValue(it).name }}", color = Burgundy, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.weight(1f))
            HbButton("Evening at the hall", { c.dismissResult() }, Modifier.fillMaxWidth(), tag = "hb_to_evening")
        }
    }
}

@Composable private fun ShopScreen(c: HeartboundController) {
    Box(Modifier.fillMaxSize().testTag("hb_shop")) {
        Bg("town", 0.4f)
        Column {
            TopBar(c, {}, "The Market")
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gifts", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Heartbound.gifts.forEach { g ->
                    ShopRow(g.icon + "  " + g.name, g.desc, g.cost, c.state.inv(g.id), c.state.gold >= g.cost) { c.buy(g.id) }
                }
                Spacer(Modifier.height(8.dp))
                Text("Gear", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Heartbound.gear.forEach { g ->
                    ShopRow("🛡️  " + g.name, "${g.desc}  (+${g.atk} ATK  +${g.def} DEF)", g.cost, c.state.inv(g.id), c.state.gold >= g.cost) { c.buy(g.id) }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable private fun ShopRow(title: String, desc: String, cost: Int, owned: Int, can: Boolean, buy: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Cream.copy(alpha = 0.94f), Soft).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title + if (owned > 0) "  ×$owned" else "", fontWeight = FontWeight.Bold, color = Ink)
            Text(desc, fontSize = 12.sp, color = Wood)
        }
        Text("🪙 $cost", color = if (can) Ink else Color.Gray, fontWeight = FontWeight.SemiBold, modifier = Modifier
            .background(if (can) Gold else Color(0xFFD7D0C8), Soft).clickable(enabled = can, onClick = buy).padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@Composable private fun UpgradeScreen(c: HeartboundController) {
    val st = c.state
    Box(Modifier.fillMaxSize().testTag("hb_upgrades")) {
        Bg("guild", 0.4f)
        Column {
            TopBar(c, {}, "Guild Works")
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Heartbound.upgrades.forEach { u ->
                    val lv = when (u.id) { "rooms" -> st.rooms; "training" -> st.training; else -> st.tavern }
                    val maxed = lv >= u.levels.size
                    val cost = u.levels.getOrNull(lv)?.first
                    val effect = if (lv == 0) u.levels.first().second else if (maxed) u.levels.last().second else u.levels[lv].second
                    Column(Modifier.fillMaxWidth().background(Cream.copy(alpha = 0.95f), Soft).padding(16.dp)) {
                        Text("${u.icon}  ${u.name}", fontWeight = FontWeight.Bold, color = Ink, fontSize = 18.sp)
                        Text("Level $lv / ${u.levels.size}", fontSize = 12.sp, color = Wood)
                        Text(effect, fontSize = 14.sp, color = Ink, modifier = Modifier.padding(vertical = 6.dp))
                        if (maxed) Text("Fully improved.", color = Color(0xFF2E6B3A), fontWeight = FontWeight.SemiBold)
                        else HbButton("Improve  ·  🪙 $cost", { c.buyUpgrade(u.id) }, Modifier.fillMaxWidth(),
                            fill = if (st.gold >= (cost ?: 0)) Burgundy else Color(0xFF6B5A58), enabled = st.gold >= (cost ?: 0))
                    }
                }
            }
        }
    }
}

@Composable private fun ProfileScreen(c: HeartboundController, id: String) {
    val h = c.state.h(id); val d = h.def
    Box(Modifier.fillMaxSize().testTag("hb_profile")) {
        Bg("guild", 0.4f)
        Column {
            TopBar(c, {}, d.name)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Portrait(id, Modifier.height(280.dp).fillMaxWidth())
                Text(d.name, fontWeight = FontWeight.Black, color = Cream, fontSize = 24.sp)
                Text("${d.epithet}  ·  Lv ${h.level} ${d.cls.title}", color = Gold, fontSize = 14.sp)
                Hearts(h.affection, 18)
                Text("${h.tier().label}  ·  ${h.affection} / 100", color = Cream, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                LinearProgressIndicator(h.affection / 100f, Modifier.fillMaxWidth().height(8.dp).clip(Soft), Gold, Cream.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))
                InfoCard("About") { Text(d.personality, color = Ink, fontSize = 14.sp); Spacer(Modifier.height(6.dp)); Text(d.backstory, color = Wood, fontSize = 13.sp) }
                InfoCard("Combat") {
                    Text("ATK ${h.atk()}    DEF ${h.defn()}    XP ${h.xp} / ${Heartbound.xpToNext(h.level)}", color = Ink, fontWeight = FontWeight.SemiBold)
                    if (h.injuredDays > 0) Text("Recovering — ${h.injuredDays} day(s).", color = Burgundy)
                    val w = h.weapon?.let { Heartbound.gearById[it] }
                    val a = h.accessory?.let { Heartbound.gearById[it] }
                    Text("Weapon: ${w?.name ?: "None"}    Charm: ${a?.name ?: "None"}", color = Wood, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    val owned = Heartbound.gear.filter { c.state.inv(it.id) > 0 }
                    if (owned.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Equip from inventory:", fontSize = 12.sp, color = Wood)
                        owned.forEach { g ->
                            Text("Equip ${g.name}", color = Cream, modifier = Modifier.padding(top = 4.dp).background(Wood, Soft).clickable { c.equip(id, g.id) }.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
                InfoCard("Bonds") {
                    if (h.tier().min >= AffTier.FRIEND.min) {
                        Text("Loves: ${d.love.joinToString { Heartbound.gift[it]?.name ?: it }}", color = Ink)
                        Text("Likes: ${d.like.joinToString { Heartbound.gift[it]?.name ?: it }}", color = Ink)
                    } else Text("Spend time with them to learn what they love.", color = Wood)
                    val ev = Story.events.filter { it.heroId == id }
                    ev.forEach { e ->
                        val seen = e.id in h.seenEvents
                        val open = h.affection >= e.reqAff
                        Text((if (seen) "✦ " else if (open) "○ " else "· ") + (e.title ?: e.id) + if (!open) "  (at ${e.reqAff})" else if (!seen) "  — ready" else "",
                            color = if (seen) Burgundy else Ink, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).background(Cream.copy(alpha = 0.95f), Soft).padding(14.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = Burgundy, fontSize = 13.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable private fun GiftScreen(c: HeartboundController, id: String) {
    val d = Heartbound.hero.getValue(id)
    val gifts = Heartbound.gifts.filter { c.state.inv(it.id) > 0 }
    Box(Modifier.fillMaxSize().testTag("hb_gift")) {
        Bg("guild", 0.4f)
        Column {
            TopBar(c, {}, "A gift for ${d.name.split(" ").first()}", back = { c.goHome() })
            Text(Content.giftHint(d, c.state.h(id).affection), color = Cream, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp).testTag("hb_hint"))
            if (gifts.isEmpty()) Text("The satchel is empty. Visit the market first.", color = Cream, modifier = Modifier.padding(20.dp))
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                gifts.forEach { g ->
                    Row(Modifier.fillMaxWidth().background(Cream, Soft).clickable { c.giveGift(id, g.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(g.icon, fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${g.name}  ×${c.state.inv(g.id)}", fontWeight = FontWeight.Bold, color = Ink)
                            Text(g.desc, fontSize = 12.sp, color = Wood)
                        }
                        Text("Give", color = Cream, modifier = Modifier.background(Burgundy, Soft).padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable private fun DialogueScreen(c: HeartboundController, instant: Boolean) {
    val p = c.play ?: return
    val line = c.currentLine()
    val speaker = line?.speaker ?: ""
    val byName = remember { Heartbound.heroes.associate { it.name to it.id } }
    val face = byName[speaker] ?: p.heroId
    val expr = line?.expr ?: "idle"
    val full = line?.text ?: ""
    val delayMs = if (instant) 0 else c.settings.delayMs()
    val already = c.lineId()?.let { c.isSeen(it) } == true
    var shown by remember(full, p.lineIndex, p.followIndex, p.followup) {
        mutableIntStateOf(if (instant || delayMs == 0 || (c.settings.skipSeen && already)) full.length else 0)
    }
    LaunchedEffect(full, p.lineIndex, p.followIndex, p.followup, instant, delayMs) {
        if (instant || delayMs == 0 || (c.settings.skipSeen && already)) { shown = full.length; c.skipSeenLines(); return@LaunchedEffect }
        shown = 0
        while (shown < full.length) { delay(delayMs.toLong()); shown++ }
    }
    val done = shown >= full.length
    val night = c.isNight()
    Box(Modifier.fillMaxSize().testTag("hb_talk").clickable {
        when {
            p.awaitingChoice -> {}
            !done -> shown = full.length
            else -> c.advance()
        }
    }) {
        Bg(p.scene.bg, 0.12f, night = night)
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("Log", onClick = { c.openLog() }, tag = "hb_log")
            Spacer(Modifier.width(8.dp))
            Pill(if (done && c.hasMoreLines()) "Skip" else "Skip", onClick = {
                shown = full.length
                var g = 0
                while (g++ < 30 && c.hasMoreLines() && !c.showingChoices()) c.advance()
                if (!c.hasMoreLines() && !c.showingChoices()) c.advance()
            })
            p.scene.title?.let { Text(it, color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp).weight(1f)) } ?: Spacer(Modifier.weight(1f))
            p.heroId?.let { hid ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(Heartbound.hero.getValue(hid).name, color = Cream, fontSize = 12.sp)
                    Hearts(c.state.h(hid).affection, 13)
                }
            }
        }
        if (face != null) {
            Portrait(face, Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.70f).padding(bottom = 150.dp), expr = expr)
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp, 8.dp)) {
            if (p.awaitingChoice) {
                c.choices().forEachIndexed { i, (ch, ok) ->
                    val need = if (!ok && ch.stat != null) "  (${ch.stat.replaceFirstChar { it.titlecase() }} ${ch.statMin})" else ""
                    Text(ch.text + need, color = if (ok) Ink else Wood, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).shadow(4.dp, Soft)
                            .background(if (ok) Cream else Color(0xFFD7D0C8), Soft)
                            .clickable(enabled = ok) { c.choose(i) }.padding(14.dp).testTag("hb_choice_$i"))
                }
            } else {
                Box(Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(18.dp)).background(Night, RoundedCornerShape(18.dp))
                    .border(1.dp, Gold.copy(alpha = 0.55f), RoundedCornerShape(18.dp)).padding(16.dp, 14.dp)) {
                    Column {
                        if (speaker.isNotBlank()) {
                            Text(speaker, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.background(Gold, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 3.dp))
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(full.take(shown), color = Cream, fontSize = 16.sp, lineHeight = 22.sp, minLines = 3, modifier = Modifier.testTag("hb_line"))
                        Text(if (done) "▸" else "…", color = Gold, fontSize = 14.sp, modifier = Modifier.align(Alignment.End))
                    }
                }
            }
        }
    }
}

@Composable private fun CreateScreen(c: HeartboundController) {
    val d = c.draft
    Box(Modifier.fillMaxSize().testTag("hb_create")) {
        Bg("guild", 0.4f)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Who walks through the door?", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = d.name, onValueChange = { c.draft = d.copy(name = it.take(16)) },
                label = { Text("Your name") }, singleLine = true,
                colors = TextFieldDefaults.colors(focusedTextColor = Cream, unfocusedTextColor = Cream, focusedContainerColor = Night, unfocusedContainerColor = Night, cursorColor = Gold, focusedIndicatorColor = Gold, unfocusedIndicatorColor = Cream, focusedLabelColor = Gold, unfocusedLabelColor = Cream),
                modifier = Modifier.fillMaxWidth().testTag("hb_name")
            )
            Spacer(Modifier.height(14.dp))
            Text("Look", color = Cream, fontWeight = FontWeight.Bold)
            Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Content.looks.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { look ->
                            Column(Modifier.weight(1f).shadow(4.dp, Soft).background(if (d.look == look.id) Gold else Cream, Soft)
                                .clickable { c.draft = d.copy(look = look.id) }.padding(8.dp).testTag("hb_look_${look.id}"),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Portrait(look.id, Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)), ContentScale.Crop)
                                Text(look.label, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
            Text("Pronouns", color = Cream, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PronounSet.ALL.forEach { pr ->
                    Pill(pr.label, onClick = { c.draft = d.copy(pronouns = pr.id) }, fill = if (d.pronouns == pr.id) Gold else Cream, tag = "hb_pr_${pr.id}")
                }
            }
            Text("Where did you come from?", color = Cream, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            Content.origins.forEach { o ->
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).background(if (d.origin == o.id) Gold else Cream.copy(alpha = 0.95f), Soft)
                    .clickable { c.draft = d.copy(origin = o.id, charm = o.charm, wit = o.wit, courage = o.courage) }.padding(12.dp).testTag("hb_origin_${o.id}")) {
                    Text(o.name, fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp)
                    Text(o.blurb, color = Wood, fontSize = 13.sp)
                    Text("Charm ${o.charm}   Wit ${o.wit}   Courage ${o.courage}", color = Burgundy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            HbButton("Enter the hall", { c.newGame(c.draft) }, Modifier.fillMaxWidth(), enabled = d.name.isNotBlank(), tag = "hb_begin")
            Spacer(Modifier.height(10.dp))
            Pill("‹  Back", onClick = { c.backToTitle() })
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable private fun SlotsScreen(c: HeartboundController) {
    Box(Modifier.fillMaxSize().testTag("hb_slots")) {
        Bg("guild", 0.45f)
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(if (c.slotMode == "new") "Choose a ledger" else "Open a ledger", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text(if (c.slotMode == "new") "A used slot will be overwritten." else "Pick a saved hall to return to.", color = Cream, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            c.slotInfos().forEach { sl ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).background(Cream.copy(alpha = 0.95f), Soft).padding(12.dp)
                    .testTag("hb_slot_${sl.index}"), verticalAlignment = Alignment.CenterVertically) {
                    if (!sl.empty) Portrait(sl.look, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)), ContentScale.Crop)
                    else Box(Modifier.size(56.dp).background(Color(0xFFD7D0C8), RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        if (sl.empty) Text("Empty slot ${sl.index + 1}", color = Wood, fontWeight = FontWeight.Bold)
                        else {
                            Text("${sl.name}  ·  ${sl.origin}", fontWeight = FontWeight.Bold, color = Ink)
                            Text("Day ${sl.day}  ·  🪙 ${sl.gold}  ·  ${sl.phase}", fontSize = 12.sp, color = Wood)
                        }
                    }
                    if (c.slotMode == "new") {
                        Text(if (sl.empty) "Begin" else "Overwrite", color = Cream, modifier = Modifier.background(Burgundy, Soft).clickable { c.openCreate(sl.index) }.padding(horizontal = 12.dp, vertical = 8.dp))
                    } else if (!sl.empty) {
                        Text("Load", color = Cream, modifier = Modifier.background(Burgundy, Soft).clickable { c.loadSlot(sl.index) }.padding(horizontal = 12.dp, vertical = 8.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Erase", color = Cream, modifier = Modifier.background(Wood, Soft).clickable { c.deleteSlot(sl.index) }.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Pill("‹  Back", onClick = { c.backToTitle() })
        }
    }
}

@Composable private fun SettingsScreen(c: HeartboundController) {
    Box(Modifier.fillMaxSize().testTag("hb_settings_screen")) {
        Bg("guild", 0.45f, night = c.state.phase == "evening")
        Column(Modifier.padding(16.dp)) {
            Text("Settings", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Spacer(Modifier.height(16.dp))
            Text("Text speed", color = Cream, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3 to "Slow", 2 to "Normal", 1 to "Fast", 0 to "Instant").forEach { (n, lab) ->
                    Pill(lab, onClick = { c.setSpeed(n) }, fill = if (c.settings.textSpeed == n) Gold else Cream)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { c.setSkipSeen(!c.settings.skipSeen) }.testTag("hb_skip")) {
                Text(if (c.settings.skipSeen) "●" else "○", color = Gold, fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Skip seen text", color = Cream, fontWeight = FontWeight.Bold)
                    Text("Already-read lines jump to the end.", color = Cream.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
            Pill("‹  Back", onClick = { c.closeMenu() })
        }
    }
}

@Composable private fun JournalScreen(c: HeartboundController) {
    Box(Modifier.fillMaxSize().testTag("hb_journal")) {
        Bg("guild", 0.4f, night = c.isNight())
        Column {
            TopBar(c, {}, "Relationship journal", back = { c.closeMenu() })
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Charm ${c.state.player.charm}   Wit ${c.state.player.wit}   Courage ${c.state.player.courage}", color = Gold, fontWeight = FontWeight.SemiBold)
                Heartbound.heroes.forEach { d ->
                    val h = c.state.h(d.id)
                    Column(Modifier.fillMaxWidth().background(Cream.copy(alpha = 0.95f), Soft).padding(12.dp).testTag("hb_j_${d.id}")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (h.recruited) Portrait(d.id, Modifier.size(56.dp).clip(CircleShape), ContentScale.Crop)
                            else Box(Modifier.size(56.dp).background(Color(0xFFD7D0C8), CircleShape), contentAlignment = Alignment.Center) { Text("?", color = Wood, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (h.recruited) d.name else "Someone not yet of the hall", fontWeight = FontWeight.Bold, color = Ink)
                                if (h.recruited) {
                                    Text("${d.cls.title}  ·  ${h.tier().label}  ·  ${h.affection}/100", fontSize = 12.sp, color = Wood)
                                    Hearts(h.affection, 12)
                                } else Text("A rumor, a knock, a path not yet taken.", fontSize = 12.sp, color = Wood)
                            }
                        }
                        if (h.recruited) {
                            Spacer(Modifier.height(6.dp))
                            Text(Content.giftHint(d, h.affection), fontSize = 13.sp, color = Ink)
                            Story.events.filter { it.heroId == d.id }.forEach { e ->
                                val seen = e.id in h.seenEvents
                                val open = h.affection >= e.reqAff
                                Text((if (seen) "✦ " else if (open) "○ " else "· ") + (e.title ?: e.id) + if (!open) "  (at ${e.reqAff})" else if (!seen) "  — ready" else "",
                                    fontSize = 12.sp, color = if (seen) Burgundy else Ink)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable private fun TrophyScreen(c: HeartboundController) {
    Box(Modifier.fillMaxSize().testTag("hb_achievements")) {
        Bg("guild", 0.4f)
        Column {
            TopBar(c, {}, "Achievements", back = { c.closeMenu() })
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val n = c.state.achievements.size
                Text("$n / ${Content.achievements.size} found", color = Gold, fontWeight = FontWeight.SemiBold)
                Content.achievements.forEach { a ->
                    val on = a.id in c.state.achievements
                    Column(Modifier.fillMaxWidth().background(if (on) Cream else Cream.copy(alpha = 0.6f), Soft).padding(12.dp)) {
                        Text(if (on) "✦  ${a.title}" else "·  ???", fontWeight = FontWeight.Bold, color = if (on) Burgundy else Wood)
                        Text(if (on) a.desc else "Not yet.", fontSize = 13.sp, color = Ink)
                    }
                }
            }
        }
    }
}

@Composable private fun SummaryScreen(c: HeartboundController) {
    val r = c.state.report
    Box(Modifier.fillMaxSize().testTag("hb_summary")) {
        Bg("guild", 0.35f, night = true)
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(22.dp)).background(Cream, RoundedCornerShape(22.dp)).padding(22.dp)) {
                Text(if (r != null) "Day ${r.day} is over" else "The hall grows quiet", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Ink)
                Spacer(Modifier.height(10.dp))
                if (r != null) {
                    r.questTitle?.let { Text((if (r.questSuccess == true) "Quest complete: " else "Returned short: ") + it, color = Wood, fontSize = 14.sp) }
                    Text(if (r.goldDelta >= 0) "🪙  +${r.goldDelta}" else "🪙  ${r.goldDelta}", fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(top = 6.dp))
                    r.aff.forEach { (id, d) ->
                        val name = Heartbound.hero[id]?.name ?: id
                        Text("$name  ${if (d >= 0) "+" else ""}$d affection", color = Burgundy, fontSize = 14.sp)
                    }
                    if (r.leveled.isNotEmpty()) Text("Level up: ${r.leveled.joinToString { Heartbound.hero.getValue(it).name }}", fontWeight = FontWeight.Bold, color = Ink)
                    r.injured?.let { Text("${Heartbound.hero.getValue(it).name} is recovering.", color = Wood) }
                    r.recruited.forEach { Text("${Heartbound.hero.getValue(it).name} joined the hall.", color = Burgundy, fontWeight = FontWeight.Bold) }
                    r.newAchievements.forEach { id -> Content.achById[id]?.let { Text("✦  ${it.title}", color = Gold, fontWeight = FontWeight.Bold) } }
                    if (r.aff.isEmpty() && r.questTitle == null && r.recruited.isEmpty()) Text("A quiet day. The kettle still sang.", color = Wood)
                }
            }
            Spacer(Modifier.weight(1f))
            HbButton("A new morning", { c.dismissSummary() }, Modifier.fillMaxWidth(), tag = "hb_next_day")
        }
    }
}

@Composable private fun LogScreen(c: HeartboundController) {
    Box(Modifier.fillMaxSize().testTag("hb_log_screen")) {
        Bg("guild", 0.5f, night = true)
        Column {
            TopBar(c, {}, "Dialogue log", back = { c.closeMenu() })
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (c.state.log.isEmpty()) Text("Nothing written yet.", color = Cream)
                c.state.log.forEach { line ->
                    Text(line, color = Cream, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().background(Night, Soft).padding(10.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
