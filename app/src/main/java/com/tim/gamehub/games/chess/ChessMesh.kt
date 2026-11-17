package com.tim.gamehub.games.chess

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Mesh(
    val pos: FloatArray,
    val nrm: FloatArray,
    val uv: FloatArray,
    val idx: ShortArray,
) {
    val vCount get() = pos.size / 3
    fun translated(x: Float, y: Float, z: Float): Mesh {
        val p = pos.copyOf()
        for (i in 0 until vCount) { p[i * 3] += x; p[i * 3 + 1] += y; p[i * 3 + 2] += z }
        return Mesh(p, nrm, uv, idx)
    }
}

fun meshMerge(parts: List<Mesh>): Mesh {
    if (parts.size == 1) return parts[0]
    val pc = parts.sumOf { it.pos.size }
    val ic = parts.sumOf { it.idx.size }
    val pos = FloatArray(pc); val nrm = FloatArray(pc); val uv = FloatArray(pc / 3 * 2)
    val idx = ShortArray(ic)
    var vo = 0; var uo = 0; var io = 0
    for (m in parts) {
        m.pos.copyInto(pos, vo); m.nrm.copyInto(nrm, vo)
        m.uv.copyInto(uv, uo)
        val base = (vo / 3)
        for (k in m.idx.indices) idx[io + k] = (m.idx[k].toInt() + base).toShort()
        vo += m.pos.size; uo += m.uv.size; io += m.idx.size
    }
    return Mesh(pos, nrm, uv, idx)
}

object ChessMeshes {
    private val cache = HashMap<String, Mesh>()
    fun cached(key: String, build: () -> Mesh): Mesh = cache.getOrPut(key, build)
    fun clear() = cache.clear()

    fun lathe(profile: List<Pair<Float, Float>>, segs: Int): Mesh {
        val n = profile.size
        val vcount = n * (segs + 1)
        val pos = FloatArray(vcount * 3)
        val nrm = FloatArray(vcount * 3)
        val uv = FloatArray(vcount * 2)
        val y0 = profile.first().second
        val y1 = profile.last().second
        val ySpan = (y1 - y0).coerceAtLeast(1e-4f)
        var vi = 0
        for (i in 0 until n) {
            val (r, y) = profile[i]
            for (s in 0..segs) {
                val a = (s.toFloat() / segs) * (PI * 2).toFloat()
                val c = cos(a); val sn = sin(a)
                pos[vi * 3] = r * c
                pos[vi * 3 + 1] = y
                pos[vi * 3 + 2] = r * sn
                uv[vi * 2] = s.toFloat() / segs
                uv[vi * 2 + 1] = (y - y0) / ySpan
                vi++
            }
        }
        // normals from neighboring rings
        vi = 0
        for (i in 0 until n) {
            val i0 = (i - 1).coerceAtLeast(0)
            val i1 = (i + 1).coerceAtMost(n - 1)
            val dr = profile[i1].first - profile[i0].first
            val dy = profile[i1].second - profile[i0].second
            val len = sqrt(dr * dr + dy * dy).coerceAtLeast(1e-5f)
            val nr = dy / len
            val ny = -dr / len
            for (s in 0..segs) {
                val a = (s.toFloat() / segs) * (PI * 2).toFloat()
                val c = cos(a); val sn = sin(a)
                var nx = nr * c; var nz = nr * sn
                val l = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                nrm[vi * 3] = nx / l; nrm[vi * 3 + 1] = ny / l; nrm[vi * 3 + 2] = nz / l
                vi++
            }
        }
        val idx = ShortArray((n - 1) * segs * 6)
        var t = 0
        val stride = segs + 1
        for (i in 0 until n - 1) {
            for (s in 0 until segs) {
                val a = i * stride + s
                val b = a + 1
                val c = a + stride
                val d = c + 1
                idx[t++] = a.toShort(); idx[t++] = c.toShort(); idx[t++] = b.toShort()
                idx[t++] = b.toShort(); idx[t++] = c.toShort(); idx[t++] = d.toShort()
            }
        }
        // caps if end radii are small
        return Mesh(pos, nrm, uv, idx)
    }

    fun disc(r: Float, y: Float, segs: Int, up: Boolean = true): Mesh {
        val vcount = segs + 2
        val pos = FloatArray(vcount * 3)
        val nrm = FloatArray(vcount * 3)
        val uv = FloatArray(vcount * 2)
        pos[0] = 0f; pos[1] = y; pos[2] = 0f
        nrm[0] = 0f; nrm[1] = if (up) 1f else -1f; nrm[2] = 0f
        uv[0] = 0.5f; uv[1] = 0.5f
        for (s in 0..segs) {
            val a = (s.toFloat() / segs) * (PI * 2).toFloat()
            val c = cos(a); val sn = sin(a)
            val i = s + 1
            pos[i * 3] = r * c; pos[i * 3 + 1] = y; pos[i * 3 + 2] = r * sn
            nrm[i * 3] = 0f; nrm[i * 3 + 1] = if (up) 1f else -1f; nrm[i * 3 + 2] = 0f
            uv[i * 2] = 0.5f + 0.5f * c; uv[i * 2 + 1] = 0.5f + 0.5f * sn
        }
        val idx = ShortArray(segs * 3)
        for (s in 0 until segs) {
            if (up) {
                idx[s * 3] = 0; idx[s * 3 + 1] = (s + 1).toShort(); idx[s * 3 + 2] = (s + 2).toShort()
            } else {
                idx[s * 3] = 0; idx[s * 3 + 1] = (s + 2).toShort(); idx[s * 3 + 2] = (s + 1).toShort()
            }
        }
        return Mesh(pos, nrm, uv, idx)
    }

    fun box(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float): Mesh {
        data class F(val ax: Float, val ay: Float, val az: Float, val bx: Float, val by: Float, val bz: Float,
                     val cx: Float, val cy: Float, val cz: Float, val dx: Float, val dy: Float, val dz: Float,
                     val nx: Float, val ny: Float, val nz: Float)
        val faces = listOf(
            F(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f),
            F(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f),
            F(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f),
            F(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f),
            F(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f),
            F(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f),
        )
        val pos = FloatArray(faces.size * 4 * 3)
        val nrm = FloatArray(faces.size * 4 * 3)
        val uv = FloatArray(faces.size * 4 * 2)
        val idx = ShortArray(faces.size * 6)
        var v = 0
        for ((fi, f) in faces.withIndex()) {
            val verts = arrayOf(f.ax to (f.ay to f.az), f.bx to (f.by to f.bz), f.cx to (f.cy to f.cz), f.dx to (f.dy to f.dz))
            val uvs = arrayOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f)
            for (k in 0..3) {
                pos[v * 3] = verts[k].first; pos[v * 3 + 1] = verts[k].second.first; pos[v * 3 + 2] = verts[k].second.second
                nrm[v * 3] = f.nx; nrm[v * 3 + 1] = f.ny; nrm[v * 3 + 2] = f.nz
                uv[v * 2] = uvs[k].first; uv[v * 2 + 1] = uvs[k].second
                v++
            }
            val b = fi * 4
            idx[fi * 6] = b.toShort(); idx[fi * 6 + 1] = (b + 1).toShort(); idx[fi * 6 + 2] = (b + 2).toShort()
            idx[fi * 6 + 3] = b.toShort(); idx[fi * 6 + 4] = (b + 2).toShort(); idx[fi * 6 + 5] = (b + 3).toShort()
        }
        return Mesh(pos, nrm, uv, idx)
    }

    fun quad(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
             x2: Float, y2: Float, z2: Float, x3: Float, y3: Float, z3: Float,
             nx: Float, ny: Float, nz: Float,
             u0: Float = 0f, v0: Float = 0f, u1: Float = 1f, v1: Float = 1f): Mesh {
        val pos = floatArrayOf(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3)
        val nrm = floatArrayOf(nx, ny, nz, nx, ny, nz, nx, ny, nz, nx, ny, nz)
        val uv = floatArrayOf(u0, v0, u1, v0, u1, v1, u0, v1)
        val idx = shortArrayOf(0, 1, 2, 0, 2, 3)
        return Mesh(pos, nrm, uv, idx)
    }

    fun boardSquares(): Mesh {
        val parts = ArrayList<Mesh>(64)
        for (r in 0..7) for (f in 0..7) {
            val x0 = f.toFloat(); val z0 = r.toFloat()
            parts += quad(x0, 0.02f, z0, x0 + 1f, 0.02f, z0, x0 + 1f, 0.02f, z0 + 1f, x0, 0.02f, z0 + 1f, 0f, 1f, 0f)
        }
        return meshMerge(parts)
    }

    fun frame(): Mesh {
        val t = 0.62f
        val h = 0.22f
        val b = 0.07f
        val x0 = -t; val x1 = 8f + t
        val z0 = -t; val z1 = 8f + t
        // outer box minus inner well approximated as four rails + beveled lips
        val rails = listOf(
            box(x0, 0f, z0, 0f, h, z1),          // file-a side
            box(8f, 0f, z0, x1, h, z1),          // file-h side
            box(0f, 0f, z0, 8f, h, 0f),          // rank-1 side
            box(0f, 0f, 8f, 8f, h, z1),          // rank-8 side
        )
        // inner bevel (down into the well)
        val bevel = listOf(
            box(0f, 0.015f, -b, 8f, h, 0f),
            box(0f, 0.015f, 8f, 8f, h, 8f + b),
            box(-b, 0.015f, 0f, 0f, h, 8f),
            box(8f, 0.015f, 0f, 8f + b, h, 8f),
        )
        return meshMerge(rails + bevel)
    }

    fun floor(r: Float = 22f, segs: Int = 48) = disc(r, -0.01f, segs, up = true)

    fun backdrop(radius: Float = 16f, y0: Float = -1.5f, y1: Float = 11f, segs: Int = 48): Mesh {
        // inward-facing cylinder section (almost full)
        val rings = 2
        val vcount = rings * (segs + 1)
        val pos = FloatArray(vcount * 3)
        val nrm = FloatArray(vcount * 3)
        val uv = FloatArray(vcount * 2)
        var vi = 0
        for (i in 0 until rings) {
            val y = if (i == 0) y0 else y1
            val vv = i.toFloat()
            for (s in 0..segs) {
                val a = (s.toFloat() / segs) * (PI * 2).toFloat() + (PI / 2).toFloat()
                val c = cos(a); val sn = sin(a)
                pos[vi * 3] = 4f + radius * c
                pos[vi * 3 + 1] = y
                pos[vi * 3 + 2] = 4f + radius * sn
                nrm[vi * 3] = -c; nrm[vi * 3 + 1] = 0f; nrm[vi * 3 + 2] = -sn
                uv[vi * 2] = s.toFloat() / segs
                uv[vi * 2 + 1] = 1f - vv
                vi++
            }
        }
        val idx = ShortArray(segs * 6)
        var t = 0
        val stride = segs + 1
        for (s in 0 until segs) {
            val a = s; val b = s + 1; val c = a + stride; val d = c + 1
            // inward
            idx[t++] = a.toShort(); idx[t++] = b.toShort(); idx[t++] = c.toShort()
            idx[t++] = b.toShort(); idx[t++] = d.toShort(); idx[t++] = c.toShort()
        }
        return Mesh(pos, nrm, uv, idx)
    }

    fun curvedCard(width: Float, height: Float, segs: Int = 10, curve: Float = 0.28f): Mesh {
        // vertical cylindrical card, centered on origin, y from 0 to height, curves around Y
        val vcount = (segs + 1) * 2
        val pos = FloatArray(vcount * 3)
        val nrm = FloatArray(vcount * 3)
        val uv = FloatArray(vcount * 2)
        var vi = 0
        for (i in 0..1) {
            val y = if (i == 0) 0f else height
            val vv = i.toFloat()
            for (s in 0..segs) {
                val t = s.toFloat() / segs
                val a = (t - 0.5f) * curve
                val r = width / curve.coerceAtLeast(0.15f)
                val x = kotlin.math.sin(a) * r
                val z = (kotlin.math.cos(a) - 1f) * r
                pos[vi * 3] = x; pos[vi * 3 + 1] = y; pos[vi * 3 + 2] = z
                nrm[vi * 3] = -kotlin.math.sin(a); nrm[vi * 3 + 1] = 0f; nrm[vi * 3 + 2] = -kotlin.math.cos(a)
                uv[vi * 2] = t; uv[vi * 2 + 1] = 1f - vv
                vi++
            }
        }
        val idx = ShortArray(segs * 6)
        var t = 0
        val stride = segs + 1
        for (s in 0 until segs) {
            val a = s; val b = s + 1; val c = a + stride; val d = c + 1
            idx[t++] = a.toShort(); idx[t++] = c.toShort(); idx[t++] = b.toShort()
            idx[t++] = b.toShort(); idx[t++] = c.toShort(); idx[t++] = d.toShort()
        }
        return Mesh(pos, nrm, uv, idx)
    }

    fun skybox(s: Float = 18f): Mesh {
        // inward-facing cube around the board center (4, 4)
        val c = 4f
        val y0 = -2f; val y1 = 12f
        val x0 = c - s; val x1 = c + s
        val z0 = c - s; val z1 = c + s
        return meshMerge(listOf(
            // +z wall (inward normal -Z)
            quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, -1f, 0f, 0f, 1f, 1f),
            // -z
            quad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, 1f, 0f, 0f, 1f, 1f),
            // +x
            quad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, -1f, 0f, 0f, 0f, 0f, 1f, 1f),
            // -x
            quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, 1f, 0f, 0f, 0f, 0f, 1f, 1f),
            // ceiling
            quad(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0f, -1f, 0f, 0f, 0f, 1f, 1f),
        ))
    }

    fun politicalBase(segs: Int): Mesh {
        val profile = listOf(
            0.00f to 0.00f,
            0.30f to 0.00f,
            0.32f to 0.025f,
            0.28f to 0.05f,
            0.24f to 0.07f,
            0.26f to 0.10f,
            0.22f to 0.14f,
            0.20f to 0.16f,
            0.18f to 0.18f,
        )
        return meshMerge(listOf(lathe(profile, segs), disc(0.18f, 0.18f, segs, true), disc(0.30f, 0.001f, segs, false)))
    }

    private fun stauntonBase(topR: Float, h: Float): List<Pair<Float, Float>> = listOf(
        0.00f to 0.00f,
        0.32f to 0.00f,
        0.34f to 0.03f,
        0.28f to 0.06f,
        0.24f to 0.08f,
        0.26f to 0.11f,
        0.22f to 0.14f,
        topR to h,
    )

    fun pawn(segs: Int) = cached("pawn$segs") {
        val p = listOf(
            0.00f to 0.00f, 0.28f to 0.00f, 0.30f to 0.03f, 0.24f to 0.06f,
            0.20f to 0.10f, 0.22f to 0.13f, 0.16f to 0.16f, 0.12f to 0.42f,
            0.14f to 0.50f, 0.20f to 0.58f, 0.22f to 0.70f, 0.18f to 0.82f,
            0.10f to 0.88f, 0.00f to 0.90f,
        )
        meshMerge(listOf(lathe(p, segs), disc(0.28f, 0.001f, segs, false)))
    }

    fun rook(segs: Int) = cached("rook$segs") {
        val p = listOf(
            0.00f to 0.00f, 0.32f to 0.00f, 0.34f to 0.03f, 0.26f to 0.07f,
            0.24f to 0.11f, 0.26f to 0.14f, 0.18f to 0.18f, 0.15f to 0.62f,
            0.20f to 0.66f, 0.22f to 0.70f, 0.22f to 0.86f,
        )
        val body = lathe(p, segs)
        val merlons = ArrayList<Mesh>()
        merlons += disc(0.22f, 0.86f, segs, true)
        merlons += disc(0.32f, 0.001f, segs, false)
        val n = 4
        for (i in 0 until n) {
            val a = i * (PI * 2 / n).toFloat()
            val x = cos(a) * 0.16f
            val z = sin(a) * 0.16f
            merlons += box(x - 0.07f, 0.86f, z - 0.07f, x + 0.07f, 1.02f, z + 0.07f)
        }
        meshMerge(listOf(body) + merlons)
    }

    fun bishop(segs: Int) = cached("bishop$segs") {
        val p = listOf(
            0.00f to 0.00f, 0.30f to 0.00f, 0.32f to 0.03f, 0.24f to 0.07f,
            0.22f to 0.12f, 0.24f to 0.15f, 0.14f to 0.20f, 0.11f to 0.55f,
            0.16f to 0.68f, 0.18f to 0.86f, 0.12f to 1.02f, 0.05f to 1.10f,
            0.07f to 1.16f, 0.00f to 1.18f,
        )
        meshMerge(listOf(lathe(p, segs), disc(0.30f, 0.001f, segs, false)))
    }

    fun queen(segs: Int) = cached("queen$segs") {
        val p = listOf(
            0.00f to 0.00f, 0.33f to 0.00f, 0.35f to 0.03f, 0.26f to 0.07f,
            0.24f to 0.13f, 0.26f to 0.16f, 0.15f to 0.22f, 0.13f to 0.62f,
            0.20f to 0.78f, 0.16f to 0.96f, 0.22f to 1.08f, 0.14f to 1.16f,
            0.00f to 1.18f,
        )
        val coronet = ArrayList<Mesh>()
        coronet += lathe(p, segs)
        coronet += disc(0.33f, 0.001f, segs, false)
        for (i in 0 until 8) {
            val a = i * (PI * 2 / 8).toFloat()
            val x = cos(a) * 0.18f
            val z = sin(a) * 0.18f
            coronet += sphere(0.045f, 8).translated(x, 1.20f, z)
        }
        meshMerge(coronet)
    }

    fun king(segs: Int) = cached("king$segs") {
        val p = listOf(
            0.00f to 0.00f, 0.34f to 0.00f, 0.36f to 0.03f, 0.26f to 0.07f,
            0.24f to 0.13f, 0.26f to 0.16f, 0.15f to 0.22f, 0.13f to 0.70f,
            0.20f to 0.86f, 0.16f to 1.04f, 0.22f to 1.16f, 0.14f to 1.24f,
            0.10f to 1.28f,
        )
        val cross = listOf(
            lathe(p, segs),
            disc(0.34f, 0.001f, segs, false),
            box(-0.035f, 1.28f, -0.035f, 0.035f, 1.52f, 0.035f),
            box(-0.14f, 1.38f, -0.035f, 0.14f, 1.45f, 0.035f),
        )
        meshMerge(cross)
    }

    fun sphere(r: Float, segs: Int): Mesh {
        val p = (0..segs).map { i ->
            val a = (i.toFloat() / segs) * PI.toFloat()
            (r * sin(a)) to (r * (1f - cos(a)))
        }
        return lathe(p, segs)
    }

    fun knight(segs: Int) = cached("knight$segs") {
        val baseProf = listOf(
            0.00f to 0.00f, 0.30f to 0.00f, 0.32f to 0.03f, 0.24f to 0.07f,
            0.22f to 0.12f, 0.24f to 0.15f, 0.18f to 0.18f, 0.16f to 0.32f,
        )
        val base = meshMerge(listOf(lathe(baseProf, segs), disc(0.30f, 0.001f, segs, false)))
        // side-view silhouette (z forward, y up), extruded in x
        val sil = listOf(
            -0.10f to 0.32f, 0.14f to 0.32f,
            0.16f to 0.48f, 0.18f to 0.62f,
            0.22f to 0.76f, 0.28f to 0.90f,
            0.20f to 1.00f, 0.06f to 1.02f,
            -0.02f to 0.96f, -0.06f to 0.90f, // ear
            -0.02f to 0.88f, 0.04f to 0.86f,
            -0.04f to 0.78f, -0.10f to 0.62f,
            -0.14f to 0.48f, -0.10f to 0.32f,
        )
        val head = extrude(sil, 0.11f)
        meshMerge(listOf(base, head))
    }

    fun extrude(sil: List<Pair<Float, Float>>, half: Float): Mesh {
        val n = sil.size
        // sides + caps
        val pos = ArrayList<Float>()
        val nrm = ArrayList<Float>()
        val uv = ArrayList<Float>()
        val idx = ArrayList<Short>()
        fun vert(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, v: Float): Int {
            val i = pos.size / 3
            pos += x; pos += y; pos += z
            nrm += nx; nrm += ny; nrm += nz
            uv += u; uv += v
            return i
        }
        // two cap loops
        val capL = IntArray(n)
        val capR = IntArray(n)
        for (i in 0 until n) {
            val (z, y) = sil[i]
            capL[i] = vert(-half, y, z, -1f, 0f, 0f, 0f, y)
            capR[i] = vert(half, y, z, 1f, 0f, 0f, 1f, y)
        }
        for (i in 1 until n - 1) {
            idx += capL[0].toShort(); idx += capL[i].toShort(); idx += capL[i + 1].toShort()
            idx += capR[0].toShort(); idx += capR[i + 1].toShort(); idx += capR[i].toShort()
        }
        for (i in 0 until n) {
            val j = (i + 1) % n
            val (z0, y0) = sil[i]
            val (z1, y1) = sil[j]
            val dz = z1 - z0; val dy = y1 - y0
            val len = sqrt(dz * dz + dy * dy).coerceAtLeast(1e-5f)
            val ny = dz / len; val nz = -dy / len
            val a = vert(-half, y0, z0, 0f, ny, nz, 0f, 0f)
            val b = vert(half, y0, z0, 0f, ny, nz, 1f, 0f)
            val c = vert(half, y1, z1, 0f, ny, nz, 1f, 1f)
            val d = vert(-half, y1, z1, 0f, ny, nz, 0f, 1f)
            idx += a.toShort(); idx += b.toShort(); idx += c.toShort()
            idx += a.toShort(); idx += c.toShort(); idx += d.toShort()
        }
        return Mesh(pos.toFloatArray(), nrm.toFloatArray(), uv.toFloatArray(), idx.toShortArray())
    }

    fun staunton(kind: Kind, segs: Int): Mesh = when (kind) {
        Kind.P -> pawn(segs)
        Kind.N -> knight(segs)
        Kind.B -> bishop(segs)
        Kind.R -> rook(segs)
        Kind.Q -> queen(segs)
        Kind.K -> king(segs)
    }

    fun particleQuad() = quad(-0.5f, -0.5f, 0f, 0.5f, -0.5f, 0f, 0.5f, 0.5f, 0f, -0.5f, 0.5f, 0f, 0f, 0f, 1f)

    fun letterQuad() = quad(-0.12f, 0f, -0.12f, 0.12f, 0f, -0.12f, 0.12f, 0f, 0.12f, -0.12f, 0f, 0.12f, 0f, 1f, 0f)
}
