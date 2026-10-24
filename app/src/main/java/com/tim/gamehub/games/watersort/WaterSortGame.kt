package com.tim.gamehub.games.watersort

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

/** Okabe-Ito based palette (colorblind-friendlier) + extras; each color also gets a symbol. */
val WATER_COLORS = listOf(
    0xFFE69F00, 0xFF56B4E9, 0xFF009E73, 0xFFF0E442, 0xFF0072B2, 0xFFD55E00,
    0xFFCC79A7, 0xFF6A3D9A, 0xFF8C564B, 0xFFB2DF8A, 0xFF1B1B3A, 0xFFA0A0A8,
).map { Color(it) }
val WATER_SYMBOLS = listOf("●", "▲", "■", "◆", "★", "✚", "≡", "▼", "◐", "✖", "☾", "#")

class WaterSortProgress(ctx: Context) {
    private val p = ctx.getSharedPreferences("watersort", Context.MODE_PRIVATE)
    var current: Int get() = p.getInt("current", 1); set(v) { p.edit().putInt("current", v).apply() }
    var unlocked: Int get() = p.getInt("unlocked", 1); set(v) { p.edit().putInt("unlocked", v).apply() }
}

@Composable
fun WaterSortGame(onExit: () -> Unit, startLevel: Int? = null) {
    val ctx = LocalContext.current
    val prog = remember { WaterSortProgress(ctx) }
    var level by remember { mutableIntStateOf(startLevel ?: prog.current) }
    var attempt by remember { mutableIntStateOf(0) }
    val initial = remember(level) { WaterSort.generate(level) }
    var tubes by remember(level, attempt) { mutableStateOf(initial) }
    val history = remember(level, attempt) { mutableStateListOf<Tubes>() }
    var selected by remember(level, attempt) { mutableStateOf<Int?>(null) }
    var extraUsed by remember(level, attempt) { mutableStateOf(false) }
    var moves by remember(level, attempt) { mutableIntStateOf(0) }
    var showSelect by remember { mutableStateOf(false) }
    val won = WaterSort.isWon(tubes)
    LaunchedEffect(won) { if (won) { prog.unlocked = maxOf(prog.unlocked, level + 1) } }
    fun goto(l: Int) { level = l; prog.current = l; attempt++ }

    fun tap(i: Int) {
        if (won) return
        val s = selected
        when {
            s == null -> if (tubes[i].isNotEmpty()) selected = i
            s == i -> selected = null
            else -> {
                val next = WaterSort.pour(tubes, s, i)
                if (next != null) { history += tubes; tubes = next; moves++; selected = null }
                else selected = if (tubes[i].isNotEmpty()) i else null
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("‹ Hub", fontSize = 16.sp) }
            Spacer(Modifier.weight(1f))
            Text("Level $level", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showSelect = true })
            Spacer(Modifier.weight(1f))
            Text("Moves $moves", fontSize = 15.sp, color = Color.Gray)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            OutlinedButton(onClick = { history.removeLastOrNull()?.let { tubes = it; moves = maxOf(0, moves - 1); selected = null } }, enabled = history.isNotEmpty() && !won,
                contentPadding = PaddingValues(horizontal = 12.dp)) { Text("↶ Undo") }
            OutlinedButton(onClick = { attempt++ }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("↻ Restart") }
            OutlinedButton(onClick = { history += tubes; tubes = tubes + listOf(emptyList()); extraUsed = true },
                enabled = !extraUsed && !won, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("+ Tube") }
            OutlinedButton(onClick = { showSelect = true }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Levels") }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Board(tubes, selected, ::tap)
            if (won) Column(
                Modifier.background(Color.White, RoundedCornerShape(20.dp)).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉 Level $level complete!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Moves: $moves", fontSize = 18.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { goto(level + 1) }) { Text("Next level") }
                    OutlinedButton(onClick = onExit) { Text("Hub") }
                }
            }
        }
        Text("Tap a tube to lift it, then tap another to pour.", color = Color.Gray, fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }

    if (showSelect) AlertDialog(
        onDismissRequest = { showSelect = false },
        confirmButton = { TextButton(onClick = { showSelect = false }) { Text("Close") } },
        title = { Text("Select level") },
        text = {
            LazyVerticalGrid(GridCells.Fixed(5), Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((1..maxOf(prog.unlocked, level)).toList()) { l ->
                    Box(Modifier.aspectRatio(1f).background(if (l == level) MaterialTheme.colorScheme.primary else Color(0xFFE9E7F5), RoundedCornerShape(10.dp))
                        .clickable { goto(l); showSelect = false }, contentAlignment = Alignment.Center) {
                        Text("$l", color = if (l == level) Color.White else Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        })
}

@Composable
private fun Board(tubes: Tubes, selected: Int?, onTap: (Int) -> Unit) {
    val rows = if (tubes.size > 7) tubes.indices.chunked((tubes.size + 1) / 2) else listOf(tubes.indices.toList())
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val perRow = rows.maxOf { it.size }
        val w = min(maxWidth / perRow - 10.dp, 64.dp)
        val h = min(w * 3.4f, (maxHeight / rows.size) - 40.dp)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { i -> Tube(tubes[i], selected == i, w, h) { onTap(i) } }
                }
            }
        }
    }
}

@Composable
private fun Tube(layers: List<Int>, lifted: Boolean, w: Dp, h: Dp, onTap: () -> Unit) {
    val lift by animateDpAsState(if (lifted) (-22).dp else 0.dp, tween(180), label = "lift")
    val tm = rememberTextMeasurer()
    val colors = (0 until WaterSort.CAP).map { i ->
        animateColorAsState(layers.getOrNull(i)?.let { WATER_COLORS[it] } ?: Color.Transparent, tween(320), label = "c$i").value
    }
    val solved = layers.size == WaterSort.CAP && WaterSort.isSolvedTube(layers)
    Canvas(Modifier.padding(top = 24.dp).offset(y = lift).size(w, h)
        .clickable(remember { MutableInteractionSource() }, null, onClick = onTap)) {
        val r = size.width / 2
        val body = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(left = 0f, top = 0f, right = size.width, bottom = size.height,
                topLeftCornerRadius = CornerRadius(8f), topRightCornerRadius = CornerRadius(8f), bottomLeftCornerRadius = CornerRadius(r), bottomRightCornerRadius = CornerRadius(r)))
        }
        drawPath(body, Color.White.copy(alpha = 0.7f))
        val top = size.height * 0.1f
        val slot = (size.height - top) / WaterSort.CAP
        clipPath(body) {
            colors.forEachIndexed { i, c ->
                if (c.alpha > 0f) {
                    val y = size.height - slot * (i + 1)
                    drawRect(c, Offset(0f, y), Size(size.width, slot + 1))
                    layers.getOrNull(i)?.let { id ->
                        val l = tm.measure(WATER_SYMBOLS[id], TextStyle(fontSize = (size.width / density * 0.32f).sp,
                            color = if (id == 3 || id == 9 || id == 11) Color(0x99000000) else Color(0xCCFFFFFF)))
                        drawText(l, topLeft = Offset((size.width - l.size.width) / 2, y + (slot - l.size.height) / 2))
                    }
                }
            }
        }
        drawPath(body, if (lifted) Color(0xFF5B4BDB) else if (solved) Color(0xFF2E9E5B) else Color(0xFF9A98B0), style = Stroke(if (lifted || solved) 6f else 4f))
    }
}
