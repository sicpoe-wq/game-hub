package com.tim.gamehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF5B4BDB), background = Color(0xFFF5F4FB))) {
                var current by rememberSaveable { mutableStateOf<String?>(null) }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.systemBarsPadding()) {
                        val game = GameRegistry.games.find { it.id == current }
                        if (game?.content != null) {
                            BackHandler { current = null }
                            game.content.invoke { current = null }
                        } else HubScreen { current = it.id }
                    }
                }
            }
        }
    }
}

@Composable
fun HubScreen(onOpen: (GameEntry) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Game Hub", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("Pick a game to play", color = Color.Gray)
            Spacer(Modifier.height(6.dp))
        }
        items(GameRegistry.games) { g ->
            Card(
                Modifier.fillMaxWidth().alpha(if (g.comingSoon) 0.55f else 1f)
                    .clickable(enabled = !g.comingSoon) { onOpen(g) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).background(g.color, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Text(g.emoji, fontSize = 30.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(g.title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                            if (g.comingSoon) {
                                Spacer(Modifier.width(8.dp))
                                Text("COMING SOON", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.background(Color.Gray, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(g.description, fontSize = 13.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}
