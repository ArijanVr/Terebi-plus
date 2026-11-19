# Übergabe terebi+ (Stand 2026-11-18)

Für einen anderen KI-Agenten. Zuerst lesen: `CLAUDE.md`, `TASKS.md`, `DECISIONS.md` im Repo und
`~/Library/Mobile Documents/com~apple~CloudDocs/Projektelab/MAIN.md` (Ablageregeln, Sicherheitsregel J).
Sprache: Deutsch (Schweiz), «ss» statt «ß».

## Verbindlichkeit

Dieses Dokument beschreibt einen Stand, keinen Dauerzustand. Bei Widerspruch gilt, von oben nach unten:

1. `Projektelab/MAIN.md` und die ausdrückliche Anweisung von Arijan in der laufenden Sitzung
2. `CLAUDE.md` (Architektur und Regeln)
3. `TASKS.md` (offene Arbeit, Status) und `DECISIONS.md` (Entscheidungen)
4. dieses Dokument

Abschnitte vor dem «Nachtrag Brandguide 1.1.0» beschreiben den Stand davor; wo sie dem Nachtrag widersprechen, gilt der Nachtrag. Anweisungen, die nur für eine Sitzung galten, sind mit ihrem Gültigkeitsbereich genannt und sonst nicht mehr bindend.

## Ziel

Minimalistischer, nativer Launcher für Google TV / Android TV («terebi+»). Apps im Zentrum, Wetter und
dynamischer Hintergrund, Autostart, WLAN-QR für Gäste. Kein Konto, keine Werbung, kein Tracking, nur Berechtigung INTERNET.
Echter Launcher, kein WebView. Die Browser-Vorschau ist nur zur Gestaltung.

## Orte

- Repo: `~/Developer/Terebi-plus` (lokales Git, **noch kein Commit, kein Remote**). Nicht in iCloud (Regel E).
- Projektordner (Doku, Logos, später Releases): `~/Library/Mobile Documents/com~apple~CloudDocs/Projektelab/03_Launcher/Terebi+/`
  mit `Projekt.md`, `Code/GitHub.md`, `Design/Logo/*.svg|jpg`. `INDEX.md` ist nachgeführt (Version 0.1.0 Entwicklung).
- Verwandt: `~/Developer/Launcher-plus` (ARJO Home, anderes Projekt). Daraus wurden AutoStartArm, SecretStore, WifiQr und der Gradle-Versionssatz übernommen.

## Aufbau

- `app/` Kotlin, Compose (foundation), DataStore, Paket `ch.arjo.terebi`, minSdk 28, Ziel 37.
  `LauncherState` (Zustand), `apps/` (AppConfig, AppRepository), `scene/SceneResolver`, `weather/` (Open-Meteo + Datei-Cache),
  `autostart/AutoStartArm`, `wifi/` (WifiQr, SecretStore), `data/SettingsStore`, `ui/` (Design, Components, HomeScreen, SettingsScreen, Overlays, Background), `MainActivity`.
- `assets/` gemeinsame Quelle für App und Vorschau: `tokens.json`, `weather-codes.json`, `scene-cases.json`, `brand/`. Wird als Android-Asset gepackt.
- `preview/` Vite + Vanilla-JS, nur localhost: `npm --prefix preview run dev` → http://localhost:5173.
- `scripts/gen-icons.mjs` erzeugt Android-Vektor-Wettersymbole aus `preview/src/icons.js`.
- `.claude/skills/` vier Projekt-Skills (tv-native-development, tv-premium-design, tv-performance-testing, tv-security-release).

## Befehle

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
./gradlew :app:testDebugUnitTest :app:assembleRelease     # 28 Tests, APK ~2,2 MB
npm --prefix preview test                                  # 6 Tests
```

`local.properties` (nicht im Repo): `sdk.dir=/opt/homebrew/share/android-commandlinetools`.
Release-APK ist nur mit dem Debug-Schlüssel signiert (zum Testen). Echte Signatur fehlt.

## Stand

Fertig und geprüft:
- Phase 0–3 (Umgebung, Designsystem, Vorschau, native App). Vorschau im Browser per Tastatur durchgespielt.
- Unit-Tests: Kotlin 28/28, Vorschau 6/6 grün (Szenenregeln gegen gemeinsame Fixtures, AppConfig, Autostart, WLAN-Payload, Parser, SettingsStore, Wochen-Mock).
- Wochenvorhersage ab heute für sieben Tage standardmässig sichtbar; alte Anzeige-Einstellungen werden einmalig umgestellt. Die bewölkte Szene bleibt auch nachts wetterabhängig und hat einen ruhigen Wolkeneffekt. Vorschau und APK bauen; sieben echte Tage wurden im Emulator angezeigt.
- Brandguide-Umsetzung: NPS-Foto «Morning Fog at Lake McDonald» als mitgeliefertes 1920 × 1080 WebP (176 KB), Wetterüberlagerungen darüber, App-Kacheln einspaltig links, Wochenkarte 624 × 568 rechts mit Temperaturfarben und gemeinsamer Skala. WLAN-QR erscheint unten in der App-Leiste, wenn aktiviert und konfiguriert. Quelle/Lizenz: `assets/backgrounds/LICENSES.md`. Native App und Vorschau visuell geprüft; QR mit Demo-Netz in der Vorschau geprüft.
- Emulator: AVD `terebi_gtv34` (Google TV, Android 14, API 34, arm64, Image `system-images;android-34;google-tv;arm64-v8a`).
  Start mit `ANDROID_SDK_ROOT=/opt/homebrew/share/android-commandlinetools /opt/homebrew/share/android-commandlinetools/emulator/emulator -avd terebi_gtv34 -no-snapshot-save -gpu host`.
  terebi+ ist dort Home (`adb shell cmd role add-role-holder android.app.role.HOME ch.arjo.terebi 0`).
  Getestet: Kaltstart 119–150 ms, App-Banner, Home/Zurück, Einstellungen, Tastatur und echte Open-Meteo-Suche, Wetter, Autostart (Countdown nach Aufwachen, Start nach 3 s, Abbruch, kein Doppelstart bei Rückkehr), Offline-Start, Langdruck-Menü, Verschieben, Ausblenden, Persistenz, 29 MB PSS.
- Fünf im Emulator gefundene Fehler behoben (u. a. Absturz beim ersten Speichern, Langdruck startete App).

Offen:
- ✅ **Commit.** Erster Commit `4f37337` (Übergabestand) mit Co-Authored-By-Zeile gesetzt. Push/Remote bleiben offen und brauchen die Freigabe von Arijan.
- **Echter Fernseher** (TCL Smart TV Pro, Android 14): nicht getestet. `adb install` und Home-Rolle nur nach Freigabe von Arijan. IP und Debugging-Zugang stehen in `~/Developer/Launcher-plus/.env.local` (nicht lesen oder weitergeben ohne Bedarf). Launcher-Rolle per Dialog ist dort ungeprüft; ADB-Weg steht in `docs/installation.md`.
- **Überholt (Nachtrag 1.1.0):** Die App nutzt **Geist** (SIL OFL), gebündelt in `app/src/main/res/font/` und in `ui/Type.kt` als `TerebiFont` gebunden. Der frühere Punkt «Inter-TTF fehlt» gilt nicht mehr. Logo/Banner/Icon sind mit Inter gerendert (`res/drawable-nodpi/logo_horizontal.png`, `ic_launcher.png`, `drawable-xhdpi/banner.png`). Offen: Geist Mono für technische Werte und eine «Über terebi+»-Ansicht.
- Nicht getestet: Wolkenbewegung im Emulator (aktuelle Wetterlage war klar), eingebetteter WLAN-QR im Emulator und Kamera-Scan, App-Deinstallation live, Verschieben über mehrere Zeilen, mehr als 12 Apps, 4K.
- Compose-UI-Tests für Fokuswege fehlen. Bildrate im Emulator 95 % ≤ 19 ms, 8 % «Janky» (Software-Rendering): auf echter Hardware neu messen (`dumpsys gfxinfo`, `am start -W`, `dumpsys meminfo`).
- Release: echte Signatur, `Releases/YYYY-MM-DD_vX.Y.Z/` mit `Release.md` im Projektelab-Ordner nach MAIN.md (nur nach Freigabe).
- ✅ `TASKS.md` nachgeführt (2026-11-18): Emulator-Spalte für Start, Apps, Reihenfolge, Wetter, Wochentage, Offline und Autostart gesetzt, neue Checkliste «Abnahme Kapitel 13».

## Regeln

- Keine Systempakete deaktivieren, kein Root, kein `pm disable` ohne ausdrückliche Zustimmung von Arijan.
- WLAN-Passwort nie loggen, nie in DataStore, nur im Keystore (SecretStore).
- Werte (Farben, Masse, Szenen) nur in `assets/*.json` ändern; beide Seiten testen gegen dieselben Dateien.
- Testergebnisse nur melden, wenn ausgeführt.
- Hinweise: Der Emulator läuft evtl. noch (`adb devices`). Ein Vite-Entwicklungsserver (Port 5173) kann noch laufen.

## Nachtrag Brandguide 1.1.0 – 2026-11-18

Nutzerauftrag: «Genau nach dieser Vorlage» (`~/Downloads/terebi-plus_brandguide/` und PDF) und bei Nutzung unter 5 % sofort zusammenfassen und für Weitergabe vorbereiten. Der Nutzer hatte zuvor «keine Tests» verlangt; für den neuen Stand wurden daher **keine Tests** ausgeführt. Buildchecks allein: `./gradlew :app:assembleDebug` und `npm --prefix preview run build`, beide erfolgreich. Kein Emulator- oder Kameratest, kein TV-Install, kein Commit.

Kanonische Vorlage ist jetzt im Projektelab unter `Design/Brandguide/1.1.0/`; im Repo `assets/brandguide-tokens.json` und abgeleitete `assets/tokens.json`. Das Guide-Mockup-Foto ist laut `LIZENZEN.txt` ein Platzhalter und darf nicht in die App. Runtime nutzt weiter das lizenzierte Lake-McDonald-Foto (`assets/backgrounds/LICENSES.md`). Neue Logos/Banner/Icon aus Guide-SVG gerendert; Geist TTF/WOFF2 lokal mit OFL. App und Vorschau haben neutrale einspaltige Rail, Header mit Uhr/Datum, Wetterkarte rechts samt sieben Tagen, WLAN-Karte unten links. Zürich ist Standard; Wettermodus Ein/Nur Bild/Aus und 15-Minuten-Abruf ergänzt. Bisherige rote Kacheln sind nach jüngster Nutzeranweisung überholt.

**Wichtig: noch kein vollständiger Brandguide-Abgleich.** Offen siehe `TASKS.md` Abschnitt «Brandguide 1.1.0». Besonders: Wetterkarte inzwischen fokussierbar und mit D-Pad angebunden, aber Fokuswege nicht auf Gerät geprüft; Einstellungs-Overlay noch alte Vollbildaufteilung, nur ein Runtime-Foto statt 5 Gruppen × 4 Phasen, dynamische Wettereffekte nur vereinfacht, adaptive Abdunklung/Glas fehlen. Keine neuen Testergebnisse behaupten. Nächster Schritt: sichtbare Vorschau gegen das Mockup vergleichen und fokussierbare Wetterkarte/Settings-Navigation bauen, danach Szenen-Set und Offline-Stufen. Temporäre Demo-WLAN-Einstellung im Emulator aus der früheren Arbeit ist möglicherweise noch vorhanden; vor weiteren Emulator-Arbeiten prüfen und bereinigen, ohne echte Zugangsdaten zu verwenden.

Fortschritt nach obigem Nachtrag: Wetterkarte und WLAN-Karte sind jetzt mit Fokusrequestern an die D-Pad-Karte angebunden; die Wetterkarte öffnet eine Wetter-Zusammenfassung. Die native Einstellungsansicht und die Vorschau nutzen nun eine rechte Seitenansicht mit Rubriken und Unterseite. Wetterdaten werden für Zürich standardmässig abgerufen, auf Europe/Zurich bezogen, alle 15 Minuten aktualisiert und höchstens 12 Stunden aus dem Datei-Cache geladen; ab 3 Stunden Alter zeigt der Hintergrund eine bewölkte Szene ohne Effekte. Diese Änderungen wurden nur gebaut (`assembleDebug`, Vite-Build), nicht getestet oder auf dem Gerät abgenommen. Die Wetter-Detailansicht mit Stundenwerten ist weiterhin offen.

Die Wetter- und Einstellungssymbole kommen jetzt aus den gelieferten Lucide-SVGs (`assets/brand/icons/`); `scripts/gen-icons.mjs` erzeugt die Android-Vektoren mit 2-dp-Strich. Die Vorschau startet im Referenzzustand Nebel/Nacht mit sieben Beispielwerten; die native App zeigt weiterhin die echten Wetterdaten. Auch diese Nachbesserung wurde nur gebaut, nicht getestet.

Wetterdaten-URL enthält jetzt Regen, Schneefall und Wind; Schneeregen-Codes sind der Winterszene zugeordnet. Native Partikel wurden auf die im Guide beschriebenen Mengen für Niesel/Regen/Starkregen und leichten/mittleren/starken Schnee erweitert; Sturmblitze (max. 10 % Deckkraft, nach Wartezeit) und Schalter «Blitze anzeigen» sind ergänzt. Noch keine GPU-/Framerate-Messung oder visuelle Abnahme. Die Quelle des endgültigen 20-Bilder-Szenensets wurde beim Nutzer als Materialfrage angefragt; bis zur Antwort nur das lizenzierte Bergfoto verwenden.

## Übergabestopp auf Nutzerwunsch – 18.11.2026

Die Arbeit wird hier unterbrochen. Nutzer wünscht eine Zusammenfassung und Übergabe wegen knapper Nutzung. **Keine weiteren App-Änderungen und keine Tests nach diesem Punkt.**

**Gültigkeit:** Die Anweisung galt für die Sitzung vom 18.11.2026 und ist am 18.11.2026 mit der Anweisung «die Arbeit fertig machen» aufgehoben. Änderungen und Tests sind wieder erlaubt; die folgenden Abschnitte beschreiben nur den damaligen Stand.

Letzter Buildstand: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :app:assembleDebug --console=plain` erfolgreich; `npm --prefix preview run build` erfolgreich. Die neuen Änderungen wurden **nicht** per Unit-Test, Browser-/Emulator-Screenshot, Kamera-Scan oder TV-Hardware geprüft. Debug-APK ist unter `app/build/outputs/apk/debug/app-debug.apk`; keine Release-Signatur, kein Commit/Push/Install auf TV.

Zuletzt ergänzt: D-Pad-Fokuswege für Home-Elemente im Code, rechte Einstellungsansicht (App/Vorschau), Standardort Zürich und Europe/Zurich, 15-Minuten-Wetterabruf, 3h-/12h-Offline-Grundlage, Lucide-Symbole, WMO-Felder Regen/Schnee/Wind, intensitätsabhängige Partikel, Schneeregen, Sturmblitz und Schalter. Die Wetterkarte öffnet derzeit nur eine kurze Zusammenfassung; Stundenwerte fehlen. Die aktuelle Vorschau verwendet Beispielwetter, die native App echte Open-Meteo-Daten.

**Verbindliche Quellen für Fortsetzung:** erst `CLAUDE.md`, `TASKS.md`, `DECISIONS.md`, dieses `docs/HANDOVER.md`, dann Projektelab `MAIN.md`, `INDEX.md` und `Design/Brandguide/1.1.0/terebi-plus_Brandguide.md` lesen. Die Brandguide-Datei ist die neue visuelle Vorgabe. Das beigelegte Mockup-Foto ist laut `LIZENZEN.txt` nicht als App-Hintergrund erlaubt. Das lizenzierte Lake-McDonald-Foto ist vorläufig im Einsatz. Eine Frage zu erzeugten Wetter-/Tageszeitvarianten wurde dem Nutzer gestellt; Antwort noch nicht bekannt. Falls keine Antwort vorliegt, keine endgültige Bildentscheidung behaupten.

**Wesentliche offene Arbeit:** Visueller Vergleich und Abnahme nach Kapitel 13, 20 passende Bildszenen bzw. genehmigte Alternative, adaptive Scrims/Glas, Stundenwetter, exakte Wetterübergänge/Hysterese/Refresh-Splitting, System-Animator-Skala und Low-RAM-Fallback, Fokus/Back/Scroll auf Gerät, Perfetto/GPU-Messungen, WLAN-QR-Scan, reale TV-Hardware. Das alte Konzept roter Kacheln ist durch Brandguide 1.1.0 ersetzt. Bestehende Docs mit älteren «erledigt»-Angaben beziehen sich auf vorherige Zwischenstände; die Liste hier und `TASKS.md` haben Vorrang für den neuen Brandguide-Abgleich.

## Nachtrag 2026-11-18 (Abend): TV+ mitgeliefert, Wetterkaltstart und Fokus

**TV+ (IP Television+) ist Teil der Auslieferung.** Das signierte Release-APK 1.2.0 (`ch.arjo.iptelevision`, Paketname unverändert) liegt als `assets/bundled/iptelevision-1.2.0.apk` im Repo und wird in die App gepackt. `.gitignore` schliesst `*.apk` aus, gibt aber `assets/bundled/*.apk` frei. `TvPlusBundleTest` prüft Version und SHA-256 (`2e8ac3f6…dea96`); eine neue TV+-Version braucht Asset, `TvPlus.VERSION` und `TvPlus.SHA256` gemeinsam.

- Beim ersten Start erscheint einmalig «TV+ installieren» (nur solange TV+ fehlt und das Asset vorhanden ist). «Später» gilt als Antwort und wird in `tvPlusOfferSeen` gespeichert; der Hinweis kommt nach einem Neustart nicht wieder.
- Dauerhaft erreichbar über Einstellungen › System: «TV+ installieren» mit Version, nach der Installation «TV+ – Installiert».
- Die Installation öffnet ausschliesslich den Android-Installer mit Bestätigung (`ACTION_VIEW` auf eine FileProvider-URI, `cache/install/`). Ohne Freigabe für unbekannte Apps öffnet terebi+ nur `ACTION_MANAGE_UNKNOWN_APP_SOURCES`; es wird nichts still installiert, kein Systempaket geändert, kein Root verwendet.
- TV+ bleibt eine eigene App, keine System-App und keine feste Kachel. Sobald installiert, erkennt die Rail sie automatisch (leanback-Activity `ch.arjo.tv.android.MainActivity`, Label «TV+»).

**Behobene Fehler aus dem Emulator-Nachtest:**

- Frische Installation zeigte keine Wetterdaten, weil `SettingsStore.read` den Standardort verlor. Der Fallback auf `d.location` (Zürich) ist zurück; nach `pm clear` erscheint «WETTER · ZÜRICH».
- Der Startfokus der Startseite zog den Fokus unter offene Overlays. `HomeScreen` erhält jetzt `covered` und setzt den Startfokus nur, wenn nichts darüber liegt; das Menü versucht den Fokus bis zu dreimal. Damit bleiben TV+-Hinweis und Einstellungen bedienbar.

**Geprüft (Emulator, Debug- und Release-APK):** 29 Unit-Tests grün, `assembleDebug` und `assembleRelease` erfolgreich; Release-APK 7,4 MB mit Asset. D-Pad: Startfokus Zeile 1, Langdruck-Menü, Zahnrad/Einstellungen, System-Zeile, Wetterkarte, TV+-Hinweis, «Später» einmalig, Installer-Bestätigung, TV+ installiert, Kachel erscheint und startet. **Nicht geprüft:** rechte Einstellungsrubriken vollständig, WLAN-QR sichtbar/Scan, frischer Offline-Start ohne Cache, TV-Hardware.

**Weiter offen und unverändert:** echte Release-Signatur, Push/Remote (Freigabe Arijan), TV-Test am TCL, Home-Rolle auf Hardware, 20-Szenen-Bildset.
