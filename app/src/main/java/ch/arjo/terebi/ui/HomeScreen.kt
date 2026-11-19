package ch.arjo.terebi.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import ch.arjo.terebi.R
import ch.arjo.terebi.apps.InstalledApp
import ch.arjo.terebi.apps.AppRepository
import ch.arjo.terebi.data.Settings
import ch.arjo.terebi.weather.WeatherUi
import ch.arjo.terebi.weather.Weather
import ch.arjo.terebi.weather.WeatherDay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

val SwissGerman: Locale = Locale.forLanguageTag("de-CH")
fun weekday(date: LocalDate): String = DateTimeFormatter.ofPattern("EEEE", SwissGerman).format(date)
fun temp(c: Double, fahrenheit: Boolean) = "${(if (fahrenheit) c * 9 / 5 + 32 else c).roundToInt()}°"

data class HomeActions(
    val launch: (String) -> Unit,
    val menu: (String) -> Unit,
    val settings: () -> Unit,
    val weather: () -> Unit,
    val wifi: () -> Unit,
    val moveApp: (String, Int) -> Unit,
    val endMove: () -> Unit,
)

/** Logo fix oben links (Guide 2.4): nie fokussierbar, bleibt in Overlays sichtbar und unverwischt. */
@Composable
fun Logo() {
    val d = LocalDesign.current
    Image(
        painterResource(R.drawable.logo_horizontal), contentDescription = "terebi+",
        modifier = Modifier.offset(x = d.dp(58), y = d.dp(34)).width(d.dp(102)).height(d.dp(30)),
    )
}

@Composable
fun HomeScreen(
    settings: Settings,
    installed: List<InstalledApp>,
    weather: WeatherUi,
    now: LocalTime,
    today: LocalDate,
    moving: String?,
    repo: AppRepository,
    wifiPayload: String?,
    homeRequest: Int,
    covered: Boolean,
    actions: HomeActions,
) {
    val d = LocalDesign.current
    val labels = remember(installed) { installed.associate { it.pkg to it.label } }
    val home = settings.apps.home().filter { it in labels }
    var focusedPkg by remember { mutableStateOf<String?>(null) }
    var focusedWeather by remember { mutableStateOf(false) }
    var focusedGear by remember { mutableStateOf(false) }
    var focusedQr by remember { mutableStateOf(false) }
    var lastTarget by remember { mutableStateOf("app") }
    val appFocus = remember(home) { home.associateWith { FocusRequester() } }
    val firstApp = appFocus[home.firstOrNull()]
    val gear = remember { FocusRequester() }
    val weatherFocus = remember { FocusRequester() }
    val qrFocus = remember { FocusRequester() }
    val qrVisible = settings.wifiEnabled && wifiPayload != null

    // Startfokus und Home-Taste: erste App, ohne Apps die Einstellungen (nie unerreichbar).
    // Liegt ein Overlay darüber, darf die Startseite den Fokus nicht an sich ziehen.
    LaunchedEffect(homeRequest, home) {
        if (!covered) runCatching { if (firstApp == null) gear.requestFocus() else firstApp.requestFocus() }
    }

    // Fokus-Gedächtnis (Guide 6.3): nach dem Schliessen eines Overlays zurück auf das zuletzt fokussierte Element
    var wasCovered by remember { mutableStateOf(covered) }
    var savedTarget by remember { mutableStateOf("app") }
    var savedPkg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(covered) {
        if (covered && !wasCovered) { savedTarget = lastTarget; savedPkg = focusedPkg }
        if (wasCovered && !covered) {
            kotlinx.coroutines.delay(80)
            runCatching {
                when {
                    savedTarget == "weather" && settings.showWeather -> weatherFocus
                    savedTarget == "gear" -> gear
                    savedTarget == "qr" && qrVisible -> qrFocus
                    else -> appFocus[savedPkg] ?: firstApp ?: gear
                }.requestFocus()
            }
        }
        wasCovered = covered
    }

    Box(Modifier.fillMaxSize()) {
        HeaderDateClock(
            DateTimeFormatter.ofPattern("EEEE, d. MMMM", SwissGerman).format(today),
            "%02d:%02d".format(now.hour, now.minute),
        )
        IconButton(R.drawable.ic_w_gear, "Einstellungen", Modifier.offset(x = d.dp(862), y = d.dp(28)).focusRequester(gear).focusProperties { down = weatherFocus; left = firstApp ?: FocusRequester.Default }, onFocus = { focusedGear = it; if (it) lastTarget = "gear" }, onClick = actions.settings)
        val currentWeather = weather.weather
        if (!settings.showWeather && currentWeather != null) {
            Label("${temp(currentWeather.tempC, settings.fahrenheit)} ${d.code(currentWeather.code).label}", d.dpSp(15), d.textSecondary,
                Modifier.offset(x = d.dp(420), y = d.dp(40)).width(d.dp(125)), maxLines = 1)
        }
        if (settings.showWeather) {
            Box(Modifier.offset(x = d.dp(634), y = d.dp(112))) {
                ForecastCard(currentWeather, settings, today, weather.offline,
                    Modifier.focusRequester(weatherFocus).focusProperties { up = gear; left = appFocus[focusedPkg] ?: firstApp ?: FocusRequester.Default },
                    onFocus = { focusedWeather = it; if (it) lastTarget = "weather" }, onClick = actions.weather)
            }
        }
        Column(Modifier.offset(x = d.dp(58), y = d.dp(112)).width(d.dp(268)).height(d.dp(if (home.size > 6) 320 else 296))) {
            if (home.isEmpty()) {
                Label("Apps hinzufügen", d.body, d.textSecondary, Modifier.weight(1f))
            } else {
                AppGrid(home, labels, settings, moving, repo, appFocus, weatherFocus, gear, if (qrVisible) qrFocus else null, Modifier.weight(1f), onFocus = { p, f -> if (f) { lastTarget = "app"; focusedPkg = p; focusedWeather = false; focusedGear = false; focusedQr = false } }, actions)
            }
        }
        wifiPayload?.takeIf { qrVisible }?.let { payload ->
            SidebarQr(payload, settings.ssid, Modifier.offset(x = d.dp(58), y = d.dp(436)).focusRequester(qrFocus)
                .focusProperties { up = appFocus[focusedPkg] ?: firstApp ?: FocusRequester.Default; right = weatherFocus }, onFocus = { focusedQr = it; if (it) lastTarget = "qr" }, onClick = actions.wifi)
        }
        val name = when { focusedWeather -> "Wetter"; focusedGear -> "Einstellungen"; focusedQr -> "WLAN für Gäste"; else -> focusedPkg?.let { labels[it] } ?: "" }
        if (name.isNotEmpty()) {
            Column(Modifier.offset(x = d.dp(346), y = d.dp(467)).width(d.dp(268)), horizontalAlignment = Alignment.CenterHorizontally) {
                Label(name, d.dpSp(20), d.text, weight = FontWeight.SemiBold, maxLines = 1)
                Label(if (moving != null) "Pfeiltasten verschieben · OK fertig" else if (focusedWeather) "OK Wetter anzeigen" else if (focusedQr) "OK QR-Code vergrössern" else if (focusedGear) "OK Einstellungen öffnen" else "OK öffnen · Halten für Optionen", d.dpSp(12), d.textSecondary, maxLines = 1)
            }
        }
    }
}

/** Datum und Uhr rechtsbündig auf x 842, gemeinsame Basislinie y 58 (Brandguide § 5.2). */
@Composable
private fun HeaderDateClock(date: String, clock: String) {
    val d = LocalDesign.current
    Layout(
        content = {
            Label(date, d.dpSp(15), d.textSecondary, maxLines = 1)
            Label(clock, d.dpSp(30), d.text, weight = FontWeight.Medium, maxLines = 1, tnum = true)
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val dateP = measurables[0].measure(loose)
        val clockP = measurables[1].measure(loose)
        val base = d.dp(58).roundToPx()
        val clockX = d.dp(842).roundToPx() - clockP.width
        val dateX = clockX - d.dp(16).roundToPx() - dateP.width
        layout(constraints.maxWidth, constraints.maxHeight) {
            clockP.place(clockX, base - clockP[FirstBaseline])
            dateP.place(dateX, base - dateP[FirstBaseline])
        }
    }
}

@Composable
private fun AppGrid(
    home: List<String>, labels: Map<String, String>, settings: Settings, moving: String?, repo: AppRepository,
    appFocus: Map<String, FocusRequester>, weatherFocus: FocusRequester, gearFocus: FocusRequester, qrFocus: FocusRequester?, modifier: Modifier, onFocus: (String, Boolean) -> Unit, actions: HomeActions,
) {
    val d = LocalDesign.current
    val cols = minOf(home.size, d.appColumns)
    val density = LocalDensity.current
    val iconPx = with(density) { d.dp(32).roundToPx() }
    val scroll = rememberScrollState()
    var focusedIdx by remember { mutableStateOf(0) }
    val rowPx = with(density) { d.dp(50).roundToPx() }
    // Pivot (Guide 7.1): die fokussierte Zeile bleibt auf Position 2
    LaunchedEffect(focusedIdx, home.size) {
        if (home.size > 6) scroll.animateScrollTo(((focusedIdx - 1).coerceAtLeast(0) * rowPx).coerceAtMost(scroll.maxValue))
    }
    val fade = if (home.size > 6) Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        val e = d.dp(24).toPx()
        if (scroll.value > 0) drawRect(Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, endY = e), size = Size(size.width, e), blendMode = BlendMode.DstIn)
        if (scroll.value < scroll.maxValue) drawRect(Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent, startY = size.height - e, endY = size.height), topLeft = Offset(0f, size.height - e), size = Size(size.width, e), blendMode = BlendMode.DstIn)
    } else Modifier
    Column(
        modifier.then(fade).verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(d.dp(4)),
    ) {
        home.chunked(cols).forEachIndexed { row, pkgs ->
            Row(horizontalArrangement = Arrangement.spacedBy(d.sidebarGap)) {
                pkgs.forEachIndexed { col, pkg ->
                    val idx = row * cols + col
                    androidx.compose.runtime.key(pkg) { AppTile(
                        idx, pkg, labels[pkg] ?: pkg, repo.icon(pkg, iconPx), pkg in settings.apps.favorites, moving == pkg, d.iconScale(settings.iconSize),
                        modifier = Modifier.focusRequester(appFocus.getValue(pkg)).focusProperties {
                            right = weatherFocus
                            if (idx == 0) up = gearFocus
                            if (idx == home.lastIndex && qrFocus != null) down = qrFocus
                        },
                        onFocus = { f -> if (f) focusedIdx = idx; onFocus(pkg, f) },
                        onClick = { if (moving != null) actions.endMove() else actions.launch(pkg) },
                        onLong = { if (moving == null) actions.menu(pkg) },
                        onMove = { delta -> actions.moveApp(pkg, if (delta == 2) cols else if (delta == -2) -cols else delta) },
                        isMoving = moving != null,
                    ) }
                }
            }
        }
    }
}

@Composable
private fun AppTile(
    index: Int, pkg: String, label: String, icon: androidx.compose.ui.graphics.ImageBitmap?, favorite: Boolean, moving: Boolean, iconScale: Float,
    modifier: Modifier, onFocus: (Boolean) -> Unit, onClick: () -> Unit, onLong: () -> Unit, onMove: (Int) -> Unit, isMoving: Boolean,
) {
    val d = LocalDesign.current
    val (focused, focusMod) = rememberFocus()
    LaunchedEffect(focused) { onFocus(focused) }
    val s by animateFloatAsState(if (focused) d.focusScale else 1f, tween(d.focusMs), label = "tileScale")
    val a by animateFloatAsState(if (focused || isMoving) 1f else d.dimUnfocused, tween(d.focusMs), label = "tileAlpha")
    val ring = d.focusRing
    val shape = RoundedCornerShape(d.appRadius)
    // Fokus folgt der verschobenen App, auch in eine andere Zeile
    val moveFocus = remember { FocusRequester() }
    if (moving) LaunchedEffect(index) { runCatching { moveFocus.requestFocus() } }
    Box(
        modifier
            .focusRequester(moveFocus)
            .size(d.dp(268), d.dp(46))
            .scale(s)
            .alpha(a)
            .then(if (focused) Modifier.shadow(d.dp(12), shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.55f), spotColor = Color.Black.copy(alpha = 0.55f)) else Modifier)
            .drawWithContent {
                drawContent()
                if (focused && moving) {
                    val off = d.ringOffset.toPx() + d.ringWidth.toPx() / 2
                    val r = d.appRadius.toPx() + off
                    drawRoundRect(ring, Offset(-off, -off), Size(size.width + 2 * off, size.height + 2 * off), CornerRadius(r), style = Stroke(d.ringWidth.toPx()))
                }
            }
            .semantics { contentDescription = label }
            .then(focusMod)
            .then(if (moving) Modifier.moveKeys(onMove) else Modifier)
            .tvClick(d.longPressMs, onLong, onClick),
    ) {
        Row(Modifier.fillMaxSize().clip(shape).background(if (focused) d.text else Color.Transparent).padding(start = d.dp(8), end = d.dp(12)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.dp(14))) {
            if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(d.dp(32) * iconScale).clip(RoundedCornerShape(d.dp(8))).then(if (focused) Modifier else Modifier.border(d.dp(1), d.text.copy(alpha = 0.10f), RoundedCornerShape(d.dp(8)))))
            Label(label, d.dpSp(17), if (focused) d.ink else d.text, weight = if (focused) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
        }
        if (favorite) Box(Modifier.align(Alignment.CenterEnd).padding(end = d.dp(8)).size(d.dp(5)).clip(CircleShape).background(if (focused) d.ink else d.textSecondary))
    }
}

/** Im Verschiebemodus fangen die Pfeiltasten ab: ±1 links/rechts, ±2 eine Zeile. */
private fun Modifier.moveKeys(onMove: (Int) -> Unit) = onPreviewKeyEvent { e ->
    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val delta = when (e.key) {
        Key.DirectionLeft -> -1
        Key.DirectionRight -> 1
        Key.DirectionUp -> -2
        Key.DirectionDown -> 2
        else -> return@onPreviewKeyEvent false
    }
    onMove(delta)
    true
}

@Composable
private fun IconButton(icon: Int, desc: String, modifier: Modifier, onFocus: (Boolean) -> Unit, onClick: () -> Unit) {
    val d = LocalDesign.current
    val (focused, focusMod) = rememberFocus()
    LaunchedEffect(focused) { onFocus(focused) }
    val s by animateFloatAsState(if (focused) 1.10f else 1f, tween(d.focusMs), label = "iconScale")
    Box(
        modifier.size(d.dp(40)).scale(s).clip(CircleShape)
            .background(if (focused) d.text else d.glass)
            .semantics { contentDescription = desc }
            .then(focusMod)
            .tvClick(d.longPressMs, null, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(icon), null, Modifier.size(d.dp(20)), colorFilter = ColorFilter.tint(if (focused) d.ink else d.text))
    }
}

@Composable
private fun SidebarQr(payload: String, ssid: String, modifier: Modifier, onFocus: (Boolean) -> Unit, onClick: () -> Unit) {
    val d = LocalDesign.current
    val (focused, focusMod) = rememberFocus()
    LaunchedEffect(focused) { onFocus(focused) }
    val s by animateFloatAsState(if (focused) 1.02f else 1f, tween(d.focusMs), label = "qrScale")
    Row(
        modifier.size(d.dp(268), d.dp(76)).scale(s)
            .drawWithContent { drawContent(); if (focused) { val o = d.dp(4).toPx(); drawRoundRect(d.focusRing, Offset(-o, -o), Size(size.width + 2 * o, size.height + 2 * o), CornerRadius(d.dp(18).toPx() + o), style = Stroke(d.dp(2).toPx())) } }
            .clip(RoundedCornerShape(d.dp(18))).background(d.glass)
            .semantics { contentDescription = "WLAN QR-Code für $ssid gross anzeigen" }
            .then(focusMod)
            .tvClick(d.longPressMs, null, onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.dp(16)),
    ) {
        Box(Modifier.padding(start = d.dp(8))) { QrCode(payload, d.dp(60), d.dp(48)) }
        Column {
            Label("WLAN · GÄSTE", d.dpSp(12), d.textSecondary, weight = FontWeight.SemiBold, maxLines = 1)
            Label(ssid, d.dpSp(17), d.text, weight = FontWeight.SemiBold, maxLines = 1)
            Label("Mit dem Handy scannen", d.dpSp(12), d.textSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun WeatherBlock(settings: Settings, ui: WeatherUi, now: LocalTime, today: LocalDate) {
    val d = LocalDesign.current
    val clock = if (settings.showClock) " · %02d:%02d".format(now.hour, now.minute) else ""
    Column(horizontalAlignment = Alignment.End) {
        val w = ui.weather
        if (!settings.showWeather || w == null) {
            val hint = when {
                !settings.showWeather -> ""
                settings.location == null -> "Wetter: Standort in den Einstellungen wählen"
                else -> "Wetter wird geladen"
            }
            Label(weekday(today) + clock, d.caption, d.text, weight = FontWeight.Medium)
            if (hint.isNotEmpty()) Label(hint, d.caption, d.textSecondary)
            return@Column
        }
        val info = d.code(w.code)
        val icon = if (info.icon == "sun" && !w.isDay) "moon" else info.icon
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.px(18))) {
            Image(painterResource(iconRes(icon)), null, Modifier.size(d.px(54)), colorFilter = ColorFilter.tint(d.text))
            Label(temp(w.tempC, settings.fahrenheit), d.display, d.text, maxLines = 1)
        }
        Spacer(Modifier.height(d.px(10)))
        val stale = if (ui.offline) " · Stand %s, offline".format(java.time.Instant.ofEpochMilli(w.fetchedAtMillis).atZone(java.time.ZoneId.systemDefault()).toLocalTime().let { "%02d:%02d".format(it.hour, it.minute) }) else ""
        Label("${w.place} · ${info.label}", d.caption, d.textSecondary, maxLines = 1)
        Label(weekday(today) + clock + stale, d.caption, d.textSecondary, maxLines = 1)
    }
}

@Composable
private fun ForecastCard(weather: Weather?, settings: Settings, today: LocalDate, offline: Boolean, modifier: Modifier, onFocus: (Boolean) -> Unit, onClick: () -> Unit) {
    val d = LocalDesign.current
    val (focused, focusMod) = rememberFocus()
    LaunchedEffect(focused) { onFocus(focused) }
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, tween(d.focusMs), label = "weatherCardScale")
    val days = weather?.days?.filter { !it.date.isBefore(today) }?.take(7).orEmpty()
    val shape = RoundedCornerShape(d.dp(24))
    Column(
        modifier.size(d.dp(268), d.dp(400)).scale(scale)
            .drawWithContent { drawContent(); if (focused) { val o = d.dp(4).toPx(); drawRoundRect(d.focusRing, Offset(-o, -o), Size(size.width + 2 * o, size.height + 2 * o), CornerRadius(d.dp(24).toPx() + o), style = Stroke(d.dp(2).toPx())) } }
            .clip(shape)
            .background(if (focused) d.glass.copy(alpha = 0.84f) else d.glass)
            .semantics { contentDescription = "Wetter und Vorhersage für sieben Tage" }
            .then(focusMod).tvClick(d.longPressMs, null, onClick)
            .padding(horizontal = d.dp(24), vertical = d.dp(20)),
    ) {
        Label("WETTER · ${(weather?.place ?: settings.location?.name ?: "Zürich").uppercase(SwissGerman)}", d.dpSp(12), d.textSecondary, weight = FontWeight.SemiBold, maxLines = 1)
        if (weather == null) {
            Spacer(Modifier.height(d.dp(42)))
            Image(painterResource(R.drawable.ic_w_cloud), null, Modifier.size(d.dp(44)), colorFilter = ColorFilter.tint(d.textSecondary))
            Spacer(Modifier.height(d.dp(16)))
            Label("Keine Verbindung", d.dpSp(17), d.text, weight = FontWeight.SemiBold)
            Label("Das Wetter erscheint, sobald das Netz zurück ist.", d.dpSp(15), d.textSecondary)
            return@Column
        }
        Spacer(Modifier.height(d.dp(10)))
        val info = d.code(weather.code)
        val currentIcon = if (info.icon == "sun" && !weather.isDay) "moon" else info.icon
        Row(Modifier.fillMaxWidth().height(d.dp(56)), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(iconRes(currentIcon)), null, Modifier.size(d.dp(44)), colorFilter = ColorFilter.tint(d.text))
            Spacer(Modifier.width(d.dp(12)))
            Label(temp(weather.tempC, settings.fahrenheit), d.dpSp(54), d.text, maxLines = 1, tnum = true)
            Spacer(Modifier.weight(1f))
            val day = days.firstOrNull()
            Column(horizontalAlignment = Alignment.End) {
                Label("H ${day?.let { temp(it.max.toDouble(), settings.fahrenheit) } ?: "–"}", d.dpSp(14), d.textSecondary, maxLines = 1, tnum = true)
                Label("T ${day?.let { temp(it.min.toDouble(), settings.fahrenheit) } ?: "–"}", d.dpSp(14), d.textSecondary, maxLines = 1, tnum = true)
            }
        }
        Spacer(Modifier.height(d.dp(8)))
        val conditionLabel = when {
            !weather.isDay && weather.code == 0 -> "Klar"
            !weather.isDay && weather.code == 1 -> "Meist klar"
            else -> info.label
        }
        Label(conditionLabel, d.dpSp(17), d.text, weight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(d.dp(12)))
        Box(Modifier.fillMaxWidth().height(d.dp(1)).background(d.text.copy(alpha = 0.10f)))
        Spacer(Modifier.height(d.dp(10)))
        if (days.isNotEmpty()) {
            val scaleMin = days.minOf { it.min } - 1
            val scaleMax = days.maxOf { it.max } + 1
            days.forEach { day ->
                ForecastRow(day, day.date == today, weather.tempC, settings.fahrenheit, scaleMin, scaleMax)
            }
        }
        Spacer(Modifier.weight(1f))
        val fetched = java.time.Instant.ofEpochMilli(weather.fetchedAtMillis).atZone(java.time.ZoneId.of("Europe/Zurich")).toLocalTime()
        Label((if (offline) "Stand" else "Open-Meteo ·") + " %02d:%02d".format(fetched.hour, fetched.minute) + if (offline) " · offline" else "", d.dpSp(12), d.textSecondary, maxLines = 1)
    }
}

@Composable
private fun ForecastRow(day: WeatherDay, isToday: Boolean, currentTemp: Double, fahrenheit: Boolean, scaleMin: Int, scaleMax: Int) {
    val d = LocalDesign.current
    Row(Modifier.fillMaxWidth().height(d.dp(28)), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(d.dp(56))) { Label(if (isToday) "Heute" else weekday(day.date).take(2), d.dpSp(15), d.text, weight = if (isToday) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1) }
        Image(painterResource(iconRes(d.code(day.code).icon)), null, Modifier.size(d.dp(20)), colorFilter = ColorFilter.tint(d.textSecondary))
        Box(Modifier.width(d.dp(28)), contentAlignment = Alignment.CenterEnd) { Label(temp(day.min.toDouble(), fahrenheit), d.dpSp(14), d.textSecondary, maxLines = 1, tnum = true) }
        Spacer(Modifier.width(d.dp(10)))
        val span = (scaleMax - scaleMin).coerceAtLeast(1).toFloat()
        Canvas(Modifier.width(d.dp(64)).height(d.dp(7))) {
            val h = d.dp(4).toPx()
            val y = (size.height - h) / 2
            drawRoundRect(d.text.copy(alpha = 0.12f), Offset(0f, y), Size(size.width, h), CornerRadius(h / 2))
            val left = ((day.min - scaleMin) / span).coerceIn(0f, 1f) * size.width
            val right = ((day.max - scaleMin) / span).coerceIn(0f, 1f) * size.width
            drawRoundRect(Brush.horizontalGradient(listOf(d.cool, d.warm), startX = 0f, endX = size.width), Offset(left, y), Size((right - left).coerceAtLeast(h), h), CornerRadius(h / 2))
            if (isToday) {
                val x = (((currentTemp - scaleMin) / span).toFloat().coerceIn(0f, 1f) * size.width)
                drawCircle(d.ink, radius = d.dp(4).toPx(), center = Offset(x, size.height / 2))
                drawCircle(d.text, radius = d.dp(3.5).toPx(), center = Offset(x, size.height / 2))
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.width(d.dp(28)), contentAlignment = Alignment.CenterEnd) { Label(temp(day.max.toDouble(), fahrenheit), d.dpSp(14), d.text, weight = FontWeight.SemiBold, maxLines = 1, tnum = true) }
    }
}

fun iconRes(name: String): Int = when (name) {
    "sun" -> R.drawable.ic_w_sun
    "moon" -> R.drawable.ic_w_moon
    "partly" -> R.drawable.ic_w_partly
    "rain" -> R.drawable.ic_w_rain
    "snow" -> R.drawable.ic_w_snow
    "fog" -> R.drawable.ic_w_fog
    "storm" -> R.drawable.ic_w_storm
    else -> R.drawable.ic_w_cloud
}

