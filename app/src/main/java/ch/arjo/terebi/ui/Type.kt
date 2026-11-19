package ch.arjo.terebi.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import ch.arjo.terebi.R

/** Geist (SIL OFL), lokal gebündelt; kein Font-Abruf zur Laufzeit. */
val TerebiFont: FontFamily = FontFamily(
    Font(R.font.geist_light, FontWeight.Light),
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

/** Geist Mono nur für technische Werte (Version, Paketname); Brandguide § 4. */
val TerebiMono: FontFamily = FontFamily(Font(R.font.geist_mono_regular, FontWeight.Normal))
