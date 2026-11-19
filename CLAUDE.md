# CLAUDE.md – terebi+

Minimalistischer Launcher für Google TV / Android TV. Deutsch (Schweiz), «ss» statt «ß».
Massgebend ist die native App in `app/`. `preview/` ist nur die Localhost-Designvorschau.

## Architektur (kurz)

- `app/` Kotlin, Jetpack Compose (foundation, ohne TV-Material), DataStore, Coroutines/StateFlow. Paket `ch.arjo.terebi`, minSdk 28.
  - `LauncherState` hält den Zustand (kein ViewModel; Activity ist singleTask und überlebt Konfigurationswechsel).
  - `apps/` AppConfig (rein, getestet) + AppRepository (PackageManager). `scene/` SceneResolver. `weather/` Open-Meteo + Datei-Cache.
  - `autostart/` AutoStartArm (Sitzung nur bei Einschalten). `wifi/` WifiQr (ZXing) + SecretStore (Keystore, AES-GCM).
  - `ui/` Design lädt `assets/tokens.json` zur Laufzeit; Screens und Overlays.
- `assets/` gemeinsame Quelle: `brandguide-tokens.json` (kanonischer Guide 1.1.0), `tokens.json` (abgeleitete Laufzeitstruktur), `weather-codes.json`, `scene-cases.json`, `brand/`. Wird als Android-Asset gepackt.
- `preview/` Vite + Vanilla-JS. Liest dieselben `assets/`-Dateien. `preview/src/icons.js` ist Quelle der Wettersymbole (`node scripts/gen-icons.mjs`).

## Befehle

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
./gradlew :app:testDebugUnitTest :app:assembleRelease
npm --prefix preview install && npm --prefix preview run dev   # http://localhost:5173
npm --prefix preview test
```

## Regeln

- Kein WebView, kein Tracking, keine Analytics, kein Konto. Berechtigungen: INTERNET (Open-Meteo, HTTPS) und REQUEST_INSTALL_PACKAGES (nur für die Installation und Aktualisierung von IP Television+ über den Android-Installer, Entscheid Arijan 2026-11-19); keine weiteren.
- Niemals Systempakete deaktivieren, kein Root, kein `pm disable` ohne ausdrückliche Zustimmung von Arijan.
- Szenen-, Token- oder Wettercode-Änderungen nur in `assets/`; beide Seiten testen gegen dieselben Dateien.
- Logik zuerst in reinem Kotlin mit Unit-Test (AppConfig, SceneResolver, AutoStartArm, WifiQr).
- WLAN-Passwort nie loggen, nie in DataStore, nie im Klartext speichern.
- Testergebnisse nur melden, wenn ausgeführt. Status in `TASKS.md`, Entscheidungen in `DECISIONS.md`.

## Definition of Done

Siehe `TASKS.md` (Abschnitt «Definition of Done») mit Status je Punkt: implementiert / automatisiert getestet / Emulator / echte Hardware.

<!-- Projektelab -->
Ablage, Releases, Design: `~/Library/Mobile Documents/com~apple~CloudDocs/Projektelab/03_Launcher/Terebi+/` nach
`Projektelab/MAIN.md`. Keine eigenen Ablageregeln.
