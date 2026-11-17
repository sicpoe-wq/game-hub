package com.tim.gamehub.games.chess

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class ChessMode { CPU, LOCAL }
enum class ChessPage { MENU, PLAY, THEMES, SETTINGS }

data class ChessPrefs(
    val board: String = "wood",
    val pieces: String = "staunton",
    val coords: Boolean = true,
    val boardAnim: Boolean = true,
    val sound: Boolean = true,
    val autoFlip: Boolean = true,
    val clockSecs: Int = 0,
    val increment: Int = 0,
    val view3d: Boolean = false,
)

data class Hist(val move: Move, val san: String, val fenBefore: String)

interface ChessStore {
    fun loadPrefs(): ChessPrefs
    fun savePrefs(p: ChessPrefs)
    fun loadGame(): String?
    fun saveGame(raw: String?)
}

class MemoryChessStore(
    private var prefs: ChessPrefs = ChessPrefs(),
    private var game: String? = null,
) : ChessStore {
    override fun loadPrefs() = prefs
    override fun savePrefs(p: ChessPrefs) { prefs = p }
    override fun loadGame() = game
    override fun saveGame(raw: String?) { game = raw }
}

class PrefsChessStore(ctx: Context) : ChessStore {
    private val p = ctx.getSharedPreferences("chess", Context.MODE_PRIVATE)
    override fun loadPrefs() = ChessPrefs(
        board = p.getString("board", "wood") ?: "wood",
        pieces = p.getString("pieces", "staunton") ?: "staunton",
        coords = p.getBoolean("coords", true),
        boardAnim = p.getBoolean("board_anim", true),
        sound = p.getBoolean("sound", true),
        autoFlip = p.getBoolean("auto_flip", true),
        clockSecs = p.getInt("clock_secs", 0),
        increment = p.getInt("clock_inc", 0),
        view3d = p.getBoolean("view3d", false),
    )
    override fun savePrefs(prefs: ChessPrefs) {
        p.edit()
            .putString("board", prefs.board).putString("pieces", prefs.pieces)
            .putBoolean("coords", prefs.coords).putBoolean("board_anim", prefs.boardAnim)
            .putBoolean("sound", prefs.sound).putBoolean("auto_flip", prefs.autoFlip)
            .putInt("clock_secs", prefs.clockSecs).putInt("clock_inc", prefs.increment)
            .putBoolean("view3d", prefs.view3d)
            .apply()
    }
    override fun loadGame() = p.getString("game", null)
    override fun saveGame(raw: String?) {
        p.edit().putString("game", raw).apply()
    }
}

class ChessController(
    val store: ChessStore,
    val instant: Boolean = false,
    private val rng: Random = Random.Default,
) {
    var page by mutableStateOf(ChessPage.MENU)
    var prefs by mutableStateOf(store.loadPrefs())
    var pos by mutableStateOf(startPos())
    var startFen by mutableStateOf(startPos().fen())
    var history by mutableStateOf(listOf<Hist>())
    var keys by mutableStateOf(listOf(startPos().fenKey()))
    var mode by mutableStateOf(ChessMode.LOCAL)
    var level by mutableStateOf(CpuLevel.MEDIUM)
    var playerSide by mutableStateOf(Side.WHITE)
    var selected by mutableStateOf<Int?>(null)
    var pendingPromo by mutableStateOf<Move?>(null)
    var status by mutableStateOf(Status.PLAY)
    var winner by mutableStateOf<Side?>(null)
    var thinking by mutableStateOf(false)
    var wTime by mutableLongStateOf(0L)
    var bTime by mutableLongStateOf(0L)
    var incMs by mutableLongStateOf(0L)
    var animFrom by mutableStateOf<Int?>(null)
    var animTo by mutableStateOf<Int?>(null)
    var animPc by mutableStateOf<Pc?>(null)
    var animTick by mutableIntStateOf(0)
    var peekBoard by mutableStateOf(prefs.board)
    var peekPieces by mutableStateOf(prefs.pieces)
    var menuMsg by mutableStateOf<String?>(null)
    var afterThemes by mutableStateOf(ChessPage.MENU)
    var cam by mutableStateOf(Cam())
    var figs by mutableStateOf(startingFigures())
    var animFig by mutableStateOf<String?>(null)

    val hasSave get() = store.loadGame() != null
    val legal: List<Move> get() = if (status.over) emptyList() else Chess.legal(pos)
    val legalFrom: List<Move> get() = selected?.let { s -> legal.filter { it.from == s } } ?: emptyList()
    val flipped: Boolean get() = when {
        mode == ChessMode.CPU -> playerSide == Side.BLACK
        prefs.autoFlip -> pos.side == Side.BLACK
        else -> false
    }
    val lastMove: Move? get() = history.lastOrNull()?.move
    val checkSq: Int? get() = if (status == Status.CHECK || status == Status.CHECKMATE) {
        val k = pos.king(pos.side); if (k >= 0) k else null
    } else null

    private var thinkGen = 0
    private var job: Job? = null
    var scope: CoroutineScope? = null
        private set
    var onSound: ((String) -> Unit)? = null

    fun attach() {
        if (scope == null) scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
    fun detach() {
        thinkGen++
        job?.cancel()
        scope?.coroutineContext?.get(Job)?.cancel()
        scope = null
    }

    fun setPref(p: ChessPrefs) { prefs = p; store.savePrefs(p) }
    fun setBoard(id: String) { peekBoard = id; setPref(prefs.copy(board = id)) }
    fun setPieces(id: String) { peekPieces = id; setPref(prefs.copy(pieces = id)) }

    fun openThemes() { afterThemes = if (page == ChessPage.PLAY) ChessPage.PLAY else ChessPage.MENU; peekBoard = prefs.board; peekPieces = prefs.pieces; page = ChessPage.THEMES }
    fun closeThemes() {
        setPref(prefs.copy(board = peekBoard, pieces = peekPieces))
        page = afterThemes
    }
    fun openSettings() { page = ChessPage.SETTINGS }
    fun resetCamera() { cam = Cam() }
    fun setView3d(on: Boolean) { setPref(prefs.copy(view3d = on)) }
    fun openMenu() { page = ChessPage.MENU; thinking = false; thinkGen++ }

    fun newCpu(level: CpuLevel, playAs: Side, clockSecs: Int = prefs.clockSecs) {
        this.level = level
        boot(ChessMode.CPU, playAs, startPos(), clockSecs)
        maybeThink()
    }

    fun newLocal(clockSecs: Int = prefs.clockSecs) {
        boot(ChessMode.LOCAL, Side.WHITE, startPos(), clockSecs)
    }

    private fun boot(mode: ChessMode, playAs: Side, start: Pos, clockSecs: Int) {
        this.mode = mode
        playerSide = playAs
        pos = start
        startFen = start.fen()
        history = emptyList()
        keys = listOf(start.fenKey())
        selected = null
        pendingPromo = null
        status = Status.PLAY
        winner = null
        thinking = false
        thinkGen++
        incMs = prefs.increment * 1000L
        val t = (if (clockSecs > 0) clockSecs else 0) * 1000L
        wTime = t; bTime = t
        clearAnim()
        figs = startingFigures()
        animFig = null
        page = ChessPage.PLAY
        persist()
        if (mode == ChessMode.CPU && pos.side != playerSide) maybeThink()
    }

    fun continueGame(): Boolean {
        val raw = store.loadGame() ?: return false
        return decode(raw).also { if (it) { page = ChessPage.PLAY; maybeThink() } }
    }

    fun tap(sq: Int) {
        if (status.over || thinking || waitingCpu()) return
        if (pendingPromo != null) return
        val from = selected
        val mine = pos.at(sq)?.side == pos.side
        if (from != null) {
            val match = legalFrom.filter { it.to == sq }
            when {
                match.isEmpty() && mine -> selected = sq
                match.isEmpty() -> selected = null
                match.size == 1 && match[0].promo == null -> playUser(match[0])
                match.any { it.promo != null } -> pendingPromo = match.first().copy(promo = null)
                else -> playUser(match.first())
            }
        } else if (mine) {
            selected = sq
        }
    }

    fun drop(from: Int, to: Int): Boolean {
        if (status.over || thinking || waitingCpu()) return false
        val match = Chess.legalFrom(pos, from).filter { it.to == to }
        if (match.isEmpty()) { selected = null; return false }
        if (match.any { it.promo != null }) { selected = from; pendingPromo = match.first().copy(promo = null); return true }
        playUser(match.first())
        return true
    }

    fun pickPromo(kind: Kind) {
        val p = pendingPromo ?: return
        pendingPromo = null
        val m = Chess.legalFrom(pos, p.from).firstOrNull { it.to == p.to && it.promo == kind } ?: return
        playUser(m)
    }

    fun cancelPromo() { pendingPromo = null }

    private fun playUser(m: Move) {
        applyMove(m, animate = !instant)
        maybeThink()
    }

    private fun applyMove(m: Move, animate: Boolean) {
        val san = Chess.san(pos, m)
        val moving = pos.at(m.from)
        if (animate && moving != null) {
            animFrom = m.from; animTo = m.to; animPc = moving; animFig = figs.getOrNull(m.from); animTick++
        } else clearAnim()
        figs = applyFigureMove(figs, m, pos.side)
        history = history + Hist(m, san, pos.fen())
        // increment clock of the mover before switching
        if (prefs.clockSecs > 0 && incMs > 0) {
            if (pos.side == Side.WHITE) wTime += incMs else bTime += incMs
        }
        pos = Chess.play(pos, m)
        keys = keys + pos.fenKey()
        selected = null
        pendingPromo = null
        status = Chess.status(pos, keys)
        winner = if (status == Status.CHECKMATE) pos.side.other else null
        persist()
        onSound?.invoke(soundFor(m, status))
    }

    private fun soundFor(m: Move, st: Status): String = when {
        st == Status.CHECKMATE -> "end"
        st == Status.STALEMATE || st == Status.DRAW_50 || st == Status.DRAW_REPEAT || st == Status.DRAW_MATERIAL -> "end"
        st == Status.CHECK -> "check"
        m.promo != null -> "promo"
        m.castle -> "castle"
        m.captured != null -> "capture"
        else -> "move"
    }

    private fun waitingCpu() = mode == ChessMode.CPU && pos.side != playerSide && !status.over

    fun maybeThink() {
        if (instant) return
        if (mode != ChessMode.CPU || status.over || pos.side == playerSide) return
        thinking = true
        val snap = pos
        val lv = level
        val gen = ++thinkGen
        val sc = scope ?: return
        job?.cancel()
        job = sc.launch {
            val move = withContext(Dispatchers.Default) { Engine.search(snap, lv, rng) }
            if (gen != thinkGen) return@launch
            thinking = false
            if (move != null && !status.over) applyMove(move, animate = true)
        }
    }

    fun undo() {
        if (history.isEmpty() || thinking) return
        thinkGen++
        thinking = false
        // vs CPU: if last move was engine (we are to move as player), undo 2
        val n = when {
            mode != ChessMode.CPU -> 1
            pos.side == playerSide && history.size >= 2 -> 2
            else -> 1
        }.coerceAtMost(history.size)
        repeat(n) {
            val h = history.last()
            history = history.dropLast(1)
            pos = fromFen(h.fenBefore)
        }
        keys = keys.dropLast(n)
        figs = replayFigures(history)
        animFig = null
        if (keys.isEmpty()) keys = listOf(pos.fenKey())
        selected = null
        pendingPromo = null
        status = Chess.status(pos, keys)
        winner = null
        clearAnim()
        persist()
    }

    fun resign() {
        if (status.over) return
        thinkGen++; thinking = false
        val loser = if (mode == ChessMode.CPU) playerSide else pos.side
        status = Status.RESIGN
        winner = loser.other
        persist()
        onSound?.invoke("end")
    }

    fun agreeDraw() {
        if (status.over) return
        thinkGen++; thinking = false
        status = Status.AGREE
        winner = null
        persist()
        onSound?.invoke("end")
    }

    fun tick(ms: Long) {
        if (status.over || thinking || prefs.clockSecs <= 0) return
        if (pos.side == Side.WHITE) {
            wTime = (wTime - ms).coerceAtLeast(0)
            if (wTime == 0L) { status = Status.TIMEOUT; winner = Side.BLACK; persist(); onSound?.invoke("end") }
        } else {
            bTime = (bTime - ms).coerceAtLeast(0)
            if (bTime == 0L) { status = Status.TIMEOUT; winner = Side.WHITE; persist(); onSound?.invoke("end") }
        }
    }

    fun captured(side: Side): List<Kind> {
        val start = fromFen(startFen)
        val need = countKinds(start, side)
        val have = countKinds(pos, side)
        val out = ArrayList<Kind>()
        for (k in listOf(Kind.Q, Kind.R, Kind.B, Kind.N, Kind.P)) {
            val d = (need[k] ?: 0) - (have[k] ?: 0)
            repeat(d.coerceAtLeast(0)) { out.add(k) }
        }
        return out
    }

    private fun countKinds(p: Pos, side: Side): Map<Kind, Int> =
        p.sq.filterNotNull().filter { it.side == side }.groupingBy { it.kind }.eachCount()

    fun clearAnim() { animFrom = null; animTo = null; animPc = null }

    fun persist() { store.saveGame(encode()) }

    fun encode(): String {
        val moves = history.joinToString(",") { it.move.uci }
        return listOf(
            "v=1", "start=$startFen", "moves=$moves",
            "mode=${mode.name}", "level=${level.name}", "play=${playerSide.name}",
            "wtime=$wTime", "btime=$bTime", "inc=$incMs",
            "status=${status.name}", "winner=${winner?.name ?: ""}",
        ).joinToString("\n")
    }

    fun decode(raw: String): Boolean = try {
        val map = raw.lines().mapNotNull { line ->
            val i = line.indexOf('=')
            if (i < 0) null else line.substring(0, i) to line.substring(i + 1)
        }.toMap()
        val start = fromFen(map["start"] ?: startPos().fen())
        startFen = start.fen()
        var p = start
        val hist = mutableListOf<Hist>()
        val ks = mutableListOf(p.fenKey())
        val ucis = map["moves"].orEmpty().split(',').filter { it.isNotBlank() }
        for (u in ucis) {
            val m = Chess.parseUci(p, u) ?: return false
            hist += Hist(m, Chess.san(p, m), p.fen())
            p = Chess.play(p, m)
            ks += p.fenKey()
        }
        pos = p
        history = hist
        keys = ks
        mode = ChessMode.valueOf(map["mode"] ?: "LOCAL")
        level = runCatching { CpuLevel.valueOf(map["level"] ?: "MEDIUM") }.getOrDefault(CpuLevel.MEDIUM)
        playerSide = runCatching { Side.valueOf(map["play"] ?: "WHITE") }.getOrDefault(Side.WHITE)
        wTime = map["wtime"]?.toLongOrNull() ?: 0L
        bTime = map["btime"]?.toLongOrNull() ?: 0L
        incMs = map["inc"]?.toLongOrNull() ?: 0L
        status = runCatching { Status.valueOf(map["status"] ?: "PLAY") }.getOrDefault(Chess.status(p, ks))
        winner = map["winner"]?.ifBlank { null }?.let { Side.valueOf(it) }
        selected = null
        pendingPromo = null
        figs = replayFigures(hist)
        animFig = null
        clearAnim()
        true
    } catch (_: Exception) { false }

    /** Test / screenshot helper. */
    fun jumpPlay(
        fen: String = startPos().fen(),
        uci: List<String> = emptyList(),
        board: String? = null,
        pieces: String? = null,
        select: Int? = null,
        mode: ChessMode = ChessMode.LOCAL,
        page: ChessPage = ChessPage.PLAY,
    ) {
        instant // keep field used
        var p = fromFen(fen)
        startFen = if (uci.isEmpty()) p.fen() else startPos().fen()
        if (uci.isEmpty() && fen != startPos().fen()) startFen = fen
        val hist = mutableListOf<Hist>()
        val ks = mutableListOf(p.fenKey())
        if (uci.isNotEmpty()) {
            p = startPos(); startFen = p.fen()
            ks.clear(); ks += p.fenKey()
            for (u in uci) {
                val m = Chess.parseUci(p, u) ?: continue
                hist += Hist(m, Chess.san(p, m), p.fen())
                p = Chess.play(p, m)
                ks += p.fenKey()
            }
        }
        pos = p
        history = hist
        keys = ks
        this.mode = mode
        selected = select
        pendingPromo = null
        status = Chess.status(p, ks)
        winner = if (status == Status.CHECKMATE) p.side.other else null
        thinking = false
        if (board != null) setBoard(board)
        if (pieces != null) setPieces(pieces)
        figs = replayFigures(hist)
        animFig = null
        this.page = page
    }
}

fun replayFigures(history: List<Hist>): Array<String?> {
    var figs = startingFigures()
    // If the game did not start from the standard initial position, identities still start at home squares
    // and we only advance when the recorded moves exist from startFen == startPos.
    var p = startPos()
    if (history.isEmpty()) return figs
    // Rebuild from the first fenBefore if it is the standard start; otherwise leave default labels.
    val first = history.first().fenBefore
    if (first.substringBefore(" ") != startPos().fen().substringBefore(" ")) return startingFigures()
    for (h in history) {
        val side = p.side
        figs = applyFigureMove(figs, h.move, side)
        p = Chess.play(p, h.move)
    }
    return figs
}
