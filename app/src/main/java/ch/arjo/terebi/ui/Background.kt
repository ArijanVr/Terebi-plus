package ch.arjo.terebi.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import ch.arjo.terebi.scene.AdaptiveScrim
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import ch.arjo.terebi.scene.SceneChoice
import ch.arjo.terebi.scene.SceneResolver
import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Hintergrundmanager-Darstellung: Verlauf und Lichtquelle aus tokens.json, Überblendung beim
 * Szenenwechsel ohne Neuladen. Effekte (Regen, Schnee, Nebel) nur wenn [animate]; sonst statisch.
 */
@Composable
fun SceneBackground(choice: SceneChoice, phase: String, weatherCode: Int, windKmh: Double, lightningEnabled: Boolean, effects: Boolean, animate: Boolean) {
    val d = LocalDesign.current
    val context = LocalContext.current
    // Szenenbild nach Himmelsgruppe und Tageszeit (Guide 9.5); fehlt es, gilt das Grundfoto
    val imageName = SceneResolver.imageName(choice.scene, phase)
    val photo = remember(imageName) {
        fun load(n: String) = runCatching { context.assets.open("backgrounds/$n.webp").use { BitmapFactory.decodeStream(it) }.asImageBitmap() }.getOrNull()
        load(imageName) ?: load("lake-mcdonald")
    }
    // Adaptive Abdunklung (Guide 7.6): einmal pro Bild im Hintergrund-Thread messen
    val globalScrim by produceState(SceneResolver.baseScrim(imageName).toFloat(), photo) {
        val p = photo ?: return@produceState
        value = withContext(Dispatchers.Default) {
            val small = Bitmap.createScaledBitmap(p.asAndroidBitmap(), 64, 36, true)
            // Zonen in dp auf 960 × 540: Rail, Header rechts, Fokus-Kontext
            val zones = mapOf("rail" to intArrayOf(58, 112, 326, 408), "header" to intArrayOf(550, 28, 842, 68), "context" to intArrayOf(346, 467, 614, 512))
            val lum = zones.mapValues { (_, r) ->
                val px = ArrayList<Double>()
                for (y in (r[1] * 36 / 540) until (r[3] * 36 / 540).coerceAtMost(36)) for (x in (r[0] * 64 / 960) until (r[2] * 64 / 960).coerceAtMost(64)) px += AdaptiveScrim.luminance(small.getPixel(x, y))
                AdaptiveScrim.percentile95(px)
            }
            AdaptiveScrim.globalAlpha(lum, SceneResolver.baseScrim(imageName)).toFloat()
        }
    }
    // Übergänge (Guide 9.8): Tageszeitwechsel 4000 ms, Wetterwechsel 2000 ms
    val previous = remember { arrayOf(choice.scene) }
    val fadeMs = remember(choice.scene) {
        val timeOfDay = setOf("sunrise", "morning", "afternoon", "sunset", "evening", "night")
        val p = previous[0]; previous[0] = choice.scene
        if (p != choice.scene && p in timeOfDay && choice.scene in timeOfDay) 4000 else d.sceneMs
    }
    val dim = 0f // Die Nachtbilder sind bereits dunkel gegradet
    Box(Modifier.fillMaxSize()) {
        if (photo != null) Image(photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Crossfade(choice.scene, animationSpec = tween(if (animate) fadeMs else 200), label = "scene") { id ->
            val s = d.scenes[id] ?: d.scenes.getValue("night")
            Box(Modifier.fillMaxSize().drawBehind {
                if (photo == null) drawScene(s) else drawPhotoShade(d.ink, globalScrim)
            })
        }
        val effect = d.scenes[choice.scene]?.effect ?: "none"
        if (effects && effect != "none") Effect(if (weatherCode in listOf(56, 57, 66, 67)) "sleet" else effect, weatherCode, windKmh, lightningEnabled, animate)
        if (dim > 0f) Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black.copy(alpha = dim)) }
    }
}

private fun DrawScope.drawSceneTint(tint: Color, id: String) {
    drawRect(tint.copy(alpha = if (id == "night") 0.06f else 0.05f))
}

private fun DrawScope.drawPhotoShade(ink: Color, global: Float) {
    drawRect(ink.copy(alpha = global))
    drawRect(Brush.horizontalGradient(0f to ink.copy(alpha = 0.82f), 0.18f to ink.copy(alpha = 0.72f), 0.34f to ink.copy(alpha = 0.50f), 0.52f to ink.copy(alpha = 0.10f), 0.62f to Color.Transparent))
    drawRect(Brush.verticalGradient(0f to ink.copy(alpha = 0.84f), 0.5f to ink.copy(alpha = 0.56f), 1f to Color.Transparent, endY = size.height * 160f / 540f), size = androidx.compose.ui.geometry.Size(size.width, size.height * 160f / 540f))
    drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.5f to ink.copy(alpha = 0.62f), 1f to ink.copy(alpha = 0.88f), startY = size.height * 340f / 540f, endY = size.height), topLeft = Offset(0f, size.height * 340f / 540f), size = androidx.compose.ui.geometry.Size(size.width, size.height * 200f / 540f))
    drawRect(Brush.horizontalGradient(0f to Color.Transparent, 1f to ink.copy(alpha = 0.45f), startX = size.width * 560f / 960f, endX = size.width), topLeft = Offset(size.width * 560f / 960f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 400f / 960f, size.height))
}

private fun DrawScope.drawScene(s: SceneSpec) {
    drawRect(Brush.verticalGradient(0f to s.stops[0], 0.55f to s.stops[1], 1f to s.stops[2]))
    val c = Offset(size.width * s.glowX, size.height * s.glowY)
    // wie CSS «circle at x y, farthest-corner»: Radius bis zur entferntesten Ecke, Glanz bis 55 %
    val r = max(max(hypot(c.x, c.y), hypot(size.width - c.x, c.y)), max(hypot(c.x, size.height - c.y), hypot(size.width - c.x, size.height - c.y)))
    drawRect(Brush.radialGradient(0f to s.glow.copy(alpha = s.glowAlpha), 0.55f to Color.Transparent, center = c, radius = r))
}

@Composable
private fun Effect(kind: String, code: Int, windKmh: Double, lightningEnabled: Boolean, animate: Boolean) {
    val d = LocalDesign.current
    val flash = remember { Animatable(0f) }
    LaunchedEffect(code, animate, lightningEnabled) {
        flash.snapTo(0f)
        if (code in listOf(95, 96, 97, 99) && lightningEnabled && animate) {
            delay(10_000)
            while (true) {
                delay(Random.nextLong(45_000, 90_001))
                flash.animateTo(0.10f, tween(60))
                flash.animateTo(0f, tween(180))
                delay(90)
                flash.animateTo(0.06f, tween(60))
                flash.animateTo(0f, tween(180))
            }
        }
    }
    val transition = if (animate) rememberInfiniteTransition(label = "weatherEffects") else null
    val rainT = if (transition != null) {
        val duration = when (code) { 51, 53, 55 -> 1400; 65, 82, 95, 96, 97, 99 -> 850; else -> 1050 }
        val value by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(duration, easing = LinearEasing)), label = "rainT")
        value
    } else 0.3f
    val snowT = if (transition != null) {
        val value by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(18_000, easing = LinearEasing)), label = "snowT")
        value
    } else 0.3f
    val cloudT = if (transition != null) {
        val value by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(45_000, easing = LinearEasing), RepeatMode.Reverse), label = "cloudT")
        value
    } else 0.3f
    val seeds = remember { FloatArray(900) { i -> ((i * 7919L + 104729L) % 1009).toFloat() / 1009f } }
    val rainCount = when (code) { 51, 53, 55 -> 60; 65, 82, 95, 96, 97, 99 -> 260; 56, 57, 66, 67 -> 80; else -> 140 }
    val snowCount = when (code) { 75, 86 -> 240; 73 -> 160; 56, 57, 66, 67 -> 60; else -> 90 }
    val rainAlpha = when (rainCount) { 60 -> 0.12f; 260 -> 0.18f; else -> 0.14f }
    val windAngle = (windKmh * 0.5).coerceIn(-15.0, 15.0).toFloat()
    Canvas(Modifier.fillMaxSize()) {
        if (kind == "rain" || kind == "sleet") {
            // Drei Tiefenebenen (nah, mittel, fern): je eigene Länge, Dicke, Helligkeit und Fallgeschwindigkeit
            // (ganze Zyklen pro Schleife, damit sie nahtlos laufen). Jeder Tropfen hat eine eigene Phase.
            val intensity = when (rainCount) { 60 -> 0.6f; 260 -> 1.3f; else -> 1f }
            val slant = (windAngle + 5f) / 57.3f
            val waterTop = size.height * 0.58f
            for (i in 0 until rainCount) {
                val layer = when (i % 10) { 0, 1 -> 0; 2, 3, 4 -> 1; else -> 2 } // 0 nah, 1 mittel, 2 fern
                val cycles = 3 - layer
                val len = d.dp(when (layer) { 0 -> 34f; 1 -> 22f; else -> 12f }).toPx() * (0.85f + seeds[i * 3 + 2] * 0.3f)
                val w = d.dp(when (layer) { 0 -> 1.4f; 1 -> 1.0f; else -> 0.7f }).toPx()
                val x = seeds[i * 3] * (size.width + len) 
                val y = ((seeds[i * 3 + 1] + rainT * cycles) % 1f) * (size.height + len) - len
                val zone = if (x < size.width * 0.36f) 0.5f else 1f
                val a = (when (layer) { 0 -> 0.11f; 1 -> 0.07f; else -> 0.035f }) * intensity * zone
                val dx = len * slant
                // weicher Verlauf: Schweif, Mitte und Kopf mit steigender Deckkraft, runde Enden
                drawLine(d.text.copy(alpha = a * 0.25f), Offset(x, y), Offset(x + dx, y + len), strokeWidth = w, cap = StrokeCap.Round)
                drawLine(d.text.copy(alpha = a * 0.5f), Offset(x + dx * 0.35f, y + len * 0.35f), Offset(x + dx, y + len), strokeWidth = w, cap = StrokeCap.Round)
                drawLine(d.text.copy(alpha = a), Offset(x + dx * 0.7f, y + len * 0.7f), Offset(x + dx, y + len), strokeWidth = w, cap = StrokeCap.Round)
            }
            // Wasserringe auf dem See: flache, aufspreizende Ellipsen, die verblassen
            if (kind == "rain") {
                val rings = when (rainCount) { 60 -> 10; 260 -> 40; else -> 22 }
                val ringStroke = d.dp(1).toPx()
                for (j in 0 until rings) {
                    val sx = seeds[(j * 3 + 1) % 900] * size.width
                    val depth = 0.3f + seeds[(j * 3 + 2) % 900] * 0.7f
                    val sy = waterTop + (size.height - waterTop) * depth
                    val phase = (seeds[(j * 3) % 900] + rainT) % 1f
                    val r = d.dp(2f + 16f * depth * phase).toPx()
                    val fade = (1f - phase) * (1f - phase) * 0.09f * intensity
                    drawOval(d.text.copy(alpha = fade), Offset(sx - r, sy - r * 0.18f), androidx.compose.ui.geometry.Size(r * 2f, r * 0.36f), style = Stroke(ringStroke))
                }
            }
        }
        if (kind == "snow" || kind == "sleet") {
            for (i in 0 until snowCount) {
                val x = seeds[i * 3] * size.width + kotlin.math.sin(snowT * 6.28318f + seeds[i * 3 + 2] * 6.28318f) * d.dp(10).toPx()
                val y = ((seeds[i * 3 + 1] + snowT) % 1f) * size.height
                val radius = d.dp(if (kind == "sleet") 1f + seeds[i * 3 + 2] else 1f + seeds[i * 3 + 2] * 2.5f).toPx()
                drawCircle(d.text.copy(alpha = 0.35f + seeds[i * 3 + 2] * 0.4f), radius, Offset(x, y))
            }
        }
        if (kind == "fog") {
            val shift = cloudT * size.width * 0.08f
            drawOval(Brush.radialGradient(listOf(d.text.copy(alpha = 0.12f), Color.Transparent), Offset(size.width * 0.3f + shift, size.height * 0.62f), size.width * 0.35f), Offset(-size.width * 0.1f + shift, size.height * 0.45f), androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.35f))
            drawOval(Brush.radialGradient(listOf(d.text.copy(alpha = 0.08f), Color.Transparent), Offset(size.width * 0.75f - shift, size.height * 0.4f), size.width * 0.3f), Offset(size.width * 0.4f - shift, size.height * 0.28f), androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.25f))
        }
        if (kind == "clouds") {
            val shift = (cloudT - 0.5f) * size.width * 0.08f
            drawOval(Brush.radialGradient(listOf(d.text.copy(alpha = 0.10f), Color.Transparent), Offset(size.width * 0.32f + shift, size.height * 0.26f), size.width * 0.32f), Offset(size.width * 0.02f + shift, size.height * 0.08f), androidx.compose.ui.geometry.Size(size.width * 0.62f, size.height * 0.35f))
            drawOval(Brush.radialGradient(listOf(d.text.copy(alpha = 0.06f), Color.Transparent), Offset(size.width * 0.72f - shift, size.height * 0.45f), size.width * 0.28f), Offset(size.width * 0.45f - shift, size.height * 0.28f), androidx.compose.ui.geometry.Size(size.width * 0.55f, size.height * 0.32f))
        }
        if (flash.value > 0f) drawRect(d.text.copy(alpha = flash.value))
    }
}
