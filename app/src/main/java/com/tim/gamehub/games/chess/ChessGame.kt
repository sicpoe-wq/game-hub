package com.tim.gamehub.games.chess

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.min

@Composable
fun ChessGame(onExit: () -> Unit, controller: ChessController? = null, instant: Boolean = false) {
    val ctx = LocalContext.current
    val c = controller ?: remember { ChessController(PrefsChessStore(ctx), instant = instant) }
    val sound = remember { ChessSound() }
    DisposableEffect(c) {
        c.attach()
        if (!instant) {
            sound.enabled = { c.prefs.sound }
            sound.attach(ctx)
            c.onSound = { sound.play(it) }
        }
        onDispose {
            c.persist()
            c.detach()
            c.onSound = null
            sound.detach()
        }
    }
    BackHandler { if (c.page != ChessPage.MENU) c.openMenu() else onExit() }
    val theme = ChessThemes.board(if (c.page == ChessPage.THEMES) c.peekBoard else c.prefs.board)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(theme.backdrop)).testTag("chess_root")) {
        when (c.page) {
            ChessPage.MENU -> MenuPage(c, onExit)
            ChessPage.PLAY -> PlayPage(c, onExit, instant)
            ChessPage.THEMES -> ThemesPage(c, instant)
            ChessPage.SETTINGS -> SettingsPage(c)
        }
    }
}

@Composable
private fun MenuPage(c: ChessController, onExit: () -> Unit) {
    var pickCpu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(onClick = onExit, tag = "btn_hub") { Text("‹  Hub", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
        Text("CHESS", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Text("Pick a board, pick a side.", color = Color.White.copy(alpha = 0.65f), fontSize = 15.sp)
        Spacer(Modifier.height(28.dp))
        if (c.hasSave) MenuBtn("Continue", "Resume the saved game", "btn_continue") { c.continueGame() }
        MenuBtn("Play vs Computer", "Offline engine, three difficulties", "btn_cpu") { pickCpu = true }
        MenuBtn("Pass and Play", "Local two-player, one device", "btn_local") { c.newLocal() }
        MenuBtn("Themes", "Boards and piece sets", "btn_themes") { c.openThemes() }
        MenuBtn("Settings", "Clock, sounds, coordinates", "btn_settings") { c.openSettings() }
    }
    if (pickCpu) CpuDialog(c) { pickCpu = false }
}

@Composable
private fun CpuDialog(c: ChessController, onClose: () -> Unit) {
    var level by remember { mutableStateOf(c.level) }
    var playAs by remember { mutableStateOf(Side.WHITE) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Play vs Computer") },
        text = {
            Column {
                Text("Difficulty", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CpuLevel.entries.forEach { lv ->
                        FilterChip(selected = level == lv, onClick = { level = lv }, label = { Text(lv.label) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Play as", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = playAs == Side.WHITE, onClick = { playAs = Side.WHITE }, label = { Text("White") })
                    FilterChip(selected = playAs == Side.BLACK, onClick = { playAs = Side.BLACK }, label = { Text("Black") })
                }
            }
        },
        confirmButton = { TextButton(onClick = { c.newCpu(level, playAs); onClose() }) { Text("Start") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}

@Composable
private fun MenuBtn(title: String, sub: String, tag: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onClick).testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun PlayPage(c: ChessController, onExit: () -> Unit, instant: Boolean) {
    var confirm by remember { mutableStateOf<String?>(null) }
    val theme = ChessThemes.board(c.prefs.board)
    val set = ChessThemes.set(c.prefs.pieces)
    if (c.prefs.clockSecs > 0 && !instant) {
        LaunchedEffect(c.status, c.thinking, c.history.size) {
            while (isActive && !c.status.over && c.prefs.clockSecs > 0) {
                delay(100)
                c.tick(100)
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(onClick = { c.openMenu() }, tag = "btn_menu") { Text("‹  Menu", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.weight(1f))
            Text("CHESS", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Pill(onClick = { c.setView3d(!c.prefs.view3d) }, tag = "btn_3d") {
                Text(if (c.prefs.view3d) "3D" else "2D", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            if (c.prefs.view3d) {
                Spacer(Modifier.width(6.dp))
                Pill(onClick = { c.resetCamera() }, tag = "btn_cam") { Text("Reset cam", color = Color.White, fontSize = 13.sp) }
            }
            Spacer(Modifier.width(6.dp))
            Pill(onClick = { if (c.history.isNotEmpty()) c.undo() }, tag = "btn_undo") { Text("Undo", color = Color.White, fontSize = 14.sp) }
        }
        Spacer(Modifier.height(6.dp))
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            OpponentRow(c, if (c.flipped) Side.WHITE else Side.BLACK, theme, set)
            val portraits = rememberPoliticalPortraits(c.prefs.pieces)
            if (c.prefs.view3d) {
                ChessBoard3DView(c, theme, set, instant, portraits, Modifier.fillMaxWidth().aspectRatio(1f).testTag("chess_board"))
            } else {
                ChessBoardView(c, theme, set, instant, portraits, Modifier.fillMaxWidth().aspectRatio(1f).testTag("chess_board"))
            }
            OpponentRow(c, if (c.flipped) Side.BLACK else Side.WHITE, theme, set)
            StatusLine(c)
        }
        MoveList(c)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(onClick = { confirm = "resign" }, tag = "btn_resign") { Text("Resign", color = Color.White, fontSize = 13.sp) }
            Pill(onClick = { confirm = "draw" }, tag = "btn_draw") { Text("Draw", color = Color.White, fontSize = 13.sp) }
            Pill(onClick = { c.openThemes() }, tag = "btn_themes") { Text("Themes", color = Color.White, fontSize = 13.sp) }
            Spacer(Modifier.weight(1f))
            Pill(onClick = onExit, tag = "btn_hub") { Text("Hub", color = Color.White, fontSize = 13.sp) }
        }
    }
    c.pendingPromo?.let { PromoDialog(c, set) }
    if (c.status.over) GameOver(c)
    when (confirm) {
        "resign" -> Confirm("Resign this game?", { c.resign(); confirm = null }, { confirm = null })
        "draw" -> Confirm("Agree to a draw?", { c.agreeDraw(); confirm = null }, { confirm = null })
    }
}

@Composable
private fun OpponentRow(c: ChessController, side: Side, theme: BoardTheme, set: PieceSet) {
    val active = !c.status.over && c.pos.side == side
    val name = buildString {
        append(if (side == Side.WHITE) "White" else "Black")
        if (c.mode == ChessMode.CPU) {
            append(if (side == c.playerSide) "  ·  You" else "  ·  CPU (${c.level.label})")
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, color = if (active) Color.White else Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            c.captured(side).forEach { k ->
                Canvas(Modifier.size(16.dp)) { drawPiece(Pc(k, side), set, Rect(0f, 0f, size.width, size.height)) }
            }
        }
        Spacer(Modifier.weight(1f))
        if (c.prefs.clockSecs > 0) {
            val ms = if (side == Side.WHITE) c.wTime else c.bTime
            Text(fmtTime(ms), color = if (ms < 20_000 && active) Color(0xFFFF8A8A) else Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                modifier = Modifier.testTag("clock_${side.name.lowercase()}"))
        }
    }
}

private fun fmtTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun StatusLine(c: ChessController) {
    val t = when {
        c.thinking -> "Thinking…"
        c.status == Status.CHECK -> "Check!"
        c.status == Status.CHECKMATE -> "Checkmate"
        c.status == Status.STALEMATE -> "Stalemate"
        c.status.over -> "Game over"
        else -> "${if (c.pos.side == Side.WHITE) "White" else "Black"} to move"
    }
    val fig = if (politicalStyleOf(c.prefs.pieces) != null)
        c.selected?.let { sq -> c.figs.getOrNull(sq)?.let { PoliticalRoster.of(it) } }
    else null
    val label = if (fig != null) "$t  ·  ${fig.name}" else t
    Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 4.dp).testTag("chess_status"))
}

@Composable
private fun MoveList(c: ChessController) {
    val text = buildString {
        c.history.forEachIndexed { i, h ->
            if (i % 2 == 0) {
                if (i > 0) append("   ")
                append("${i / 2 + 1}. ")
            } else append(' ')
            append(h.san)
        }
    }
    Text(
        text.ifBlank { "Moves will appear here" },
        color = Color.White.copy(alpha = if (c.history.isEmpty()) 0.4f else 0.8f),
        fontSize = 12.sp,
        maxLines = 2,
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp).testTag("move_list"),
    )
}

@Composable
private fun ChessBoardView(c: ChessController, theme: BoardTheme, set: PieceSet, instant: Boolean, portraits: Map<String, ImageBitmap> = emptyMap(), modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    var dragSq by remember { mutableStateOf<Int?>(null) }
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    var boardRect by remember { mutableStateOf(Rect.Zero) }
    var animP by remember { mutableFloatStateOf(1f) }
    val timeMs = if (instant || !c.prefs.boardAnim) 800L else {
        var t by remember { mutableLongStateOf(800L) }
        LaunchedEffect(c.prefs.boardAnim) {
            val start = withFrameMillisZero()
            while (isActive) {
                t = 800L + frameDelta(start)
                delay(16)
            }
        }
        t
    }
    LaunchedEffect(c.animTick) {
        if (instant || c.animFrom == null) { animP = 1f; return@LaunchedEffect }
        animP = 0f
        animate(0f, 1f, animationSpec = tween(180)) { v, _ -> animP = v }
        c.clearAnim()
        animP = 1f
    }
    val legalTos = c.legalFrom.map { it.to }.toSet()
    val caps = c.legalFrom.filter { it.captured != null }.map { it.to }.toSet()
    Canvas(modifier.pointerInput(c.flipped, c.status, c.thinking, c.pos, c.selected) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val sq = sqAt(boardRect, down.position, c.flipped) ?: return@awaitEachGesture
            val canGrab = !c.status.over && !c.thinking && c.pos.at(sq)?.side == c.pos.side &&
                (c.mode != ChessMode.CPU || c.pos.side == c.playerSide)
            var dragged = false
            val start = down.position
            if (canGrab) {
                drag(down.id) { change ->
                    if ((change.position - start).getDistance() > 16f) {
                        dragged = true
                        dragSq = sq
                        dragPos = change.position
                    }
                    change.consume()
                }
            }
            if (dragged && dragSq != null) {
                val dest = sqAt(boardRect, dragPos ?: down.position, c.flipped)
                if (dest != null) c.drop(dragSq!!, dest) else c.selected = null
                dragSq = null; dragPos = null
            } else {
                c.tap(sq)
            }
        }
    }) {
        val pad = min(size.width, size.height) * 0.03f
        val side = min(size.width, size.height) - pad * 2
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        boardRect = Rect(left, top, left + side, top + side)
        drawBoard(
            theme, boardRect, c.prefs.coords, c.selected, legalTos, caps,
            c.lastMove?.from, c.lastMove?.to, c.checkSq, c.flipped,
            c.prefs.boardAnim && !instant, timeMs, measurer,
        )
        val hide = mutableSetOf<Int>()
        if (dragSq != null) hide += dragSq!!
        if (animP < 1f && c.animTo != null) hide += c.animTo!!
        val political = politicalStyleOf(c.prefs.pieces)
        for (sq in 0..63) {
            if (sq in hide) continue
            val pc = c.pos.at(sq) ?: continue
            drawThemedPiece(pc, set, sqRect(boardRect, sq, c.flipped), c.figs.getOrNull(sq), portraits, political)
        }
        if (animP < 1f && c.animFrom != null && c.animTo != null && c.animPc != null) {
            val a = sqRect(boardRect, c.animFrom!!, c.flipped)
            val b = sqRect(boardRect, c.animTo!!, c.flipped)
            val r = Rect(
                a.left + (b.left - a.left) * animP,
                a.top + (b.top - a.top) * animP,
                a.right + (b.right - a.right) * animP,
                a.bottom + (b.bottom - a.bottom) * animP,
            )
            drawThemedPiece(c.animPc!!, set, r, c.animFig, portraits, political)
        }
        val ds = dragSq; val dp = dragPos
        if (ds != null && dp != null) {
            val pc = c.pos.at(ds)
            if (pc != null) {
                val s = boardRect.width / 8f
                drawThemedPiece(pc, set, Rect(dp.x - s / 2, dp.y - s * 1.15f, dp.x + s / 2, dp.y + s * 0.15f), c.figs.getOrNull(ds), portraits, political)
            }
        }
    }
}

private fun withFrameMillisZero(): Long = android.os.SystemClock.uptimeMillis()
private fun frameDelta(start: Long): Long = android.os.SystemClock.uptimeMillis() - start

@Composable
private fun PromoDialog(c: ChessController, set: PieceSet) {
    val side = c.pos.side
    AlertDialog(
        onDismissRequest = { c.cancelPromo() },
        title = { Text("Promote pawn") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (k in listOf(Kind.Q, Kind.R, Kind.B, Kind.N)) {
                    Box(Modifier.size(56.dp).clickable { c.pickPromo(k) }.testTag("promo_${k.letter}"), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) { drawPiece(Pc(k, side), set, Rect(0f, 0f, size.width, size.height)) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { c.cancelPromo() }) { Text("Cancel") } },
    )
}

@Composable
private fun GameOver(c: ChessController) {
    val title = when (c.status) {
        Status.CHECKMATE -> "Checkmate"
        Status.STALEMATE -> "Stalemate"
        Status.DRAW_50 -> "Draw · 50-move rule"
        Status.DRAW_REPEAT -> "Draw · threefold repetition"
        Status.DRAW_MATERIAL -> "Draw · insufficient material"
        Status.AGREE -> "Draw agreed"
        Status.RESIGN -> "${if (c.winner == Side.WHITE) "Black" else "White"} resigned"
        Status.TIMEOUT -> "Time out"
        else -> "Game over"
    }
    val sub = when {
        c.status == Status.STALEMATE || c.status == Status.DRAW_50 || c.status == Status.DRAW_REPEAT || c.status == Status.DRAW_MATERIAL || c.status == Status.AGREE -> "It's a draw."
        c.winner == Side.WHITE -> "White wins."
        c.winner == Side.BLACK -> "Black wins."
        else -> ""
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = { Text(sub) },
        confirmButton = {
            TextButton(onClick = {
                if (c.mode == ChessMode.CPU) c.newCpu(c.level, c.playerSide) else c.newLocal()
            }) { Text("New game") }
        },
        dismissButton = { TextButton(onClick = { c.openMenu() }) { Text("Menu") } },
    )
}

@Composable
private fun Confirm(msg: String, yes: () -> Unit, no: () -> Unit) {
    AlertDialog(onDismissRequest = no, title = { Text(msg) }, confirmButton = { TextButton(onClick = yes) { Text("Yes") } }, dismissButton = { TextButton(onClick = no) { Text("Cancel") } })
}

@Composable
fun ThemesPage(c: ChessController, instant: Boolean) {
    val theme = ChessThemes.board(c.peekBoard)
    val set = ChessThemes.set(c.peekPieces)
    val preview = remember {
        val p = startPos()
        // a lively mid-opening position for the live preview
        var x = p
        for (u in listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6")) x = Chess.play(x, Chess.parseUci(x, u)!!)
        x
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(onClick = { c.closeThemes() }, tag = "btn_back") {
                Text("‹  Back", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Text("THEMES", color = Color.White, fontSize = 16.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Pill(onClick = { c.closeThemes() }, tag = "btn_done") { Text("Done", color = Color.White, fontSize = 14.sp) }
        }
        Text("Board and pieces are chosen independently. Preview updates live.", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text("Board view", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp))
            ThemeChip("2D", !c.prefs.view3d, "view_2d") { c.setView3d(false) }
            Spacer(Modifier.width(8.dp))
            ThemeChip("3D", c.prefs.view3d, "view_3d") { c.setView3d(true) }
            if (c.prefs.view3d) {
                Spacer(Modifier.width(8.dp))
                ThemeChip("Reset cam", false, "btn_cam") { c.resetCamera() }
            }
        }
        val measurer = rememberTextMeasurer()
        val tMs = 800L
        val portraits = rememberPoliticalPortraits(c.peekPieces)
        val political = politicalStyleOf(c.peekPieces)
        val previewFigs = remember { replayFigures(run {
            val hist = mutableListOf<Hist>()
            var p = startPos()
            for (u in listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6")) {
                val m = Chess.parseUci(p, u)!!
                hist += Hist(m, Chess.san(p, m), p.fen())
                p = Chess.play(p, m)
            }
            hist
        }) }
        if (c.prefs.view3d) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f).padding(horizontal = 16.dp).testTag("theme_preview")) {
                drawBoard3D(
                    theme, c.cam, true, parseSq("f3"),
                    setOf(parseSq("d4"), parseSq("e5"), parseSq("g5"), parseSq("h4"), parseSq("d2")),
                    setOf(parseSq("e5")), parseSq("f1"), parseSq("b5"), null, false,
                    preview, set, previewFigs, portraits, political, emptySet(),
                    null, null, null, null, 1f, null, null, measurer,
                )
            }
        } else {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f).padding(horizontal = 28.dp).testTag("theme_preview")) {
                val side = min(size.width, size.height)
                val left = (size.width - side) / 2f
                val board = Rect(left, 0f, left + side, side)
                drawBoard(theme, board, true, parseSq("f3"), setOf(parseSq("d4"), parseSq("e5"), parseSq("g5"), parseSq("h4"), parseSq("d2")), setOf(parseSq("e5")), parseSq("f1"), parseSq("b5"), null, false, !instant && c.prefs.boardAnim, tMs, measurer)
                for (sq in 0..63) {
                    val pc = preview.at(sq) ?: continue
                    drawThemedPiece(pc, set, sqRect(board, sq, false), previewFigs.getOrNull(sq), portraits, political)
                }
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false).padding(top = 10.dp)) {
            Text("Boards", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            FlowRowish {
                ChessThemes.boards.forEach { b ->
                    ThemeChip(b.name, c.peekBoard == b.id, "board_${b.id}") { c.peekBoard = b.id; c.setPref(c.prefs.copy(board = b.id)) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Piece sets", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            FlowRowish {
                ChessThemes.availableSets().forEach { s ->
                    ThemeChip(s.name, c.peekPieces == s.id, "pieces_${s.id}") { c.peekPieces = s.id; c.setPref(c.prefs.copy(pieces = s.id)) }
                }
            }
            if (political != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    if (political == PoliticalStyle.PARODY)
                        "Parody — not affiliated with or endorsed by anyone depicted. Cartoon caricatures of public figures, treated even-handedly."
                    else
                        "Likeness set for private use only. Respectful portraits of public figures; not affiliated with or endorsed by anyone depicted.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.testTag("political_disclaimer"),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FlowRowish(content: @Composable RowScope.() -> Unit) {
    // Simple wrapping via two-or-more rows is harder without FlowRow (M3 1.2+ may have it).
    // Use a column of chunks of 2-3 by laying out as a wrapping Box with FlowRow if available.
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).testTag(tag)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) { Text(label, color = Color.White, fontSize = 13.sp) }
}

@Composable
private fun SettingsPage(c: ChessController) {
    val p = c.prefs
    fun set(np: ChessPrefs) { c.setPref(np) }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(onClick = { c.openMenu() }, tag = "btn_back") { Text("‹  Back", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.weight(1f))
            Text("SETTINGS", color = Color.White, fontSize = 16.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Toggle("Coordinates", p.coords, "opt_coords") { set(p.copy(coords = it)) }
        Toggle("Board animations", p.boardAnim, "opt_anim") { set(p.copy(boardAnim = it)) }
        Toggle("Sound effects", p.sound, "opt_sound") { set(p.copy(sound = it)) }
        Toggle("Auto-flip board (pass-and-play)", p.autoFlip, "opt_flip") { set(p.copy(autoFlip = it)) }
        Spacer(Modifier.height(12.dp))
        Text("Chess clock (new games)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        val clocks = listOf(0 to "Off", 180 to "3 min", 300 to "5 min", 600 to "10 min", 900 to "15+10")
        FlowRowish {
            clocks.forEach { (secs, label) ->
                val sel = if (secs == 900) p.clockSecs == 900 else p.clockSecs == secs && p.increment == 0
                ThemeChip(label, sel, "clock_$secs") {
                    if (secs == 900) set(p.copy(clockSecs = 900, increment = 10))
                    else set(p.copy(clockSecs = secs, increment = 0))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Sound effects are original and released under CC0. See LICENSE-AUDIO.md.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    }
}

@Composable
private fun Toggle(label: String, value: Boolean, tag: String, on: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag(tag).clickable { on(!value) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = on)
    }
}

@Composable
fun Pill(onClick: (() -> Unit)? = null, tag: String? = null, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun rememberPoliticalPortraits(setId: String): Map<String, ImageBitmap> {
    val ctx = LocalContext.current
    val style = politicalStyleOf(setId)
    return remember(setId) {
        if (style == null) emptyMap()
        else PoliticalRoster.all.mapNotNull { f ->
            ctx.politicalBitmap(style, f.id)?.let { f.id to it }
        }.toMap()
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawThemedPiece(
    pc: Pc,
    set: PieceSet,
    r: Rect,
    figId: String?,
    portraits: Map<String, ImageBitmap>,
    political: PoliticalStyle?,
) {
    val bmp = if (political != null && figId != null) portraits[figId] else null
    if (bmp == null) {
        drawPiece(pc, set, r)
        return
    }
    val iw = bmp.width.toFloat()
    val ih = bmp.height.toFloat()
    val targetH = r.height * 1.32f
    val targetW = targetH * (iw / ih.coerceAtLeast(1f))
    val left = r.center.x - targetW / 2f
    val top = r.bottom - targetH
    drawImage(
        bmp,
        dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
        dstSize = androidx.compose.ui.unit.IntSize(targetW.toInt().coerceAtLeast(1), targetH.toInt().coerceAtLeast(1)),
    )
}
