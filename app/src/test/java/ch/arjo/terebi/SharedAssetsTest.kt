package ch.arjo.terebi

import ch.arjo.terebi.scene.SceneChoice
import ch.arjo.terebi.scene.SceneResolver
import ch.arjo.terebi.ui.Design
import ch.arjo.terebi.ui.weekday
import ch.arjo.terebi.weather.OpenMeteo
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.LocalDate

/** Prüft die native App gegen dieselben Dateien wie die Vorschau (assets/). Robolectric liefert org.json. */
@RunWith(RobolectricTestRunner::class)
class SharedAssetsTest {
    private fun asset(name: String) = JSONObject(File("../assets/$name").readText())

    @Test fun scenesMatchSharedFixtures() {
        val cases = asset("scene-cases.json").getJSONArray("cases")
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            assertEquals(
                c.toString(),
                SceneChoice(c.getString("scene"), c.getBoolean("dim")),
                SceneResolver.resolve(c.getInt("minute"), c.getInt("sunrise"), c.getInt("sunset"), c.getInt("code")),
            )
        }
    }

    @Test fun tokensLoadAndCoverEveryScene() {
        val d = Design(asset("tokens.json"), asset("weather-codes.json"), 960)
        assertEquals(11, d.scenes.size)
        assertTrue(d.scenes.values.all { it.stops.size == 3 })
        assertEquals(Design.color("#EF3E36"), d.accent)
        assertEquals("Gewitter", d.code(95).label)
        assertEquals("Bedeckt", d.code(12345).label) // unbekannt -> Fallback
        assertEquals(1, d.appColumns)
        assertEquals(Design.color("#8DB3E2"), d.temperatureColor(-15.0))
        assertEquals(Design.color("#F0C27B"), d.temperatureColor(35.0))
        // 1920-px-Tokens auf 960 dp Bildschirmbreite: halbe Werte
        assertEquals(4f, d.appGap.value, 0.01f)
    }

    @Test fun weekdayIsFullySpelledOut() {
        assertEquals("Mittwoch", weekday(LocalDate.of(2026, 11, 18)))
        assertEquals("Sonntag", weekday(LocalDate.of(2026, 11, 22)))
    }

    @Test fun parsesForecastWithSunTimes() {
        val json = """{"current":{"temperature_2m":6.4,"weather_code":61,"is_day":0},
            "daily":{"time":["2026-11-17","2026-11-18","2026-11-19"],"weather_code":[3,61,0],
            "temperature_2m_max":[8.1,7.6,9.0],"temperature_2m_min":[1.0,2.4,0.2],
            "sunrise":["2026-11-17T07:28","2026-11-18T07:30","2026-11-19T07:31"],
            "sunset":["2026-11-17T16:46","2026-11-18T16:45","2026-11-19T16:44"]}}"""
        val w = OpenMeteo.parseForecast(json, "Zürich", 0L, LocalDate.of(2026, 11, 18))
        assertEquals(61, w.code)
        assertEquals(false, w.isDay)
        assertEquals(7 * 60 + 30, w.sunriseMin)
        assertEquals(16 * 60 + 45, w.sunsetMin)
        assertEquals(2, w.days.size)
    }

    @Test fun onlyHttpsOpenMeteoHosts() {
        assertTrue(OpenMeteo.forecastUrl(47.37, 8.55, "Europe/Zurich").startsWith("https://api.open-meteo.com/"))
        assertTrue(OpenMeteo.forecastUrl(47.37, 8.55, "Europe/Zurich").contains("timezone=Europe%2FZurich"))
        assertTrue(OpenMeteo.forecastUrl(47.37, 8.55, "Europe/Zurich").contains("forecast_days=7"))
    }
}
