package ch.arjo.terebi

import ch.arjo.terebi.autostart.AutoStartArm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zeiten nach dem echten Ablauf am TCL (System-Log 11.11.2026). */
class AutoStartArmTest {
    @Test
    fun firstStartAfterBootFiresOnResume() {
        val a = AutoStartArm()
        a.onLauncherCreated(sinceBootMs = 40_000)
        assertTrue(a.onResume(40_050))
    }

    @Test
    fun resumeBeforeScreenOnBroadcastFiresOnBroadcast() {
        // TCL: screen_toggled 1 → onResume nach 24 ms → SCREEN_ON-Sendung später
        val a = AutoStartArm()
        assertFalse(a.onResume(1_000_024))
        assertTrue("Sendung nach dem Resume muss sofort starten", a.onScreenOn(1_000_400))
    }

    @Test
    fun screenOnBeforeResumeFiresOnResume() {
        val a = AutoStartArm()
        a.onPause()
        assertFalse(a.onScreenOn(1_000_000))
        assertTrue(a.onResume(1_000_030))
    }

    @Test
    fun secondSignalForSameWakeIsIgnored() {
        // Display-Listener und Sendung kommen beide
        val a = AutoStartArm()
        a.onResume(1_000_000)
        assertTrue(a.onScreenOn(1_000_010))
        assertFalse(a.onScreenOn(1_000_300))
    }

    @Test
    fun quickOffOnFiresAgain() {
        val a = AutoStartArm()
        a.onResume(1_000_000)
        assertTrue(a.onScreenOn(1_000_010))
        a.onPause(); a.onScreenOff()
        assertFalse(a.onResume(1_008_000))
        assertTrue("kurz aus und wieder an muss erneut reagieren", a.onScreenOn(1_008_050))
    }

    @Test
    fun returningFromAppDoesNotFire() {
        val a = AutoStartArm()
        a.onResume(1_000_000)
        assertTrue(a.onScreenOn(1_000_010))
        a.onPause()
        assertFalse(a.onResume(1_004_000))
    }

    @Test
    fun lateHomeAfterWindowDoesNotFire() {
        val a = AutoStartArm()
        a.onPause()
        a.onScreenOn(1_000_000)
        assertFalse(a.onResume(1_000_000 + AutoStartArm.WINDOW_MS + 1))
    }

    @Test
    fun launcherRestartLongAfterBootDoesNotFire() {
        val a = AutoStartArm()
        a.onLauncherCreated(sinceBootMs = 2 * 60 * 60_000L)
        assertFalse(a.onResume(2 * 60 * 60_000L + 1_000))
    }

    @Test
    fun screenOffDisarms() {
        val a = AutoStartArm()
        a.onPause()
        a.onScreenOn(1_000_000)
        a.onScreenOff()
        assertFalse(a.onResume(1_001_000))
    }
}
