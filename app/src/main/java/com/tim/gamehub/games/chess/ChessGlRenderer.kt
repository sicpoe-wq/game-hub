package com.tim.gamehub.games.chess

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

data class GlPiece(
    val sq: Int,
    val kind: Kind,
    val side: Side,
    val fig: String?,
)

data class GlScene(
    val cam: Cam = Cam(),
    val highQuality: Boolean = true,
    val coords: Boolean = true,
    val selected: Int? = null,
    val legal: Set<Int> = emptySet(),
    val captures: Set<Int> = emptySet(),
    val lastFrom: Int? = null,
    val lastTo: Int? = null,
    val checkSq: Int? = null,
    val flipped: Boolean = false,
    val pieces: List<GlPiece> = emptyList(),
    val animFrom: Int? = null,
    val animTo: Int? = null,
    val animKind: Kind? = null,
    val animSide: Side? = null,
    val animFig: String? = null,
    val animP: Float = 1f,
    val animCapSq: Int? = null,
    val animPromo: Boolean = false,
    val animMate: Boolean = false,
    val dragSq: Int? = null,
    val dragWorld: Vec3? = null,
    val boardId: String = "wood",
    val setId: String = "staunton",
    val flyP: Float = 1f,
    val mateP: Float = 1f,
    val timeSec: Float = 0f,
    val idle: Boolean = false,
    val capTick: Int = 0,
)

private const val TAG = "ChessGL"

private const val VS = """
attribute vec3 aPos;
attribute vec3 aNrm;
attribute vec2 aUv;
uniform mat4 uMVP;
uniform mat4 uModel;
uniform mat3 uNrmM;
varying vec3 vN;
varying vec3 vW;
varying vec2 vUv;
void main() {
  vec4 w = uModel * vec4(aPos, 1.0);
  vW = w.xyz;
  vN = normalize(uNrmM * aNrm);
  vUv = aUv;
  gl_Position = uMVP * vec4(aPos, 1.0);
}
"""

private const val FS = """
precision mediump float;
uniform sampler2D uTex;
uniform vec4 uColor;
uniform vec3 uLight;
uniform vec3 uEye;
uniform float uAmb;
uniform float uSpec;
uniform float uUseTex;
uniform float uCut;
uniform float uEmit;
uniform float uUnlit;
varying vec3 vN;
varying vec3 vW;
varying vec2 vUv;
void main() {
  vec4 t = (uUseTex > 0.5) ? texture2D(uTex, vUv) : vec4(1.0);
  vec4 c = t * uColor;
  if (c.a < uCut) discard;
  if (uUnlit > 0.5) {
    gl_FragColor = vec4(c.rgb + c.rgb * uEmit, c.a);
    return;
  }
  vec3 n = normalize(vN);
  vec3 l = normalize(uLight);
  float ndl = max(dot(n, l), 0.0);
  vec3 v = normalize(uEye - vW);
  vec3 h = normalize(l + v);
  float sp = pow(max(dot(n, h), 0.0), 28.0) * uSpec;
  float lit = uAmb + (1.0 - uAmb) * ndl;
  vec3 rgb = c.rgb * lit + vec3(sp) + c.rgb * uEmit;
  gl_FragColor = vec4(rgb, c.a);
}
"""

class GpuMesh(mesh: Mesh) {
    val vCount = mesh.vCount
    val iCount = mesh.idx.size
    val pos: FloatBuffer = fb(mesh.pos)
    val nrm: FloatBuffer = fb(mesh.nrm)
    val uv: FloatBuffer = fb(mesh.uv)
    val idx: ShortBuffer = sb(mesh.idx)
}

private fun fb(a: FloatArray): FloatBuffer =
    ByteBuffer.allocateDirect(a.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(a); position(0) }
private fun sb(a: ShortArray): ShortBuffer =
    ByteBuffer.allocateDirect(a.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(a); position(0) }

class ChessGlRenderer(private val ctx: Context) : GLSurfaceView.Renderer {
    @Volatile var scene: GlScene = GlScene()
    private var w = 1
    private var h = 1
    private var prog = 0
    private var aPos = 0
    private var aNrm = 0
    private var aUv = 0
    private var uMVP = 0
    private var uModel = 0
    private var uNrmM = 0
    private var uTex = 0
    private var uColor = 0
    private var uLight = 0
    private var uEye = 0
    private var uAmb = 0
    private var uSpec = 0
    private var uUseTex = 0
    private var uCut = 0
    private var uEmit = 0
    private var uUnlit = 0
    private val tex = HashMap<String, Int>()
    private val gpu = HashMap<String, GpuMesh>()
    private var white = 0
    private var lastCapTick = -1
    private val parts = ArrayList<Part>()
    private var startNs = 0L

    private class Part(var x: Float, var y: Float, var z: Float, var vx: Float, var vy: Float, var vz: Float, var life: Float, var r: Float, var g: Float, var b: Float, var size: Float)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        prog = link(VS, FS)
        aPos = GLES20.glGetAttribLocation(prog, "aPos")
        aNrm = GLES20.glGetAttribLocation(prog, "aNrm")
        aUv = GLES20.glGetAttribLocation(prog, "aUv")
        uMVP = GLES20.glGetUniformLocation(prog, "uMVP")
        uModel = GLES20.glGetUniformLocation(prog, "uModel")
        uNrmM = GLES20.glGetUniformLocation(prog, "uNrmM")
        uTex = GLES20.glGetUniformLocation(prog, "uTex")
        uColor = GLES20.glGetUniformLocation(prog, "uColor")
        uLight = GLES20.glGetUniformLocation(prog, "uLight")
        uEye = GLES20.glGetUniformLocation(prog, "uEye")
        uAmb = GLES20.glGetUniformLocation(prog, "uAmb")
        uSpec = GLES20.glGetUniformLocation(prog, "uSpec")
        uUseTex = GLES20.glGetUniformLocation(prog, "uUseTex")
        uCut = GLES20.glGetUniformLocation(prog, "uCut")
        uEmit = GLES20.glGetUniformLocation(prog, "uEmit")
        uUnlit = GLES20.glGetUniformLocation(prog, "uUnlit")
        white = solidTex(0xffffffff.toInt())
        GLES20.glClearColor(0.05f, 0.05f, 0.07f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        startNs = System.nanoTime()
        tex.clear(); gpu.clear()
        ChessMeshes.clear()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        w = width.coerceAtLeast(1); h = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, w, h)
    }

    override fun onDrawFrame(gl: GL10?) {
        val s = scene
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (prog == 0) return
        GLES20.glUseProgram(prog)
        val time = s.timeSec
        val fly = easeOut(s.flyP)
        val baseCam = s.cam.clamped()
        val intro = Cam(yaw = 0.05f, pitch = 1.22f, dist = 16.5f)
        val cam = Cam(
            yaw = lerp(intro.yaw, baseCam.yaw, fly) + sin(s.mateP * PI.toFloat()) * 0.7f * (if (s.animMate) 1f else 0f),
            pitch = lerp(intro.pitch, baseCam.pitch, fly),
            dist = lerp(intro.dist, baseCam.dist, fly),
        ).clamped()
        val target = Vec3(4f, 0.25f, 4f)
        val eye = camEye(cam, target)
        val view = matLookAt(eye, target, Vec3(0f, 1f, 0f))
        val aspect = w.toFloat() / h.toFloat()
        val proj = matPerspective(0.85f, aspect, 0.2f, 60f)
        val vp = proj * view
        val light = Vec3(0.45f, 0.82f, 0.35f).norm()
        GLES20.glUniform3f(uLight, light.x, light.y, light.z)
        GLES20.glUniform3f(uEye, eye.x, eye.y, eye.z)

        val set = ChessThemes.set(s.setId)
        val theme = ChessThemes.board(s.boardId)
        val texs = boardTextures(s.boardId)
        val bgName = backdropRes(s.boardId, s.setId)
        val political = politicalStyleOf(s.setId)
        val segs = if (s.highQuality) 28 else 14

        // backdrop (unlit)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDepthMask(false)
        draw(mesh("sky", { ChessMeshes.skybox(if (s.highQuality) 18f else 16f) }),
            matIdentity(), vp, bind(bgName, white), 1f, 1f, 1f, 1f, amb = 1f, spec = 0f, useTex = true, cut = 0.02f, emit = 0.12f, unlit = true)
        draw(mesh("bg", { ChessMeshes.backdrop(if (s.highQuality) 16f else 14f, segs = if (s.highQuality) 48 else 24) }),
            matIdentity(), vp, bind(bgName, white), 1f, 1f, 1f, 1f, amb = 1f, spec = 0f, useTex = true, cut = 0.02f, emit = 0.08f, unlit = true)
        // floor tint
        val floorCol = if (s.boardId == "neon") floatArrayOf(0.04f, 0.05f, 0.08f, 1f) else floatArrayOf(0.12f, 0.10f, 0.09f, 1f)
        draw(mesh("floor", { ChessMeshes.floor() }), matIdentity(), vp, white, floorCol[0], floorCol[1], floorCol[2], 1f, amb = 0.7f, spec = 0.05f, useTex = false, cut = 0.02f, emit = 0f, unlit = false)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        // frame
        draw(mesh("frame", { ChessMeshes.frame() }), matIdentity(), vp, bind(texs.frame, white), 1f, 1f, 1f, 1f, amb = 0.38f, spec = 0.25f, useTex = true, cut = 0.02f, emit = 0f)
        // metal trim on the top outer rim only (not a slab over the squares)
        val trimH0 = 0.205f; val trimH1 = 0.228f
        val o0 = -0.64f; val o1 = 8.64f; val i0 = -0.50f; val i1 = 8.50f
        val trimParts = listOf(
            mesh("trimA", { ChessMeshes.box(o0, trimH0, o0, o1, trimH1, i0) }),
            mesh("trimB", { ChessMeshes.box(o0, trimH0, i1, o1, trimH1, o1) }),
            mesh("trimC", { ChessMeshes.box(o0, trimH0, i0, i0, trimH1, i1) }),
            mesh("trimD", { ChessMeshes.box(i1, trimH0, i0, o1, trimH1, i1) }),
        )
        val te = if (s.boardId == "neon") 0.25f else 0f
        for (tm in trimParts) {
            draw(tm, matIdentity(), vp, bind(texs.trim, white), 1f, 1f, 1f, 1f, amb = 0.4f, spec = 0.55f, useTex = true, cut = 0.02f, emit = te)
        }

        // squares (disable cull: +Y quads are viewed from above)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        val sqMesh = mesh("sq", { ChessMeshes.quad(0f, 0.02f, 0f, 1f, 0.02f, 0f, 1f, 0.02f, 1f, 0f, 0.02f, 1f, 0f, 1f, 0f) })
        for (sq in 0..63) {
            val v = viewSq(sq, false)
            val f = (v and 7).toFloat()
            val r = (v shr 3).toFloat()
            val lightSq = ((v and 7) + (v shr 3)) and 1 == 0
            val name = if (lightSq) texs.light else texs.dark
            val cr = if (lightSq) 1.05f else 0.58f
            val cg = if (lightSq) 1.02f else 0.50f
            val cb = if (lightSq) 0.98f else 0.40f
            val model = matTranslate(f, 0f, r)
            draw(sqMesh, model, vp, bind(name, white), cr, cg, cb, 1f, amb = 0.70f, spec = 0.08f, useTex = true, cut = 0.02f, emit = 0f)
            // overlays
            val overlay = when {
                s.checkSq == sq -> floatArrayOf(0.85f, 0.15f, 0.12f, 0.42f + 0.20f * (0.5f + 0.5f * sin(time * 6f)))
                s.selected == sq -> floatArrayOf(theme.select.red, theme.select.green, theme.select.blue, 0.40f)
                sq == s.lastFrom || sq == s.lastTo -> floatArrayOf(theme.last.red, theme.last.green, theme.last.blue, 0.32f)
                sq in s.captures -> floatArrayOf(0.85f, 0.2f, 0.15f, 0.28f)
                sq in s.legal -> floatArrayOf(theme.dot.red, theme.dot.green, theme.dot.blue, 0.22f)
                else -> null
            }
            if (overlay != null) {
                draw(sqMesh, matTranslate(f, 0.021f, r), vp, white, overlay[0], overlay[1], overlay[2], overlay[3],
                    amb = 1f, spec = 0f, useTex = false, cut = 0.02f, emit = if (s.checkSq == sq) 0.3f else 0f, unlit = true)
            }
        }

        // engraved coordinates
        if (s.coords) {
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            val letters = mesh("letter", { ChessMeshes.letterQuad() })
            val atlas = bind("tex_letter", white)
            for (i in 0..7) {
                val u0 = (i % 4) / 4f
                val v0row = (i / 4) / 4f
                // files A-H along rank 1 side (z = -0.30), x = 0.5+i
                drawLetter(letters, vp, atlas, i,  i + 0.5f, 0.226f, -0.30f, 0f)
                // ranks 1-8 along file a (x = -0.30), z = 0.5+i ; atlas index 8+i
                drawLetter(letters, vp, atlas, 8 + i, -0.30f, 0.226f, i + 0.5f, 0f)
            }
            GLES20.glEnable(GLES20.GL_CULL_FACE)
        }

        GLES20.glEnable(GLES20.GL_CULL_FACE)
        // pieces
        val hide = HashSet<Int>()
        s.dragSq?.let { hide += it }
        if (s.animP < 0.999f && s.animTo != null) hide += s.animTo!!
        val politicalAlpha = ArrayList<() -> Unit>()
        for (pc in s.pieces) {
            if (pc.sq in hide) continue
            val world = pieceWorld(pc.sq, s, time)
            if (political != null) politicalAlpha += { drawPolitical(pc.kind, pc.side, pc.fig, political, world, vp, eye, set, segs, s, time) }
            else drawClassic(pc.kind, pc.side, world, vp, set, segs, s, time, pc.sq)
        }
        if (s.animP < 0.999f && s.animFrom != null && s.animTo != null && s.animKind != null && s.animSide != null) {
            val world = animWorld(s)
            val tmp = GlPiece(s.animTo!!, s.animKind, s.animSide, s.animFig)
            if (political != null) politicalAlpha += { drawPolitical(tmp.kind, tmp.side, tmp.fig, political, world, vp, eye, set, segs, s, time) }
            else drawClassic(tmp.kind, tmp.side, world, vp, set, segs, s, time, tmp.sq)
        }
        s.dragSq?.let { ds ->
            val piece = s.pieces.find { it.sq == ds }
            val dw = s.dragWorld
            if (piece != null && dw != null) {
                if (political != null) politicalAlpha += { drawPolitical(piece.kind, piece.side, piece.fig, political, dw, vp, eye, set, segs, s, time) }
                else drawClassic(piece.kind, piece.side, dw, vp, set, segs, s, time, ds)
            }
        }
        // alpha cards back-to-front
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        politicalAlpha.forEach { it() }

        // promo gold ring
        if (s.animPromo && s.animP < 1f && s.animTo != null) {
            val wld = sqWorld(s.animTo!!, s.flipped, 0.05f + s.animP * 0.9f)
            val sc = 0.15f + s.animP * 0.55f
            val model = matTranslate(wld.x, wld.y, wld.z) * matScale(sc, 0.05f, sc)
            draw(mesh("ring", { ChessMeshes.disc(1f, 0f, 24) }), model, vp, bind("tex_gold", white),
                1f, 0.85f, 0.4f, 1f - s.animP, amb = 1f, spec = 0.6f, useTex = true, cut = 0.05f, emit = 0.7f, unlit = true)
        }

        // particles
        if (s.capTick != lastCapTick) {
            lastCapTick = s.capTick
            if (s.animCapSq != null) spawnBurst(s, time)
        }
        stepParts(s, 1f / 60f)
        drawParts(vp, eye)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }

    private fun pieceWorld(sq: Int, s: GlScene, time: Float): Vec3 {
        val base = sqWorld(sq, s.flipped, 0f)
        val idle = if (s.idle) sin(time * 2.05f + sq * 1.7f) * 0.012f else 0f
        return Vec3(base.x, base.y + idle, base.z)
    }

    private fun animWorld(s: GlScene): Vec3 {
        val a = sqWorld(s.animFrom!!, s.flipped, 0f)
        val b = sqWorld(s.animTo!!, s.flipped, 0f)
        val t = easeInOut(s.animP)
        val dx = b.x - a.x; val dz = b.z - a.z
        val dist = sqrt(dx * dx + dz * dz)
        val lift = 0.38f * dist.coerceAtMost(1.4f)
        return Vec3(a.x + dx * t, a.y + (b.y - a.y) * t + sin(PI.toFloat() * t) * lift, a.z + dz * t)
    }

    private fun drawClassic(kind: Kind, side: Side, world: Vec3, vp: Mat4, set: PieceSet, segs: Int, s: GlScene, time: Float, sq: Int) {
        val m = mesh("st-$kind-$segs") { ChessMeshes.staunton(kind, segs) }
        val lean = if (s.idle) sin(time * 1.3f + sq * 0.9f) * 0.04f else 0f
        var model = matTranslate(world.x, world.y, world.z) * matRotateZ(lean)
        // black pieces face white
        if (side == Side.BLACK) model = matTranslate(world.x, world.y, world.z) * matRotateY(PI.toFloat()) * matRotateZ(lean)
        val fill = if (side == Side.WHITE) set.wFill else set.bFill
        val glow = set.glow
        var emit = if (glow != null) 0.18f else 0f
        if (s.checkSq == sq && kind == Kind.K) emit += 0.35f * (0.5f + 0.5f * sin(time * 6f))
        val spec = when (set.style) {
            PieceStyle.MINIMAL -> 0.08f
            PieceStyle.NEON -> 0.45f
            else -> 0.32f
        }
        val amb = if (set.style == PieceStyle.NEON) 0.28f else 0.38f
        draw(m, model, vp, white, fill.red, fill.green, fill.blue, 1f, amb, spec, useTex = false, cut = 0.02f, emit = emit)
    }

    private fun drawPolitical(kind: Kind, side: Side, fig: String?, style: PoliticalStyle, world: Vec3, vp: Mat4, eye: Vec3, set: PieceSet, segs: Int, s: GlScene, time: Float) {
        val base = mesh("pbase$segs") { ChessMeshes.politicalBase(segs) }
        val ring = if (side == Side.WHITE) floatArrayOf(0.75f, 0.16f, 0.18f, 1f) else floatArrayOf(0.16f, 0.32f, 0.72f, 1f)
        val wood = floatArrayOf(0.72f, 0.52f, 0.28f, 1f)
        val bm = matTranslate(world.x, world.y, world.z)
        draw(base, bm, vp, bind("tex_wood_lt", white), wood[0], wood[1], wood[2], 1f, amb = 0.4f, spec = 0.2f, useTex = true, cut = 0.02f, emit = 0f)
        // party ring
        val ringM = matTranslate(world.x, world.y + 0.055f, world.z) * matScale(0.30f, 0.02f, 0.30f)
        draw(mesh("disc24") { ChessMeshes.disc(1f, 0f, 24) }, ringM, vp, white, ring[0], ring[1], ring[2], 1f, amb = 0.5f, spec = 0.15f, useTex = false, cut = 0.02f, emit = 0.05f)
        val h = when (kind) { Kind.K -> 1.36f; Kind.Q -> 1.26f; Kind.R -> 1.16f; Kind.B -> 1.24f; Kind.N -> 1.18f; Kind.P -> 1.02f }
        val wid = 0.66f
        val card = mesh("card") { ChessMeshes.curvedCard(wid, h, if (s.highQuality) 12 else 6, 0.32f) }
        val yaw = atan2(eye.x - world.x, eye.z - world.z) + PI.toFloat()
        val sway = if (s.idle) sin(time * 1.4f + (fig ?: "").hashCode() * 0.01f) * 0.05f else 0f
        val cm = matTranslate(world.x, world.y + 0.18f, world.z) * matRotateY(yaw + sway)
        val texName = if (fig != null) politicalResName(style, fig) else null
        val tid = if (texName != null) bind(texName, white) else white
        var emit = 0f
        if (s.checkSq != null && kind == Kind.K && s.checkSq == s.pieces.find { it.kind == Kind.K && it.side != side }?.sq) {
            // handled on the checked king below
        }
        // check pulse if this piece is the checked king
        val isCheckedKing = kind == Kind.K && s.pieces.any { it.kind == Kind.K && it.sq == s.checkSq && it.fig == fig }
        if (s.checkSq != null && isCheckedKing) emit = 0.4f * (0.5f + 0.5f * sin(time * 6f))
        // simpler: if this world is on checkSq
        // (sq not passed) — use distance to check square
        s.checkSq?.let { cs ->
            val c = sqWorld(cs, s.flipped, 0f)
            if (kotlin.math.abs(c.x - world.x) < 0.1f && kotlin.math.abs(c.z - world.z) < 0.1f && kind == Kind.K)
                emit = 0.4f * (0.5f + 0.5f * sin(time * 6f))
        }
        draw(card, cm, vp, tid, 1f, 1f, 1f, 1f, amb = 0.72f, spec = 0.06f, useTex = true, cut = 0.10f, emit = emit)
    }

    private fun drawLetter(m: GpuMesh, vp: Mat4, atlas: Int, index: Int, x: Float, y: Float, z: Float, yaw: Float) {
        val col = (index % 4) / 4f
        val row = (index / 4) / 4f
        // remap UVs by baking a scaled/translated model isn't enough; use a dedicated uv offset via drawing a custom quad
        val u0 = col; val v0 = 1f - row - 0.25f; val u1 = col + 0.25f; val v1 = 1f - row
        val q = mesh("L$index") {
            ChessMeshes.quad(-0.13f, 0f, -0.13f, 0.13f, 0f, -0.13f, 0.13f, 0f, 0.13f, -0.13f, 0f, 0.13f, 0f, 1f, 0f, u0, v0, u1, v1)
        }
        val model = matTranslate(x, y, z) * matRotateY(yaw)
        draw(q, model, vp, atlas, 0.95f, 0.88f, 0.70f, 0.95f, amb = 1f, spec = 0f, useTex = true, cut = 0.15f, emit = 0.05f, unlit = true)
    }

    private fun spawnBurst(s: GlScene, time: Float) {
        val sq = s.animCapSq ?: return
        val wld = sqWorld(sq, s.flipped, 0.3f)
        val n = if (s.highQuality) 28 else 14
        val rng = kotlin.random.Random(s.capTick * 997 + 13)
        repeat(n) {
            val a = rng.nextFloat() * (PI * 2).toFloat()
            val sp = 0.8f + rng.nextFloat() * 1.6f
            parts += Part(
                wld.x, wld.y, wld.z,
                cos(a) * sp, 1.2f + rng.nextFloat() * 1.6f, sin(a) * sp,
                0.55f + rng.nextFloat() * 0.25f,
                1f, 0.75f + rng.nextFloat() * 0.2f, 0.35f,
                0.10f + rng.nextFloat() * 0.08f,
            )
        }
    }

    private fun stepParts(s: GlScene, dt: Float) {
        val it = parts.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) { it.remove(); continue }
            p.vy -= 3.2f * dt
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt
        }
    }

    private fun drawParts(vp: Mat4, eye: Vec3) {
        if (parts.isEmpty()) return
        val q = mesh("pquad") { ChessMeshes.particleQuad() }
        val t = bind("tex_particle", white)
        GLES20.glDepthMask(false)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        for (p in parts) {
            val yaw = atan2(eye.x - p.x, eye.z - p.z) + PI.toFloat()
            val model = matTranslate(p.x, p.y, p.z) * matRotateY(yaw) * matScale(p.size, p.size, p.size)
            val a = (p.life / 0.7f).coerceIn(0f, 1f)
            draw(q, model, vp, t, p.r, p.g, p.b, a, amb = 1f, spec = 0f, useTex = true, cut = 0.05f, emit = 0.6f, unlit = true)
        }
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDepthMask(true)
    }

    private fun mesh(key: String, build: () -> Mesh): GpuMesh = gpu.getOrPut(key) { GpuMesh(build()) }

    private fun bind(name: String, fallback: Int): Int {
        tex[name]?.let { return it }
        val id = loadTex(name) ?: fallback
        tex[name] = id
        return id
    }

    private fun loadTex(name: String): Int? {
        val resId = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (resId == 0) return null
        val opts = BitmapFactory.Options().apply { inScaled = false }
        val bmp = BitmapFactory.decodeResource(ctx.resources, resId, opts) ?: return null
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val tid = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tid)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        bmp.recycle()
        return tid
    }

    private fun solidTex(argb: Int): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val bmp = android.graphics.Bitmap.createBitmap(intArrayOf(argb), 1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        bmp.recycle()
        return ids[0]
    }

    private fun draw(
        mesh: GpuMesh, model: Mat4, vp: Mat4, texId: Int,
        r: Float, g: Float, b: Float, a: Float,
        amb: Float, spec: Float, useTex: Boolean, cut: Float, emit: Float, unlit: Boolean = false,
    ) {
        val mvp = vp * model
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp.m, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, model.m, 0)
        GLES20.glUniformMatrix3fv(uNrmM, 1, false, model.normalMatrix(), 0)
        GLES20.glUniform4f(uColor, r, g, b, a)
        GLES20.glUniform1f(uAmb, amb)
        GLES20.glUniform1f(uSpec, spec)
        GLES20.glUniform1f(uUseTex, if (useTex) 1f else 0f)
        GLES20.glUniform1f(uCut, cut)
        GLES20.glUniform1f(uEmit, emit)
        GLES20.glUniform1f(uUnlit, if (unlit) 1f else 0f)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glUniform1i(uTex, 0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glEnableVertexAttribArray(aNrm)
        GLES20.glEnableVertexAttribArray(aUv)
        mesh.pos.position(0); mesh.nrm.position(0); mesh.uv.position(0); mesh.idx.position(0)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, mesh.pos)
        GLES20.glVertexAttribPointer(aNrm, 3, GLES20.GL_FLOAT, false, 0, mesh.nrm)
        GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 0, mesh.uv)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.iCount, GLES20.GL_UNSIGNED_SHORT, mesh.idx)
    }

    private fun link(vsSrc: String, fsSrc: String): Int {
        fun compile(type: Int, src: String): Int {
            val sh = GLES20.glCreateShader(type)
            GLES20.glShaderSource(sh, src)
            GLES20.glCompileShader(sh)
            val ok = IntArray(1)
            GLES20.glGetShaderiv(sh, GLES20.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] == 0) Log.e(TAG, GLES20.glGetShaderInfoLog(sh))
            return sh
        }
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, compile(GLES20.GL_VERTEX_SHADER, vsSrc))
        GLES20.glAttachShader(p, compile(GLES20.GL_FRAGMENT_SHADER, fsSrc))
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) Log.e(TAG, GLES20.glGetProgramInfoLog(p))
        return p
    }
}

fun easeInOut(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return if (x < 0.5f) 2f * x * x else 1f - (-2f * x + 2f) * (-2f * x + 2f) / 2f
}
fun easeOut(t: Float): Float {
    val x = 1f - t.coerceIn(0f, 1f)
    return 1f - x * x
}
fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** EGL config chooser: try MSAA samples, then fall back to 0. */
class MsaaChooser(private val samples: Int) : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val want = intArrayOf(2, 4, 1, 0).filter { it <= samples || it == 0 }.distinct()
        for (s in want) {
            val attrs = if (s > 0) intArrayOf(
                EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_SAMPLE_BUFFERS, 1, EGL10.EGL_SAMPLES, s,
                EGL10.EGL_NONE,
            ) else intArrayOf(
                EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_NONE,
            )
            val out = arrayOfNulls<EGLConfig>(1)
            val n = IntArray(1)
            if (egl.eglChooseConfig(display, attrs, out, 1, n) && n[0] > 0 && out[0] != null) return out[0]!!
        }
        val fallback = intArrayOf(EGL10.EGL_RENDERABLE_TYPE, 4, EGL10.EGL_NONE)
        val out = arrayOfNulls<EGLConfig>(1)
        val n = IntArray(1)
        egl.eglChooseConfig(display, fallback, out, 1, n)
        return out[0]!!
    }
}
