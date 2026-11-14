package com.tim.gamehub.games.heartbound

import com.tim.gamehub.R

data class CgDef(val id: String, val title: String, val sceneId: String, val drawable: Int)

object Gallery {
    val all = listOf(
        CgDef("kael_confess", "Kael's Confession", "kael_confess", R.drawable.cg_kael_confess),
        CgDef("mira_confess", "Mira's Confession", "mira_confess", R.drawable.cg_mira_confess),
        CgDef("rin_confess", "Rin's Confession", "rin_confess", R.drawable.cg_rin_confess),
        CgDef("sera_confess", "Sera's Confession", "sera_confess", R.drawable.cg_sera_confess),
        CgDef("yuna_confess", "Yuna's Confession", "yuna_confess", R.drawable.cg_yuna_confess),
        CgDef("nim_confess", "Nim's Confession", "nim_confess", R.drawable.cg_nim_confess),
        CgDef("kael_vow", "Kael's Vow", "kael_propose", R.drawable.cg_kael_vow),
        CgDef("mira_vow", "Mira's Vow", "mira_propose", R.drawable.cg_mira_vow),
        CgDef("rin_vow", "Rin's Vow", "rin_propose", R.drawable.cg_rin_vow),
        CgDef("sera_vow", "Sera's Vow", "sera_propose", R.drawable.cg_sera_vow),
        CgDef("yuna_vow", "Yuna's Vow", "yuna_propose", R.drawable.cg_yuna_vow),
        CgDef("nim_vow", "Nim's Vow", "nim_propose", R.drawable.cg_nim_vow),
        CgDef("kael_ending", "A Life with Kael", "kael_ending", R.drawable.cg_kael_ending),
        CgDef("mira_ending", "A Life with Mira", "mira_ending", R.drawable.cg_mira_ending),
        CgDef("rin_ending", "A Life with Rin", "rin_ending", R.drawable.cg_rin_ending),
        CgDef("sera_ending", "A Life with Sera", "sera_ending", R.drawable.cg_sera_ending),
        CgDef("yuna_ending", "A Life with Yuna", "yuna_ending", R.drawable.cg_yuna_ending),
        CgDef("nim_ending", "A Life with Nim", "nim_ending", R.drawable.cg_nim_ending),
        CgDef("plot_letter", "The Sealed Letter", "plot_letter", R.drawable.cg_plot_letter),
        CgDef("plot_climax", "The Visitor Withdraws", "plot_climax", R.drawable.cg_plot_climax),
        CgDef("ending_hall", "The Hall Endures", "ending_hall", R.drawable.cg_ending_hall),
        CgDef("ending_lanterns", "The Lamps Kept", "ending_lanterns", R.drawable.cg_ending_lanterns),
        CgDef("ending_iron", "Iron and Light", "ending_iron_light", R.drawable.cg_ending_iron),
        CgDef("ending_quiet", "A Quiet Life", "ending_quiet", R.drawable.cg_ending_quiet),
    )
    val byId = all.associateBy { it.id }
    val byScene = all.associateBy { it.sceneId }

    fun shouldShowAtStart(sceneId: String): Boolean {
        return sceneId.startsWith("plot_") || sceneId.startsWith("ending_") || sceneId.endsWith("_ending")
    }
}
