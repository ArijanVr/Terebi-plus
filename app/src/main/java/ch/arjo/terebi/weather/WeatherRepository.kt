package ch.arjo.terebi.weather

import android.content.Context
import android.util.Log
import ch.arjo.terebi.scene.ConditionHysteresis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class WeatherUi(val weather: Weather? = null, val offline: Boolean = false)

data class Location(val name: String, val lat: Double, val lon: Double)

/**
 * Wetter mit Datei-Cache. Abruf beim Start, dann alle 15 min; bei Fehler erneut nach 5, 15, 30 min.
 * Ohne Netz bleiben die letzten Daten bis 12 Stunden sichtbar und werden als offline markiert.
 * Netzwerkfehler blockieren nie die Oberfläche: alles läuft in Dispatchers.IO.
 */
class WeatherRepository(context: Context) {
    private val cacheFile = File(context.filesDir, "weather_cache.json")
    private val _state = MutableStateFlow(WeatherUi())
    val state: StateFlow<WeatherUi> = _state.asStateFlow()
    private val zone: ZoneId get() = ZoneId.of("Europe/Zurich")

    /** Läuft, solange die Startseite sichtbar ist; bei Ortswechsel neu starten. */
    suspend fun run(loc: Location, force: Boolean = false) {
        val key = key(loc)
        val cached = withContext(Dispatchers.IO) { readCache(key, loc.name) }
        // Hysterese (Guide 9.8): Zustand erst nach zwei gleichen Abfragen übernehmen; der Cache gilt sofort
        val hysteresis = ConditionHysteresis()
        _state.value = WeatherUi(cached?.let { it.copy(code = hysteresis.accept(it.code)) }, offline = false)
        val interval = TimeUnit.MINUTES.toMillis(INTERVAL_MIN)
        val backoff = longArrayOf(5, 15, 30).map { TimeUnit.MINUTES.toMillis(it) }
        var failures = 0
        // Ein alter Cache mit vier Tagen soll nach dem Upgrade sofort durch die Wochenprognose ersetzt werden.
        var last = if (force || (cached != null && cached.days.size < 7)) 0L else cached?.fetchedAtMillis ?: 0L
        while (true) {
            val age = System.currentTimeMillis() - last
            if (failures == 0 && age in 0 until interval) {
                delay(interval - age)
                continue
            }
            val fresh = runCatching { withContext(Dispatchers.IO) { fetch(loc, key) } }
                .onFailure { Log.w(TAG, "Wetterabruf fehlgeschlagen (${loc.name}): ${it::class.java.simpleName}: ${it.message}") }
                .getOrNull()
            if (fresh != null) {
                failures = 0
                last = fresh.fetchedAtMillis
                _state.value = WeatherUi(fresh.copy(code = hysteresis.accept(fresh.code)), offline = false)
            } else {
                _state.value = WeatherUi(_state.value.weather, offline = true)
                delay(backoff[failures.coerceAtMost(backoff.lastIndex)])
                failures++
            }
        }
    }

    fun clearCache() {
        cacheFile.delete()
        _state.value = WeatherUi()
    }

    private fun key(l: Location) = "${l.lat},${l.lon},${zone.id}"

    private fun fetch(loc: Location, key: String): Weather {
        val body = httpGet(OpenMeteo.forecastUrl(loc.lat, loc.lon, zone.id), OpenMeteo.FORECAST_HOST)
        val now = System.currentTimeMillis()
        val w = OpenMeteo.parseForecast(body, loc.name, now, LocalDate.now(zone))
        val tmp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
        tmp.writeText(JSONObject().put("key", key).put("fetchedAt", now).put("raw", body).toString())
        tmp.renameTo(cacheFile)
        return w
    }

    private fun readCache(key: String, place: String): Weather? = runCatching {
        if (!cacheFile.exists()) return null
        val o = JSONObject(cacheFile.readText())
        if (o.getString("key") != key) return null
        val at = o.getLong("fetchedAt")
        if (System.currentTimeMillis() - at > TimeUnit.HOURS.toMillis(12)) return null
        OpenMeteo.parseForecast(o.getString("raw"), place, at, LocalDate.now(zone))
    }.getOrNull()

    suspend fun searchPlaces(query: String): Result<List<Place>> = withContext(Dispatchers.IO) {
        runCatching { OpenMeteo.parseGeocoding(httpGet(OpenMeteo.geocodingUrl(query), OpenMeteo.GEOCODING_HOST)) }
    }

    companion object {
        private const val TAG = "TerebiWeather"
        const val INTERVAL_MIN = 15L

        fun httpGet(url: String, expectedHost: String): String {
            val u = URL(url)
            require(u.protocol == "https" && u.host == expectedHost) { "Ziel nicht erlaubt" }
            val c = u.openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 10_000
                c.readTimeout = 10_000
                c.instanceFollowRedirects = false
                c.setRequestProperty("Accept", "application/json")
                if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
                return c.inputStream.bufferedReader().use { it.readText() }
            } finally {
                c.disconnect()
            }
        }
    }
}
