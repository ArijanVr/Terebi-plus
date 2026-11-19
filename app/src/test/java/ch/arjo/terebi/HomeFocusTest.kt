package ch.arjo.terebi

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import ch.arjo.terebi.apps.AppConfig
import ch.arjo.terebi.apps.AppRepository
import ch.arjo.terebi.apps.InstalledApp
import ch.arjo.terebi.data.Settings
import ch.arjo.terebi.ui.Design
import ch.arjo.terebi.ui.HomeActions
import ch.arjo.terebi.ui.HomeScreen
import ch.arjo.terebi.ui.LocalDesign
import ch.arjo.terebi.weather.WeatherUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/** D-Pad-Karte der Startseite nach Brandguide 6.2 (Robolectric, ohne Emulator). */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-mdpi")
class HomeFocusTest {
    @get:Rule val rule = createComposeRule()

    private val apps = listOf("a" to "Alpha", "b" to "Beta", "c" to "Gamma")
    private val weather = "Wetter und Vorhersage für sieben Tage"

    private fun show() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val design = Design.load(ctx, 960)
        val settings = Settings(apps = AppConfig(order = apps.map { it.first }, known = apps.map { it.first }.toSet()))
        rule.setContent {
            CompositionLocalProvider(LocalDesign provides design) {
                HomeScreen(
                    settings, apps.map { InstalledApp(it.first, it.second) }, WeatherUi(), LocalTime.of(20, 0), LocalDate.of(2026, 11, 18),
                    null, AppRepository(ctx), null, 0, false,
                    HomeActions({}, {}, {}, {}, {}, { _, _ -> }, {}),
                )
            }
        }
        rule.waitForIdle()
    }

    private fun press(key: Key) { rule.onRoot().performKeyInput { pressKey(key) }; rule.waitForIdle() }

    @Test fun startFocusIsFirstRailRow() { show(); rule.onNodeWithContentDescription("Alpha").assertIsFocused() }

    @Test fun downMovesThroughRailAndStaysOnLastRow() {
        show()
        press(Key.DirectionDown); rule.onNodeWithContentDescription("Beta").assertIsFocused()
        press(Key.DirectionDown); rule.onNodeWithContentDescription("Gamma").assertIsFocused()
        press(Key.DirectionDown); rule.onNodeWithContentDescription("Gamma").assertIsFocused() // kein Umlauf
    }

    @Test fun rightGoesToWeatherAndLeftReturnsToLastRailRow() {
        show()
        press(Key.DirectionDown)
        press(Key.DirectionRight); rule.onNodeWithContentDescription(weather).assertIsFocused()
        press(Key.DirectionLeft); rule.onNodeWithContentDescription("Beta").assertIsFocused() // Fokus-Gedächtnis
    }

    @Test fun gearIsAboveWeatherAndAboveFirstRow() {
        show()
        press(Key.DirectionUp); rule.onNodeWithContentDescription("Einstellungen").assertIsFocused()
        press(Key.DirectionDown); rule.onNodeWithContentDescription(weather).assertIsFocused()
        press(Key.DirectionUp); rule.onNodeWithContentDescription("Einstellungen").assertIsFocused()
        press(Key.DirectionLeft); rule.onNodeWithContentDescription("Alpha").assertIsFocused()
    }
}
