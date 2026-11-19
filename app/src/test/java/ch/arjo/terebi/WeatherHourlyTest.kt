package ch.arjo.terebi

import ch.arjo.terebi.weather.OpenMeteo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class WeatherHourlyTest {
    private fun json(hourly: String) = """{"current":{"time":"2026-11-18T22:45","temperature_2m":6.4,"weather_code":61,"is_day":0},
        "daily":{"time":["2026-11-18"],"weather_code":[61],"temperature_2m_max":[7.6],"temperature_2m_min":[2.4],
        "sunrise":["2026-11-18T07:30"],"sunset":["2026-11-18T16:45"]}$hourly}"""

    @Test fun hourlyStartsAtCurrentHourAndIsCappedAt24() {
        val times = (20..47).map { LocalDateTime.of(2026, 11, 18, 0, 0).plusHours(it.toLong() - 20 + 20).toString().take(16) }
        val hourly = ""","hourly":{"time":[${times.joinToString(",") { "\"$it\"" }}],
            "temperature_2m":[${times.indices.joinToString(",") { "5.${it % 10}" }}],
            "weather_code":[${times.indices.joinToString(",") { "3" }}],
            "precipitation_probability":[${times.indices.joinToString(",") { "40" }}]}"""
        val w = OpenMeteo.parseForecast(json(hourly), "Zürich", 0L, LocalDate.of(2026, 11, 18))
        assertEquals(24, w.hours.size)
        assertEquals(LocalDateTime.of(2026, 11, 18, 22, 0), w.hours.first().time)
        assertEquals(40, w.hours.first().precipPct)
    }

    @Test fun missingHourlyIsEmptyNotAnError() {
        val w = OpenMeteo.parseForecast(json(""), "Zürich", 0L, LocalDate.of(2026, 11, 18))
        assertTrue(w.hours.isEmpty())
    }

    @Test fun urlRequestsHourlyFields() {
        val u = OpenMeteo.forecastUrl(47.37, 8.54, "Europe/Zurich")
        assertTrue(u.contains("hourly=temperature_2m,weather_code,precipitation_probability"))
        assertTrue(u.contains("forecast_hours=26"))
    }
}
