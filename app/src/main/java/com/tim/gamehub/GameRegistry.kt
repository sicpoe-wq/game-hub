package com.tim.gamehub

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tim.gamehub.games.wordsearch.WordSearchGame

/** A game listed in the hub. To add a game: write its Composable and add an entry here. */
data class GameEntry(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val color: Color,
    val content: (@Composable (onExit: () -> Unit) -> Unit)? = null, // null = coming soon
) { val comingSoon get() = content == null }

object GameRegistry {
    val games = listOf(
        GameEntry("wordsearch", "Word Search", "🔤", "Find hidden words in themed letter grids. Three difficulties.", Color(0xFF5B4BDB),
            content = { onExit -> WordSearchGame(onExit) }),
        GameEntry("2048", "2048", "🔢", "Slide and merge tiles to reach 2048.", Color(0xFFE8833A)),
        GameEntry("snake", "Snake", "🐍", "Classic snake with swipe controls.", Color(0xFF2E9E5B)),
    )
}
