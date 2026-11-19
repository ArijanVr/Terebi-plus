package ch.arjo.terebi.autostart

/**
 * Entscheidet, wann der Autostart-Countdown beginnt. Auslöser: Einschalten (erster Start nach dem
 * Booten) oder Aufwachen aus Standby (Bildschirm an). Rückkehr aus einer App löst nichts aus.
 *
 * Am TCL kommt onResume beim Aufwachen rund 20 ms nach «Bildschirm an», die Systemsendung
 * SCREEN_ON aber erst danach. Deshalb ist die Reihenfolge egal: was zuletzt eintrifft, startet sofort.
 * Zeiten in Millisekunden seit dem Booten (SystemClock.elapsedRealtime).
 */
class AutoStartArm(private val windowMs: Long = WINDOW_MS) {
    private var armedAt = -1L
    private var resumedAt = -1L
    private var lastFiredAt = Long.MIN_VALUE / 2

    /** Erster Start des Launchers kurz nach dem Booten. */
    fun onLauncherCreated(sinceBootMs: Long) {
        if (sinceBootMs <= BOOT_GRACE_MS) armedAt = sinceBootMs
    }

    /**
     * Bildschirm geht an (Sendung oder Display-Listener, beide dürfen kommen).
     * Liefert true, wenn der Countdown jetzt beginnen soll.
     */
    fun onScreenOn(nowMs: Long): Boolean {
        if (nowMs - lastFiredAt < DEDUPE_MS) return false
        armedAt = nowMs
        return tryFire(nowMs)
    }

    /** Bildschirm geht aus: offene Auslöser verfallen. */
    fun onScreenOff() {
        armedAt = -1L
        resumedAt = -1L
    }

    /** Startbildschirm ist sichtbar. Liefert true, wenn der Countdown jetzt beginnen soll. */
    fun onResume(nowMs: Long): Boolean {
        resumedAt = nowMs
        return tryFire(nowMs)
    }

    fun onPause() {
        resumedAt = -1L
    }

    private fun tryFire(nowMs: Long): Boolean {
        val armed = armedAt >= 0 && nowMs - armedAt in 0..windowMs
        val resumed = resumedAt >= 0
        if (!armed || !resumed) {
            if (armedAt >= 0 && nowMs - armedAt > windowMs) armedAt = -1L
            return false
        }
        armedAt = -1L
        lastFiredAt = nowMs
        return true
    }

    companion object {
        /** Startbildschirm muss innert 10 s nach dem Auslöser erscheinen, sonst verfällt er. */
        const val WINDOW_MS = 10_000L
        /** Zweites Signal für dasselbe Aufwachen (Sendung und Display-Listener) wird ignoriert. */
        const val DEDUPE_MS = 5_000L
        /** Launcher-Start gilt bis 5 min nach dem Booten als «Einschalten». */
        const val BOOT_GRACE_MS = 5 * 60_000L
    }
}
