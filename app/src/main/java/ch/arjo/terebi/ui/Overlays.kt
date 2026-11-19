package ch.arjo.terebi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import ch.arjo.terebi.wifi.WifiQr

/** Kleines Menü (App-Optionen, Hinweise). Erste Zeile erhält den Fokus. */
@Composable
fun MenuCard(title: String, text: String?, items: List<Pair<String, () -> Unit>>) {
    val d = LocalDesign.current
    val first = remember { FocusRequester() }
    // Der Fokus kann erst nach dem ersten Layout gesetzt werden; ohne Wiederholung bleibt das Menü
    // gelegentlich unfokussiert und die Fernbedienung bedient die Startseite darunter.
    LaunchedEffect(title) {
        repeat(3) {
            if (runCatching { first.requestFocus() }.isSuccess) return@LaunchedEffect
            kotlinx.coroutines.delay(50)
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(d.px(560)).clip(RoundedCornerShape(d.px(24))).background(d.ink.copy(alpha = 0.94f)).padding(horizontal = d.px(20), vertical = d.px(28)),
            verticalArrangement = Arrangement.spacedBy(d.px(6)),
        ) {
            Label(title, d.title, d.text, Modifier.padding(start = d.px(24), bottom = d.px(10)), weight = FontWeight.Medium)
            if (text != null) Label(text, d.caption, d.textSecondary, Modifier.padding(start = d.px(24), end = d.px(24), bottom = d.px(10)), maxLines = 6)
            items.forEachIndexed { i, (label, action) ->
                SettingRow(label, if (i == 0) Modifier.focusRequester(first) else Modifier, onClick = action)
            }
        }
    }
}

/** WLAN-QR für Gäste. Gross genug für Smartphone-Kameras aus Sofadistanz; schliesst nach [autoCloseSec]. */
@Composable
fun QrOverlay(payload: String, ssid: String, autoCloseSec: Int, onClose: () -> Unit) {
    val d = LocalDesign.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    if (autoCloseSec > 0) LaunchedEffect(payload) { kotlinx.coroutines.delay(autoCloseSec * 1000L); onClose() }
    Box(Modifier.fillMaxSize().background(d.ink.copy(alpha = 0.88f)), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(d.px(80)), verticalAlignment = Alignment.CenterVertically) {
            QrCode(payload, d.px(600), d.px(520))
            Column(Modifier.width(d.px(560)), verticalArrangement = Arrangement.spacedBy(d.px(12))) {
                Label("WLAN für Gäste", d.title * 1.6f, d.text, weight = FontWeight.Medium)
                Label("Kamera auf den Code richten und verbinden.", d.body, d.textSecondary)
                Label("Netz $ssid", d.body, d.text)
                Label((if (autoCloseSec > 0) "Schliesst nach ${autoCloseSec / 60} min. " else "") + "Zurück schliesst.", d.body, d.textSecondary)
                // Fokusanker, damit OK und Zurück hier landen
                SettingRow("Schliessen", Modifier.focusRequester(focus), onClick = onClose)
            }
        }
    }
}

@Composable
fun QrCode(payload: String, cardSize: Dp, codeSize: Dp) {
    val d = LocalDesign.current
    val matrix = remember(payload) { WifiQr.matrix(payload) }
    Box(Modifier.size(cardSize).clip(RoundedCornerShape(d.px(16))).background(Color.White), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(codeSize)) {
            val m = size.width / matrix.size
            for (y in 0 until matrix.size) for (x in 0 until matrix.size) {
                if (matrix[x, y]) drawRect(d.ink, Offset(x * m, y * m), Size(m + 0.5f, m + 0.5f))
            }
        }
    }
}

/** Dezenter Autostart-Hinweis mit Fortschrittsring. */
@Composable
fun CountdownPill(appLabel: String, secondsLeft: Int, progress: Float) {
    val d = LocalDesign.current
    Box(Modifier.fillMaxSize().padding(bottom = d.px(72)), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier.clip(RoundedCornerShape(d.px(999))).background(d.ink.copy(alpha = 0.78f)).padding(start = d.px(22), end = d.px(30), top = d.px(18), bottom = d.px(18)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.px(22)),
        ) {
            Box(Modifier.size(d.px(52)), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(d.px(52))) {
                    val w = d.px(3).toPx()
                    drawCircle(d.text.copy(alpha = 0.18f), style = Stroke(w))
                    drawArc(d.accent, -90f, 360f * progress, false, style = Stroke(w), topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w))
                }
                Label("$secondsLeft", d.body, d.text, weight = FontWeight.Medium)
            }
            Column {
                Label("$appLabel startet", d.body, d.text, maxLines = 1)
                Label("Beliebige Taste: abbrechen", d.caption, d.textSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
fun Toast(text: String) {
    val d = LocalDesign.current
    Box(Modifier.fillMaxSize().padding(top = d.px(72)), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.clip(RoundedCornerShape(d.px(16))).background(d.ink.copy(alpha = 0.92f)).padding(horizontal = d.px(28), vertical = d.px(16))) {
            Label(text, d.caption, d.text, maxLines = 3)
        }
    }
}


/** Wetter-Detail als rechtes Overlay (Guide 7.3): Stundenwerte der nächsten 24 Stunden. */
@Composable
fun WeatherDetail(w: ch.arjo.terebi.weather.Weather, fahrenheit: Boolean, onClose: () -> Unit) {
    val d = LocalDesign.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(3) {
            if (runCatching { first.requestFocus() }.isSuccess) return@LaunchedEffect
            kotlinx.coroutines.delay(50)
        }
    }
    Box(Modifier.fillMaxSize().background(d.ink.copy(alpha = 0.40f))) {
        Column(Modifier.align(Alignment.CenterEnd).width(d.dp(360)).fillMaxHeight().background(d.ink.copy(alpha = 0.96f)).padding(d.dp(32))) {
            Label("Wetter · ${w.place}", d.dpSp(24), d.text, Modifier.padding(bottom = d.dp(24)), weight = FontWeight.SemiBold, maxLines = 1)
            SettingRow("Zurück", Modifier.focusRequester(first), onClick = onClose)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.dp(4))) {
                items(w.hours) { h ->
                    val min = h.time.hour * 60
                    val night = min < w.sunriseMin || min >= w.sunsetMin
                    val cond = when { night && h.code == 0 -> "Klar"; night && h.code == 1 -> "Meist klar"; else -> d.code(h.code).label }
                    val label = "%02d:00  %s".format(h.time.hour, cond)
                    SettingRow(label, value = temp(h.tempC.toDouble(), fahrenheit) + if (h.precipPct >= 20) "  ${h.precipPct} %" else "", onClick = {})
                }
            }
            Label("Nächste 24 Stunden. Quelle: Open-Meteo.com (CC BY 4.0). Regenwahrscheinlichkeit ab 20 %.", d.dpSp(12), d.textSecondary, Modifier.padding(top = d.dp(12)))
        }
    }
}
