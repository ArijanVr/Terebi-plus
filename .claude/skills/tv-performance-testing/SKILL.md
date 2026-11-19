---
name: tv-performance-testing
description: Test- und Messablauf für terebi+ (Unit-Tests, Vorschau, Emulator, echter TV). Verwenden vor jedem Abschluss einer Phase und bei Performance-Fragen.
---

# terebi+ Tests und Leistung

1. Immer: `npm --prefix preview test` und `./gradlew :app:testDebugUnitTest`.
2. Vorschau-Durchgang nur per Tastatur: Startseite → jede Einstellungsrubrik → Zurück-Kette → QR → Autostart mit Abbruch.
3. Emulator (Android-TV-Image) oder TV: Home-Taste, App-Start und Rückkehr (kein erneuter Autostart), Offline-Start (Flugmodus/WLAN aus).
4. Messen statt vermuten: `adb shell am start -W ch.arjo.terebi/.MainActivity` (Kaltstart), `adb shell dumpsys gfxinfo ch.arjo.terebi` (Janky Frames), `adb shell dumpsys meminfo ch.arjo.terebi`.
5. Ergebnis in TASKS.md eintragen, getrennt nach automatisiert / Emulator / Hardware. Nichts als getestet melden, was nicht lief.

Referenzen: developer.android.com/topic/performance/vitals/launch-time · /studio/profile
