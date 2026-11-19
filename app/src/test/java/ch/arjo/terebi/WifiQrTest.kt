package ch.arjo.terebi

import ch.arjo.terebi.wifi.WifiQr
import ch.arjo.terebi.wifi.WifiSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testwerte sind erfunden; keine echten Netznamen oder Passwörter (Guide 13). */
class WifiQrTest {
    @Test
    fun escapesAllSpecialCharacters() {
        assertEquals("a\\\\b\\;c\\,d\\:e\\\"f", WifiQr.escape("a\\b;c,d:e\"f"))
    }

    @Test
    fun wpaPayloadWithSpecialCharacters() {
        val p = WifiQr.payload("Gast;Netz", "pa:ss,\"w\\d", WifiSecurity.WPA, hidden = false)
        assertEquals("WIFI:T:WPA;S:Gast\\;Netz;P:pa\\:ss\\,\\\"w\\\\d;;", p)
    }

    @Test
    fun saeHiddenAddsFlag() {
        assertEquals("WIFI:T:SAE;S:Test;P:x;H:true;;", WifiQr.payload("Test", "x", WifiSecurity.SAE, hidden = true))
    }

    @Test
    fun openNetworkHasNoPassword() {
        assertEquals("WIFI:T:nopass;S:Offen;;", WifiQr.payload("Offen", "ignoriert", WifiSecurity.NOPASS, hidden = false))
    }

    @Test
    fun unicodeSurvives() {
        val p = WifiQr.payload("Café ☕", "Grüezi", WifiSecurity.WPA, hidden = false)
        assertEquals("WIFI:T:WPA;S:Café ☕;P:Grüezi;;", p)
        val m = WifiQr.matrix(p)
        assertTrue(m.size >= 21)
    }

    @Test
    fun moduleSizeKeepsQuietZoneAndIsAtLeastFourOnCard() {
        val m = WifiQr.matrix(WifiQr.payload("Gast-WLAN", "beispiel-passwort-123", WifiSecurity.WPA, false))
        val px = WifiQr.modulePx(m.size, 184, 16)
        assertTrue("Modul $px px bei ${m.size} Modulen", px >= 4)
        assertTrue((184 - m.size * px) / 2 >= 4 * px)
        assertTrue((184 - m.size * px) / 2 >= 16)
    }
}
