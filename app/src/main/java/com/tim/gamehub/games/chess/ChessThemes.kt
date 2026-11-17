package com.tim.gamehub.games.chess

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class BoardAnim { NONE, PULSE, PETALS, SPARKLE, LANTERNS, VEINS }

data class BoardTheme(
    val id: String,
    val name: String,
    val light: Color,
    val dark: Color,
    val frame: Color,
    val coord: Color,
    val last: Color,
    val select: Color,
    val check: Color,
    val dot: Color,
    val anim: BoardAnim = BoardAnim.NONE,
    val backdrop: List<Color> = listOf(Color(0xFF1A1A1E), Color(0xFF121214)),
)

enum class PieceStyle { STAUNTON, MINIMAL, NEON, FANTASY, CHIBI, POLITICAL }

data class PieceSet(
    val id: String,
    val name: String,
    val style: PieceStyle,
    val wFill: Color,
    val wStroke: Color,
    val bFill: Color,
    val bStroke: Color,
    val glow: Color? = null,
)

object ChessThemes {
    val boards = listOf(
        BoardTheme(
            "wood", "Classic Wood",
            light = Color(0xFFE6C99A), dark = Color(0xFFB0783A),
            frame = Color(0xFF5A3314), coord = Color(0xFF3A220E),
            last = Color(0x66F0D878), select = Color(0xAAE8D050),
            check = Color(0xAAE04040), dot = Color(0xCC2A1A0A),
        ),
        BoardTheme(
            "marble", "Marble",
            light = Color(0xFFECE8E1), dark = Color(0xFF7D8696),
            frame = Color(0xFF3E4450), coord = Color(0xFF2A2E36),
            last = Color(0x6678A0D0), select = Color(0xAA9CB8D8),
            check = Color(0xAAC83636), dot = Color(0xCC1E2430),
            anim = BoardAnim.VEINS,
            backdrop = listOf(Color(0xFF2A2C32), Color(0xFF1A1C20)),
        ),
        BoardTheme(
            "tournament", "Tournament Green",
            light = Color(0xFFEEEED2), dark = Color(0xFF769656),
            frame = Color(0xFF312E2B), coord = Color(0xFF1E1C1A),
            last = Color(0xAABACA44), select = Color(0xAAB8D060),
            check = Color(0xAAC84646), dot = Color(0xCC2E2A26),
            backdrop = listOf(Color(0xFF262522), Color(0xFF1B1A18)),
        ),
        BoardTheme(
            "neon", "Neon Cyber",
            light = Color(0xFF1A2438), dark = Color(0xFF0B1020),
            frame = Color(0xFF00E5FF), coord = Color(0xFF7AF0FF),
            last = Color(0x6600E5FF), select = Color(0xAAFF3DE0),
            check = Color(0xCCFF2A6A), dot = Color(0xEE00E5FF),
            anim = BoardAnim.PULSE,
            backdrop = listOf(Color(0xFF050814), Color(0xFF120818)),
        ),
        BoardTheme(
            "fantasy", "Enchanted",
            light = Color(0xFFD4C4F0), dark = Color(0xFF5A3A8A),
            frame = Color(0xFFC9A24A), coord = Color(0xFFF0E0A8),
            last = Color(0x66F0D060), select = Color(0xAAE8C050),
            check = Color(0xCCE05080), dot = Color(0xEEF4E0A0),
            anim = BoardAnim.SPARKLE,
            backdrop = listOf(Color(0xFF1A1028), Color(0xFF0E0818)),
        ),
        BoardTheme(
            "sakura", "Sakura",
            light = Color(0xFFFFE8F0), dark = Color(0xFFE090A8),
            frame = Color(0xFF8A3048), coord = Color(0xFF5A2030),
            last = Color(0x66FFB0C8), select = Color(0xAAFF8AB0),
            check = Color(0xAAD04050), dot = Color(0xCC5A2030),
            anim = BoardAnim.PETALS,
            backdrop = listOf(Color(0xFF3A1824), Color(0xFF1E1016)),
        ),
        BoardTheme(
            "lumenreach", "Lumenreach",
            light = Color(0xFF3D4A72), dark = Color(0xFF1C2448),
            frame = Color(0xFFC9A24A), coord = Color(0xFFE8D090),
            last = Color(0x66C9A24A), select = Color(0xAAC9A24A),
            check = Color(0xCCE06070), dot = Color(0xEEE8D090),
            anim = BoardAnim.LANTERNS,
            backdrop = listOf(Color(0xFF121028), Color(0xFF1A1020)),
        ),
    )

    val sets = listOf(
        PieceSet("staunton", "Classic Staunton", PieceStyle.STAUNTON,
            wFill = Color(0xFFF6F0E0), wStroke = Color(0xFF3A2A18),
            bFill = Color(0xFF2A2A2E), bStroke = Color(0xFFE8E0D0)),
        PieceSet("minimal", "Minimal Flat", PieceStyle.MINIMAL,
            wFill = Color(0xFFF2F2F0), wStroke = Color(0xFF222226),
            bFill = Color(0xFF222226), bStroke = Color(0xFFF2F2F0)),
        PieceSet("neon", "Neon Glow", PieceStyle.NEON,
            wFill = Color(0xFF041018), wStroke = Color(0xFF00F0FF),
            bFill = Color(0xFF180414), bStroke = Color(0xFFFF3DE0),
            glow = Color(0xFF7A5CFF)),
        PieceSet("fantasy", "Fantasy", PieceStyle.FANTASY,
            wFill = Color(0xFFF0D890), wStroke = Color(0xFF6A4810),
            bFill = Color(0xFF4A2878), bStroke = Color(0xFFE0C070),
            glow = Color(0x66F0D060)),
        PieceSet("chibi", "Anime Chibi", PieceStyle.CHIBI,
            wFill = Color(0xFFFFF0E8), wStroke = Color(0xFF5A3040),
            bFill = Color(0xFF3A3060), bStroke = Color(0xFFFFD0E0)),
        PieceSet("political_parody", "Political (Parody)", PieceStyle.POLITICAL,
            wFill = Color(0xFFF8F0EA), wStroke = Color(0xFF8A2030),
            bFill = Color(0xFF1A2A58), bStroke = Color(0xFF8EC0F0)),
        PieceSet("political_likeness", "Political (Likeness)", PieceStyle.POLITICAL,
            wFill = Color(0xFFF8F0EA), wStroke = Color(0xFF8A2030),
            bFill = Color(0xFF1A2A58), bStroke = Color(0xFF8EC0F0)),
    )

    fun board(id: String) = boards.find { it.id == id } ?: boards.first()
    fun set(id: String) = sets.find { it.id == id } ?: sets.first()
    fun availableSets(): List<PieceSet> = sets.filter {
        it.id != "political_likeness" || likenessBuildEnabled()
    }
}

fun viewSq(sq: Int, flipped: Boolean) = if (flipped) 63 - sq else sq

fun sqRect(board: Rect, sq: Int, flipped: Boolean): Rect {
    val v = viewSq(sq, flipped)
    val f = v and 7
    val r = v shr 3
    val s = board.width / 8f
    return Rect(board.left + f * s, board.bottom - (r + 1) * s, board.left + (f + 1) * s, board.bottom - r * s)
}

fun sqAt(board: Rect, p: Offset, flipped: Boolean): Int? {
    if (!board.contains(p)) return null
    val s = board.width / 8f
    val f = ((p.x - board.left) / s).toInt().coerceIn(0, 7)
    val rFromTop = ((p.y - board.top) / s).toInt().coerceIn(0, 7)
    val r = 7 - rFromTop
    val v = r * 8 + f
    return if (flipped) 63 - v else v
}

fun DrawScope.drawBoard(
    theme: BoardTheme,
    board: Rect,
    coords: Boolean,
    selected: Int?,
    legal: Set<Int>,
    captures: Set<Int>,
    lastFrom: Int?,
    lastTo: Int?,
    checkSq: Int?,
    flipped: Boolean,
    animOn: Boolean,
    timeMs: Long,
    measurer: TextMeasurer?,
) {
    val s = board.width / 8f
    // frame
    drawRect(theme.frame, Offset(board.left - s * 0.08f, board.top - s * 0.08f), Size(board.width + s * 0.16f, board.height + s * 0.16f))
    for (sq in 0..63) {
        val r = sqRect(board, sq, flipped)
        val light = ((file(sq) + rank(sq)) and 1) == 1
        var color = if (light) theme.light else theme.dark
        if (theme.id == "neon") {
            color = if (light) Color(0xFF152038) else Color(0xFF0A101C)
        }
        drawRect(color, r.topLeft, r.size)
        if (theme.id == "marble") drawMarbleVeins(r, light, sq)
        if (theme.id == "wood") drawWoodGrain(r, light, sq)
        if (sq == lastFrom || sq == lastTo) drawRect(theme.last, r.topLeft, r.size)
        if (sq == checkSq) drawRect(theme.check, r.topLeft, r.size)
        if (sq == selected) drawRect(theme.select, r.topLeft, r.size)
        if (animOn && theme.anim == BoardAnim.PULSE) {
            val pulse = (0.5f + 0.5f * sin(timeMs / 420.0 * 2 * PI).toFloat())
            val glow = if (light) Color(0x3300E5FF) else Color(0x2200E5FF)
            drawRect(glow.copy(alpha = glow.alpha * (0.35f + 0.65f * pulse)), r.topLeft, r.size)
            if (!light) {
                drawRect(Color.Transparent, r.topLeft, r.size, style = Stroke(width = 1.4f * pulse + 0.6f))
                drawRect(Color(0x6600E5FF).copy(alpha = 0.25f + 0.35f * pulse), r.topLeft, r.size, style = Stroke(1.2f))
            }
        }
    }
    // legal marks
    for (sq in legal) {
        val r = sqRect(board, sq, flipped)
        val cap = sq in captures
        val c = theme.dot
        if (cap) {
            drawCircle(Color.Transparent, r.width * 0.42f, r.center, style = Stroke(r.width * 0.08f))
            drawCircle(c.copy(alpha = 0.85f), r.width * 0.42f, r.center, style = Stroke(r.width * 0.08f))
        } else {
            drawCircle(c.copy(alpha = 0.75f), r.width * 0.14f, r.center)
        }
    }
    if (animOn) drawBoardAnim(theme, board, timeMs)
    if (coords && measurer != null) {
        val style = TextStyle(color = theme.coord, fontSize = (s * 0.18f / density).sp)
        for (f in 0..7) {
            val name = "${'a' + if (flipped) 7 - f else f}"
            val layout = measurer.measure(name, style)
            val x = board.left + f * s + s - layout.size.width - 2f
            val y = board.bottom - layout.size.height - 2f
            drawText(layout, topLeft = Offset(x, y))
        }
        for (rk in 0..7) {
            val name = "${if (flipped) rk + 1 else 8 - rk}"
            val layout = measurer.measure(name, style)
            val x = board.left + 3f
            val y = board.top + rk * s + 2f
            drawText(layout, topLeft = Offset(x, y))
        }
    }
}

private fun DrawScope.drawWoodGrain(r: Rect, light: Boolean, sq: Int) {
    val tint = if (light) Color(0x22A06830) else Color(0x22000000)
    val seed = sq * 17 + 3
    for (i in 0..3) {
        val y = r.top + r.height * ((i * 0.22f + (seed % 5) * 0.03f) % 1f)
        drawLine(tint, Offset(r.left, y), Offset(r.right, y + r.height * 0.04f * if (i % 2 == 0) 1 else -1), strokeWidth = 1.2f)
    }
}

private fun DrawScope.drawMarbleVeins(r: Rect, light: Boolean, sq: Int) {
    val tint = if (light) Color(0x33808090) else Color(0x33E8E8F0)
    val a = Offset(r.left + (sq % 3) * r.width * 0.2f, r.top)
    val b = Offset(r.right - (sq % 2) * r.width * 0.15f, r.bottom)
    drawLine(tint, a, b, strokeWidth = 1.6f)
    drawLine(tint.copy(alpha = 0.12f), Offset(a.x + r.width * 0.3f, a.y), Offset(b.x - r.width * 0.1f, b.y), strokeWidth = 1.1f)
}

private fun DrawScope.drawBoardAnim(theme: BoardTheme, board: Rect, timeMs: Long) {
    when (theme.anim) {
        BoardAnim.PETALS -> {
            for (i in 0 until 16) {
                val t = (timeMs + i * 400L) % 7000L / 7000f
                val x = board.left + ((i * 37) % 100) / 100f * board.width
                val y = board.top - 10f + t * (board.height + 20f)
                val rot = t * 360f + i * 20f
                val w = board.width * 0.028f
                rotate(rot, Offset(x, y)) {
                    drawOval(Color(0xFFFFC0D0).copy(alpha = 0.75f), Offset(x - w, y - w * 0.5f), Size(w * 2, w))
                }
            }
        }
        BoardAnim.SPARKLE -> {
            for (i in 0 until 12) {
                val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin((timeMs / 200.0 + i) * 1.7).toFloat())
                val x = board.left + ((i * 53 + 11) % 100) / 100f * board.width
                val y = board.top + ((i * 31 + 19) % 100) / 100f * board.height
                val rad = board.width * 0.012f * (0.7f + twinkle)
                drawCircle(Color(0xFFF0D060).copy(alpha = 0.35f * twinkle), rad * 2.2f, Offset(x, y))
                drawCircle(Color(0xFFFFF4C0).copy(alpha = 0.85f * twinkle), rad, Offset(x, y))
            }
        }
        BoardAnim.LANTERNS -> {
            for (i in 0 until 8) {
                val t = (timeMs + i * 900L) % 8000L / 8000f
                val x = board.left + ((i * 41 + 7) % 100) / 100f * board.width + sin(t * 6.0 + i).toFloat() * 8f
                val y = board.bottom - t * (board.height + 24f)
                val rad = board.width * 0.025f
                drawCircle(Color(0x55FFC070), rad * 2.4f, Offset(x, y))
                drawCircle(Color(0xCCFFD090), rad, Offset(x, y))
            }
        }
        else -> {}
    }
}

fun DrawScope.drawPiece(pc: Pc, set: PieceSet, r: Rect) {
    val fill = if (pc.side == Side.WHITE) set.wFill else set.bFill
    val stroke = if (pc.side == Side.WHITE) set.wStroke else set.bStroke
    val pad = r.width * 0.08f
    val box = Rect(r.left + pad, r.top + pad, r.right - pad, r.bottom - pad)
    translate(box.left, box.top) {
        scale(box.width / 100f, box.height / 100f, Offset.Zero) {
            when (set.style) {
                PieceStyle.MINIMAL -> drawMinimal(pc.kind, fill, stroke)
                PieceStyle.NEON -> drawNeon(pc.kind, fill, stroke, set.glow)
                PieceStyle.FANTASY -> drawFantasy(pc.kind, fill, stroke, pc.side)
                PieceStyle.CHIBI -> drawChibi(pc.kind, fill, stroke, pc.side)
                PieceStyle.STAUNTON, PieceStyle.POLITICAL -> drawStaunton(pc.kind, fill, stroke)
            }
        }
    }
}

private fun DrawScope.drawStaunton(kind: Kind, fill: Color, stroke: Color) {
    val st = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun body() {
        // shared base
        drawRoundRect(fill, Offset(22f, 82f), Size(56f, 12f), androidx.compose.ui.geometry.CornerRadius(4f, 4f))
        drawRoundRect(stroke, Offset(22f, 82f), Size(56f, 12f), androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = st)
        drawRect(fill, Offset(28f, 76f), Size(44f, 8f))
        drawRect(stroke, Offset(28f, 76f), Size(44f, 8f), style = Stroke(2.2f))
    }
    when (kind) {
        Kind.P -> {
            body()
            drawCircle(fill, 16f, Offset(50f, 44f))
            drawCircle(stroke, 16f, Offset(50f, 44f), style = st)
            val p = Path().apply { moveTo(34f, 76f); lineTo(38f, 58f); quadraticTo(50f, 50f, 62f, 58f); lineTo(66f, 76f); close() }
            drawPath(p, fill); drawPath(p, stroke, style = st)
        }
        Kind.R -> {
            body()
            val p = Path().apply {
                moveTo(28f, 76f); lineTo(28f, 38f)
                lineTo(22f, 38f); lineTo(22f, 24f); lineTo(32f, 24f); lineTo(32f, 32f)
                lineTo(43f, 32f); lineTo(43f, 24f); lineTo(57f, 24f); lineTo(57f, 32f)
                lineTo(68f, 32f); lineTo(68f, 24f); lineTo(78f, 24f); lineTo(78f, 38f)
                lineTo(72f, 38f); lineTo(72f, 76f); close()
            }
            drawPath(p, fill); drawPath(p, stroke, style = st)
        }
        Kind.N -> {
            body()
            val p = knightPath()
            drawPath(p, fill); drawPath(p, stroke, style = st)
            drawCircle(stroke, 2.6f, Offset(58f, 32f))
        }
        Kind.B -> {
            body()
            val p = Path().apply {
                moveTo(34f, 76f); quadraticTo(30f, 50f, 50f, 18f); quadraticTo(70f, 50f, 66f, 76f); close()
            }
            drawPath(p, fill); drawPath(p, stroke, style = st)
            drawLine(stroke, Offset(50f, 28f), Offset(50f, 52f), 2.6f)
            drawCircle(fill, 7f, Offset(50f, 18f))
            drawCircle(stroke, 7f, Offset(50f, 18f), style = st)
        }
        Kind.Q -> {
            body()
            val p = Path().apply {
                moveTo(30f, 76f); lineTo(24f, 34f); lineTo(38f, 52f); lineTo(50f, 22f)
                lineTo(62f, 52f); lineTo(76f, 34f); lineTo(70f, 76f); close()
            }
            drawPath(p, fill); drawPath(p, stroke, style = st)
            for (x in floatArrayOf(24f, 38f, 50f, 62f, 76f)) {
                val y = if (x == 50f) 18f else 30f
                drawCircle(fill, 5f, Offset(x, y)); drawCircle(stroke, 5f, Offset(x, y), style = Stroke(2.2f))
            }
        }
        Kind.K -> {
            body()
            val p = Path().apply {
                moveTo(32f, 76f); lineTo(28f, 40f); quadraticTo(50f, 28f, 72f, 40f); lineTo(68f, 76f); close()
            }
            drawPath(p, fill); drawPath(p, stroke, style = st)
            drawLine(stroke, Offset(50f, 10f), Offset(50f, 28f), 4f)
            drawLine(stroke, Offset(40f, 18f), Offset(60f, 18f), 4f)
            drawRoundRect(fill, Offset(30f, 36f), Size(40f, 8f), androidx.compose.ui.geometry.CornerRadius(3f))
            drawRoundRect(stroke, Offset(30f, 36f), Size(40f, 8f), androidx.compose.ui.geometry.CornerRadius(3f), style = Stroke(2.2f))
        }
    }
}

private fun knightPath() = Path().apply {
    moveTo(26f, 76f)
    lineTo(30f, 62f)
    cubicTo(20f, 54f, 22f, 40f, 32f, 32f)
    cubicTo(30f, 22f, 40f, 14f, 50f, 16f)
    cubicTo(54f, 8f, 66f, 8f, 70f, 16f)
    cubicTo(82f, 18f, 84f, 30f, 74f, 34f)
    cubicTo(80f, 44f, 70f, 52f, 66f, 58f)
    lineTo(70f, 76f)
    close()
}

private fun DrawScope.drawMinimal(kind: Kind, fill: Color, stroke: Color) {
    val st = Stroke(3.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (kind) {
        Kind.P -> { drawCircle(fill, 16f, Offset(50f, 42f)); drawCircle(stroke, 16f, Offset(50f, 42f), style = st)
            drawRoundRect(fill, Offset(32f, 58f), Size(36f, 28f), androidx.compose.ui.geometry.CornerRadius(8f))
            drawRoundRect(stroke, Offset(32f, 58f), Size(36f, 28f), androidx.compose.ui.geometry.CornerRadius(8f), style = st) }
        Kind.N -> { val p = Path().apply { moveTo(28f, 84f); lineTo(32f, 50f); lineTo(46f, 28f); lineTo(70f, 22f); lineTo(76f, 36f); lineTo(60f, 42f); lineTo(72f, 84f); close() }
            drawPath(p, fill); drawPath(p, stroke, style = st) }
        Kind.B -> { val p = Path().apply { moveTo(50f, 14f); lineTo(72f, 84f); lineTo(28f, 84f); close() }
            drawPath(p, fill); drawPath(p, stroke, style = st); drawLine(stroke, Offset(50f, 32f), Offset(50f, 64f), 3f) }
        Kind.R -> { drawRect(fill, Offset(26f, 36f), Size(48f, 50f)); drawRect(stroke, Offset(26f, 36f), Size(48f, 50f), style = st)
            drawRect(fill, Offset(20f, 18f), Size(16f, 20f)); drawRect(fill, Offset(42f, 18f), Size(16f, 20f)); drawRect(fill, Offset(64f, 18f), Size(16f, 20f))
            drawRect(stroke, Offset(20f, 18f), Size(60f, 20f), style = st) }
        Kind.Q -> { drawCircle(fill, 22f, Offset(50f, 48f)); drawCircle(stroke, 22f, Offset(50f, 48f), style = st)
            drawRoundRect(fill, Offset(28f, 68f), Size(44f, 18f), androidx.compose.ui.geometry.CornerRadius(6f))
            drawRoundRect(stroke, Offset(28f, 68f), Size(44f, 18f), androidx.compose.ui.geometry.CornerRadius(6f), style = st)
            for (x in floatArrayOf(28f, 50f, 72f)) drawCircle(fill, 6f, Offset(x, 20f)).also { drawCircle(stroke, 6f, Offset(x, 20f), style = Stroke(2.4f)) } }
        Kind.K -> { drawRect(fill, Offset(36f, 28f), Size(28f, 56f)); drawRect(stroke, Offset(36f, 28f), Size(28f, 56f), style = st)
            drawRect(fill, Offset(20f, 44f), Size(60f, 16f)); drawRect(stroke, Offset(20f, 44f), Size(60f, 16f), style = st)
            drawRect(fill, Offset(44f, 12f), Size(12f, 20f)); drawRect(stroke, Offset(44f, 12f), Size(12f, 20f), style = st) }
    }
}

private fun DrawScope.drawNeon(kind: Kind, fill: Color, stroke: Color, glow: Color?) {
    val g = glow ?: stroke
    // draw staunton-like outlines with glow
    fun strokePath(p: Path) {
        drawPath(p, g.copy(alpha = 0.35f), style = Stroke(10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, fill)
        drawPath(p, stroke, style = Stroke(3.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    when (kind) {
        Kind.P -> {
            val p = Path().apply { addOval(Rect(34f, 24f, 66f, 56f)); moveTo(36f, 54f); lineTo(30f, 86f); lineTo(70f, 86f); lineTo(64f, 54f); close() }
            strokePath(p)
        }
        Kind.R -> {
            val p = Path().apply {
                moveTo(28f, 86f); lineTo(28f, 40f); lineTo(22f, 40f); lineTo(22f, 22f); lineTo(34f, 22f); lineTo(34f, 30f)
                lineTo(44f, 30f); lineTo(44f, 22f); lineTo(56f, 22f); lineTo(56f, 30f); lineTo(66f, 30f); lineTo(66f, 22f)
                lineTo(78f, 22f); lineTo(78f, 40f); lineTo(72f, 40f); lineTo(72f, 86f); close()
            }
            strokePath(p)
        }
        Kind.N -> strokePath(knightPath())
        Kind.B -> {
            val p = Path().apply { moveTo(34f, 86f); quadraticTo(28f, 50f, 50f, 16f); quadraticTo(72f, 50f, 66f, 86f); close() }
            strokePath(p)
            drawLine(stroke, Offset(50f, 32f), Offset(50f, 58f), 3f)
        }
        Kind.Q -> {
            val p = Path().apply { moveTo(28f, 86f); lineTo(20f, 30f); lineTo(36f, 50f); lineTo(50f, 18f); lineTo(64f, 50f); lineTo(80f, 30f); lineTo(72f, 86f); close() }
            strokePath(p)
        }
        Kind.K -> {
            val p = Path().apply { moveTo(30f, 86f); lineTo(26f, 40f); quadraticTo(50f, 26f, 74f, 40f); lineTo(70f, 86f); close() }
            strokePath(p)
            drawLine(g.copy(alpha = 0.4f), Offset(50f, 8f), Offset(50f, 28f), 8f)
            drawLine(stroke, Offset(50f, 8f), Offset(50f, 28f), 3.4f)
            drawLine(stroke, Offset(38f, 16f), Offset(62f, 16f), 3.4f)
        }
    }
}

private fun DrawScope.drawFantasy(kind: Kind, fill: Color, stroke: Color, side: Side) {
    drawStaunton(kind, fill, stroke)
    val gem = if (side == Side.WHITE) Color(0xFF70E0FF) else Color(0xFFFF80C0)
    val gy = when (kind) { Kind.P -> 44f; Kind.N -> 36f; Kind.B -> 36f; Kind.R -> 50f; Kind.Q -> 48f; Kind.K -> 50f }
    drawCircle(Color.White.copy(alpha = 0.5f), 6.5f, Offset(50f, gy))
    drawCircle(gem, 4.4f, Offset(50f, gy))
}

private fun DrawScope.drawChibi(kind: Kind, fill: Color, stroke: Color, side: Side) {
    val st = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // feet / body
    drawRoundRect(fill, Offset(30f, 70f), Size(40f, 22f), androidx.compose.ui.geometry.CornerRadius(10f))
    drawRoundRect(stroke, Offset(30f, 70f), Size(40f, 22f), androidx.compose.ui.geometry.CornerRadius(10f), style = st)
    // head
    val head = Offset(50f, 42f)
    if (kind == Kind.N) {
        val ear = Path().apply { moveTo(28f, 36f); lineTo(18f, 14f); lineTo(40f, 26f); close() }
        drawPath(ear, fill); drawPath(ear, stroke, style = st)
        val ear2 = Path().apply { moveTo(62f, 24f); lineTo(78f, 12f); lineTo(74f, 34f); close() }
        drawPath(ear2, fill); drawPath(ear2, stroke, style = st)
    }
    drawCircle(fill, 26f, head)
    drawCircle(stroke, 26f, head, style = st)
    // eyes
    val eye = if (side == Side.WHITE) Color(0xFF303040) else Color(0xFFFFE8F0)
    drawCircle(eye, 3.4f, Offset(42f, 42f))
    drawCircle(eye, 3.4f, Offset(58f, 42f))
    drawArc(stroke, 200f, 140f, false, Offset(42f, 50f), Size(16f, 10f), style = Stroke(2.2f))
    when (kind) {
        Kind.K -> {
            val c = Path().apply { moveTo(34f, 22f); lineTo(40f, 8f); lineTo(50f, 18f); lineTo(60f, 8f); lineTo(66f, 22f); close() }
            drawPath(c, Color(0xFFFFE080)); drawPath(c, stroke, style = Stroke(2.4f))
        }
        Kind.Q -> {
            for (x in floatArrayOf(34f, 50f, 66f)) drawCircle(Color(0xFFFFE080), 5f, Offset(x, 16f))
        }
        Kind.B -> {
            drawLine(stroke, Offset(50f, 10f), Offset(50f, 22f), 3f)
            drawCircle(fill, 5f, Offset(50f, 10f)); drawCircle(stroke, 5f, Offset(50f, 10f), style = Stroke(2.2f))
        }
        Kind.R -> {
            drawRect(fill, Offset(32f, 12f), Size(36f, 14f)); drawRect(stroke, Offset(32f, 12f), Size(36f, 14f), style = Stroke(2.4f))
        }
        Kind.P -> { /* just the face */ }
        Kind.N -> { /* ears already */ }
    }
}
