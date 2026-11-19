package ch.arjo.terebi.scene

import kotlin.math.abs

/** Ergebnis des Hintergrundmanagers: Szenen-Id aus tokens.json und ob sie nachts abgedunkelt wird. */
data class SceneChoice(val scene: String, val dim: Boolean)

/**
 * Bildet Tageszeit und Wetter auf eine Szene ab. Gleiche Regeln wie preview/src/logic.js;
 * beide prüfen assets/scene-cases.json. Zeiten in Minuten seit Mitternacht, Gerätezeitzone.
 */
object SceneResolver {
    const val WINDOW_MIN = 45

    fun category(code: Int): String = when {
        code in listOf(95, 96, 97, 99) -> "storm"
        code in listOf(56, 57, 66, 67) || code in 71..77 || code == 85 || code == 86 -> "snow"
        code in 51..55 || code in 61..65 || code in 80..82 -> "rain"
        code == 45 || code == 48 -> "fog"
        code in 0..2 -> "clear"
        else -> "cloudy"
    }

    fun resolve(minute: Int, sunrise: Int, sunset: Int, code: Int): SceneChoice {
        val cat = category(code)
        // Wetterlagen bleiben auch abends sichtbar; dunkle Wetterszenen werden sanft abgedunkelt.
        if (cat != "clear") {
            return SceneChoice(cat, minute < sunrise - WINDOW_MIN || minute > sunset + WINDOW_MIN)
        }
        return SceneChoice(
            when {
                abs(minute - sunrise) <= WINDOW_MIN -> "sunrise"
                abs(minute - sunset) <= WINDOW_MIN -> "sunset"
                minute in (sunrise + 1) until sunset -> if (minute < 12 * 60) "morning" else "afternoon"
                minute in (sunset + 1) until 22 * 60 -> "evening"
                else -> "night"
            },
            dim = false,
        )
    }

    /** Tageszeit nach Brandguide 9.4: Dämmerung ± 45 min um Sonnenauf- und -untergang. */
    fun phase(minute: Int, sunrise: Int, sunset: Int): String = when {
        abs(minute - sunrise) <= WINDOW_MIN -> "dawn"
        abs(minute - sunset) <= WINDOW_MIN -> "dusk"
        minute in (sunrise + 1) until sunset -> "day"
        else -> "night"
    }

    /** Himmelsgruppe der Szenenbilder (Guide 9.5). */
    fun group(sceneId: String): String = when (sceneId) {
        "rain", "storm" -> "wet"
        "snow" -> "winter"
        "fog" -> "fog"
        "cloudy" -> "cloudy"
        else -> "clear"
    }

    /**
     * Dateiname des Szenenbilds ohne Endung: `scene_<gruppe>_<phase>_1`. Klare Szenen tragen ihre Tageszeit in der
     * Szenen-Id (manuell gewählt), Wetterszenen nutzen die berechnete [phase].
     */
    fun imageName(sceneId: String, phase: String): String {
        val g = group(sceneId)
        val p = if (g != "clear") phase else when (sceneId) {
            "sunrise" -> "dawn"
            "sunset" -> "dusk"
            "evening", "night" -> "night"
            else -> "day"
        }
        return "scene_${g}_${p}_1"
    }

    /** Globaler Scrim je Szene (Guide 9.5). */
    fun baseScrim(sceneImage: String): Double {
        val parts = sceneImage.split('_')
        val g = parts.getOrNull(1) ?: "clear"
        val p = parts.getOrNull(2) ?: "day"
        val row = when (g) {
            "clear" -> doubleArrayOf(0.20, 0.22, 0.18, 0.16)
            "cloudy" -> doubleArrayOf(0.18, 0.20, 0.18, 0.16)
            "fog" -> doubleArrayOf(0.20, 0.22, 0.18, 0.16)
            "wet" -> doubleArrayOf(0.18, 0.18, 0.18, 0.16)
            else -> doubleArrayOf(0.24, 0.28, 0.20, 0.16)
        }
        return row[listOf("dawn", "day", "dusk", "night").indexOf(p).coerceAtLeast(1)]
    }
}
