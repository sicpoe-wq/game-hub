package com.tim.gamehub.games.wordsearch

import kotlin.random.Random

enum class Difficulty(val label: String, val size: Int, val words: Int, val diagonal: Boolean, val reversed: Boolean) {
    EASY("Easy", 8, 6, false, false), MEDIUM("Medium", 10, 8, true, false), HARD("Hard", 12, 10, true, true)
}

val THEMES: Map<String, List<String>> = mapOf(
    "Animals" to listOf("TIGER","ZEBRA","HORSE","MOUSE","EAGLE","SHARK","WHALE","PANDA","KOALA","OTTER","SNAKE","CAMEL","LION","BEAR","WOLF","GIRAFFE","MONKEY","RABBIT","TURTLE","PARROT"),
    "Food" to listOf("PIZZA","PASTA","BREAD","APPLE","MANGO","SUSHI","TACO","BURGER","CHEESE","SALAD","HONEY","GRAPE","LEMON","BACON","WAFFLE","DONUT","PEACH","CURRY","NOODLE","CANDY"),
    "Sports" to listOf("SOCCER","TENNIS","HOCKEY","GOLF","RUGBY","BOXING","SKIING","SURF","CHESS","POLO","ROWING","KARATE","CRICKET","BASEBALL","DIVING","CYCLING","ARCHERY","JUDO","SKATE","TRACK"),
    "Space" to listOf("PLANET","COMET","ORBIT","GALAXY","ROCKET","MARS","VENUS","SATURN","PLUTO","NEBULA","STAR","MOON","COSMOS","METEOR","JUPITER","SOLAR","ALIEN","CRATER","LUNAR","PULSAR"),
    "Colors" to listOf("RED","BLUE","GREEN","YELLOW","PURPLE","ORANGE","PINK","BROWN","BLACK","WHITE","GRAY","VIOLET","INDIGO","CYAN","MAROON","BEIGE","TEAL","OLIVE","CORAL","AMBER"),
)

data class Cell(val r: Int, val c: Int)
data class Placed(val word: String, val cells: List<Cell>)
class Puzzle(val grid: List<CharArray>, val words: List<Placed>) { val size get() = grid.size }

object Generator {
    fun generate(theme: String, d: Difficulty, rnd: Random = Random.Default): Puzzle {
        val n = d.size
        val dirs = buildList {
            add(0 to 1); add(1 to 0)
            if (d.diagonal) { add(1 to 1); add(-1 to 1) }
            if (d.reversed) { add(0 to -1); add(-1 to 0); add(-1 to -1); add(1 to -1) }
        }
        repeat(50) {
            val g = Array(n) { CharArray(n) { ' ' } }
            val placed = mutableListOf<Placed>()
            for (w in THEMES.getValue(theme).filter { it.length <= n }.shuffled(rnd)) {
                if (placed.size >= d.words) break
                for (t in 0 until 200) {
                    val (dr, dc) = dirs.random(rnd)
                    val r0 = rnd.nextInt(n); val c0 = rnd.nextInt(n)
                    val cells = w.indices.map { Cell(r0 + dr * it, c0 + dc * it) }
                    if (cells.all { it.r in 0 until n && it.c in 0 until n } &&
                        cells.withIndex().all { (i, c) -> g[c.r][c.c] == ' ' || g[c.r][c.c] == w[i] }) {
                        cells.forEachIndexed { i, c -> g[c.r][c.c] = w[i] }
                        placed += Placed(w, cells); break
                    }
                }
            }
            if (placed.size >= d.words) {
                for (r in 0 until n) for (c in 0 until n) if (g[r][c] == ' ') g[r][c] = 'A' + rnd.nextInt(26)
                return Puzzle(g.toList(), placed)
            }
        }
        error("could not generate")
    }

    /** Cells on a straight line (horizontal/vertical/diagonal) from a to b, or null. */
    fun line(a: Cell, b: Cell): List<Cell>? {
        val dr = b.r - a.r; val dc = b.c - a.c
        if (dr != 0 && dc != 0 && kotlin.math.abs(dr) != kotlin.math.abs(dc)) return null
        val len = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
        val sr = Integer.signum(dr); val sc = Integer.signum(dc)
        return (0..len).map { Cell(a.r + sr * it, a.c + sc * it) }
    }

    /** Selection matches a word if same cells forward or backward. */
    fun match(sel: List<Cell>, words: List<Placed>): Placed? =
        words.firstOrNull { it.cells == sel || it.cells == sel.reversed() }
}
