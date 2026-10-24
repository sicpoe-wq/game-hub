package com.tim.gamehub.games.watersort

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.*
import kotlin.random.Random

/** Okabe-Ito based palette (colorblind-friendlier) + extras; each color also gets a symbol. */
val WATER_COLORS = listOf(
    0xFFE69F00, 0xFF56B4E9, 0xFF009E73, 0xFFF0E442, 0xFF0072B2, 0xFFD55E00,
    0xFFCC79A7, 0xFF8E5BD0, 0xFF8C564B, 0xFFB2DF8A, 0xFF2B2B52, 0xFFA0A0A8,
).map { Color(it) }
val WATER_SYMBOLS = listOf("●", "▲", "■", "◆", "★", "✚", "≡", "▼", "◐", "✖", "☾", "#")
private val DARK_SYMBOL = setOf(3, 9, 11, 1)

private val BG = Brush.verticalGradient(listOf(Color(0xFF1B1F3B), Color(0xFF2A2350), Color(0xFF3A2459)))
private val ACCENT = Color(0xFFFFD166)

class WaterSortProgress(ctx: Context) : ProgressStore {
    private val p = ctx.getSharedPreferences("watersort", Context.MODE_PRIVATE)
    override var current: Int get() = p.getInt("current", 1); set(v) { p.edit().putInt("current", v).apply() }
    override var unlocked: Int get() = p.getInt("unlocked", 1); set(v) { p.edit().putInt("unlocked", v).apply() }
}

@Composable
fun WaterSortGame(onExit: () -> Unit, startLevel: Int? = null, controller: WaterSortController? = null) {
    val ctx = LocalContext.current
    // ONE controller for the whole session; level changes reset state inside it.
    val c = controller ?: remember { WaterSortProgress(ctx).let { WaterSortController(it, startLevel ?: it.current) } }
    var showSelect by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(BG)) {
        Backdrop()
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(onClick = onExit, tag = "btn_hub") { Text("‹  Hub", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showSelect = true }) {
                    Text("WATER SORT", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, letterSpacing = 2.sp)
                    Text("Level ${c.level} ▾", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Pill(tag = "moves_pill") { Text("Moves ${c.moves}", color = Color.White, fontSize = 14.sp, modifier = Modifier.testTag("moves")) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                ActionButton("↶", "Undo", c.canUndo, "btn_undo") { c.undo() }
                ActionButton("↻", "Restart", true, "btn_restart") { c.restart() }
                ActionButton("+", if (c.extraUsed) "Tube 0" else "Tube 1", !c.extraUsed && !c.won, "btn_tube") { c.addTube() }
                ActionButton("☰", "Levels", true, "btn_levels") { showSelect = true }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { Board(c) }
            Text("Tap a tube to lift it, then tap another to pour.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        if (c.won) WinOverlay(c.level, c.moves, onNext = { c.nextLevel() }, onExit = onExit)
    }

    if (showSelect) AlertDialog(
        onDismissRequest = { showSelect = false },
        confirmButton = { TextButton(onClick = { showSelect = false }) { Text("Close") } },
        title = { Text("Select level") },
        text = {
            LazyVerticalGrid(GridCells.Fixed(5), Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((1..c.unlocked).toList()) { l ->
                    Box(Modifier.aspectRatio(1f).background(if (l == c.level) MaterialTheme.colorScheme.primary else Color(0xFFE9E7F5), RoundedCornerShape(10.dp))
                        .clickable { c.load(l); showSelect = false }.testTag("level_$l"), contentAlignment = Alignment.Center) {
                        Text("$l", color = if (l == c.level) Color.White else Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        })
}

@Composable
private fun Backdrop() {
    Canvas(Modifier.fillMaxSize()) {
        val rnd = Random(3)
        repeat(70) {
            drawCircle(Color.White.copy(alpha = 0.03f + rnd.nextFloat() * 0.05f), radius = 1f + rnd.nextFloat() * 2.5f,
                center = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height))
        }
        drawCircle(Brush.radialGradient(listOf(Color(0x335B8CFF), Color.Transparent), Offset(size.width * 0.2f, size.height * 0.15f), size.width * 0.7f))
        drawCircle(Brush.radialGradient(listOf(Color(0x33FF6FB5), Color.Transparent), Offset(size.width * 0.9f, size.height * 0.85f), size.width * 0.7f))
    }
}

@Composable
private fun Pill(onClick: (() -> Unit)? = null, tag: String, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.alpha(if (enabled) 1f else 0.38f).background(Color.White.copy(alpha = 0.12f), CircleShape)
        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
        .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
        .testTag(tag).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
private fun ActionButton(icon: String, label: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    Column(Modifier.alpha(if (enabled) 1f else 0.35f).clickable(enabled = enabled, onClick = onClick).testTag(tag).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(46.dp).background(Brush.verticalGradient(listOf(Color(0xFF6C63FF), Color(0xFF4B3FD1))), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape), contentAlignment = Alignment.Center) {
            Text(icon, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun Board(c: WaterSortController) {
    val tubes = c.tubes
    val rows = if (tubes.size > 7) tubes.indices.chunked((tubes.size + 1) / 2) else listOf(tubes.indices.toList())
    val bounds = remember { mutableStateMapOf<Int, Rect>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val anim = remember { Animatable(0f) }
    var active by remember { mutableStateOf<PourEvent?>(null) }
    val pour = c.lastPour
    LaunchedEffect(pour?.id) {
        if (pour == null) { active = null; anim.snapTo(0f); return@LaunchedEffect }
        active = pour; anim.snapTo(0f)
        anim.animateTo(1f, tween(700, easing = LinearEasing))
        active = null
    }
    val t = anim.value
    val a = active
    // 0..0.28 move+tilt in, 0.28..0.72 pour stream, 0.72..1 return
    val tilt = when { a == null -> 0f; t < 0.28f -> FastOutSlowInEasing.transform(t / 0.28f); t < 0.72f -> 1f; else -> 1f - FastOutSlowInEasing.transform((t - 0.72f) / 0.28f) }

    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        val perRow = rows.maxOf { it.size }
        val w = min(maxWidth / perRow - 10.dp, 60.dp)
        val h = min(w * 3.3f, (maxHeight / rows.size) - 44.dp)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth().zIndex(if (a != null && a.from in row) 1f else 0f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { i ->
                        val src = bounds[i]; val dst = a?.let { bounds[it.to] }
                        var dx = 0f; var dy = 0f; var rot = 0f
                        if (a != null && a.from == i && src != null && dst != null) {
                            val side = if (dst.center.x >= src.center.x) 1f else -1f
                            dx = (dst.center.x - side * dst.width * 0.55f - src.center.x) * tilt
                            dy = (dst.top - dst.height * 0.22f - src.top) * tilt
                            rot = side * 72f * tilt
                        }
                        Tube(tubes[i], c.selected == i, w, h,
                            Modifier.testTag("tube$i").onGloballyPositioned { co ->
                                val p = co.positionInRoot() - origin
                                bounds[i] = Rect(p, Size(co.size.width.toFloat(), co.size.height.toFloat()))
                            }.zIndex(if (a?.from == i) 2f else 0f)
                                .graphicsLayer { translationX = dx; translationY = dy; rotationZ = rot; transformOrigin = TransformOrigin(0.5f, 0.12f) },
                            onTap = { c.tap(i) })
                    }
                }
            }
        }
        // Liquid stream overlay (purely visual, never intercepts touches)
        if (a != null && t in 0.26f..0.74f) {
            val src = bounds[a.from]; val dst = bounds[a.to]
            if (src != null && dst != null) Canvas(Modifier.fillMaxSize()) {
                val side = if (dst.center.x >= src.center.x) 1f else -1f
                val mouth = Offset(dst.center.x - side * dst.width * 0.55f, dst.top - dst.height * 0.22f + src.height * 0.12f)
                val grow = ((t - 0.26f) / 0.1f).coerceIn(0f, 1f); val shrink = ((0.74f - t) / 0.1f).coerceIn(0f, 1f)
                val end = Offset(dst.center.x, dst.top + dst.height * 0.55f)
                val start = lerp(mouth, end, 1f - shrink)
                val stop = lerp(mouth, end, grow)
                val col = WATER_COLORS[a.color]
                val path = Path().apply { moveTo(start.x, start.y); quadraticTo(lerp(start.x, stop.x, 0.8f), start.y, stop.x, stop.y) }
                drawPath(path, col, style = Stroke(dst.width * 0.13f, cap = StrokeCap.Round))
                drawPath(path, Color.White.copy(alpha = 0.35f), style = Stroke(dst.width * 0.04f, cap = StrokeCap.Round))
            }
        }
    }
}

private fun lerp(a: Offset, b: Offset, f: Float) = Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f)
private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f
private fun lighten(c: Color, f: Float) = androidx.compose.ui.graphics.lerp(c, Color.White, f)
private fun darken(c: Color, f: Float) = androidx.compose.ui.graphics.lerp(c, Color.Black, f)

@Composable
private fun Tube(layers: List<Int>, lifted: Boolean, w: Dp, h: Dp, modifier: Modifier, onTap: () -> Unit) {
    val lift by animateDpAsState(if (lifted) (-26).dp else 0.dp, spring(dampingRatio = 0.55f, stiffness = 500f), label = "lift")
    val glow by animateFloatAsState(if (lifted) 1f else 0f, tween(200), label = "glow")
    val tm = rememberTextMeasurer()
    // per-slot animated fill level + remembered color so draining layers keep their color
    val fills = (0 until WaterSort.CAP).map { i ->
        val present = i < layers.size
        animateFloatAsState(if (present) 1f else 0f, tween(300, delayMillis = if (present) 230 else 140), label = "f$i").value
    }
    val shown = remember { IntArray(WaterSort.CAP) { -1 } }
    layers.forEachIndexed { i, id -> shown[i] = id }
    val solved = layers.size == WaterSort.CAP && WaterSort.isSolvedTube(layers)
    val cap by animateFloatAsState(if (solved) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 300f), label = "cap")
    val sparkle = remember { Animatable(0f) }
    LaunchedEffect(solved) { if (solved) { sparkle.snapTo(0f); sparkle.animateTo(1f, tween(900)) } else sparkle.snapTo(0f) }

    Canvas(modifier.padding(top = 30.dp).offset(y = lift).size(w, h)
        .clickable(remember { MutableInteractionSource() }, null, onClick = onTap)) {
        val pad = size.width * 0.1f
        val left = pad; val right = size.width - pad; val bw = right - left
        val top = size.height * 0.05f; val bottom = size.height * 0.93f
        val r = bw / 2
        val body = Path().apply {
            addRoundRect(RoundRect(left = left, top = top, right = right, bottom = bottom,
                topLeftCornerRadius = CornerRadius(4f), topRightCornerRadius = CornerRadius(4f),
                bottomLeftCornerRadius = CornerRadius(r), bottomRightCornerRadius = CornerRadius(r)))
        }
        // shadow
        drawOval(Color.Black.copy(alpha = 0.28f), Offset(left - bw * 0.05f, bottom - size.height * 0.015f), Size(bw * 1.1f, size.height * 0.05f))
        // selection glow
        if (glow > 0f) {
            drawPath(body, ACCENT.copy(alpha = 0.18f * glow), style = Stroke(bw * 0.38f))
            drawPath(body, ACCENT.copy(alpha = 0.35f * glow), style = Stroke(bw * 0.16f))
        }
        // glass back
        drawPath(body, Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.12f)), left, right))
        val liquidTop = top + (bottom - top) * 0.1f
        val slot = (bottom - liquidTop) / WaterSort.CAP
        clipPath(body) {
            var surface = -1f; var surfaceColor = Color.Transparent
            for (i in 0 until WaterSort.CAP) {
                val f = fills[i]; val id = shown[i]
                if (f <= 0.001f || id < 0) continue
                val col = WATER_COLORS[id]
                val yb = bottom - slot * i
                val yt = yb - slot * f
                drawRect(Brush.verticalGradient(listOf(lighten(col, 0.18f), col, darken(col, 0.18f)), yt, yb + 1), Offset(left, yt), Size(bw, yb - yt + 1))
                if (f > 0.6f) {
                    val l = tm.measure(WATER_SYMBOLS[id], TextStyle(fontSize = (bw / density * 0.30f).sp,
                        color = if (id in DARK_SYMBOL) Color(0x66000000) else Color(0x88FFFFFF)))
                    drawText(l, topLeft = Offset(left + (bw - l.size.width) / 2, yb - slot / 2 - l.size.height / 2), alpha = ((f - 0.6f) / 0.4f))
                }
                surface = yt; surfaceColor = col
            }
            if (surface > 0f) drawOval(lighten(surfaceColor, 0.4f).copy(alpha = 0.9f), Offset(left, surface - slot * 0.07f), Size(bw, slot * 0.14f))
            // glass highlight
            drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.05f)), top, bottom),
                Offset(left + bw * 0.14f, top + (bottom - top) * 0.06f), Size(bw * 0.13f, (bottom - top) * 0.78f), CornerRadius(bw))
        }
        drawPath(body, Color.White.copy(alpha = if (solved) 0.85f else 0.55f), style = Stroke(1.6f * density))
        // lip
        drawRoundRect(Color.White.copy(alpha = 0.6f), Offset(left - bw * 0.08f, top - 2.5f * density), Size(bw * 1.16f, 4.5f * density), CornerRadius(3f * density))
        // cork cap on completed tubes
        if (cap > 0.01f) {
            val cw = bw * 0.9f * cap; val ch = size.height * 0.07f * cap
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFD9A066), Color(0xFF9C6B3C)), top - ch, top + 2), Offset(size.width / 2 - cw / 2, top - ch * 0.9f), Size(cw, ch), CornerRadius(ch * 0.3f))
        }
        val s = sparkle.value
        if (s in 0.01f..0.99f) {
            val center = Offset(size.width / 2, top)
            for (k in 0 until 8) {
                val ang = k * PI.toFloat() / 4 + 0.3f
                val dist = bw * (0.3f + 1.1f * s)
                drawStar(Offset(center.x + cos(ang) * dist, center.y + sin(ang) * dist * 0.8f - bw * 0.3f * s), bw * 0.09f * (1 - s) + 1f, ACCENT.copy(alpha = 1 - s))
            }
        }
    }
}

private fun DrawScope.drawStar(c: Offset, r: Float, color: Color) {
    val p = Path()
    for (k in 0 until 8) {
        val rad = if (k % 2 == 0) r else r * 0.35f
        val ang = k * PI.toFloat() / 4
        val pt = Offset(c.x + cos(ang) * rad, c.y + sin(ang) * rad)
        if (k == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
    }
    p.close(); drawPath(p, color)
}

@Composable
private fun WinOverlay(level: Int, moves: Int, onNext: () -> Unit, onExit: () -> Unit) {
    // Fades in after the last pour animation. Only composed while the level is won, so it can't block the next level.
    val appear by animateFloatAsState(1f, tween(350, delayMillis = 600), label = "win")
    val inf = rememberInfiniteTransition(label = "conf")
    val time by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "t")
    val confetti = remember { List(60) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextInt(WATER_COLORS.size)) } }
    Box(Modifier.fillMaxSize().alpha(appear).background(Color(0xAA0D0F24))
        .clickable(remember { MutableInteractionSource() }, null) {}.testTag("win_overlay"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            confetti.forEachIndexed { k, (x, y0, ci) ->
                val y = ((y0 + time * (0.6f + (k % 5) * 0.12f)) % 1f) * size.height
                val xx = x * size.width + sin((time * 6f + k) * 2f) * 12f
                rotate(time * 720f + k * 40f, Offset(xx, y)) { drawRect(WATER_COLORS[ci], Offset(xx - 5, y - 3), Size(10f * density / 2.6f + 6, 6f * density / 2.6f + 3)) }
            }
        }
        Column(Modifier.padding(28.dp).background(Brush.verticalGradient(listOf(Color(0xFF3B2F7A), Color(0xFF241D52))), RoundedCornerShape(28.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp)).padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    rotate(time * 360f) {
                        for (k in 0 until 12) rotate(k * 30f) {
                            drawPath(Path().apply { moveTo(center.x, center.y); lineTo(center.x - 9f, 0f); lineTo(center.x + 9f, 0f); close() }, ACCENT.copy(alpha = 0.25f))
                        }
                    }
                    drawStar(center, size.minDimension * 0.33f, ACCENT)
                }
            }
            Text("Level $level complete!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Solved in $moves moves", color = Color.White.copy(alpha = 0.75f), fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(18.dp))
            Button(onClick = onNext, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = ACCENT, contentColor = Color(0xFF241D52)),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_next")) { Text("Next level  ›", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            TextButton(onClick = onExit) { Text("Back to hub", color = Color.White.copy(alpha = 0.8f)) }
        }
    }
}
