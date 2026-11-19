package ch.arjo.terebi

import androidx.test.core.app.ApplicationProvider
import ch.arjo.terebi.data.SettingsStore
import ch.arjo.terebi.weather.Location
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsStoreTest {
    private val store = SettingsStore(ApplicationProvider.getApplicationContext())

    /**
     * Erster Start: kein Standort gespeichert, keine Autostart-App. Absturz im Emulator am 2026-11-18.
     * Ohne gespeicherten Ort gilt der Standardort, sonst bliebe die Wetterkarte leer.
     */
    @Test fun updateWithoutLocationDoesNotCrash() = runTest {
        store.update { it.copy(iconSize = "L") }
        val s = store.settings.first()
        assertEquals("L", s.iconSize)
        assertEquals("Zürich", s.location?.name)
    }

    @Test fun locationRoundTripAndRemoval() = runTest {
        store.update { it.copy(location = Location("Bern", 46.95, 7.45), autostartPkg = "x") }
        assertEquals("Bern", store.settings.first().location?.name)
        // Entfernen fällt auf den Standardort zurück; die App hat keine Oberfläche zum Leeren.
        store.update { it.copy(location = null, autostartPkg = null) }
        assertEquals("Zürich", store.settings.first().location?.name)
        assertNull(store.settings.first().autostartPkg)
    }

    @Test fun weeklyForecastCanStillBeTurnedOff() = runTest {
        assertEquals(true, store.settings.first().showForecast)
        store.update { it.copy(showForecast = false) }
        assertEquals(false, store.settings.first().showForecast)
    }
}
