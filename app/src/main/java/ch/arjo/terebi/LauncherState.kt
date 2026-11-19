package ch.arjo.terebi

import android.content.Context
import ch.arjo.terebi.apps.AppRepository
import ch.arjo.terebi.apps.InstalledApp
import ch.arjo.terebi.data.Settings
import ch.arjo.terebi.data.SettingsStore
import ch.arjo.terebi.weather.Place
import ch.arjo.terebi.weather.WeatherRepository
import ch.arjo.terebi.wifi.SecretStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Overlays über der Startseite. Höchstens eines ist offen; Einstellungen können darunter liegen. */
sealed interface Overlay {
    data class AppMenu(val pkg: String) : Overlay
    data class Info(val title: String, val text: String) : Overlay
    data object Qr : Overlay
    /** Stundenwerte der nächsten 24 Stunden (Guide 7.3). */
    data object WeatherDetail : Overlay
    /** Einmaliger Hinweis auf das mitgelieferte TV+-APK. */
    data object InstallTvPlus : Overlay
}

/**
 * Zustand des Launchers. Lebt so lange wie die Activity (singleTask, keine Neuerstellung bei
 * Konfigurationswechseln). Kein ViewModel nötig.
 */
class LauncherState(context: Context, private val scope: CoroutineScope) {
    val store = SettingsStore(context)
    val apps = AppRepository(context)
    val weather = WeatherRepository(context)
    val secrets = SecretStore(context)

    val installed = MutableStateFlow<List<InstalledApp>>(emptyList())
    val settingsOpen = MutableStateFlow(false)
    val overlay = MutableStateFlow<Overlay?>(null)
    val moving = MutableStateFlow<String?>(null)
    val toast = MutableStateFlow<Pair<String, Long>?>(null)
    /** Laufender Autostart-Countdown (Sitzungsnummer) oder null. Höchstens einer gleichzeitig. */
    val countdownToken = MutableStateFlow<Int?>(null)
    var sessionCounter = 0
    val homeRequest = MutableStateFlow(0)
    val weatherRefresh = MutableStateFlow(0)
    /** Zählt jede Änderung am gespeicherten WLAN-Passwort, damit QR-Karte und Einstellungen sofort nachziehen. */
    val secretVersion = MutableStateFlow(0)

    /** Von der Activity gesetzt: RoleManager-Dialog oder Home-Einstellungen. */
    var onRequestHomeRole: () -> Unit = {}
    fun requestHomeRole() = onRequestHomeRole()

    fun update(t: (Settings) -> Settings) = scope.launch { store.update(t) }

    /** Installierte Apps neu lesen und Konfiguration bereinigen (neu, deinstalliert). */
    fun refreshApps() = scope.launch {
        apps.clearCache() // Icons und Banner neu lesen, sonst bleibt nach einem App-Update das alte Logo
        val list = withContext(Dispatchers.IO) { apps.installed() }
        installed.value = list
        store.update { s ->
            val synced = s.apps.sync(list.map { it.pkg })
            val asPkg = s.autostartPkg?.takeIf { p -> list.any { it.pkg == p } }
            s.copy(apps = synced, autostartPkg = asPkg, autostartEnabled = s.autostartEnabled && asPkg != null)
        }
    }

    fun launch(pkg: String) {
        if (!apps.launch(pkg)) message("App lässt sich nicht starten. Sie wurde eventuell deinstalliert.")
    }

    fun message(text: String) {
        toast.value = text to System.nanoTime()
    }

    suspend fun search(query: String): Result<List<Place>> = weather.searchPlaces(query)

    fun goHome() {
        overlay.value = null
        settingsOpen.value = false
        moving.value = null
        homeRequest.value++
    }

}
