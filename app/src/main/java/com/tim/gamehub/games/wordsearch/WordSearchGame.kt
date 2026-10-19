package com.tim.gamehub.games.wordsearch

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val HIGHLIGHTS = listOf(0xFFFF6B6B, 0xFF4ECDC4, 0xFFFFB84C, 0xFF7A6FF0, 0xFF6BCB77, 0xFFF06FB5, 0xFF4D96FF, 0xFFC08B5C, 0xFF00B8A9, 0xFFFF8E3C).map { Color(it).copy(alpha = 0.55f) }

fun fmt(s: Long) = "%d:%02d".format(s / 60, s % 60)

class BestTimes(ctx: Context) {
    private val p = ctx.getSharedPreferences("wordsearch_best", Context.MODE_PRIVATE)
    fun get(d: Difficulty): Long? = p.getLong(d.name, -1).takeIf { it >= 0 }
    fun submit(d: Difficulty, s: Long): Boolean { val b = get(d); return if (b == null || s < b) { p.edit().putLong(d.name, s).apply(); true } else false }
}

@Composable
fun WordSearchGame(onExit: () -> Unit) {
    val ctx = LocalContext.current
    val best = remember { BestTimes(ctx) }
    var diff by remember { mutableStateOf(Difficulty.EASY) }
    var theme by remember { mutableStateOf(THEMES.keys.random()) }
    var seed by remember { mutableIntStateOf(0) }
    val puzzle = remember(diff, theme, seed) { Generator.generate(theme, diff) }
    val found = remember(puzzle) { mutableStateListOf<Placed>() }
    var seconds by remember(puzzle) { mutableLongStateOf(0L) }
    var newBest by remember(puzzle) { mutableStateOf(false) }
    val won = found.size == puzzle.words.size
    LaunchedEffect(puzzle, won) { while (!won) { delay(1000); seconds++ } }
    LaunchedEffect(won) { if (won) newBest = best.submit(diff, seconds) }
    fun newGame() { theme = THEMES.keys.random(); seed++ }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("‹ Hub", fontSize = 16.sp) }
            Spacer(Modifier.weight(1f))
            Text("⏱ ${fmt(seconds)}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Button(onClick = ::newGame) { Text("New game") }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { d ->
                FilterChip(selected = d == diff, onClick = { diff = d; newGame() }, label = { Text(d.label) })
            }
        }
        Text("Theme: $theme   •   Best: ${best.get(diff)?.let(::fmt) ?: "—"}", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Box {
            Grid(puzzle, found) { if (it !in found && !won) found += it }
            if (won) Column(
                Modifier.matchParentSize().background(Color.White.copy(alpha = 0.93f), RoundedCornerShape(16.dp)),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Text("🎉 You found them all!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Time: ${fmt(seconds)}", fontSize = 20.sp)
                if (newBest) Text("New best time!", color = Color(0xFF2E9E5B), fontWeight = FontWeight.SemiBold)
                else Text("Best: ${best.get(diff)?.let(::fmt)}", color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = ::newGame) { Text("Play again") }
                    OutlinedButton(onClick = onExit) { Text("Back to hub") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Words (${found.size}/${puzzle.words.size})", fontWeight = FontWeight.SemiBold)
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            puzzle.words.forEachIndexed { i, w ->
                val f = w in found
                Text(w.word, fontSize = 15.sp,
                    color = if (f) Color.Gray else Color.Black,
                    textDecoration = if (f) TextDecoration.LineThrough else null,
                    modifier = Modifier.background(if (f) HIGHLIGHTS[found.indexOf(w) % HIGHLIGHTS.size] else Color(0xFFE9E7F5), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
            }
        }
    }
}

@Composable
private fun Grid(p: Puzzle, found: List<Placed>, onFound: (Placed) -> Unit) {
    val n = p.size
    val tm = rememberTextMeasurer()
    var start by remember(p) { mutableStateOf<Cell?>(null) }
    var end by remember(p) { mutableStateOf<Cell?>(null) }
    var cellPx by remember { mutableFloatStateOf(1f) }
    fun cellAt(o: Offset) = Cell((o.y / cellPx).toInt().coerceIn(0, n - 1), (o.x / cellPx).toInt().coerceIn(0, n - 1))
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(1f).background(Color.White, RoundedCornerShape(16.dp))
            .pointerInput(p) {
                detectDragGestures(
                    onDragStart = { start = cellAt(it); end = start },
                    onDrag = { ch, _ -> end = cellAt(ch.position) },
                    onDragEnd = {
                        val s = start; val e = end
                        if (s != null && e != null) Generator.line(s, e)?.let { Generator.match(it, p.words) }?.let(onFound)
                        start = null; end = null
                    },
                    onDragCancel = { start = null; end = null })
            }
    ) {
        cellPx = size.width / n
        val cs = cellPx
        fun center(c: Cell) = Offset(c.c * cs + cs / 2, c.r * cs + cs / 2)
        found.forEachIndexed { i, w ->
            drawLine(HIGHLIGHTS[i % HIGHLIGHTS.size], center(w.cells.first()), center(w.cells.last()), cs * 0.8f, StrokeCap.Round)
        }
        val s = start; val e = end
        if (s != null && e != null) {
            val ln = Generator.line(s, e)
            drawLine(if (ln != null) Color(0x665B4BDB) else Color(0x33999999), center(s), center(e), cs * 0.8f, StrokeCap.Round)
        }
        val style = TextStyle(fontSize = (cs / density * 0.5f).sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF222233))
        for (r in 0 until n) for (c in 0 until n) {
            val l = tm.measure(p.grid[r][c].toString(), style)
            drawText(l, topLeft = center(Cell(r, c)) - Offset(l.size.width / 2f, l.size.height / 2f))
        }
    }
}
