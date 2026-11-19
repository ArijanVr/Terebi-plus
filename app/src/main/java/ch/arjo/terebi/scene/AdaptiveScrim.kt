package ch.arjo.terebi.scene

import kotlin.math.min
import kotlin.math.pow

/**
 * Adaptive Abdunklung (Brandguide 7.6): Hinter Text muss die effektive Abdunklung gegenüber dem hellsten
 * Bildanteil mindestens 0.69 erreichen. Reicht sie nicht, steigt der globale Scrim in 0.04-Schritten bis 0.40.
 */
object AdaptiveScrim {
    const val BASE = 0.16
    private const val STEP = 0.04
    private const val MAX = 0.40
    private const val MIN_EFFECTIVE = 0.69

    /** Textzone mit der Abdunklung aller lokalen Scrims ohne den globalen (aus der Kontrasttabelle 3.4 zurückgerechnet). */
    data class Zone(val name: String, val localAlpha: Double)

    val TEXT_ZONES = listOf(Zone("rail", 0.652), Zone("header", 0.651), Zone("context", 0.801))

    fun effective(global: Double, local: Double) = 1 - (1 - global) * (1 - local)

    /** Gesuchte Abdunklung, damit eine Fläche der Luminanz [lum] (0..1) so dunkel wie reines Weiss unter 0.69 wird. */
    fun required(lum: Double) = if (lum <= 1 - MIN_EFFECTIVE) 0.0 else 1 - (1 - MIN_EFFECTIVE) / lum

    /** [lum95] je Zonenname: 95. Perzentil der relativen Luminanz. Fehlt eine Zone, gilt reines Weiss. */
    fun globalAlpha(lum95: Map<String, Double>, base: Double = BASE): Double {
        var g = base
        while (g + 1e-9 < MAX && TEXT_ZONES.any { effective(g, it.localAlpha) < required(lum95[it.name] ?: 1.0) }) g += STEP
        return min(g, MAX)
    }

    private fun linear(c: Int): Double = (c / 255.0).let { if (it <= 0.04045) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }

    fun luminance(argb: Int): Double =
        0.2126 * linear((argb shr 16) and 0xFF) + 0.7152 * linear((argb shr 8) and 0xFF) + 0.0722 * linear(argb and 0xFF)

    fun percentile95(values: List<Double>): Double = if (values.isEmpty()) 1.0 else values.sorted()[((values.size - 1) * 0.95).toInt()]
}
