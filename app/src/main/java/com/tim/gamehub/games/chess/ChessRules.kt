package com.tim.gamehub.games.chess

enum class Side { WHITE, BLACK;
    val other get() = if (this == WHITE) BLACK else WHITE
    val home get() = if (this == WHITE) 0 else 7
    val dir get() = if (this == WHITE) 1 else -1
}

enum class Kind { P, N, B, R, Q, K;
    val letter get() = name
}

data class Pc(val kind: Kind, val side: Side) {
    val fen: Char get() = if (side == Side.WHITE) kind.letter[0] else kind.letter[0].lowercaseChar()
}

/** 0 = a1 … 63 = h8. */
data class Move(
    val from: Int,
    val to: Int,
    val promo: Kind? = null,
    val castle: Boolean = false,
    val ep: Boolean = false,
    val captured: Kind? = null,
) {
    val uci: String get() = sqName(from) + sqName(to) + (promo?.letter?.lowercase() ?: "")
}

data class Pos(
    val sq: Array<Pc?>,
    val side: Side = Side.WHITE,
    val wk: Boolean = true,
    val wq: Boolean = true,
    val bk: Boolean = true,
    val bq: Boolean = true,
    val ep: Int? = null,
    val half: Int = 0,
    val full: Int = 1,
) {
    override fun equals(other: Any?) = other is Pos && fen().substringBeforeLast(' ').substringBeforeLast(' ') ==
        other.fen().substringBeforeLast(' ').substringBeforeLast(' ')
    override fun hashCode() = fenKey().hashCode()
    fun at(i: Int) = sq[i]
    fun king(s: Side): Int { for (i in 0..63) if (sq[i]?.kind == Kind.K && sq[i]?.side == s) return i; return -1 }
    fun fenKey(): String = fenParts()
    fun copyBoard() = copy(sq = sq.copyOf())
}

fun file(i: Int) = i and 7
fun rank(i: Int) = i shr 3
fun sqOf(f: Int, r: Int) = r * 8 + f
fun onBoard(f: Int, r: Int) = f in 0..7 && r in 0..7
fun sqName(i: Int) = "${'a' + file(i)}${rank(i) + 1}"
fun parseSq(n: String): Int = sqOf(n[0] - 'a', n[1] - '1')

fun startPos(): Pos = fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")

fun fromFen(fen: String): Pos {
    val p = fen.trim().split(Regex("\\s+"))
    val board = arrayOfNulls<Pc>(64)
    var i = 0
    for (c in p[0]) when {
        c == '/' -> {}
        c.isDigit() -> i += c - '0'
        else -> {
            val side = if (c.isUpperCase()) Side.WHITE else Side.BLACK
            val kind = Kind.valueOf(c.uppercase())
            val r = 7 - i / 8; val f = i % 8
            board[sqOf(f, r)] = Pc(kind, side)
            i++
        }
    }
    val side = if (p.getOrNull(1) == "b") Side.BLACK else Side.WHITE
    val cr = p.getOrNull(2) ?: "KQkq"
    val ep = p.getOrNull(3)?.let { if (it == "-") null else parseSq(it) }
    val half = p.getOrNull(4)?.toIntOrNull() ?: 0
    val full = p.getOrNull(5)?.toIntOrNull() ?: 1
    return Pos(board, side, 'K' in cr, 'Q' in cr, 'k' in cr, 'q' in cr, ep, half, full)
}

fun Pos.fenParts(): String = buildString {
    for (r in 7 downTo 0) {
        var empty = 0
        for (f in 0..7) {
            val pc = sq[sqOf(f, r)]
            if (pc == null) empty++ else {
                if (empty > 0) { append(empty); empty = 0 }
                append(pc.fen)
            }
        }
        if (empty > 0) append(empty)
        if (r > 0) append('/')
    }
    append(' ').append(if (side == Side.WHITE) 'w' else 'b').append(' ')
    val c = buildString {
        if (wk) append('K'); if (wq) append('Q'); if (bk) append('k'); if (bq) append('q')
    }
    append(c.ifEmpty { "-" }).append(' ')
    append(ep?.let { sqName(it) } ?: "-")
}

fun Pos.fen(): String = "${fenParts()} $half $full"

object Chess {
    private val N_D = intArrayOf(1, 2, 2, 1, -1, -2, -2, -1)
    private val N_R = intArrayOf(2, 1, -1, -2, -2, -1, 1, 2)
    private val K_D = intArrayOf(1, 1, 0, -1, -1, -1, 0, 1)
    private val K_R = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
    private val B_D = intArrayOf(1, 1, -1, -1)
    private val B_R = intArrayOf(1, -1, 1, -1)
    private val R_D = intArrayOf(1, 0, -1, 0)
    private val R_R = intArrayOf(0, 1, 0, -1)

    fun legal(pos: Pos): List<Move> {
        val raw = ArrayList<Move>(48)
        gen(pos, raw)
        val out = ArrayList<Move>(raw.size)
        for (m in raw) if (!inCheck(play(pos, m), pos.side)) out.add(m)
        return out
    }

    fun legalFrom(pos: Pos, from: Int): List<Move> = legal(pos).filter { it.from == from }

    fun inCheck(pos: Pos, side: Side = pos.side): Boolean { val k = pos.king(side); return k >= 0 && attacked(pos, k, side.other) }

    fun attacked(pos: Pos, target: Int, by: Side): Boolean {
        val tf = file(target); val tr = rank(target)
        // pawns
        val pd = if (by == Side.WHITE) -1 else 1 // from pawn toward target is opposite of pawn dir? attacker pawn is behind target
        // white pawn attacks up-right/up-left, so white pawn is at target + (-1,-1) or (-1,+1) in (file,rank) wait
        // white pawn on e4 (file 4, rank 3) attacks d5 and f5 (rank+1). So pawn is at (tf±1, tr-dir) where dir is pawn dir.
        val dir = by.dir
        for (df in intArrayOf(-1, 1)) {
            val f = tf - df; val r = tr - dir
            if (onBoard(f, r)) {
                val pc = pos.sq[sqOf(f, r)]
                if (pc?.kind == Kind.P && pc.side == by) return true
            }
        }
        // knights
        for (i in 0..7) {
            val f = tf + N_D[i]; val r = tr + N_R[i]
            if (onBoard(f, r)) {
                val pc = pos.sq[sqOf(f, r)]
                if (pc?.kind == Kind.N && pc.side == by) return true
            }
        }
        // king
        for (i in 0..7) {
            val f = tf + K_D[i]; val r = tr + K_R[i]
            if (onBoard(f, r)) {
                val pc = pos.sq[sqOf(f, r)]
                if (pc?.kind == Kind.K && pc.side == by) return true
            }
        }
        // bishops / queens
        if (slideHit(pos, tf, tr, B_D, B_R, by, true)) return true
        // rooks / queens
        if (slideHit(pos, tf, tr, R_D, R_R, by, false)) return true
        return false
    }

    private fun slideHit(pos: Pos, tf: Int, tr: Int, df: IntArray, dr: IntArray, by: Side, bishop: Boolean): Boolean {
        for (k in df.indices) {
            var f = tf + df[k]; var r = tr + dr[k]
            while (onBoard(f, r)) {
                val pc = pos.sq[sqOf(f, r)]
                if (pc != null) {
                    if (pc.side == by && (pc.kind == Kind.Q || pc.kind == if (bishop) Kind.B else Kind.R)) return true
                    break
                }
                f += df[k]; r += dr[k]
            }
        }
        return false
    }

    private fun gen(pos: Pos, out: MutableList<Move>) {
        for (from in 0..63) {
            val pc = pos.sq[from] ?: continue
            if (pc.side != pos.side) continue
            when (pc.kind) {
                Kind.P -> genPawn(pos, from, pc.side, out)
                Kind.N -> genLeap(pos, from, pc.side, N_D, N_R, out)
                Kind.K -> { genLeap(pos, from, pc.side, K_D, K_R, out); genCastle(pos, from, pc.side, out) }
                Kind.B -> genSlide(pos, from, pc.side, B_D, B_R, out)
                Kind.R -> genSlide(pos, from, pc.side, R_D, R_R, out)
                Kind.Q -> { genSlide(pos, from, pc.side, B_D, B_R, out); genSlide(pos, from, pc.side, R_D, R_R, out) }
            }
        }
    }

    private fun add(out: MutableList<Move>, from: Int, to: Int, captured: Kind?, promo: Boolean, ep: Boolean = false) {
        if (promo) {
            for (k in arrayOf(Kind.Q, Kind.R, Kind.B, Kind.N)) out.add(Move(from, to, k, captured = captured))
        } else out.add(Move(from, to, ep = ep, captured = captured))
    }

    private fun genPawn(pos: Pos, from: Int, side: Side, out: MutableList<Move>) {
        val f = file(from); val r = rank(from)
        val dir = side.dir
        val start = if (side == Side.WHITE) 1 else 6
        val last = if (side == Side.WHITE) 7 else 0
        val r1 = r + dir
        if (onBoard(f, r1) && pos.sq[sqOf(f, r1)] == null) {
            add(out, from, sqOf(f, r1), null, r1 == last)
            val r2 = r + dir * 2
            if (r == start && onBoard(f, r2) && pos.sq[sqOf(f, r2)] == null) {
                add(out, from, sqOf(f, r2), null, false)
            }
        }
        for (df in intArrayOf(-1, 1)) {
            val cf = f + df; val cr = r + dir
            if (!onBoard(cf, cr)) continue
            val to = sqOf(cf, cr)
            val hit = pos.sq[to]
            if (hit != null && hit.side != side) add(out, from, to, hit.kind, cr == last)
            else if (pos.ep == to) add(out, from, to, Kind.P, false, ep = true)
        }
    }

    private fun genLeap(pos: Pos, from: Int, side: Side, df: IntArray, dr: IntArray, out: MutableList<Move>) {
        val f = file(from); val r = rank(from)
        for (i in df.indices) {
            val nf = f + df[i]; val nr = r + dr[i]
            if (!onBoard(nf, nr)) continue
            val to = sqOf(nf, nr)
            val hit = pos.sq[to]
            if (hit == null || hit.side != side) out.add(Move(from, to, captured = hit?.kind))
        }
    }

    private fun genSlide(pos: Pos, from: Int, side: Side, df: IntArray, dr: IntArray, out: MutableList<Move>) {
        val f0 = file(from); val r0 = rank(from)
        for (k in df.indices) {
            var f = f0 + df[k]; var r = r0 + dr[k]
            while (onBoard(f, r)) {
                val to = sqOf(f, r)
                val hit = pos.sq[to]
                if (hit == null) out.add(Move(from, to))
                else {
                    if (hit.side != side) out.add(Move(from, to, captured = hit.kind))
                    break
                }
                f += df[k]; r += dr[k]
            }
        }
    }

    private fun genCastle(pos: Pos, from: Int, side: Side, out: MutableList<Move>) {
        if (inCheck(pos, side)) return
        val home = side.home
        if (from != sqOf(4, home)) return
        fun tryCastle(right: Boolean, empty: IntArray, pass: IntArray, rookFile: Int, toFile: Int) {
            if (!right) return
            val rookSq = sqOf(rookFile, home)
            val rook = pos.sq[rookSq]
            if (rook?.kind != Kind.R || rook.side != side) return
            for (fl in empty) if (pos.sq[sqOf(fl, home)] != null) return
            for (fl in pass) if (attacked(pos, sqOf(fl, home), side.other)) return
            out.add(Move(from, sqOf(toFile, home), castle = true))
        }
        if (side == Side.WHITE) {
            tryCastle(pos.wk, intArrayOf(5, 6), intArrayOf(5, 6), 7, 6)
            tryCastle(pos.wq, intArrayOf(1, 2, 3), intArrayOf(2, 3), 0, 2)
        } else {
            tryCastle(pos.bk, intArrayOf(5, 6), intArrayOf(5, 6), 7, 6)
            tryCastle(pos.bq, intArrayOf(1, 2, 3), intArrayOf(2, 3), 0, 2)
        }
    }

    fun play(pos: Pos, m: Move): Pos {
        val b = pos.sq.copyOf()
        val piece = b[m.from] ?: error("empty from ${m.uci}")
        var half = pos.half + 1
        if (piece.kind == Kind.P || m.captured != null) half = 0
        b[m.from] = null
        if (m.ep) {
            val cap = m.to + if (piece.side == Side.WHITE) -8 else 8
            b[cap] = null
        }
        if (m.castle) {
            when (m.to) {
                sqOf(6, 0) -> { b[sqOf(7, 0)] = null; b[sqOf(5, 0)] = Pc(Kind.R, Side.WHITE) }
                sqOf(2, 0) -> { b[sqOf(0, 0)] = null; b[sqOf(3, 0)] = Pc(Kind.R, Side.WHITE) }
                sqOf(6, 7) -> { b[sqOf(7, 7)] = null; b[sqOf(5, 7)] = Pc(Kind.R, Side.BLACK) }
                sqOf(2, 7) -> { b[sqOf(0, 7)] = null; b[sqOf(3, 7)] = Pc(Kind.R, Side.BLACK) }
            }
        }
        b[m.to] = if (m.promo != null) Pc(m.promo, piece.side) else piece
        var wk = pos.wk; var wq = pos.wq; var bk = pos.bk; var bq = pos.bq
        fun dropRook(sq: Int) {
            when (sq) {
                0 -> wq = false; 7 -> wk = false
                56 -> bq = false; 63 -> bk = false
            }
        }
        if (piece.kind == Kind.K) {
            if (piece.side == Side.WHITE) { wk = false; wq = false } else { bk = false; bq = false }
        }
        if (piece.kind == Kind.R) dropRook(m.from)
        if (m.captured == Kind.R) dropRook(m.to)
        // ep square
        val ep = if (piece.kind == Kind.P && kotlin.math.abs(rank(m.to) - rank(m.from)) == 2) {
            sqOf(file(m.from), (rank(m.from) + rank(m.to)) / 2)
        } else null
        val full = if (pos.side == Side.BLACK) pos.full + 1 else pos.full
        return Pos(b, piece.side.other, wk, wq, bk, bq, ep, half, full)
    }

    fun status(pos: Pos, historyKeys: List<String> = emptyList()): Status {
        if (insufficient(pos)) return Status.DRAW_MATERIAL
        val key = pos.fenKey()
        if (historyKeys.count { it == key } >= 3) return Status.DRAW_REPEAT
        if (pos.half >= 100) return Status.DRAW_50
        val moves = legal(pos)
        val check = inCheck(pos)
        return when {
            moves.isEmpty() && check -> Status.CHECKMATE
            moves.isEmpty() -> Status.STALEMATE
            check -> Status.CHECK
            else -> Status.PLAY
        }
    }

    fun insufficient(pos: Pos): Boolean {
        val pcs = pos.sq.filterNotNull()
        val nonKing = pcs.filter { it.kind != Kind.K }
        if (nonKing.isEmpty()) return true
        if (nonKing.size == 1 && nonKing[0].kind in setOf(Kind.N, Kind.B)) return true
        if (nonKing.size == 2 && nonKing.all { it.kind == Kind.B } && nonKing[0].side != nonKing[1].side) {
            val squares = (0..63).filter { pos.sq[it]?.kind == Kind.B }
            // same-color bishops cannot mate
            if (squares.size == 2) {
                val c1 = (file(squares[0]) + rank(squares[0])) and 1
                val c2 = (file(squares[1]) + rank(squares[1])) and 1
                if (c1 == c2) return true
            }
        }
        return false
    }

    fun perft(pos: Pos, depth: Int): Long {
        if (depth == 0) return 1
        val moves = legal(pos)
        if (depth == 1) return moves.size.toLong()
        var n = 0L
        for (m in moves) n += perft(play(pos, m), depth - 1)
        return n
    }

    fun san(pos: Pos, m: Move): String {
        if (m.castle) return if (file(m.to) == 6) "O-O" else "O-O-O"
        val piece = pos.sq[m.from]!!
        val dest = sqName(m.to)
        val cap = if (m.captured != null) "x" else ""
        val promo = m.promo?.let { "=${it.letter}" } ?: ""
        val body = if (piece.kind == Kind.P) {
            val src = if (m.captured != null) "${'a' + file(m.from)}" else ""
            "$src$cap$dest$promo"
        } else {
            val others = legal(pos).filter { it.to == m.to && it.from != m.from && pos.sq[it.from]?.kind == piece.kind }
            var dis = ""
            if (others.isNotEmpty()) {
                val sameFile = others.any { file(it.from) == file(m.from) }
                val sameRank = others.any { rank(it.from) == rank(m.from) }
                dis = when {
                    !sameFile -> "${'a' + file(m.from)}"
                    !sameRank -> "${rank(m.from) + 1}"
                    else -> sqName(m.from)
                }
            }
            "${piece.kind.letter}$dis$cap$dest$promo"
        }
        val next = play(pos, m)
        val st = status(next)
        val mark = when (st) {
            Status.CHECKMATE -> "#"
            Status.CHECK -> "+"
            else -> ""
        }
        return body + mark
    }

    fun parseUci(pos: Pos, uci: String): Move? {
        val u = uci.trim().lowercase()
        if (u.length !in 4..5) return null
        val from = parseSq(u.substring(0, 2))
        val to = parseSq(u.substring(2, 4))
        val promo = u.getOrNull(4)?.uppercase()?.let { Kind.valueOf(it.toString()) }
        return legal(pos).firstOrNull { it.from == from && it.to == to && it.promo == promo }
    }
}

enum class Status { PLAY, CHECK, CHECKMATE, STALEMATE, DRAW_50, DRAW_REPEAT, DRAW_MATERIAL, RESIGN, TIMEOUT, AGREE }

val Status.over get() = this != Status.PLAY && this != Status.CHECK
