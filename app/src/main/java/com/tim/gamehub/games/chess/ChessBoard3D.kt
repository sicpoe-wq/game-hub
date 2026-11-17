package com.tim.gamehub.games.chess

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

data class Cam(
    val yaw: Float = 0.40f,
    val pitch: Float = 0.88f,
    val dist: Float = 10.0f,
) {
    fun clamped() = copy(
        pitch = pitch.coerceIn(0.35f, 1.35f),
        dist = dist.coerceIn(7.5f, 18f),
    )
}

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun len() = kotlin.math.sqrt(x * x + y * y + z * z)
    fun norm(): Vec3 { val l = len(); return if (l < 1e-6f) this else this * (1f / l) }
}

class Projector(val cam: Cam, val w: Float, val h: Float) {
    private val target = Vec3(4f, 0f, 4f)
    private val eye: Vec3
    private val right: Vec3
    private val up: Vec3
    private val fwd: Vec3
    private val scale: Float
    init {
        val c = cam.clamped()
        eye = Vec3(
            target.x + sin(c.yaw) * cos(c.pitch) * c.dist,
            target.y + sin(c.pitch) * c.dist,
            target.z + cos(c.yaw) * cos(c.pitch) * c.dist,
        )
        fwd = (target - eye).norm()
        right = fwd.cross(Vec3(0f, 1f, 0f)).norm()
        up = right.cross(fwd).norm()
        scale = (min(w, h) * 0.55f) / tan(0.48f)
    }
    fun project(v: Vec3): Pair<Offset, Float>? {
        val rel = v - eye
        val z = rel.dot(fwd)
        if (z < 0.35f) return null
        val x = rel.dot(right)
        val y = rel.dot(up)
        return Offset(w * 0.5f + x * scale / z, h * 0.52f - y * scale / z) to z
    }
    fun depth(v: Vec3) = (v - eye).dot(fwd)
}

fun sqWorld(sq: Int, flipped: Boolean, y: Float = 0f): Vec3 {
    val v = viewSq(sq, flipped)
    return Vec3((v and 7) + 0.5f, y, (v shr 3) + 0.5f)
}

fun worldToSq(p: Vec3, flipped: Boolean): Int? {
    val f = p.x.toInt()
    val r = p.z.toInt()
    if (f !in 0..7 || r !in 0..7) return null
    val v = r * 8 + f
    return if (flipped) 63 - v else v
}

/** Ray from camera through a screen point onto the y=0 board plane. */
fun hitBoard(proj: Projector, cam: Cam, screen: Offset, w: Float, h: Float, flipped: Boolean): Int? {
    val c = cam.clamped()
    val target = Vec3(4f, 0f, 4f)
    val eye = Vec3(
        target.x + sin(c.yaw) * cos(c.pitch) * c.dist,
        target.y + sin(c.pitch) * c.dist,
        target.z + cos(c.yaw) * cos(c.pitch) * c.dist,
    )
    val fwd = (target - eye).norm()
    val right = fwd.cross(Vec3(0f, 1f, 0f)).norm()
    val up = right.cross(fwd).norm()
    val scale = (min(w, h) * 0.55f) / tan(0.48f)
    val nx = (screen.x - w * 0.5f) / scale
    val ny = (h * 0.52f - screen.y) / scale
    // dir in camera space at z=1
    val dir = (fwd + right * nx + up * ny).norm()
    if (abs(dir.y) < 1e-5f) return null
    val t = -eye.y / dir.y
    if (t < 0) return null
    val hit = eye + dir * t
    return worldToSq(hit, flipped)
}

fun DrawScope.drawBoard3D(
    theme: BoardTheme,
    cam: Cam,
    coords: Boolean,
    selected: Int?,
    legal: Set<Int>,
    captures: Set<Int>,
    lastFrom: Int?,
    lastTo: Int?,
    checkSq: Int?,
    flipped: Boolean,
    pos: Pos,
    set: PieceSet,
    figs: Array<String?>,
    portraits: Map<String, ImageBitmap>,
    political: PoliticalStyle?,
    hide: Set<Int>,
    animFrom: Int?,
    animTo: Int?,
    animPc: Pc?,
    animFig: String?,
    animP: Float,
    dragSq: Int?,
    dragPos: Offset?,
    measurer: TextMeasurer?,
) {
    val proj = Projector(cam, size.width, size.height)
    val thick = 0.28f
    // rim / sides of the board box, back faces first
    val corners = listOf(Vec3(0f, 0f, 0f), Vec3(8f, 0f, 0f), Vec3(8f, 0f, 8f), Vec3(0f, 0f, 8f))
    val low = corners.map { it.copy(y = -thick) }
    data class Face(val pts: List<Vec3>, val col: Color, val depth: Float)
    val faces = ArrayList<Face>()
    fun addFace(pts: List<Vec3>, col: Color) {
        val mid = pts.fold(Vec3(0f, 0f, 0f)) { a, b -> a + b } * (1f / pts.size)
        faces += Face(pts, col, proj.depth(mid))
    }
    addFace(listOf(low[0], low[1], corners[1], corners[0]), theme.frame)
    addFace(listOf(low[1], low[2], corners[2], corners[1]), theme.frame)
    addFace(listOf(low[2], low[3], corners[3], corners[2]), theme.frame)
    addFace(listOf(low[3], low[0], corners[0], corners[3]), theme.frame)
    addFace(low, theme.frame.copy(alpha = 1f).let { Color(red = it.red * 0.55f, green = it.green * 0.55f, blue = it.blue * 0.55f) })
    // squares as faces
    data class SqFace(val sq: Int, val pts: List<Vec3>, val col: Color, val depth: Float)
    val squares = ArrayList<SqFace>(64)
    for (sq in 0..63) {
        val v = viewSq(sq, flipped)
        val f = (v and 7).toFloat()
        val r = (v shr 3).toFloat()
        val pts = listOf(Vec3(f, 0f, r), Vec3(f + 1, 0f, r), Vec3(f + 1, 0f, r + 1), Vec3(f, 0f, r + 1))
        val light = ((file(sq) + rank(sq)) and 1) == 1
        var col = if (light) theme.light else theme.dark
        if (sq == lastFrom || sq == lastTo) col = lerpColor(col, theme.last, 0.55f)
        if (sq == checkSq) col = lerpColor(col, theme.check, 0.55f)
        if (sq == selected) col = lerpColor(col, theme.select, 0.55f)
        val mid = Vec3(f + 0.5f, 0f, r + 0.5f)
        squares += SqFace(sq, pts, col, proj.depth(mid))
    }
    for (face in faces.sortedByDescending { it.depth }) {
        val p = pathOf(proj, face.pts) ?: continue
        drawPath(p, face.col)
    }
    for (s in squares.sortedByDescending { it.depth }) {
        val p = pathOf(proj, s.pts) ?: continue
        drawPath(p, s.col)
        drawPath(p, Color.Black.copy(alpha = 0.12f), style = Stroke(1f))
        if (s.sq in legal) {
            val c = sqWorld(s.sq, flipped, 0.02f)
            val pr = proj.project(c) ?: continue
            val rad = 28f / pr.second
            if (s.sq in captures) drawCircle(theme.dot.copy(alpha = 0.85f), rad * 2.2f, pr.first, style = Stroke(rad * 0.45f))
            else drawCircle(theme.dot.copy(alpha = 0.8f), rad * 0.7f, pr.first)
        }
    }
    if (coords && measurer != null) {
        val style = TextStyle(color = theme.coord, fontSize = 10.sp)
        for (f in 0..7) {
            val name = "${'a' + if (flipped) 7 - f else f}"
            val pr = proj.project(Vec3(f + 0.5f, 0f, -0.28f)) ?: continue
            val layout = measurer.measure(name, style)
            drawText(layout, topLeft = Offset(pr.first.x - layout.size.width / 2f, pr.first.y))
        }
        for (rk in 0..7) {
            val name = "${if (flipped) 8 - rk else rk + 1}"
            val pr = proj.project(Vec3(-0.28f, 0f, rk + 0.5f)) ?: continue
            val layout = measurer.measure(name, style)
            drawText(layout, topLeft = Offset(pr.first.x - layout.size.width, pr.first.y - layout.size.height / 2f))
        }
    }
    data class Spr(val depth: Float, val draw: DrawScope.() -> Unit)
    val sprites = ArrayList<Spr>(40)
    fun addPiece(sq: Int, pc: Pc, figId: String?, world: Vec3) {
        sprites += Spr(proj.depth(world)) {
            drawPieceBillboard(proj, world, pc, set, figId, portraits, political)
        }
    }
    for (sq in 0..63) {
        if (sq in hide) continue
        val pc = pos.at(sq) ?: continue
        addPiece(sq, pc, figs.getOrNull(sq), sqWorld(sq, flipped, 0f))
    }
    if (animP < 1f && animFrom != null && animTo != null && animPc != null) {
        val a = sqWorld(animFrom, flipped)
        val b = sqWorld(animTo, flipped)
        val hop = sin(animP * PI.toFloat()) * 1.15f
        val wld = Vec3(a.x + (b.x - a.x) * animP, hop, a.z + (b.z - a.z) * animP)
        addPiece(animTo, animPc, animFig, wld)
    }
    for (s in sprites.sortedByDescending { it.depth }) s.draw(this)
    val ds = dragSq; val dp = dragPos
    if (ds != null && dp != null) {
        val pc = pos.at(ds) ?: return
        val figId = figs.getOrNull(ds)
        // draw at finger as a 2D sprite
        val side = size.width / 9f
        val r = Rect(dp.x - side * 0.4f, dp.y - side * 1.05f, dp.x + side * 0.4f, dp.y)
        drawSpriteOrVector(r, pc, set, figId, portraits, political)
    }
}

private fun pathOf(proj: Projector, pts: List<Vec3>): Path? {
    val p = Path()
    pts.forEachIndexed { i, v ->
        val pr = proj.project(v) ?: return null
        if (i == 0) p.moveTo(pr.first.x, pr.first.y) else p.lineTo(pr.first.x, pr.first.y)
    }
    p.close()
    return p
}

private fun lerpColor(a: Color, overlay: Color, t: Float): Color {
    val alpha = overlay.alpha * t
    return Color(
        a.red * (1 - alpha) + overlay.red * alpha,
        a.green * (1 - alpha) + overlay.green * alpha,
        a.blue * (1 - alpha) + overlay.blue * alpha,
        1f,
    )
}

private fun DrawScope.drawPieceBillboard(
    proj: Projector,
    world: Vec3,
    pc: Pc,
    set: PieceSet,
    figId: String?,
    portraits: Map<String, ImageBitmap>,
    political: PoliticalStyle?,
) {
    val base = proj.project(world) ?: return
    val head = proj.project(world.copy(y = pieceH(pc.kind))) ?: return
    val midDepth = base.second
    val top = head.first.y
    val bot = base.first.y
    val cx = base.first.x
    val hPx = (bot - top).coerceAtLeast(4f)
    val bmp = if (political != null && figId != null) portraits[figId] else null
    val width = if (bmp != null) {
        hPx * (bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f))
    } else {
        hPx * 0.62f * pieceW(pc.kind)
    }
    // shadow
    val shadow = (hPx * 0.28f).coerceAtLeast(6f)
    drawOval(Color.Black.copy(alpha = 0.28f), Offset(cx - shadow, bot - shadow * 0.25f), Size(shadow * 2f, shadow * 0.55f))
    val rect = Rect(cx - width / 2f, top, cx + width / 2f, bot)
    drawSpriteOrVector(rect, pc, set, figId, portraits, political)
}

private fun pieceH(k: Kind) = when (k) {
    Kind.P -> 1.15f; Kind.N -> 1.45f; Kind.B -> 1.55f; Kind.R -> 1.40f; Kind.Q -> 1.75f; Kind.K -> 1.90f
}
private fun pieceW(k: Kind) = when (k) {
    Kind.P -> 0.85f; Kind.N -> 1.0f; Kind.B -> 0.95f; Kind.R -> 0.95f; Kind.Q -> 1.05f; Kind.K -> 1.08f
}

private fun DrawScope.drawSpriteOrVector(
    r: Rect,
    pc: Pc,
    set: PieceSet,
    figId: String?,
    portraits: Map<String, ImageBitmap>,
    political: PoliticalStyle?,
) {
    val bmp = if (political != null && figId != null) portraits[figId] else null
    if (bmp != null) {
        val iw = bmp.width.toFloat(); val ih = bmp.height.toFloat()
        val scale = min(r.width / iw, r.height / ih)
        val dw = iw * scale; val dh = ih * scale
        val left = r.left + (r.width - dw) / 2f
        val top = r.bottom - dh
        drawImage(bmp, dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(dw.toInt().coerceAtLeast(1), dh.toInt().coerceAtLeast(1)))
    } else {
        drawPiece(pc, set, r)
    }
}

@Composable
fun ChessBoard3DView(
    c: ChessController,
    theme: BoardTheme,
    set: PieceSet,
    instant: Boolean,
    portraits: Map<String, ImageBitmap>,
    modifier: Modifier = Modifier,
) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    var dragSq by remember { mutableStateOf<Int?>(null) }
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    var animP by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(c.animTick) {
        if (instant || c.animFrom == null) { animP = 1f; return@LaunchedEffect }
        animP = 0f
        androidx.compose.animation.core.animate(0f, 1f, animationSpec = androidx.compose.animation.core.tween(220)) { v, _ -> animP = v }
        c.clearAnim()
        animP = 1f
    }
    val legalTos = c.legalFrom.map { it.to }.toSet()
    val caps = c.legalFrom.filter { it.captured != null }.map { it.to }.toSet()
    val political = politicalStyleOf(c.prefs.pieces)
    var viewW by remember { mutableFloatStateOf(1f) }
    var viewH by remember { mutableFloatStateOf(1f) }
    Canvas(modifier.pointerInput(c.flipped, c.status, c.thinking, c.pos, c.selected, c.cam) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val cam0 = c.cam
            val hit = hitBoard(Projector(cam0, size.width.toFloat(), size.height.toFloat()), cam0, down.position, size.width.toFloat(), size.height.toFloat(), c.flipped)
            val canGrab = hit != null && !c.status.over && !c.thinking && c.pos.at(hit)?.side == c.pos.side &&
                (c.mode != ChessMode.CPU || c.pos.side == c.playerSide)
            var draggedPiece = false
            var rotated = false
            var last = down.position
            val pointers = mutableMapOf(down.id to down.position)
            val start = down.position
            drag(down.id) { change ->
                val dt = change.position - last
                last = change.position
                val moved = (change.position - start).getDistance()
                if (canGrab && !rotated && (draggedPiece || moved > 18f)) {
                    draggedPiece = true
                    dragSq = hit
                    dragPos = change.position
                } else if (!draggedPiece && moved > 12f) {
                    rotated = true
                    val next = Cam(
                        yaw = c.cam.yaw - dt.x * 0.006f,
                        pitch = c.cam.pitch + dt.y * 0.005f,
                        dist = c.cam.dist,
                    ).clamped()
                    c.cam = next
                }
                change.consume()
            }
            // pinch zoom is handled in a second pointerInput below
            if (draggedPiece && dragSq != null) {
                val dest = hitBoard(Projector(c.cam, size.width.toFloat(), size.height.toFloat()), c.cam, dragPos ?: start, size.width.toFloat(), size.height.toFloat(), c.flipped)
                if (dest != null) c.drop(dragSq!!, dest) else c.selected = null
                dragSq = null; dragPos = null
            } else if (!rotated) {
                if (hit != null) c.tap(hit)
            }
        }
    }.pointerInput(c.cam) {
        // two-finger pinch zoom
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var lastSpan = -1f
            while (true) {
                val ev = awaitPointerEvent()
                val pressed = ev.changes.filter { it.pressed }
                if (pressed.size < 2) {
                    if (lastSpan > 0f) ev.changes.forEach { it.consume() }
                    if (pressed.isEmpty()) break else continue
                }
                val a = pressed[0].position
                val b = pressed[1].position
                val span = (a - b).getDistance()
                if (lastSpan > 0f && abs(span - lastSpan) > 1f) {
                    val factor = lastSpan / span
                    c.cam = c.cam.copy(dist = c.cam.dist * factor).clamped()
                }
                lastSpan = span
                ev.changes.forEach { it.consume() }
            }
        }
    }) {
        viewW = size.width; viewH = size.height
        val hide = mutableSetOf<Int>()
        if (dragSq != null) hide += dragSq!!
        if (animP < 1f && c.animTo != null) hide += c.animTo!!
        drawBoard3D(
            theme, c.cam, c.prefs.coords, c.selected, legalTos, caps,
            c.lastMove?.from, c.lastMove?.to, c.checkSq, c.flipped,
            c.pos, set, c.figs, portraits, political, hide,
            c.animFrom, c.animTo, c.animPc, c.animFig, animP,
            dragSq, dragPos, measurer,
        )
    }
}
