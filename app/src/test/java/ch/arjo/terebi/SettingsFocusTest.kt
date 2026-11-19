package ch.arjo.terebi

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import ch.arjo.terebi.data.Settings
import ch.arjo.terebi.ui.Design
import ch.arjo.terebi.ui.LocalDesign
import ch.arjo.terebi.ui.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Einstellungen per D-Pad: Rubrikenliste, jede Rubrik öffnen, Zurück (Brandguide 7.5). */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-mdpi")
class SettingsFocusTest {
    @get:Rule val rule = createComposeRule()

    private val sections = listOf("Apps", "Darstellung", "Wetter", "Autostart", "WLAN", "System")

    private fun show() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val design = Design.load(ctx, 960)
        val state = LauncherState(ctx, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
        rule.setContent { CompositionLocalProvider(LocalDesign provides design) { SettingsScreen(state, Settings(), emptyList()) } }
        rule.waitForIdle()
    }

    /** Der Fokus liegt auf der Zeile, die den Text enthält. */
    private fun focused(text: String) = rule.onNode(isFocused()).assert(hasAnyDescendant(hasText(text)))

    private fun press(key: Key) { rule.onRoot().performKeyInput { pressKey(key) }; rule.waitForIdle() }

    @Test fun startsOnFirstSectionAndWalksDownWithoutWrap() {
        show()
        focused("Apps")
        sections.drop(1).forEach { press(Key.DirectionDown); focused(it) }
        press(Key.DirectionDown); focused("System")
    }

    @Test fun everySectionOpensWithBackRowFocusedAndReturnsToTheList() {
        show()
        sections.forEachIndexed { i, name ->
            repeat(i) { press(Key.DirectionDown) }
            focused(name)
            press(Key.DirectionCenter)
            focused("Zurück")
            press(Key.DirectionCenter)
            focused(name)
            repeat(i) { press(Key.DirectionUp) }
        }
    }

    @Test fun sectionContentsAreReachableByDownAndNeverTrapFocus() {
        show()
        // Apps ist ohne installierte Apps leer; die übrigen Rubriken haben bedienbare Zeilen
        sections.drop(1).forEachIndexed { i, name ->
            repeat(i + 1) { press(Key.DirectionDown) }
            focused(name)
            press(Key.DirectionCenter)
            focused("Zurück")
            press(Key.DirectionDown)
            rule.onNode(isFocused()).assert(!hasAnyDescendant(hasText("Zurück")))
            press(Key.DirectionUp); focused("Zurück")
            press(Key.DirectionUp); focused("Zurück") // kein Umlauf nach oben
            press(Key.DirectionCenter); focused(name)
            repeat(i + 1) { press(Key.DirectionUp) }
        }
    }
}
