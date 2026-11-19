package ch.arjo.terebi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ch.arjo.terebi.apps.AppConfig
import ch.arjo.terebi.weather.Location
import ch.arjo.terebi.wifi.WifiSecurity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Alle Einstellungen. Das WLAN-Passwort liegt nicht hier, sondern verschlüsselt im SecretStore. */
data class Settings(
    val background: String = "auto",
    val effects: Boolean = true,
    val activeWeatherMode: String = "ON",
    val lightningEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val iconSize: String = "M",
    val showClock: Boolean = true,
    val showForecast: Boolean = true,
    val showWeather: Boolean = true,
    val fahrenheit: Boolean = false,
    val location: Location? = Location("Zürich", 47.3769, 8.5417),
    val autostartEnabled: Boolean = false,
    val autostartPkg: String? = null,
    val autostartSeconds: Int = 3,
    val wifiEnabled: Boolean = false,
    val ssid: String = "",
    val security: WifiSecurity = WifiSecurity.WPA,
    val wifiHidden: Boolean = false,
    val qrAutoCloseSec: Int = 120,
    val apps: AppConfig = AppConfig(),
    /** Einmaliger Hinweis «TV+ installieren» wurde gezeigt (auch bei «Später»). */
    val tvPlusOfferSeen: Boolean = false,
)

private val Context.store: DataStore<Preferences> by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private object K {
        val background = stringPreferencesKey("background")
        val effects = booleanPreferencesKey("effects")
        val activeWeatherMode = stringPreferencesKey("active_weather_mode")
        val lightningEnabled = booleanPreferencesKey("lightning_enabled")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val iconSize = stringPreferencesKey("icon_size")
        val showClock = booleanPreferencesKey("show_clock")
        val showForecast = booleanPreferencesKey("show_forecast")
        val forecastWeekMigrated = booleanPreferencesKey("forecast_week_migrated")
        val showWeather = booleanPreferencesKey("show_weather")
        val fahrenheit = booleanPreferencesKey("fahrenheit")
        val locName = stringPreferencesKey("loc_name")
        val locLat = doublePreferencesKey("loc_lat")
        val locLon = doublePreferencesKey("loc_lon")
        val asEnabled = booleanPreferencesKey("autostart_enabled")
        val asPkg = stringPreferencesKey("autostart_pkg")
        val asSeconds = intPreferencesKey("autostart_seconds")
        val wifiEnabled = booleanPreferencesKey("wifi_enabled")
        val ssid = stringPreferencesKey("wifi_ssid")
        val security = stringPreferencesKey("wifi_security")
        val wifiHidden = booleanPreferencesKey("wifi_hidden")
        val qrAutoClose = intPreferencesKey("qr_auto_close")
        val tvPlusOffered = booleanPreferencesKey("tv_plus_offered")
        val order = stringPreferencesKey("apps_order")
        val hidden = stringSetPreferencesKey("apps_hidden")
        val favorites = stringSetPreferencesKey("apps_favorites")
        val known = stringSetPreferencesKey("apps_known")
    }

    val settings: Flow<Settings> = context.store.data.map(::read)

    suspend fun update(transform: (Settings) -> Settings) {
        context.store.edit { p -> write(p, transform(read(p))) }
    }

    private fun read(p: Preferences): Settings {
        val d = Settings()
        return Settings(
            background = p[K.background] ?: d.background,
            effects = p[K.effects] ?: d.effects,
            activeWeatherMode = p[K.activeWeatherMode] ?: d.activeWeatherMode,
            lightningEnabled = p[K.lightningEnabled] ?: d.lightningEnabled,
            reduceMotion = p[K.reduceMotion] ?: d.reduceMotion,
            iconSize = p[K.iconSize] ?: d.iconSize,
            showClock = p[K.showClock] ?: d.showClock,
            // Die bisherige Voreinstellung war «aus» und wurde bei jeder Änderung gespeichert.
            // Beim Wechsel zur Wochenansicht einmalig einschalten; danach gilt der Nutzerschalter.
            showForecast = if (p[K.forecastWeekMigrated] == true) p[K.showForecast] ?: d.showForecast else true,
            showWeather = p[K.showWeather] ?: d.showWeather,
            fahrenheit = p[K.fahrenheit] ?: d.fahrenheit,
            // Ohne gespeicherten Ort gilt der Standardort; sonst bliebe die Wetterkarte auf einer frischen Installation leer.
            location = p[K.locName]?.let { Location(it, p[K.locLat] ?: 0.0, p[K.locLon] ?: 0.0) } ?: d.location,
            autostartEnabled = p[K.asEnabled] ?: d.autostartEnabled,
            autostartPkg = p[K.asPkg],
            autostartSeconds = (p[K.asSeconds] ?: d.autostartSeconds).coerceIn(1, 5),
            wifiEnabled = p[K.wifiEnabled] ?: d.wifiEnabled,
            ssid = p[K.ssid] ?: d.ssid,
            security = WifiSecurity.entries.firstOrNull { it.name == p[K.security] } ?: d.security,
            wifiHidden = p[K.wifiHidden] ?: d.wifiHidden,
            qrAutoCloseSec = p[K.qrAutoClose] ?: d.qrAutoCloseSec,
            tvPlusOfferSeen = p[K.tvPlusOffered] ?: d.tvPlusOfferSeen,
            apps = AppConfig(
                order = p[K.order]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList(),
                hidden = p[K.hidden] ?: emptySet(),
                favorites = p[K.favorites] ?: emptySet(),
                known = p[K.known] ?: emptySet(),
            ),
        )
    }

    private fun write(p: androidx.datastore.preferences.core.MutablePreferences, s: Settings) {
        p[K.background] = s.background
        p[K.effects] = s.effects
        p[K.activeWeatherMode] = s.activeWeatherMode
        p[K.lightningEnabled] = s.lightningEnabled
        p[K.reduceMotion] = s.reduceMotion
        p[K.iconSize] = s.iconSize
        p[K.showClock] = s.showClock
        p[K.showForecast] = s.showForecast
        p[K.forecastWeekMigrated] = true
        p[K.showWeather] = s.showWeather
        p[K.fahrenheit] = s.fahrenheit
        // Entfernen mit -= : remove() ist als nicht-nullbares T deklariert und stürzt bei fehlendem Double-Schlüssel ab.
        val loc = s.location
        if (loc != null) { p[K.locName] = loc.name; p[K.locLat] = loc.lat; p[K.locLon] = loc.lon } else { p -= K.locName; p -= K.locLat; p -= K.locLon }
        p[K.asEnabled] = s.autostartEnabled
        val pkg = s.autostartPkg
        if (pkg != null) p[K.asPkg] = pkg else p -= K.asPkg
        p[K.asSeconds] = s.autostartSeconds.coerceIn(1, 5)
        p[K.wifiEnabled] = s.wifiEnabled
        p[K.ssid] = s.ssid
        p[K.security] = s.security.name
        p[K.wifiHidden] = s.wifiHidden
        p[K.qrAutoClose] = s.qrAutoCloseSec
        p[K.tvPlusOffered] = s.tvPlusOfferSeen
        p[K.order] = s.apps.order.joinToString("\n")
        p[K.hidden] = s.apps.hidden
        p[K.favorites] = s.apps.favorites
        p[K.known] = s.apps.known
    }

    suspend fun reset() {
        context.store.edit { it.clear() }
    }
}
