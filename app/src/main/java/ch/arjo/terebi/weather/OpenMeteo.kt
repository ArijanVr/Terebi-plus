package ch.arjo.terebi.weather

import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.roundToInt

data class WeatherDay(val date: LocalDate, val code: Int, val min: Int, val max: Int)

data class WeatherHour(val time: LocalDateTime, val tempC: Int, val code: Int, val precipPct: Int)

data class Weather(
    val place: String,
    val tempC: Double,
    val code: Int,
    val isDay: Boolean,
    /** Minuten seit Mitternacht in Gerätezeitzone (Open-Meteo liefert in der angefragten Zone). */
    val sunriseMin: Int,
    val sunsetMin: Int,
    val days: List<WeatherDay>,
    val fetchedAtMillis: Long,
    val rainMm: Double = 0.0,
    val snowfallCm: Double = 0.0,
    val windKmh: Double = 0.0,
    val windDirectionDeg: Double = 0.0,
    /** Nächste 24 Stunden ab der laufenden Stunde; leer, wenn ein älterer Zwischenspeicher keine Stundenwerte hat. */
    val hours: List<WeatherHour> = emptyList(),
)

data class Place(val name: String, val detail: String, val latitude: Double, val longitude: Double)

/** Open-Meteo (CC BY 4.0, kein Schlüssel). Nur HTTPS, nur diese zwei Hosts. */
object OpenMeteo {
    const val FORECAST_HOST = "api.open-meteo.com"
    const val GEOCODING_HOST = "geocoding-api.open-meteo.com"

    fun forecastUrl(lat: Double, lon: Double, zoneId: String): String =
        "https://$FORECAST_HOST/v1/forecast?latitude=${fmt(lat)}&longitude=${fmt(lon)}" +
            "&current=temperature_2m,weather_code,is_day,rain,snowfall,wind_speed_10m,wind_direction_10m" +
            "&hourly=temperature_2m,weather_code,precipitation_probability&forecast_hours=26" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset" +
            "&timezone=${URLEncoder.encode(zoneId, "UTF-8")}&forecast_days=7"

    fun geocodingUrl(query: String): String =
        "https://$GEOCODING_HOST/v1/search?name=${URLEncoder.encode(query, "UTF-8")}&count=6&language=de"

    private fun fmt(v: Double) = String.format(Locale.ROOT, "%.4f", v)

    /** Wirft bei fehlenden Feldern; der Aufrufer behandelt das als «keine Daten». */
    fun parseForecast(json: String, place: String, fetchedAtMillis: Long, today: LocalDate): Weather {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val days = (0 until dates.length()).mapNotNull { i ->
            val codes = daily.getJSONArray("weather_code")
            val maxs = daily.getJSONArray("temperature_2m_max")
            val mins = daily.getJSONArray("temperature_2m_min")
            if (codes.isNull(i) || maxs.isNull(i) || mins.isNull(i)) return@mapNotNull null
            WeatherDay(LocalDate.parse(dates.getString(i)), codes.getInt(i), mins.getDouble(i).roundToInt(), maxs.getDouble(i).roundToInt())
        }.filter { !it.date.isBefore(today) }
        require(days.isNotEmpty()) { "keine Tage ab heute" }
        val todayIdx = (0 until dates.length()).firstOrNull { LocalDate.parse(dates.getString(it)) == today } ?: 0
        fun minuteOf(key: String, fallback: Int) = runCatching {
            LocalDateTime.parse(daily.getJSONArray(key).getString(todayIdx)).let { it.hour * 60 + it.minute }
        }.getOrDefault(fallback)
        return Weather(
            place = place,
            tempC = current.getDouble("temperature_2m"),
            code = current.getInt("weather_code"),
            isDay = current.optInt("is_day", 1) == 1,
            sunriseMin = minuteOf("sunrise", 7 * 60),
            sunsetMin = minuteOf("sunset", 18 * 60),
            days = days,
            fetchedAtMillis = fetchedAtMillis,
            rainMm = current.optDouble("rain", 0.0),
            snowfallCm = current.optDouble("snowfall", 0.0),
            windKmh = current.optDouble("wind_speed_10m", 0.0),
            windDirectionDeg = current.optDouble("wind_direction_10m", 0.0),
            hours = parseHours(root, current.optString("time")),
        )
    }

    private fun parseHours(root: JSONObject, currentTime: String): List<WeatherHour> {
        val h = root.optJSONObject("hourly") ?: return emptyList()
        val times = h.optJSONArray("time") ?: return emptyList()
        val temps = h.optJSONArray("temperature_2m") ?: return emptyList()
        val codes = h.optJSONArray("weather_code") ?: return emptyList()
        val probs = h.optJSONArray("precipitation_probability")
        val from = runCatching { LocalDateTime.parse(currentTime).withMinute(0).withSecond(0).withNano(0) }.getOrNull()
        return (0 until times.length()).mapNotNull { i ->
            if (temps.isNull(i) || codes.isNull(i)) return@mapNotNull null
            val t = runCatching { LocalDateTime.parse(times.getString(i)) }.getOrNull() ?: return@mapNotNull null
            if (from != null && t.isBefore(from)) return@mapNotNull null
            WeatherHour(t, temps.getDouble(i).roundToInt(), codes.getInt(i), if (probs == null || probs.isNull(i)) 0 else probs.getInt(i))
        }.take(24)
    }

    fun parseGeocoding(json: String): List<Place> {
        val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val o = results.getJSONObject(i)
            Place(
                name = o.getString("name"),
                detail = listOf(o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", "),
                latitude = o.getDouble("latitude"),
                longitude = o.getDouble("longitude"),
            )
        }
    }
}
