package ch.arjo.terebi

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.lifecycleScope
import ch.arjo.terebi.autostart.AutoStartArm
import ch.arjo.terebi.apps.TvPlus
import ch.arjo.terebi.scene.SceneChoice
import ch.arjo.terebi.scene.SceneResolver
import ch.arjo.terebi.ui.CountdownPill
import ch.arjo.terebi.ui.Design
import ch.arjo.terebi.ui.HomeActions
import ch.arjo.terebi.ui.HomeScreen
import ch.arjo.terebi.ui.Logo
import ch.arjo.terebi.ui.LocalDesign
import ch.arjo.terebi.ui.MenuCard
import ch.arjo.terebi.ui.QrOverlay
import ch.arjo.terebi.ui.SceneBackground
import ch.arjo.terebi.ui.WeatherDetail
import ch.arjo.terebi.ui.SettingsScreen
import ch.arjo.terebi.ui.Toast
import ch.arjo.terebi.wifi.WifiQr
import ch.arjo.terebi.wifi.WifiSecurity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var state: LauncherState
    private val arm = AutoStartArm()
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> if (arm.onScreenOn(SystemClock.elapsedRealtime())) startAutostartSession()
                Intent.ACTION_SCREEN_OFF -> { arm.onScreenOff(); state.countdownToken.value = null }
            }
        }
    }
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { state.refreshApps() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = LauncherState(applicationContext, lifecycleScope)
        state.onRequestHomeRole = ::requestHomeRole
        arm.onLauncherCreated(SystemClock.elapsedRealtime())
        ContextCompat.registerReceiver(this, screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(this, packageReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED); addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent { Root(state) }
    }

    override fun onResume() {
        super.onResume()
        state.refreshApps()
        if (arm.onResume(SystemClock.elapsedRealtime())) startAutostartSession()
    }

    override fun onPause() {
        super.onPause()
        arm.onPause()
        state.countdownToken.value = null // nie im Hintergrund weiterzählen
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home-Taste, während terebi+ schon vorne ist: alles schliessen, Fokus auf erste App
        if (intent.hasCategory(Intent.CATEGORY_HOME)) state.goHome()
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    /** Eine Sitzung pro Einschalten. Mehrfache Signale fängt AutoStartArm ab; hier nie zwei Countdowns. */
    private fun startAutostartSession() {
        if (state.countdownToken.value == null) state.countdownToken.value = ++state.sessionCounter
    }

    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (rm.isRoleHeld(RoleManager.ROLE_HOME)) { state.message("terebi+ ist bereits der Startbildschirm."); return }
                runCatching { roleRequest.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)) }.onSuccess { return }
            }
        }
        if (!state.apps.openHomeSettings()) {
            state.message("Dieses Gerät erlaubt die Auswahl nicht in den Einstellungen. Anleitung: docs/installation.md")
        }
    }
}

@Composable
private fun Root(state: LauncherState) {
    val ctx = LocalContext.current
    val width = LocalConfiguration.current.screenWidthDp
    val design = remember(width) { Design.load(ctx, width) }
    val s = state.store.settings.collectAsState(initial = null).value
    val installed by state.installed.collectAsState()
    val weather by state.weather.state.collectAsState()
    val settingsOpen by state.settingsOpen.collectAsState()
    val overlay by state.overlay.collectAsState()
    val moving by state.moving.collectAsState()
    val toast by state.toast.collectAsState()
    val countdown by state.countdownToken.collectAsState()
    val homeRequest by state.homeRequest.collectAsState()
    val refresh by state.weatherRefresh.collectAsState()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val visible = lifecycle.isAtLeast(Lifecycle.State.STARTED)

    val zone = remember { ZoneId.of("Europe/Zurich") }
    var now by remember { mutableStateOf(LocalTime.now(zone)) }
    var today by remember { mutableStateOf(LocalDate.now(zone)) }
    var lastKey by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var idle by remember { mutableStateOf(false) }

    // Uhr und Leerlauf nur, solange sichtbar
    LaunchedEffect(visible) {
        while (visible) {
            now = LocalTime.now(zone); today = LocalDate.now(zone)
            idle = SystemClock.uptimeMillis() - lastKey > design.idleReduceAfterMs
            delay(((60 - now.second) * 1000L).coerceAtLeast(1000L))
        }
    }
    // Wetter nur, solange sichtbar; Ortswechsel oder «Aktualisieren» startet neu
    LaunchedEffect(visible, s?.location, refresh) {
        val loc = s?.location
        if (visible && loc != null) state.weather.run(loc, force = refresh > 0)
    }

    // Einmalig: solange TV+ fehlt, auf das mitgelieferte APK hinweisen. «Später» gilt als Antwort.
    LaunchedEffect(s != null) {
        val st = s ?: return@LaunchedEffect
        if (!st.tvPlusOfferSeen && !TvPlus.isInstalled(ctx) && TvPlus.hasBundle(ctx)) {
            state.update { it.copy(tvPlusOfferSeen = true) }
            state.overlay.value = Overlay.InstallTvPlus
        }
    }

    CompositionLocalProvider(LocalDesign provides design) {
        if (s == null) return@CompositionLocalProvider
        // Bewegung reduzieren: terebi+-Schalter oder System-Animator-Skala 0 (Guide 9.9)
        val still = s.reduceMotion || android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        val weatherAge = weather.weather?.let { System.currentTimeMillis() - it.fetchedAtMillis } ?: Long.MAX_VALUE
        val staleScene = weatherAge > TimeUnit.HOURS.toMillis(3)
        val choice = if (s.activeWeatherMode == "OFF") SceneChoice("afternoon", false) else if (staleScene && weather.weather != null) SceneChoice("cloudy", false) else if (s.background != "auto") SceneChoice(s.background, false) else {
            val w = weather.weather
            SceneResolver.resolve(now.hour * 60 + now.minute, w?.sunriseMin ?: 420, w?.sunsetMin ?: 1080, w?.code ?: 1)
        }
        // Wechsel warten, solange in den letzten 3 s eine Taste gedrückt wurde (Guide 9.8)
        var shownChoice by remember { mutableStateOf(choice) }
        LaunchedEffect(choice) {
            // In den Einstellungen bleibt die Hintergrundwahl sofort sichtbar
            while (!settingsOpen && SystemClock.uptimeMillis() - lastKey < 3000) delay(500)
            shownChoice = choice
        }
        val secretVersion by state.secretVersion.collectAsState()
        val wifiPayload = remember(s.wifiEnabled, s.ssid, s.security, s.wifiHidden, settingsOpen, secretVersion) {
            val password = if (s.wifiEnabled) state.secrets.readPassword().orEmpty() else ""
            if (!s.wifiEnabled || s.ssid.isBlank() || (s.security != WifiSecurity.NOPASS && password.isEmpty())) null
            else WifiQr.payload(s.ssid, password, s.security, s.wifiHidden)
        }
        val autostartLabel = s.autostartPkg?.let { p -> installed.firstOrNull { it.pkg == p }?.label }

        Box(
            Modifier.fillMaxSize().onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                lastKey = SystemClock.uptimeMillis(); idle = false
                when {
                    countdown != null -> { state.countdownToken.value = null; state.message("Autostart abgebrochen"); true }
                    e.key == Key.Menu -> { state.settingsOpen.value = true; true }
                    else -> false
                }
            },
        ) {
            val covered = settingsOpen || overlay != null
            Box(Modifier.fillMaxSize().then(if (covered && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(design.px(28)) else Modifier)) {
            SceneBackground(shownChoice, SceneResolver.phase(now.hour * 60 + now.minute, weather.weather?.sunriseMin ?: 420, weather.weather?.sunsetMin ?: 1080), weather.weather?.code ?: 3, weather.weather?.windKmh ?: 0.0, s.lightningEnabled,
                s.activeWeatherMode == "ON" && s.effects && !still && !staleScene && !covered && lifecycle.isAtLeast(Lifecycle.State.RESUMED),
                animate = visible && !idle && !still && !covered)
            HomeScreen(
                s, installed, weather, now, today, moving, state.apps, wifiPayload, homeRequest, covered,
                HomeActions(
                    launch = state::launch,
                    menu = { state.overlay.value = Overlay.AppMenu(it) },
                    settings = { state.settingsOpen.value = true },
                    weather = {
                        val w = weather.weather
                        state.overlay.value = if (w == null || w.hours.isEmpty()) Overlay.Info("Wetter", if (w == null) "Wetter gerade nicht verfügbar." else "${w.tempC.toInt()}° · ${design.code(w.code).label}. Stundenwerte erscheinen nach der nächsten Aktualisierung. Quelle: Open-Meteo.") else Overlay.WeatherDetail
                    },
                    wifi = { state.overlay.value = Overlay.Qr },
                    moveApp = { pkg, delta -> state.update { it.copy(apps = it.apps.moveOnHome(pkg, delta)) } },
                    endMove = { state.moving.value = null; state.message("Reihenfolge gespeichert") },
                ),
            )
            }
            if (covered && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) Box(Modifier.fillMaxSize().background(design.ink.copy(alpha = 0.6f)))
            if (settingsOpen) SettingsScreen(state, s, installed)
            when (val o = overlay) {
                is Overlay.AppMenu -> AppMenu(state, s, o.pkg, installed.firstOrNull { it.pkg == o.pkg }?.label ?: o.pkg)
                is Overlay.Info -> MenuCard(o.title, o.text, listOf("Verstanden" to { state.overlay.value = null }))
                Overlay.InstallTvPlus -> {
                    val scope = rememberCoroutineScope()
                    MenuCard(
                        "TV+ installieren",
                        "IP Television+ ${TvPlus.VERSION} ist in terebi+ enthalten. Android zeigt die übliche Installation und fragt nach der Bestätigung; terebi+ ändert keine Systemapps.",
                        listOf(
                            "Installieren" to {
                                state.overlay.value = null
                                scope.launch {
                                    when (TvPlus.install(ctx)) {
                                        TvPlus.Result.OPENED -> state.message("Android fragt nach der Bestätigung")
                                        TvPlus.Result.NEEDS_PERMISSION -> state.message("Zuerst «Unbekannte Apps installieren» für terebi+ erlauben, dann erneut installieren")
                                        TvPlus.Result.BUNDLE_MISSING -> state.message("TV+-APK fehlt in dieser terebi+-Version")
                                    }
                                }
                            },
                            "Später" to { state.overlay.value = null },
                        ),
                    )
                }
                Overlay.WeatherDetail -> weather.weather?.let { WeatherDetail(it, s.fahrenheit) { state.overlay.value = null } }
                Overlay.Qr -> {
                    if (wifiPayload == null) {
                        LaunchedEffect(Unit) { state.overlay.value = null; state.message("Zuerst WLAN-Name und Passwort in Einstellungen › WLAN eintragen") }
                    } else {
                        QrOverlay(wifiPayload, s.ssid, s.qrAutoCloseSec) { state.overlay.value = null }
                    }
                }
                null -> Unit
            }
            Logo()
            countdown?.let { token ->
                if (!s.autostartEnabled || autostartLabel == null) {
                    LaunchedEffect(token) {
                        state.countdownToken.value = null
                        if (s.autostartEnabled) state.message("Autostart-App nicht gefunden. terebi+ bleibt offen.")
                    }
                } else {
                    var left by remember(token) { mutableStateOf(s.autostartSeconds) }
                    var progress by remember(token) { mutableStateOf(0f) }
                    LaunchedEffect(token) {
                        val total = s.autostartSeconds * 1000L
                        val t0 = SystemClock.uptimeMillis()
                        while (true) {
                            val el = SystemClock.uptimeMillis() - t0
                            progress = (el.toFloat() / total).coerceAtMost(1f)
                            left = ((total - el + 999) / 1000).toInt().coerceAtLeast(0)
                            if (el >= total) break
                            delay(100)
                        }
                        state.countdownToken.value = null
                        state.launch(s.autostartPkg)
                    }
                    CountdownPill(autostartLabel, left, progress)
                }
            }
            toast?.let { (text, id) ->
                LaunchedEffect(id) { delay(3600); if (state.toast.value?.second == id) state.toast.value = null }
                Toast(text)
            }
        }

        // Zurück: Overlay, dann Einstellungen, dann Verschieben. Auf der Startseite nie den Launcher verlassen.
        BackHandler {
            when {
                overlay != null -> state.overlay.value = null
                settingsOpen -> state.settingsOpen.value = false
                moving != null -> state.moving.value = null
            }
        }
    }
}

@Composable
private fun AppMenu(state: LauncherState, s: ch.arjo.terebi.data.Settings, pkg: String, label: String) {
    val shown = pkg !in s.apps.hidden
    val fav = pkg in s.apps.favorites
    val close = { state.overlay.value = null }
    MenuCard(
        label,
        if (shown) null else "Ausgeblendet. Bleibt auf dem Fernseher installiert.",
        buildList {
            add((if (shown) "Aus terebi+ ausblenden" else "In terebi+ anzeigen") to { state.update { it.copy(apps = it.apps.toggleHidden(pkg)) }; close() })
            add((if (fav) "Kein Favorit mehr" else "Als Favorit") to { state.update { it.copy(apps = it.apps.toggleFavorite(pkg)) }; close() })
            if (shown) add("Verschieben" to { close(); state.settingsOpen.value = false; state.moving.value = pkg })
            add("In Liste nach oben" to { state.update { it.copy(apps = it.apps.moveInOrder(pkg, -1)) } })
            add("In Liste nach unten" to { state.update { it.copy(apps = it.apps.moveInOrder(pkg, 1)) } })
            add("App-Info (Android)" to { close(); state.apps.openAppInfo(pkg) })
        },
    )
}
