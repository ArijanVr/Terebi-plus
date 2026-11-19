---
name: tv-native-development
description: Regeln für Änderungen an der nativen terebi+-App (Kotlin, Compose, D-Pad, Launcher-Intents). Verwenden bei jeder Änderung in app/.
---

# terebi+ native Entwicklung

1. Logik zuerst rein in Kotlin mit JUnit-Test (`apps/AppConfig`, `scene/SceneResolver`, `autostart/AutoStartArm`, `wifi/WifiQr`).
2. Jedes fokussierbare Element: `rememberFocus()` + `tvClick(...)` (ui/Components.kt). Kein `clickable` ohne D-Pad-Weg.
3. Zurück schliesst immer nur die oberste Ebene; auf der Startseite verlässt Zurück den Launcher nie.
4. Einstellungen bleiben erreichbar: Zahnrad oben rechts + Menütaste, auch ohne sichtbare Apps.
5. Werte nur aus `assets/tokens.json` über `LocalDesign`; keine Zahlen in Composables erfinden, ausser `d.px(...)` aus dem 1920-Raster.
6. Netzwerk nur in `Dispatchers.IO`, nur `https://*.open-meteo.com`, nie die UI blockieren.
7. Prüfen: `./gradlew :app:testDebugUnitTest :app:assembleRelease` ohne Warnungen.

Referenzen: developer.android.com/training/tv/start/start · /develop/ui/compose/touch-input/focus ·
/reference/android/app/role/RoleManager · /reference/android/content/pm/PackageManager#getLeanbackLaunchIntentForPackage
