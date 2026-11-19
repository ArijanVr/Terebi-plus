package ch.arjo.terebi

import ch.arjo.terebi.scene.ConditionHysteresis
import ch.arjo.terebi.scene.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test

class ConditionHysteresisTest {
    @Test fun everyWmoCodeOfGuide93IsMapped() {
        val expected = mapOf(
            WeatherCondition.CLEAR to listOf(0, 1), WeatherCondition.PARTLY_CLOUDY to listOf(2), WeatherCondition.OVERCAST to listOf(3, 999),
            WeatherCondition.FOG to listOf(45, 48), WeatherCondition.DRIZZLE to listOf(51, 53, 55),
            WeatherCondition.RAIN to listOf(61, 63, 80, 81), WeatherCondition.HEAVY_RAIN to listOf(65, 82),
            WeatherCondition.THUNDERSTORM to listOf(95, 96, 97, 99), WeatherCondition.SNOW to listOf(71, 73, 75, 77, 85, 86),
            WeatherCondition.SLEET to listOf(56, 57, 66, 67),
        )
        expected.forEach { (c, codes) -> codes.forEach { assertEquals("Code $it", c, WeatherCondition.of(it)) } }
    }

    @Test fun firstReportIsTakenAtOnce() {
        assertEquals(61, ConditionHysteresis().accept(61))
    }

    @Test fun newConditionNeedsTwoConsecutiveReports() {
        val h = ConditionHysteresis()
        assertEquals(0, h.accept(0))
        assertEquals(0, h.accept(3))   // einmal Bedeckt: noch Sonnig
        assertEquals(3, h.accept(3))   // zweimal: übernommen
    }

    @Test fun aBlipBetweenIsIgnored() {
        val h = ConditionHysteresis()
        h.accept(0)
        h.accept(3)
        assertEquals(0, h.accept(0))   // zurück: Blip verworfen
        assertEquals(0, h.accept(3))   // zählt wieder von vorn
    }

    @Test fun heavyRainAndThunderAreImmediate() {
        val h = ConditionHysteresis()
        h.accept(3)
        assertEquals(65, h.accept(65))
        h.accept(3); h.accept(3)
        assertEquals(95, h.accept(95))
    }

    @Test fun refinedCodeOfSameConditionIsTakenAtOnce() {
        val h = ConditionHysteresis()
        h.accept(61)
        assertEquals(63, h.accept(63))
    }
}
