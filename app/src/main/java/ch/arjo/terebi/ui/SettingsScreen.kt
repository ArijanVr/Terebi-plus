package ch.arjo.terebi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import ch.arjo.terebi.BuildConfig
import ch.arjo.terebi.LauncherState
import ch.arjo.terebi.Overlay
import ch.arjo.terebi.apps.InstalledApp
import ch.arjo.terebi.apps.TvPlus
import ch.arjo.terebi.data.Settings
import ch.arjo.terebi.weather.Location
import ch.arjo.terebi.weather.Place
import ch.arjo.terebi.wifi.WifiSecurity
import kotlinx.coroutines.launch

private val SECTIONS = listOf("apps" to "Apps", "look" to "Darstellung", "weather" to "Wetter", "autostart" to "Autostart", "wifi" to "WLAN", "system" to "System")
val SCENE_LABELS = linkedMapOf(
    "auto" to "Automatisch", "sunrise" to "Sonnenaufgang", "morning" to "Sonniger Morgen", "afternoon" to "Sonniger Nachmittag",
    "cloudy" to "Bewölkter Tag", "sunset" to "Sonnenuntergang", "evening" to "Abend", "night" to "Nacht",
    "rain" to "Regen", "snow" to "Schneefall", "fog" to "Nebel", "storm" to "Gewitter",
)
private fun <T> List<T>.next(v: T): T = this[(indexOf(v) + 1).mod(size)]

@Composable
fun SettingsScreen(state: LauncherState, s: Settings, installed: List<InstalledApp>, initialSection: String = "apps") {
    val d = LocalDesign.current
    var section by remember { mutableStateOf<String?>(null) }
    var lastSection by remember { mutableStateOf(initialSection) }
    section?.let { lastSection = it }
    val sectionFocus = remember { SECTIONS.associate { it.first to FocusRequester() } }
    val backFocus = remember { FocusRequester() }
    BackHandler(enabled = section != null) { section = null }
    LaunchedEffect(section) { runCatching { if (section == null) sectionFocus.getValue(lastSection).requestFocus() else backFocus.requestFocus() } }

    Box(Modifier.fillMaxSize().background(d.ink.copy(alpha = 0.40f))) {
        Column(Modifier.align(Alignment.CenterEnd).width(d.dp(360)).fillMaxHeight()
            .background(d.ink.copy(alpha = 0.96f)).padding(d.dp(32))) {
            Label(if (section == null) "Einstellungen" else SECTIONS.first { it.first == section }.second,
                d.dpSp(24), d.text, Modifier.padding(bottom = d.dp(24)), weight = FontWeight.SemiBold)
            if (section == null) {
                SECTIONS.forEach { (key, label) ->
                    SettingRow(label, Modifier.focusRequester(sectionFocus.getValue(key))) { section = key }
                }
            } else {
                SettingRow("Zurück", Modifier.focusRequester(backFocus)) { section = null }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.dp(4))) {
                    item { SectionBody(section!!, state, s, installed) }
                }
                Label(NOTES.getValue(section!!), d.dpSp(12), d.textSecondary, Modifier.padding(top = d.dp(12)))
            }
        }
    }
}

private val NOTES = mapOf(
    "apps" to "OK öffnet Optionen. «Ausblenden» entfernt eine App nur aus terebi+. Sie bleibt auf dem Fernseher installiert.",
    "look" to "Der Hintergrund ist live sichtbar. Automatisch folgt er Tageszeit und Wetter.",
    "weather" to "Wetterdaten: Open-Meteo.com (CC BY 4.0). Aktualisierung alle 15 Minuten, ohne Verbindung bleiben die letzten Daten.",
    "autostart" to "Startet die App nur nach dem Einschalten des Fernsehers, nie bei Rückkehr aus einer App. Jede Taste bricht ab.",
    "wifi" to "Der QR-Code entsteht nur auf dem Gerät. Das Passwort wird verschlüsselt gespeichert und nie gesendet.",
    "system" to "terebi+ verändert keine Systempakete und braucht kein Root.",
)

@Composable
private fun Modifier.widthIn900() = this.then(Modifier.width(LocalDesign.current.px(900)))

@Composable
private fun SectionBody(section: String, state: LauncherState, s: Settings, installed: List<InstalledApp>) {
    val d = LocalDesign.current
    val scope = rememberCoroutineScope()
    val labels = installed.associate { it.pkg to it.label }
    var confirm by remember(section) { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(d.px(6))) {
        when (section) {
            "apps" -> s.apps.order.filter { it in labels }.forEach { pkg ->
                val shown = pkg !in s.apps.hidden
                SettingRow(
                    (if (pkg in s.apps.favorites) "★ " else "") + labels.getValue(pkg),
                    value = if (shown) "Angezeigt" else "Ausgeblendet", muted = !shown,
                ) { state.overlay.value = Overlay.AppMenu(pkg) }
            }
            "look" -> {
                SettingRow("Hintergrund", value = SCENE_LABELS[s.background]) { state.update { it.copy(background = SCENE_LABELS.keys.toList().next(it.background)) } }
                SettingRow("Wettereffekte", toggle = s.effects) { state.update { it.copy(effects = !it.effects) } }
                SettingRow("Animationen reduzieren", toggle = s.reduceMotion) { state.update { it.copy(reduceMotion = !it.reduceMotion) } }
                SettingRow("Icon-Grösse", value = mapOf("S" to "Klein", "M" to "Mittel", "L" to "Gross")[s.iconSize]) { state.update { it.copy(iconSize = listOf("S", "M", "L").next(it.iconSize)) } }
            }
            "weather" -> {
                var results by remember { mutableStateOf<List<Place>?>(null) }
                SettingRow("Aktives Wetter", value = mapOf("ON" to "Ein", "IMAGE_ONLY" to "Nur Bild", "OFF" to "Aus")[s.activeWeatherMode]) {
                    state.update { it.copy(activeWeatherMode = listOf("ON", "IMAGE_ONLY", "OFF").next(it.activeWeatherMode)) }
                }
                if (s.activeWeatherMode == "ON") SettingRow("Blitze anzeigen", toggle = s.lightningEnabled) { state.update { it.copy(lightningEnabled = !it.lightningEnabled) } }
                SettingRow("Wetteranzeige", toggle = s.showWeather) { state.update { it.copy(showWeather = !it.showWeather) } }
                SettingRow("Standort", value = s.location?.name ?: "Nicht gewählt") {}
                TextRow("Stadt suchen", "", "Name eingeben") { q ->
                    if (q.isBlank()) return@TextRow
                    scope.launch {
                        state.search(q.trim())
                            .onSuccess { results = it }
                            .onFailure { state.message("Suche nicht möglich. Internetverbindung prüfen.") }
                    }
                }
                results?.let { list ->
                    if (list.isEmpty()) SettingRow("Keine Treffer") {}
                    list.forEach { p ->
                        SettingRow(p.name, value = p.detail) {
                            state.update { it.copy(location = Location(p.name, p.latitude, p.longitude)) }
                            results = null
                            state.message("Standort: ${p.name}")
                        }
                    }
                }
                SettingRow("Wetter aktualisieren") { state.weatherRefresh.value++ }
                SettingRow("Temperatureinheit", value = if (s.fahrenheit) "°F" else "°C") { state.update { it.copy(fahrenheit = !it.fahrenheit) } }
            }
            "autostart" -> {
                val candidates = s.apps.order.filter { it in labels }
                SettingRow("Autostart", toggle = s.autostartEnabled) {
                    state.update { it.copy(autostartEnabled = !it.autostartEnabled && it.autostartPkg != null) }
                    if (s.autostartPkg == null) state.message("Zuerst eine App wählen")
                }
                SettingRow("App", value = s.autostartPkg?.let { labels[it] } ?: "Keine") {
                    if (candidates.isNotEmpty()) state.update { it.copy(autostartPkg = if (it.autostartPkg in candidates) candidates.next(it.autostartPkg!!) else candidates.first()) }
                }
                SettingRow("Countdown", value = "${s.autostartSeconds} s") { state.update { it.copy(autostartSeconds = if (it.autostartSeconds >= 5) 1 else it.autostartSeconds + 1) } }
            }
            "wifi" -> {
                SettingRow("QR-Code-Funktion", toggle = s.wifiEnabled) { state.update { it.copy(wifiEnabled = !it.wifiEnabled) } }
                TextRow("WLAN-Name (SSID)", s.ssid, "z. B. Zuhause") { v -> state.update { it.copy(ssid = v.trim()) } }
                SettingRow("Sicherheit", value = s.security.label) { state.update { it.copy(security = WifiSecurity.entries.toList().next(it.security)) } }
                if (s.security != WifiSecurity.NOPASS) {
                    val version by state.secretVersion.collectAsState()
                    val pwSet = remember(version) { !state.secrets.readPassword().isNullOrEmpty() }
                    TextRow("Passwort", if (pwSet) "••••••••" else "", "Passwort", password = true, liveCommit = true) { v ->
                        if (v != "••••••••" && v != state.secrets.readPassword()) {
                            state.secrets.savePassword(v); state.secretVersion.value++; state.message("Passwort gespeichert")
                        }
                    }
                }
                SettingRow("Verstecktes Netz", toggle = s.wifiHidden) { state.update { it.copy(wifiHidden = !it.wifiHidden) } }
                SettingRow("Automatisch schliessen", value = if (s.qrAutoCloseSec == 0) "Nie" else "${s.qrAutoCloseSec / 60} min") {
                    state.update { it.copy(qrAutoCloseSec = listOf(60, 120, 300, 0).next(it.qrAutoCloseSec)) }
                }
                SettingRow("QR-Code anzeigen") { state.overlay.value = Overlay.Qr }
                SettingRow(if (confirm == "wifi") "Wirklich löschen? OK bestätigt" else "Zugangsdaten löschen") {
                    if (confirm == "wifi") {
                        confirm = null
                        state.secrets.clear()
                        state.secretVersion.value++
                        state.update { it.copy(ssid = "", wifiEnabled = false) }
                        state.message("WLAN-Zugangsdaten gelöscht")
                    } else confirm = "wifi"
                }
            }
            "system" -> {
                val ctx = LocalContext.current
                if (TvPlus.isInstalled(ctx)) {
                    SettingRow("TV+", value = "Installiert") {}
                } else if (TvPlus.hasBundle(ctx)) {
                    SettingRow("TV+ installieren", value = TvPlus.VERSION) {
                        scope.launch {
                            when (TvPlus.install(ctx)) {
                                TvPlus.Result.OPENED -> state.message("Android fragt nach der Bestätigung")
                                TvPlus.Result.NEEDS_PERMISSION -> state.message("Zuerst «Unbekannte Apps installieren» für terebi+ erlauben, dann erneut installieren")
                                TvPlus.Result.BUNDLE_MISSING -> state.message("TV+-APK fehlt in dieser terebi+-Version")
                            }
                        }
                    }
                }
                SettingRow("Version", value = BuildConfig.VERSION_NAME, mono = true) {}
                SettingRow("Paket", value = ctx.packageName, mono = true) {}
                SettingRow("Android", value = "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})", mono = true) {}
                SettingRow("Android-Einstellungen öffnen") { state.apps.openSystemSettings() }
                SettingRow("Als Standard-Launcher einrichten") { state.requestHomeRole() }
                SettingRow("Cache leeren", value = "Wetter, Banner") { state.weather.clearCache(); state.apps.clearCache(); state.weatherRefresh.value++; state.message("Cache geleert") }
                SettingRow(if (confirm == "reset") "Wirklich zurücksetzen? OK bestätigt" else "Konfiguration zurücksetzen") {
                    if (confirm == "reset") {
                        confirm = null
                        scope.launch { state.store.reset(); state.secrets.clear(); state.refreshApps(); state.message("Konfiguration zurückgesetzt") }
                    } else confirm = "reset"
                }
                SettingRow("Original-Launcher wiederherstellen", value = "Anleitung") {
                    state.overlay.value = Overlay.Info(
                        "Original-Launcher",
                        "terebi+ deaktiviert keine Systempakete. Zurück zum Google-TV-Startbildschirm: Einstellungen › Apps › terebi+ › Standard löschen, oder terebi+ deinstallieren. Hat der Hersteller die Auswahl gesperrt: ADB-Weg in docs/installation.md.",
                    )
                }
                SettingRow("Version", value = BuildConfig.VERSION_NAME) {}
            }
        }
    }
}

/**
 * Zeile mit Textfeld. OK öffnet das Feld unter der Zeile (Bildschirmtastatur), «Fertig» übernimmt.
 * Die Zeile bleibt immer im Baum: Verschwindet ein fokussiertes Element, springt Compose zum ersten
 * fokussierbaren Element (Rubrik «Apps») und die Rubrik wechselt mit.
 */
@Composable
private fun TextRow(label: String, value: String, placeholder: String, password: Boolean = false, liveCommit: Boolean = false, onCommit: (String) -> Unit) {
    val d = LocalDesign.current
    var editing by remember { mutableStateOf(false) }
    // Beim Passwort darf die Anzeige «••••••••» das Eingabefeld nicht zurücksetzen, sobald gespeichert ist
    var text by remember(if (password) "" else value) { mutableStateOf(value) }
    val field = remember { FocusRequester() }
    val row = remember { FocusRequester() }
    // Reihenfolge: erst Fokus zurück auf die Zeile, dann das Feld entfernen
    // Fernbedienungs-Tastaturen melden «Fertig» nicht immer: bei liveCommit speichert jede Eingabe nach kurzer Pause, beim Schliessen sicher
    val close = { if (liveCommit && text.isNotEmpty()) onCommit(text); runCatching { row.requestFocus() }; editing = false }
    if (liveCommit) LaunchedEffect(text, editing) { if (editing && text.isNotEmpty()) { kotlinx.coroutines.delay(400); onCommit(text) } }
    BackHandler(enabled = editing) { close() }
    SettingRow(label, Modifier.focusRequester(row), value = if (editing) "Eingabe …" else value.ifEmpty { placeholder }) {
        text = if (password) "" else value
        editing = true
    }
    if (editing) {
        LaunchedEffect(Unit) { runCatching { field.requestFocus() } }
        Box(
            Modifier.fillMaxWidth().heightIn(min = d.px(72)).clip(RoundedCornerShape(d.px(14))).background(d.text.copy(alpha = 0.12f)).padding(horizontal = d.px(24)),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                text, { text = it },
                modifier = Modifier.fillMaxWidth().focusRequester(field),
                singleLine = true,
                textStyle = TextStyle(color = d.text, fontSize = d.body, fontFamily = TerebiFont),
                cursorBrush = SolidColor(d.text),
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Text, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onCommit(text)
                    close()
                }),
            )
        }
    }
}
