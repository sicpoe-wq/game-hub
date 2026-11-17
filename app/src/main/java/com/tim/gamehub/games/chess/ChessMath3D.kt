package com.tim.gamehub.games.chess

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Column-major 4x4, matching OpenGL. */
class Mat4(val m: FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)) {
    operator fun times(o: Mat4): Mat4 {
        val r = FloatArray(16)
        for (c in 0..3) for (row in 0..3) {
            var s = 0f
            for (k in 0..3) s += m[k * 4 + row] * o.m[c * 4 + k]
            r[c * 4 + row] = s
        }
        return Mat4(r)
    }
    fun transform(v: Vec3, w: Float = 1f): Vec3 {
        val x = m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12] * w
        val y = m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13] * w
        val z = m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14] * w
        val ww = m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15] * w
        return if (w == 0f || kotlin.math.abs(ww) < 1e-8f) Vec3(x, y, z) else Vec3(x / ww, y / ww, z / ww)
    }
    fun normalMatrix(): FloatArray {
        // upper 3x3 inverse-transpose of the model matrix (assuming no skew we can invert 3x3)
        val a = m
        val c00 = a[5] * a[10] - a[9] * a[6]
        val c01 = a[8] * a[6] - a[4] * a[10]
        val c02 = a[4] * a[9] - a[8] * a[5]
        val c10 = a[9] * a[2] - a[1] * a[10]
        val c11 = a[0] * a[10] - a[8] * a[2]
        val c12 = a[8] * a[1] - a[0] * a[9]
        val c20 = a[1] * a[6] - a[5] * a[2]
        val c21 = a[4] * a[2] - a[0] * a[6]
        val c22 = a[0] * a[5] - a[4] * a[1]
        val det = a[0] * c00 + a[1] * c01 + a[2] * c02
        val inv = if (kotlin.math.abs(det) < 1e-8f) 1f else 1f / det
        return floatArrayOf(
            c00 * inv, c01 * inv, c02 * inv,
            c10 * inv, c11 * inv, c12 * inv,
            c20 * inv, c21 * inv, c22 * inv,
        )
    }
}

fun matIdentity() = Mat4()

fun matTranslate(x: Float, y: Float, z: Float) = Mat4(floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    x, y, z, 1f,
))

fun matScale(x: Float, y: Float, z: Float) = Mat4(floatArrayOf(
    x, 0f, 0f, 0f,
    0f, y, 0f, 0f,
    0f, 0f, z, 0f,
    0f, 0f, 0f, 1f,
))

fun matRotateY(a: Float): Mat4 {
    val c = cos(a); val s = sin(a)
    return Mat4(floatArrayOf(
        c, 0f, -s, 0f,
        0f, 1f, 0f, 0f,
        s, 0f, c, 0f,
        0f, 0f, 0f, 1f,
    ))
}

fun matRotateZ(a: Float): Mat4 {
    val c = cos(a); val s = sin(a)
    return Mat4(floatArrayOf(
        c, s, 0f, 0f,
        -s, c, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f,
    ))
}

fun matRotateX(a: Float): Mat4 {
    val c = cos(a); val s = sin(a)
    return Mat4(floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, c, s, 0f,
        0f, -s, c, 0f,
        0f, 0f, 0f, 1f,
    ))
}

fun matLookAt(eye: Vec3, target: Vec3, up0: Vec3): Mat4 {
    val f = (target - eye).norm()
    val s = f.cross(up0).norm()
    val u = s.cross(f)
    val rot = Mat4(floatArrayOf(
        s.x, u.x, -f.x, 0f,
        s.y, u.y, -f.y, 0f,
        s.z, u.z, -f.z, 0f,
        0f, 0f, 0f, 1f,
    ))
    return rot * matTranslate(-eye.x, -eye.y, -eye.z)
}

fun matPerspective(fovy: Float, aspect: Float, near: Float, far: Float): Mat4 {
    val t = tan(fovy / 2f)
    val a = 1f / (aspect * t)
    val b = 1f / t
    val nf = 1f / (near - far)
    return Mat4(floatArrayOf(
        a, 0f, 0f, 0f,
        0f, b, 0f, 0f,
        0f, 0f, (far + near) * nf, -1f,
        0f, 0f, (2f * far * near) * nf, 0f,
    ))
}

fun camEye(cam: Cam, target: Vec3 = Vec3(4f, 0.25f, 4f)): Vec3 {
    val c = cam.clamped()
    return Vec3(
        target.x + sin(c.yaw) * cos(c.pitch) * c.dist,
        target.y + sin(c.pitch) * c.dist,
        target.z + cos(c.yaw) * cos(c.pitch) * c.dist,
    )
}

fun backdropRes(boardId: String, pieceId: String): String = when {
    pieceId == "political_parody" -> "bg_capitol"
    pieceId == "political_likeness" -> "bg_oval"
    else -> when (boardId) {
        "neon" -> "bg_neon"
        "sakura" -> "bg_sakura"
        "fantasy" -> "bg_enchanted"
        "lumenreach" -> "bg_lumenreach"
        "marble" -> "bg_marble"
        "tournament" -> "bg_tournament"
        else -> "bg_study"
    }
}

data class SquareTex(val light: String, val dark: String, val frame: String, val trim: String)

fun boardTextures(boardId: String): SquareTex = when (boardId) {
    "marble" -> SquareTex("tex_marble_lt", "tex_marble_dk", "tex_stone_fr", "tex_metal")
    "tournament" -> SquareTex("tex_felt_lt", "tex_felt_dk", "tex_felt_fr", "tex_metal")
    "neon" -> SquareTex("tex_neon_lt", "tex_neon_dk", "tex_metal", "tex_metal")
    "fantasy" -> SquareTex("tex_enchant_lt", "tex_enchant_dk", "tex_wood_fr", "tex_gold")
    "sakura" -> SquareTex("tex_sakura_lt", "tex_sakura_dk", "tex_wood_fr", "tex_gold")
    "lumenreach" -> SquareTex("tex_lumen_lt", "tex_lumen_dk", "tex_stone_fr", "tex_gold")
    else -> SquareTex("tex_wood_lt", "tex_wood_dk", "tex_wood_fr", "tex_gold")
}
