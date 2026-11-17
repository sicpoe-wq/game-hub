package com.tim.gamehub.games.chess

import kotlin.math.max
import kotlin.random.Random

enum class CpuLevel(val label: String, val depth: Int) {
    EASY("Easy", 2), MEDIUM("Medium", 3), HARD("Hard", 4)
}

object Engine {
    private const val INF = 40_000
    private const val MATE = 30_000
    private val MAT = intArrayOf(100, 320, 330, 500, 900, 20_000) // P..K

    // Midgame PST, a1..h8, white perspective.
    private val PST = arrayOf(
        // P
        intArrayOf(
             0,  0,  0,  0,  0,  0,  0,  0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
             5,  5, 10, 25, 25, 10,  5,  5,
             0,  0,  0, 20, 20,  0,  0,  0,
             5, -5,-10,  0,  0,-10, -5,  5,
             5, 10, 10,-20,-20, 10, 10,  5,
             0,  0,  0,  0,  0,  0,  0,  0,
        ),
        // N
        intArrayOf(
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50,
        ),
        // B
        intArrayOf(
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-10,-10,-10,-10,-10,-20,
        ),
        // R
        intArrayOf(
             0,  0,  0,  0,  0,  0,  0,  0,
             5, 10, 10, 10, 10, 10, 10,  5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
             0,  0,  0,  5,  5,  0,  0,  0,
        ),
        // Q
        intArrayOf(
            -20,-10,-10, -5, -5,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5,  5,  5,  5,  0,-10,
             -5,  0,  5,  5,  5,  5,  0, -5,
              0,  0,  5,  5,  5,  5,  0, -5,
            -10,  5,  5,  5,  5,  5,  0,-10,
            -10,  0,  5,  0,  0,  0,  0,-10,
            -20,-10,-10, -5, -5,-10,-10,-20,
        ),
        // K mid
        intArrayOf(
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -20,-30,-30,-40,-40,-30,-30,-20,
            -10,-20,-20,-20,-20,-20,-20,-10,
             20, 20,  0,  0,  0,  0, 20, 20,
             20, 30, 10,  0,  0, 10, 30, 20,
        ),
    )

    fun search(pos: Pos, level: CpuLevel, rng: Random = Random.Default): Move? {
        val moves = ordered(Chess.legal(pos))
        if (moves.isEmpty()) return null
        var best = -INF
        val scored = ArrayList<Pair<Move, Int>>(moves.size)
        for (m in moves) {
            var s = -alpha(Chess.play(pos, m), level.depth - 1, -INF, INF, 1)
            if (level == CpuLevel.EASY) s += rng.nextInt(-35, 36)
            scored.add(m to s)
            if (s > best) best = s
        }
        val window = if (level == CpuLevel.EASY) 40 else 0
        val cands = scored.filter { it.second >= best - window }
        return cands[rng.nextInt(cands.size)].first
    }

    private fun alpha(pos: Pos, depth: Int, alpha0: Int, beta0: Int, ply: Int): Int {
        if (Chess.insufficient(pos) || pos.half >= 100) return 0
        if (depth <= 0) return quiesce(pos, alpha0, beta0, 4)
        val moves = ordered(Chess.legal(pos))
        if (moves.isEmpty()) return if (Chess.inCheck(pos)) -MATE + ply else 0
        var alpha = alpha0
        var best = -INF
        for (m in moves) {
            val s = -alpha(Chess.play(pos, m), depth - 1, -beta0, -alpha, ply + 1)
            if (s > best) best = s
            if (best > alpha) alpha = best
            if (alpha >= beta0) break
        }
        return best
    }

    private fun quiesce(pos: Pos, alpha0: Int, beta0: Int, qd: Int): Int {
        val stand = eval(pos)
        if (qd == 0) return stand
        var alpha = max(alpha0, stand)
        if (alpha >= beta0) return alpha
        val inCheck = Chess.inCheck(pos)
        val moves = ordered(Chess.legal(pos)).let { all ->
            if (inCheck) all else all.filter { it.captured != null || it.promo != null }
        }
        if (inCheck && moves.isEmpty()) return -MATE
        for (m in moves) {
            val s = -quiesce(Chess.play(pos, m), -beta0, -alpha, qd - 1)
            if (s > alpha) alpha = s
            if (alpha >= beta0) break
        }
        return alpha
    }

    fun eval(pos: Pos): Int {
        var s = 0
        for (i in 0..63) {
            val pc = pos.sq[i] ?: continue
            val v = MAT[pc.kind.ordinal] + PST[pc.kind.ordinal][if (pc.side == Side.WHITE) i xor 56 else i]
            s += if (pc.side == Side.WHITE) v else -v
        }
        return if (pos.side == Side.WHITE) s else -s
    }

    private fun ordered(moves: List<Move>): List<Move> = moves.sortedByDescending { m ->
        var s = 0
        if (m.promo != null) s += 800 + MAT[m.promo.ordinal]
        if (m.captured != null) s += 10 * MAT[m.captured.ordinal]
        if (m.castle) s += 40
        s
    }
}
