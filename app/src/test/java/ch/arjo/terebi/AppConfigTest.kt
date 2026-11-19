package ch.arjo.terebi

import ch.arjo.terebi.apps.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigTest {
    private val base = AppConfig().sync(listOf("a", "b", "c", "d"))

    @Test fun firstSyncShowsAllInInstallOrder() {
        assertEquals(listOf("a", "b", "c", "d"), base.home())
    }

    @Test fun hiddenAppStaysHiddenAfterResync() {
        val c = base.toggleHidden("b").sync(listOf("a", "b", "c", "d"))
        assertEquals(listOf("a", "c", "d"), c.home())
    }

    @Test fun newAppAppendedUninstalledCleanedUp() {
        val c = base.toggleHidden("c").toggleFavorite("d").sync(listOf("a", "b", "d", "e"))
        assertEquals(listOf("a", "b", "d", "e"), c.order)
        assertEquals(emptySet<String>(), c.hidden) // c deinstalliert: aus der Konfiguration entfernt
        assertEquals(setOf("d"), c.favorites)
        assertEquals(listOf("d", "a", "b", "e"), c.home())
    }

    @Test fun reorderSurvivesResync() {
        val c = base.moveInOrder("d", -1).moveInOrder("d", -1)
        assertEquals(listOf("a", "d", "b", "c"), c.sync(listOf("c", "b", "a", "d")).order)
    }

    @Test fun moveOnHomeKeepsFavoritesFirst() {
        val c = base.toggleFavorite("c")
        assertEquals(listOf("c", "a", "b", "d"), c.home())
        assertEquals(c, c.moveOnHome("a", -1)) // würde über den Favoriten springen
        assertEquals(listOf("c", "b", "a", "d"), c.moveOnHome("a", 1).home())
        assertEquals(c, c.moveOnHome("d", 1)) // Rand
    }
}
