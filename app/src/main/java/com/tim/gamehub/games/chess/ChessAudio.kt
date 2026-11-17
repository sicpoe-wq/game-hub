package com.tim.gamehub.games.chess

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.tim.gamehub.R

class ChessSound {
    private var pool: SoundPool? = null
    private val ids = mutableMapOf<String, Int>()
    var enabled: () -> Boolean = { true }

    fun attach(ctx: Context) {
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val p = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        ids["move"] = p.load(ctx, R.raw.chess_move, 1)
        ids["capture"] = p.load(ctx, R.raw.chess_capture, 1)
        ids["check"] = p.load(ctx, R.raw.chess_check, 1)
        ids["castle"] = p.load(ctx, R.raw.chess_castle, 1)
        ids["promo"] = p.load(ctx, R.raw.chess_promo, 1)
        ids["end"] = p.load(ctx, R.raw.chess_end, 1)
        pool = p
    }

    fun play(name: String) {
        if (!enabled()) return
        val p = pool ?: return
        val id = ids[name] ?: ids["move"] ?: return
        p.play(id, 0.85f, 0.85f, 1, 0, 1f)
    }

    fun detach() {
        pool?.release()
        pool = null
        ids.clear()
    }
}
