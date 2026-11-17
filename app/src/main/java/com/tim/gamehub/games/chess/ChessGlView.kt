package com.tim.gamehub.games.chess

import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs

class ChessGlSurface(ctx: Context, highQuality: Boolean) : GLSurfaceView(ctx) {
    val renderer = ChessGlRenderer(ctx)
    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(MsaaChooser(if (highQuality) 4 else 0))
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
        isFocusable = false
        isClickable = false
    }
}

@Composable
fun ChessBoard3DView(
    c: ChessController,
    theme: BoardTheme,
    set: PieceSet,
    instant: Boolean,
    portraits: Map<String, androidx.compose.ui.graphics.ImageBitmap>,
    modifier: Modifier = Modifier,
) {
    ChessBoardGlView(c, instant, modifier)
}

@Composable
fun ChessBoardGlView(
    c: ChessController,
    instant: Boolean,
    modifier: Modifier = Modifier,
    previewPos: Pos? = null,
    previewFigs: Array<String?>? = null,
    previewSelect: Int? = null,
    previewLegal: Set<Int> = emptySet(),
    previewCaps: Set<Int> = emptySet(),
    previewLastFrom: Int? = null,
    previewLastTo: Int? = null,
) {
    var dragSq by remember { mutableStateOf<Int?>(null) }
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    var animP by remember { mutableFloatStateOf(1f) }
    var flyP by remember { mutableFloatStateOf(1f) }
    var mateP by remember { mutableFloatStateOf(1f) }
    var timeSec by remember { mutableFloatStateOf(0f) }
    val highQ = c.prefs.highQuality

    LaunchedEffect(Unit) {
        val t0 = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            timeSec = ((now - t0) / 1_000_000_000.0).toFloat()
        }
    }
    LaunchedEffect(c.animTick) {
        if (instant || c.animFrom == null) { animP = 1f; return@LaunchedEffect }
        animP = 0f
        animate(0f, 1f, animationSpec = tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { v, _ -> animP = v }
        c.clearAnim()
        animP = 1f
    }
    LaunchedEffect(c.flyInTick) {
        if (instant || c.flyInTick == 0) { flyP = 1f; return@LaunchedEffect }
        flyP = 0f
        animate(0f, 1f, animationSpec = tween(1100, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { v, _ -> flyP = v }
        flyP = 1f
    }
    LaunchedEffect(c.status, c.history.size) {
        if (instant || c.status != Status.CHECKMATE) { mateP = 1f; return@LaunchedEffect }
        mateP = 0f
        animate(0f, 1f, animationSpec = tween(1600, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { v, _ -> mateP = v }
        mateP = 1f
    }

    val pos = previewPos ?: c.pos
    val figs = previewFigs ?: c.figs
    val legalTos = if (previewPos != null) previewLegal else c.legalFrom.map { it.to }.toSet()
    val caps = if (previewPos != null) previewCaps else c.legalFrom.filter { it.captured != null }.map { it.to }.toSet()
    val selected = if (previewPos != null) previewSelect else c.selected
    val lastFrom = if (previewPos != null) previewLastFrom else c.lastMove?.from
    val lastTo = if (previewPos != null) previewLastTo else c.lastMove?.to
    val pieces = remember(pos, figs) {
        (0..63).mapNotNull { sq ->
            val pc = pos.at(sq) ?: return@mapNotNull null
            GlPiece(sq, pc.kind, pc.side, figs.getOrNull(sq))
        }
    }
    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> ChessGlSurface(ctx, highQ) },
            update = { view ->
                val w = view.width.coerceAtLeast(1).toFloat()
                val hgt = view.height.coerceAtLeast(1).toFloat()
                val dw = if (dragSq != null && dragPos != null) {
                    val hit = hitBoard(Projector(c.cam, w, hgt), c.cam, dragPos!!, w, hgt, c.flipped)
                    if (hit != null) sqWorld(hit, c.flipped, 0.2f) else {
                        // lift above plane along camera ray is already handled by hit; fallback
                        sqWorld(dragSq!!, c.flipped, 0.3f)
                    }
                } else null
                view.renderer.scene = GlScene(
                    cam = c.cam,
                    highQuality = highQ,
                    coords = c.prefs.coords,
                    selected = selected,
                    legal = legalTos,
                    captures = caps,
                    lastFrom = lastFrom,
                    lastTo = lastTo,
                    checkSq = if (previewPos != null) null else c.checkSq,
                    flipped = if (previewPos != null) false else c.flipped,
                    pieces = pieces,
                    animFrom = c.animFrom,
                    animTo = c.animTo,
                    animKind = c.animPc?.kind,
                    animSide = c.animPc?.side,
                    animFig = c.animFig,
                    animP = animP,
                    animCapSq = c.animCapSq,
                    animPromo = c.animPromo,
                    animMate = c.status == Status.CHECKMATE,
                    dragSq = dragSq,
                    dragWorld = dw,
                    boardId = c.peekBoard.ifBlank { c.prefs.board }.let { if (c.page == ChessPage.THEMES) c.peekBoard else c.prefs.board },
                    setId = if (c.page == ChessPage.THEMES) c.peekPieces else c.prefs.pieces,
                    flyP = flyP,
                    mateP = mateP,
                    timeSec = timeSec,
                    idle = highQ && c.prefs.boardAnim && !instant,
                    capTick = c.animTick,
                )
            },
        )
        Box(Modifier.fillMaxSize().pointerInput(c.flipped, c.status, c.thinking, c.pos, c.selected, c.cam, previewPos) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val cam0 = c.cam
                val preview = previewPos != null
                val hit = hitBoard(Projector(cam0, size.width.toFloat(), size.height.toFloat()), cam0, down.position, size.width.toFloat(), size.height.toFloat(), if (preview) false else c.flipped)
                val canGrab = !preview && hit != null && !c.status.over && !c.thinking && c.pos.at(hit)?.side == c.pos.side &&
                    (c.mode != ChessMode.CPU || c.pos.side == c.playerSide)
                var draggedPiece = false
                var rotated = false
                var last = down.position
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
                        c.cam = Cam(
                            yaw = c.cam.yaw - dt.x * 0.006f,
                            pitch = c.cam.pitch + dt.y * 0.005f,
                            dist = c.cam.dist,
                        ).clamped()
                    }
                    change.consume()
                }
                if (preview) {
                    // camera only
                } else if (draggedPiece && dragSq != null) {
                    val dest = hitBoard(Projector(c.cam, size.width.toFloat(), size.height.toFloat()), c.cam, dragPos ?: start, size.width.toFloat(), size.height.toFloat(), c.flipped)
                    if (dest != null) c.drop(dragSq!!, dest) else c.selected = null
                    dragSq = null; dragPos = null
                } else if (!rotated) {
                    if (hit != null) c.tap(hit)
                }
            }
        }.pointerInput(c.cam) {
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
        })
    }
}

@Composable
fun ChessThemeGlPreview(
    c: ChessController,
    modifier: Modifier = Modifier,
) {
    // Reuse the live controller camera and peeked themes via page == THEMES already read in ChessBoardGlView.
    ChessBoardGlView(c, instant = true, modifier = modifier)
}
