package com.tim.gamehub.games.heartbound

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.tim.gamehub.BuildConfig
import com.tim.gamehub.R
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

private fun smooth(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun appear(now: Int, start: Int, dur: Int): Float =
    smooth((now - start) / dur.toFloat())

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private val HeroSweep = listOf("kael", "mira", "sera", "yuna", "rin", "nim")

private fun portraitRes(id: String) = when (id) {
    "mira" -> R.drawable.portrait_mira
    "rin" -> R.drawable.portrait_rin
    "sera" -> R.drawable.portrait_sera
    "yuna" -> R.drawable.portrait_yuna
    "nim" -> R.drawable.portrait_nim
    else -> R.drawable.portrait_kael
}

@Composable
internal fun TitleScreen(c: HeartboundController, onExit: () -> Unit) {
    c.beginIntroIfNeeded()
    val frozen = c.introFreezeMs
    var elapsed by remember { mutableIntStateOf(frozen ?: 0) }
    LaunchedEffect(frozen, c.introMode) {
        if (frozen != null) { elapsed = frozen; return@LaunchedEffect }
        if (c.introMode != "play") return@LaunchedEffect
        val origin = SystemClock.uptimeMillis()
        while (c.introMode == "play") {
            val t = (SystemClock.uptimeMillis() - origin).toInt()
            elapsed = t
            if (t >= IntroTiming.DURATION) { c.skipIntro(); break }
            delay(16)
        }
    }
    val now = frozen ?: elapsed
    val showMenu = c.introMode == "done" && frozen == null
    Box(
        Modifier
            .fillMaxSize()
            .testTag(if (showMenu) "hb_title" else "hb_intro")
            .then(
                if (!showMenu && frozen == null) Modifier.clickable { c.skipIntro() }
                else Modifier
            )
    ) {
        TitleBackdrop(now = if (showMenu) IntroTiming.DURATION else now, menu = showMenu)
        if (!showMenu) TitleIntroForeground(now)
        if (showMenu) TitleMenu(c, onExit)
        else if (now > 900) {
            Text(
                "Tap to skip",
                color = Cream.copy(alpha = 0.8f),
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
            )
        }
    }
}

@Composable
private fun TitleBackdrop(now: Int, menu: Boolean) {
    val pulse = rememberInfiniteTransition(label = "title_drift")
    val drift = pulse.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(22_000, easing = LinearEasing), RepeatMode.Restart),
        label = "d"
    )
    val wave = if (menu) drift.value else 0f
    val skyY = lerp(16f, 0f, appear(now, 0, 2400)) + if (menu) 6f * sin(wave * 2f * PI.toFloat()).toFloat() else 0f
    val townY = lerp(100f, 0f, appear(now, IntroTiming.TOWN, 2600)) + if (menu) 4f * sin(wave * 2f * PI.toFloat() + 1f).toFloat() else 0f
    val hallY = lerp(180f, 0f, appear(now, IntroTiming.HALL, 2600))
    val skyA = appear(now, 0, 1200)
    Box(Modifier.fillMaxSize().background(Color(0xFF07060E))) {
        Image(
            painterResource(R.drawable.title_sky), "Night sky over Lumenreach",
            Modifier.fillMaxSize().graphicsLayer { translationY = skyY; alpha = skyA },
            contentScale = ContentScale.Crop
        )
        Image(
            painterResource(R.drawable.title_town), "Lumenreach",
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.58f)
                .graphicsLayer { translationY = townY; alpha = appear(now, IntroTiming.TOWN, 1400) },
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.BottomCenter
        )
        Image(
            painterResource(R.drawable.title_hall), "The guild hall",
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.55f)
                .graphicsLayer { translationY = hallY; alpha = appear(now, IntroTiming.HALL, 1400) },
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.BottomCenter
        )
        TitleLanterns(now, menu, wave)
        // gentle vignette so menu type stays readable
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0x6607060E), Color.Transparent, Color.Transparent, Color(0x9907060E))
                )
            )
        )
    }
}

@Composable
private fun TitleLanterns(now: Int, menu: Boolean, wave: Float) {
    val gate = if (menu) 1f else appear(now, IntroTiming.LANTERNS, 1600)
    val tSec = if (menu) 8f + wave * 10f else now / 1000f
    val painter = painterResource(R.drawable.title_lantern)
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // sparkles
            for (i in 0 until 28) {
                val seed = i * 17.3f
                val phase = (tSec * (0.15f + (i % 5) * 0.03f) + seed) % 1f
                val x = ((seed * 13) % 100) / 100f * w
                val y = (1.05f - phase * 1.2f) * h
                val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin((tSec * 3f + seed) * 2.0).toFloat())
                drawCircle(
                    Color(0xFFFFE7A0).copy(alpha = gate * twinkle * 0.7f),
                    radius = 2.2f + (i % 3),
                    center = Offset(x, y)
                )
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            for (i in 0 until 9) {
                val phase = (tSec * (0.06f + (i % 3) * 0.015f) + i * 0.11f)
                val cycle = phase - kotlin.math.floor(phase.toDouble()).toFloat()
                val xFrac = (0.07f + (i * 0.11f) % 0.86f) + 0.045f * sin((phase * 2.4f) * 2f * PI.toFloat()).toFloat()
                val yFrac = 1.08f - cycle * 1.25f
                val size = 28.dp + 8.dp * (i % 4)
                val fade = gate * when {
                    cycle < 0.08f -> cycle / 0.08f
                    cycle > 0.88f -> (1f - cycle) / 0.12f
                    else -> 1f
                }
                Image(
                    painter, null,
                    Modifier
                        .offset(x = maxWidth * xFrac - size / 2, y = maxHeight * yFrac)
                        .size(size)
                        .graphicsLayer { alpha = fade.coerceIn(0f, 1f) * 0.92f },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun TitleIntroForeground(now: Int) {
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .offset(y = (-18).dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            HeroSweep.forEachIndexed { i, id ->
                val a = appear(now, IntroTiming.HEROES + i * 160, 900)
                val fromLeft = i % 2 == 0
                val slide = lerp(if (fromLeft) -80f else 80f, 0f, a)
                Image(
                    painterResource(portraitRes(id)), id,
                    Modifier
                        .weight(1f)
                        .height(150.dp)
                        .graphicsLayer {
                            alpha = a * 0.9f
                            translationX = slide
                            translationY = lerp(16f, 0f, a)
                        }
                        .clip(Soft),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply {
                        setToSaturation(0.2f)
                    })
                )
            }
        }
        TitleLogo(now, Modifier.align(Alignment.TopCenter).padding(top = 72.dp).fillMaxWidth(0.9f))
    }
}

@Composable
private fun TitleLogo(now: Int, modifier: Modifier = Modifier) {
    val a = appear(now, IntroTiming.LOGO, 1400)
    val scale = lerp(0.86f, 1f, a)
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = a * 0.55f; scaleX = 1.18f; scaleY = 1.7f }
                .background(
                    Brush.radialGradient(listOf(Gold.copy(alpha = 0.55f), Color.Transparent)),
                    Soft
                )
        )
        Image(
            painterResource(R.drawable.logo_heartbound), "HEARTBOUND",
            Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = a; scaleX = scale; scaleY = scale },
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
private fun TitleMenu(c: HeartboundController, onExit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.7f))
        TitleLogo(IntroTiming.DURATION, Modifier.fillMaxWidth(0.9f))
        Spacer(Modifier.height(8.dp))
        Text("Lead a guild. Win their hearts.", color = Cream.copy(alpha = 0.92f), fontSize = 15.sp)
        Text("The hall at Lumenreach is yours now.", color = Cream.copy(alpha = 0.7f), fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))
        if (c.hasSave) {
            HbButton("Continue", { c.loadSave() }, Modifier.fillMaxWidth(), Gold, tag = "hb_continue")
            Spacer(Modifier.height(10.dp))
        }
        HbButton(if (c.hasSave) "New Game" else "Begin", { c.openSlots("new") }, Modifier.fillMaxWidth(), tag = "hb_new")
        Spacer(Modifier.height(10.dp))
        if (c.hasSave) {
            HbButton("Load", { c.openSlots("load") }, Modifier.fillMaxWidth(), Wood, tag = "hb_load")
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HbButton("Gallery", { c.openGalleryFromTitle() }, Modifier.weight(1f), Wood, tag = "hb_btn_gallery")
            HbButton("Settings", { c.openSettings() }, Modifier.weight(1f), Wood, tag = "hb_settings")
        }
        Spacer(Modifier.height(12.dp))
        Pill("‹  Back to Hub", onClick = onExit, tag = "hb_hub")
        Spacer(Modifier.weight(1f))
        Text("v${BuildConfig.VERSION_NAME}", color = Cream.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
    }
}
