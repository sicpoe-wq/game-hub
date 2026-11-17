package com.tim.gamehub

import com.tim.gamehub.games.chess.*
import org.junit.Assert.*
import org.junit.Test

class ChessTest {
    private fun p(fen: String) = fromFen(fen)

    @Test fun startPerft() {
        val s = startPos()
        assertEquals(20, Chess.perft(s, 1))
        assertEquals(400, Chess.perft(s, 2))
        assertEquals(8902, Chess.perft(s, 3))
        assertEquals(197281, Chess.perft(s, 4))
    }

    @Test fun kiwipetePerft() {
        val s = p("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -")
        assertEquals(48, Chess.perft(s, 1))
        assertEquals(2039, Chess.perft(s, 2))
        assertEquals(97862, Chess.perft(s, 3))
    }

    @Test fun position3Perft() {
        val s = p("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -")
        assertEquals(14, Chess.perft(s, 1))
        assertEquals(191, Chess.perft(s, 2))
        assertEquals(2812, Chess.perft(s, 3))
        assertEquals(43238, Chess.perft(s, 4))
    }

    @Test fun position5Perft() {
        val s = p("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8")
        assertEquals(44, Chess.perft(s, 1))
        assertEquals(1486, Chess.perft(s, 2))
        assertEquals(62379, Chess.perft(s, 3))
    }

    @Test fun fenRoundTripAndEnPassant() {
        val s = startPos()
        val e4 = Chess.parseUci(s, "e2e4")!!
        val after = Chess.play(s, e4)
        assertEquals("e3", sqName(after.ep!!))
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", after.fen())
        val back = fromFen(after.fen())
        assertEquals(after.fen(), back.fen())
        // black d7d5 then white e4xd5 ep after d5? actually after e4, black e7e5 no.
        // classic: white e4, black d5, white e5, black f5, white exf6
        var pos = s
        for (u in listOf("e2e4", "d7d5", "e4e5", "f7f5")) pos = Chess.play(pos, Chess.parseUci(pos, u)!!)
        assertEquals("f6", sqName(pos.ep!!))
        val cap = Chess.parseUci(pos, "e5f6")!!
        assertTrue(cap.ep)
        pos = Chess.play(pos, cap)
        assertNull(pos.at(parseSq("f5")))
        assertEquals(Kind.P, pos.at(parseSq("f6"))?.kind)
    }

    @Test fun castlingAndRights() {
        val s = p("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        assertNotNull(Chess.parseUci(s, "e1g1"))
        assertNotNull(Chess.parseUci(s, "e1c1"))
        val ooo = Chess.play(s, Chess.parseUci(s, "e1c1")!!)
        assertEquals(Kind.K, ooo.at(parseSq("c1"))?.kind)
        assertEquals(Kind.R, ooo.at(parseSq("d1"))?.kind)
        assertFalse(ooo.wk || ooo.wq)
        // cannot castle out of check
        val chk = p("r3k2r/8/8/8/8/8/4r3/R3K2R w KQkq - 0 1")
        assertNull(Chess.parseUci(chk, "e1g1"))
        // cannot castle through check
        val thru = p("r3k2r/8/8/8/8/8/5r2/R3K2R w KQkq - 0 1")
        assertNull(Chess.parseUci(thru, "e1g1"))
        assertNotNull(Chess.parseUci(thru, "e1c1"))
    }

    @Test fun promotionCheckmateStalemate() {
        val promo = p("4k3/P7/8/8/8/8/8/4K3 w - - 0 1")
        val moves = Chess.legal(promo).filter { it.from == parseSq("a7") }
        assertEquals(setOf(Kind.Q, Kind.R, Kind.B, Kind.N), moves.map { it.promo }.toSet())
        val q = moves.first { it.promo == Kind.Q }
        assertEquals("a8=Q+", Chess.san(promo, q))
        // fool's mate
        var pos = startPos()
        for (u in listOf("f2f3", "e7e5", "g2g4", "d8h4")) pos = Chess.play(pos, Chess.parseUci(pos, u)!!)
        assertEquals(Status.CHECKMATE, Chess.status(pos))
        // stalemate
        val stale = p("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")
        assertEquals(Status.STALEMATE, Chess.status(stale))
    }

    @Test fun drawsFiftyThreefoldMaterial() {
        assertTrue(Chess.insufficient(p("8/8/8/8/8/8/8/4K2k w - - 0 1")))
        assertTrue(Chess.insufficient(p("8/8/8/8/8/8/8/3NK2k w - - 0 1")))
        assertTrue(Chess.insufficient(p("8/8/8/8/8/8/8/3BK2k w - - 0 1")))
        assertFalse(Chess.insufficient(p("8/8/8/8/8/8/8/3QK2k w - - 0 1")))
        val fifty = p("8/8/8/8/8/8/8/4K2k w - - 100 80")
        // two kings: insufficient takes priority
        assertEquals(Status.DRAW_MATERIAL, Chess.status(fifty))
        val fifty2 = p("4k3/4p3/8/8/8/8/4P3/4K3 w - - 100 80")
        assertEquals(Status.DRAW_50, Chess.status(fifty2))
        // threefold: start, knights out and back
        var pos = startPos()
        val keys = mutableListOf(pos.fenKey())
        for (u in listOf("g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8")) {
            pos = Chess.play(pos, Chess.parseUci(pos, u)!!)
            keys += pos.fenKey()
        }
        assertEquals(Status.DRAW_REPEAT, Chess.status(pos, keys))
    }

    @Test fun sanDisambiguation() {
        val s = p("8/8/8/8/8/8/8/R3K2R w KQ - 0 1")
        // both rooks can go to a? wait two rooks on a1 and h1, both can go to... d1? only queenside path
        val d1 = Chess.parseUci(s, "a1d1")!!
        // h1 rook also can go to d1? yes along rank if empty - e1 has king, so no.
        val a3 = Chess.parseUci(s, "a1a3")!!
        assertEquals("Ra3", Chess.san(s, a3))
        val two = p("8/8/8/8/8/8/8/R3K1N1 w Q - 0 1")
        // skip
        val knights = p("4k3/8/8/3N1N2/8/8/8/4K3 w - - 0 1")
        val m = Chess.parseUci(knights, "d5e3")!!
        val m2 = Chess.parseUci(knights, "f5e3")!!
        assertEquals("Nde3", Chess.san(knights, m))
        assertEquals("Nfe3", Chess.san(knights, m2))
    }

    @Test fun engineTakesHangingQueen() {
        val s = p("4k3/8/8/8/8/8/7q/4K2R w - - 0 1")
        val m = Engine.search(s, CpuLevel.MEDIUM, kotlin.random.Random(1))!!
        assertEquals("h1h2", m.uci) // rook takes the hanging queen, only sensible move besides walking the king
        // actually king can also move; queen hangs on h2. Rook xh2 is best.
        assertEquals(parseSq("h2"), m.to)
    }

    @Test fun saveRoundTripAndUndo() {
        val store = MemoryChessStore()
        val c = ChessController(store, instant = true)
        c.newLocal()
        c.tap(parseSq("e2")); c.tap(parseSq("e4"))
        c.tap(parseSq("e7")); c.tap(parseSq("e5"))
        assertEquals("e4", c.history[0].san)
        assertEquals("e5", c.history[1].san)
        val raw = c.encode()
        val c2 = ChessController(MemoryChessStore(), instant = true)
        assertTrue(c2.decode(raw))
        assertEquals(c.pos.fen(), c2.pos.fen())
        assertEquals(2, c2.history.size)
        c2.undo()
        assertEquals(1, c2.history.size)
        assertEquals("e2e4", Chess.parseUci(startPos(), "e2e4")!!.uci)
    }
}
