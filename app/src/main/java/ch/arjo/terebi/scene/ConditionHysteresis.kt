package ch.arjo.terebi.scene

/** Wetterzustände nach Brandguide 9.3 (WMO-Code → Zustand, unbekannte Codes → OVERCAST). */
enum class WeatherCondition {
    CLEAR, PARTLY_CLOUDY, OVERCAST, FOG, DRIZZLE, RAIN, HEAVY_RAIN, THUNDERSTORM, SNOW, SLEET;

    companion object {
        fun of(code: Int): WeatherCondition = when (code) {
            0, 1 -> CLEAR
            2 -> PARTLY_CLOUDY
            45, 48 -> FOG
            51, 53, 55 -> DRIZZLE
            61, 63, 80, 81 -> RAIN
            65, 82 -> HEAVY_RAIN
            95, 96, 97, 99 -> THUNDERSTORM
            71, 73, 75, 77, 85, 86 -> SNOW
            56, 57, 66, 67 -> SLEET
            else -> OVERCAST
        }
    }
}

/**
 * Hysterese (Brandguide 9.8): Ein neuer Zustand gilt erst, wenn zwei Abfragen hintereinander ihn melden.
 * Wechsel zu HEAVY_RAIN oder THUNDERSTORM gelten sofort. Eine neue Instanz pro Ort: der erste Wert gilt sofort.
 */
class ConditionHysteresis {
    private var shown: Int? = null
    private var pending: WeatherCondition? = null

    fun accept(code: Int): Int {
        val cond = WeatherCondition.of(code)
        val current = shown
        when {
            current == null || cond == WeatherCondition.of(current) ||
                cond == WeatherCondition.HEAVY_RAIN || cond == WeatherCondition.THUNDERSTORM || pending == cond -> {
                shown = code; pending = null
            }
            else -> pending = cond
        }
        return shown!!
    }
}
