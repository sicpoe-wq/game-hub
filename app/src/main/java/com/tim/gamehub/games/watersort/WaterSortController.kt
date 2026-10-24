package com.tim.gamehub.games.watersort

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

interface ProgressStore { var current: Int; var unlocked: Int }

class MemoryProgress(override var current: Int = 1, override var unlocked: Int = 1) : ProgressStore

data class PourEvent(val id: Int, val from: Int, val to: Int, val color: Int, val amount: Int)

/**
 * Single long-lived owner of all Water Sort state. The UI holds ONE instance for the whole
 * game session and every handler calls into it, so no click handler can ever be bound to a
 * previous level's state objects. [load] resets every per-level field in one place.
 */
class WaterSortController(private val store: ProgressStore, startLevel: Int = store.current) {
    var level by mutableIntStateOf(1); private set
    var tubes by mutableStateOf<Tubes>(emptyList()); private set
    var selected by mutableStateOf<Int?>(null); private set
    var extraUsed by mutableStateOf(false); private set
    var moves by mutableIntStateOf(0); private set
    var lastPour by mutableStateOf<PourEvent?>(null); private set
    private val history = mutableStateListOf<Tubes>()
    private var pourId = 0
    private val cache = HashMap<Int, Tubes>()

    val canUndo get() = history.isNotEmpty() && !won
    val won get() = tubes.isNotEmpty() && WaterSort.isWon(tubes)
    val unlocked get() = maxOf(store.unlocked, level)

    init { load(startLevel) }

    fun load(l: Int) {
        level = l; store.current = l
        tubes = cache.getOrPut(l) { WaterSort.generate(l) }
        history.clear(); selected = null; extraUsed = false; moves = 0; lastPour = null
    }
    fun restart() = load(level)
    fun nextLevel() = load(level + 1)

    /** Returns true if a pour happened. */
    fun tap(i: Int): Boolean {
        if (won || i !in tubes.indices) return false
        val s = selected
        when {
            s == null -> if (tubes[i].isNotEmpty()) selected = i
            s == i -> selected = null
            else -> {
                val amount = WaterSort.pourAmount(tubes, s, i)
                if (amount > 0) {
                    val color = tubes[s].last()
                    history += tubes
                    tubes = WaterSort.pour(tubes, s, i)!!
                    moves++; selected = null
                    lastPour = PourEvent(++pourId, s, i, color, amount)
                    if (won) store.unlocked = maxOf(store.unlocked, level + 1)
                    return true
                }
                selected = if (tubes[i].isNotEmpty()) i else null
            }
        }
        return false
    }

    fun undo() {
        if (!canUndo) return
        tubes = history.removeAt(history.size - 1)
        moves = maxOf(0, moves - 1); selected = null; lastPour = null
    }

    fun addTube() {
        if (extraUsed || won) return
        history += tubes; tubes = tubes + listOf(emptyList()); extraUsed = true; selected = null
    }
}
