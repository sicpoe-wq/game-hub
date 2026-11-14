package com.tim.gamehub.games.heartbound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.tim.gamehub.R

/** Original looping themes and short cues. No-ops until [attach] is called (tests stay silent). */
object HbSound {
    private var app: Context? = null
    private var music: MediaPlayer? = null
    private var pool: SoundPool? = null
    private val sounds = mutableMapOf<String, Int>()
    private var track: String? = null
    private var last: HbSettings = HbSettings()

    fun attach(ctx: Context) {
        if (app != null) return
        try {
            val c = ctx.applicationContext
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
            val p = pool!!
            sounds["tap"] = p.load(c, R.raw.sfx_tap, 1)
            sounds["hit"] = p.load(c, R.raw.sfx_hit, 1)
            sounds["heal"] = p.load(c, R.raw.sfx_heal, 1)
            sounds["level"] = p.load(c, R.raw.sfx_level, 1)
            sounds["gift"] = p.load(c, R.raw.sfx_gift, 1)
            sounds["ach"] = p.load(c, R.raw.sfx_ach, 1)
            app = c
        } catch (_: Exception) {
            app = null
            pool = null
            sounds.clear()
        }
    }

    fun detach() {
        try { music?.release() } catch (_: Exception) {}
        music = null
        track = null
        try { pool?.release() } catch (_: Exception) {}
        pool = null
        sounds.clear()
        app = null
    }

    private fun level(n: Int, muted: Boolean) = if (muted) 0f else n.coerceIn(0, 10) / 10f

    fun themeFor(screen: HbScreen, play: ScenePlay?, started: Boolean = true): String {
        if (screen == HbScreen.Title) return "title"
        if (!started && screen != HbScreen.Battle) return "title"
        if (screen == HbScreen.Battle) return "battle"
        if (screen == HbScreen.Evening) return "evening"
        if (screen == HbScreen.Dialogue) {
            val id = play?.scene?.id ?: ""
            val kind = play?.kind ?: ""
            if (id.contains("ending") || kind == "ending") return "ending"
            if (id.contains("confess") || id.contains("propose") || kind in setOf("date", "visit", "birthday")) return "romance"
            if (play?.scene?.bg == "tavern") return "evening"
        }
        return "hall"
    }

    fun cue(settings: HbSettings, screen: HbScreen, play: ScenePlay?, started: Boolean = true) {
        last = settings
        start(themeFor(screen, play, started), settings)
    }

    fun start(theme: String, settings: HbSettings) {
        val ctx = app ?: return
        last = settings
        val v = level(settings.musicVol, settings.muted)
        if (theme == track && music != null) {
            try {
                music?.setVolume(v, v)
                if (v <= 0f) music?.pause()
                else if (music?.isPlaying == false) music?.start()
            } catch (_: Exception) {}
            return
        }
        try { music?.release() } catch (_: Exception) {}
        music = null
        val res = when (theme) {
            "evening" -> R.raw.mus_evening
            "battle" -> R.raw.mus_battle
            "romance" -> R.raw.mus_romance
            "ending" -> R.raw.mus_ending
            "title" -> R.raw.mus_title
            else -> R.raw.mus_hall
        }
        track = theme
        try {
            music = MediaPlayer.create(ctx, res)?.apply {
                isLooping = true
                setVolume(v, v)
                if (v > 0f) start()
            }
        } catch (_: Exception) {
            music = null
            track = null
        }
    }

    fun sfx(kind: String, settings: HbSettings = last) {
        last = settings
        val v = level(settings.sfxVol, settings.muted)
        if (v <= 0f) return
        val id = sounds[kind] ?: return
        try { pool?.play(id, v, v, 1, 0, 1f) } catch (_: Exception) {}
    }

    fun tap() = sfx("tap")
}
