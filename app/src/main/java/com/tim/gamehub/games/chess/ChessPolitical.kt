package com.tim.gamehub.games.chess

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import com.tim.gamehub.BuildConfig

enum class Party { REPUBLICAN, DEMOCRAT }

data class Figure(
    val id: String,
    val name: String,
    val role: String,
    val party: Party,
    val kind: Kind,
    /** 0–7 for pawns (a–h file on that side's pawn rank); 0 or 1 for doubled pieces (rook/knight/bishop). Ignored for K/Q. */
    val slot: Int = 0,
    val source: String,
)

object PoliticalRoster {
    val all: List<Figure> = listOf(
        // Republicans (White by default)
        Figure("trump", "Donald J. Trump", "President of the United States", Party.REPUBLICAN, Kind.K,
            source = "USA.gov, current president as of Nov 2026; sworn in Jan 20, 2025. White House releases, Nov 2026."),
        Figure("melania", "Melania Trump", "First Lady of the United States", Party.REPUBLICAN, Kind.Q,
            source = "White House, First Lady statements dated Nov 5–12, 2026."),
        Figure("vance", "JD Vance", "Vice President of the United States", Party.REPUBLICAN, Kind.R, 0,
            source = "USA.gov, current vice president as of Nov 2026; sworn in Jan 20, 2025."),
        Figure("johnson", "Mike Johnson", "Speaker of the House (sitting, 119th Congress)", Party.REPUBLICAN, Kind.R, 1,
            source = "house.gov/leadership, Nov 2026: still the sitting Speaker during the lame-duck session."),
        Figure("thune", "John Thune", "Senate Republican Leader", Party.REPUBLICAN, Kind.B, 0,
            source = "CBS News / Axios, Nov 10, 2026: Senate Republicans reelected Thune as their leader."),
        Figure("jordan", "Jim Jordan", "House Republican Leader-elect (120th Congress)", Party.REPUBLICAN, Kind.B, 1,
            source = "CBS News, Nov 2026: Jordan defeated Steve Scalise 106–73 to lead House Republicans in the next Congress."),
        Figure("barrasso", "John Barrasso", "Senate Republican Whip", Party.REPUBLICAN, Kind.N, 0,
            source = "CBS News, Nov 2026: Senate Republicans reelected Barrasso as whip."),
        Figure("desantis", "Ron DeSantis", "Governor of Florida (sitting through Jan 5, 2027)", Party.REPUBLICAN, Kind.N, 1,
            source = "Tallahassee Democrat / NBC Miami, Nov 2026: DeSantis remains governor until Jan 5, 2027; Byron Donalds is governor-elect."),
        Figure("cruz", "Ted Cruz", "U.S. Senator, Texas", Party.REPUBLICAN, Kind.P, 0,
            source = "Not up in 2026; continuing Senate term after 2024 reelection."),
        Figure("hawley", "Josh Hawley", "U.S. Senator, Missouri", Party.REPUBLICAN, Kind.P, 1,
            source = "Not up in 2026; continuing Senate term after 2024 reelection."),
        Figure("cotton", "Tom Cotton", "U.S. Senator, Arkansas; Senate GOP Conference Chair", Party.REPUBLICAN, Kind.P, 2,
            source = "CBS News, Nov 2026: reelected Conference Chair, remaining a senator in the next Congress."),
        Figure("scalise", "Steve Scalise", "U.S. Representative, LA-1; House Majority Leader (119th)", Party.REPUBLICAN, Kind.P, 3,
            source = "house.gov/leadership; reelected in 2026 (Electoral Meridian / results coverage)."),
        Figure("emmer", "Tom Emmer", "U.S. Representative, MN-6; House Majority Whip (119th)", Party.REPUBLICAN, Kind.P, 4,
            source = "house.gov/leadership; reelected in 2026."),
        Figure("mcclain", "Lisa McClain", "U.S. Representative; House Republican Conference Chair", Party.REPUBLICAN, Kind.P, 5,
            source = "house.gov/leadership, Nov 2026."),
        Figure("tscott", "Tim Scott", "U.S. Senator, South Carolina", Party.REPUBLICAN, Kind.P, 6,
            source = "Continuing term (last elected 2022, not the 2026 Class II seat)."),
        Figure("blackburn", "Marsha Blackburn", "U.S. Senator, Tennessee", Party.REPUBLICAN, Kind.P, 7,
            source = "Continuing term after 2024 reelection."),

        // Democrats (Black by default)
        Figure("jeffries", "Hakeem Jeffries", "House Democratic Leader; Speaker-designate", Party.DEMOCRAT, Kind.K,
            source = "house.gov/leadership; AP / Axios, Nov 4, 2026: launched Speaker bid after Democrats won the House, widely expected to become the first Black Speaker. Formal Speakership vote is Jan 3, 2027; House Democrats' leadership meeting Nov 18, 2026."),
        Figure("clark", "Katherine Clark", "House Democratic Whip; Majority Leader-designate", Party.DEMOCRAT, Kind.Q,
            source = "house.gov/leadership; Boston Globe / WCVB, Nov 15, 2026: expected to become the first woman House Majority Leader."),
        Figure("schumer", "Chuck Schumer", "Senate Democratic Leader", Party.DEMOCRAT, Kind.R, 0,
            source = "Sitting Senate Democratic Leader, Nov 2026; expected incoming Majority Leader (caucus vote scheduled Dec 1, 2026). Scripps News / Punchbowl via contemporaneous coverage."),
        Figure("aguilar", "Pete Aguilar", "House Democratic Caucus Chairman; Majority Whip-designate", Party.DEMOCRAT, Kind.R, 1,
            source = "house.gov/leadership; Axios, Nov 4, 2026: slated as incoming Majority Whip."),
        Figure("newsom", "Gavin Newsom", "Governor of California (sitting through Jan 2027)", Party.DEMOCRAT, Kind.B, 0,
            source = "Term-limited; remains governor until January 2027 (VC Star, Nov 4, 2026)."),
        Figure("whitmer", "Gretchen Whitmer", "Governor of Michigan (sitting through Jan 2027)", Party.DEMOCRAT, Kind.B, 1,
            source = "Term-limited; remains governor until January 2027."),
        Figure("lieu", "Ted Lieu", "House Democratic Caucus Vice Chair", Party.DEMOCRAT, Kind.N, 0,
            source = "house.gov/leadership, Nov 2026."),
        Figure("warren", "Elizabeth Warren", "U.S. Senator, Massachusetts", Party.DEMOCRAT, Kind.N, 1,
            source = "Continuing term after 2024 reelection (not up in 2026)."),
        Figure("aoc", "Alexandria Ocasio-Cortez", "U.S. Representative, NY-14", Party.DEMOCRAT, Kind.P, 0,
            source = "Reelected 2026, NY-14 (election-results coverage, ~78% of the vote)."),
        Figure("raskin", "Jamie Raskin", "U.S. Representative, MD-8", Party.DEMOCRAT, Kind.P, 1,
            source = "Reelected 2026, MD-8."),
        Figure("booker", "Cory Booker", "U.S. Senator, New Jersey", Party.DEMOCRAT, Kind.P, 2,
            source = "Reelected 2026 (CBS Philadelphia; Decision Desk HQ)."),
        Figure("schiff", "Adam Schiff", "U.S. Senator, California", Party.DEMOCRAT, Kind.P, 3,
            source = "Continuing term after 2024 election to the Senate."),
        Figure("warnock", "Raphael Warnock", "U.S. Senator, Georgia", Party.DEMOCRAT, Kind.P, 4,
            source = "Continuing term (last elected 2022, next regular election 2028)."),
        Figure("murphy", "Chris Murphy", "U.S. Senator, Connecticut", Party.DEMOCRAT, Kind.P, 5,
            source = "Continuing term after 2024 reelection."),
        Figure("klobuchar", "Amy Klobuchar", "U.S. Senator, Minnesota", Party.DEMOCRAT, Kind.P, 6,
            source = "Continuing term after 2024 reelection."),
        Figure("duckworth", "Tammy Duckworth", "U.S. Senator, Illinois", Party.DEMOCRAT, Kind.P, 7,
            source = "Continuing term (last elected 2022, next regular election 2028)."),
    )

    fun of(id: String) = all.find { it.id == id }
    fun starting(party: Party, kind: Kind, slot: Int): Figure? =
        all.find { it.party == party && it.kind == kind && it.slot == slot }
}

/** Maps each occupied square to a roster id so political pawns keep their identity when they move. */
fun startingFigures(): Array<String?> {
    val a = arrayOfNulls<String>(64)
    fun place(side: Side, party: Party) {
        val home = side.home
        val pawnRank = if (side == Side.WHITE) 1 else 6
        a[sqOf(4, home)] = PoliticalRoster.starting(party, Kind.K, 0)?.id
        a[sqOf(3, home)] = PoliticalRoster.starting(party, Kind.Q, 0)?.id
        a[sqOf(0, home)] = PoliticalRoster.starting(party, Kind.R, 0)?.id
        a[sqOf(7, home)] = PoliticalRoster.starting(party, Kind.R, 1)?.id
        a[sqOf(2, home)] = PoliticalRoster.starting(party, Kind.B, 0)?.id
        a[sqOf(5, home)] = PoliticalRoster.starting(party, Kind.B, 1)?.id
        a[sqOf(1, home)] = PoliticalRoster.starting(party, Kind.N, 0)?.id
        a[sqOf(6, home)] = PoliticalRoster.starting(party, Kind.N, 1)?.id
        for (f in 0..7) a[sqOf(f, pawnRank)] = PoliticalRoster.starting(party, Kind.P, f)?.id
    }
    place(Side.WHITE, Party.REPUBLICAN)
    place(Side.BLACK, Party.DEMOCRAT)
    return a
}

fun applyFigureMove(figs: Array<String?>, m: Move, sideThatMoved: Side): Array<String?> {
    val a = figs.copyOf()
    if (m.ep) {
        val cap = m.to + if (sideThatMoved == Side.WHITE) -8 else 8
        a[cap] = null
    }
    if (m.castle) {
        when (m.to) {
            sqOf(6, 0) -> { a[sqOf(5, 0)] = a[sqOf(7, 0)]; a[sqOf(7, 0)] = null }
            sqOf(2, 0) -> { a[sqOf(3, 0)] = a[sqOf(0, 0)]; a[sqOf(0, 0)] = null }
            sqOf(6, 7) -> { a[sqOf(5, 7)] = a[sqOf(7, 7)]; a[sqOf(7, 7)] = null }
            sqOf(2, 7) -> { a[sqOf(3, 7)] = a[sqOf(0, 7)]; a[sqOf(0, 7)] = null }
        }
    }
    a[m.to] = a[m.from]
    a[m.from] = null
    return a
}

enum class PoliticalStyle { PARODY, LIKENESS }

fun politicalStyleOf(setId: String): PoliticalStyle? = when (setId) {
    "political_parody" -> PoliticalStyle.PARODY
    "political_likeness" -> PoliticalStyle.LIKENESS
    else -> null
}

fun likenessBuildEnabled(): Boolean = BuildConfig.POLITICAL_LIKENESS

fun politicalResName(style: PoliticalStyle, id: String): String =
    (if (style == PoliticalStyle.PARODY) "pp_" else "pl_") + id

fun Context.politicalBitmap(style: PoliticalStyle, id: String): ImageBitmap? {
    val name = politicalResName(style, id)
    val resId = resources.getIdentifier(name, "drawable", packageName)
    if (resId == 0) return null
    return try {
        BitmapFactory.decodeResource(resources, resId)?.asImageBitmap()
    } catch (_: Exception) { null }
}

fun Context.likenessAssetsPresent(): Boolean {
    if (!likenessBuildEnabled()) return false
    return resources.getIdentifier("pl_trump", "drawable", packageName) != 0
}
