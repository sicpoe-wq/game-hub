package com.tim.gamehub.games.watersort

import kotlin.random.Random

/** Each tube is a list of color ids, bottom first. */
typealias Tubes = List<List<Int>>

data class LevelSpec(val colors: Int, val emptyTubes: Int = 2) { val tubes get() = colors + emptyTubes }

object WaterSort {
    const val CAP = 4
    const val MAX_COLORS = 12

    fun spec(level: Int) = LevelSpec((3 + (level - 1) / 3).coerceAtMost(MAX_COLORS))

    fun topRun(t: List<Int>): Int {
        if (t.isEmpty()) return 0
        var n = 0
        while (n < t.size && t[t.size - 1 - n] == t.last()) n++
        return n
    }

    fun canPour(s: Tubes, from: Int, to: Int): Boolean =
        from != to && s[from].isNotEmpty() && s[to].size < CAP && (s[to].isEmpty() || s[to].last() == s[from].last())

    fun pourAmount(s: Tubes, from: Int, to: Int): Int =
        if (!canPour(s, from, to)) 0 else minOf(topRun(s[from]), CAP - s[to].size)

    /** Returns new state, or null if the pour is illegal. */
    fun pour(s: Tubes, from: Int, to: Int): Tubes? {
        val n = pourAmount(s, from, to)
        if (n == 0) return null
        val c = s[from].last()
        return s.mapIndexed { i, t ->
            when (i) { from -> t.dropLast(n); to -> t + List(n) { c }; else -> t }
        }
    }

    fun isSolvedTube(t: List<Int>) = t.isEmpty() || (t.size == CAP && t.all { it == t[0] })
    fun isWon(s: Tubes) = s.all(::isSolvedTube)

    private fun key(s: Tubes) = s.map { it.joinToString(",") }.sorted().joinToString("|")

    /** DFS solver with visited-state pruning. Returns list of moves or null if none found within [limit] states. */
    fun solve(start: Tubes, limit: Int = 200_000): List<Pair<Int, Int>>? {
        val seen = HashSet<String>()
        val path = ArrayList<Pair<Int, Int>>()
        fun dfs(s: Tubes): Boolean {
            if (isWon(s)) return true
            if (!seen.add(key(s)) || seen.size > limit) return false
            val moves = ArrayList<Pair<Int, Int>>()
            for (f in s.indices) {
                val t0 = s[f]
                if (t0.isEmpty() || isSolvedTube(t0)) continue
                val uniform = topRun(t0) == t0.size
                var emptyTried = false
                for (t in s.indices) {
                    if (!canPour(s, f, t)) continue
                    if (s[t].isEmpty()) { if (uniform || emptyTried) continue; emptyTried = true; moves += f to t }
                    else moves.add(0, f to t)
                }
            }
            for (m in moves) {
                path += m
                if (dfs(pour(s, m.first, m.second)!!)) return true
                path.removeAt(path.size - 1)
            }
            return false
        }
        return if (dfs(start)) path.toList() else null
    }

    /** Deterministic, solver-verified puzzle for a level. */
    fun generate(level: Int): Tubes {
        val spec = spec(level)
        val rnd = Random(level * 7919L + 17)
        while (true) {
            val layers = (0 until spec.colors).flatMap { c -> List(CAP) { c } }.shuffled(rnd)
            val filled = layers.chunked(CAP)
            if (filled.any { t -> t.all { it == t[0] } }) continue      // no pre-solved tubes
            val s = filled + List(spec.emptyTubes) { emptyList() }
            if (solve(s) != null) return s
        }
    }
}
