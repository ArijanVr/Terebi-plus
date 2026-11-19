# TASKS – terebi+

Stand 2026-11-18. Status: ✅ erledigt · 🧪 automatisiert getestet · 🌐 in der Vorschau geprüft · ⏳ offen.
Emulator: Google TV, Android 14 (API 34, arm64, AVD `terebi_gtv34`), Stand 2026-11-18. Echte Hardware: noch **nicht** getestet.

## Phasen

- ✅ Phase 0 Umgebung: JDK 21, Gradle-Wrapper 9.9.1, SDK 34/36/37, Node 24; Google-TV-Emulator API 34 eingerichtet.
- ✅ Phase 1 Designsystem: `assets/tokens.json`, 11 Szenen, Logo-Ableitungen.
- ✅ Phase 2 Localhost-Vorschau (`preview/`), im Browser durchgespielt.
- ✅ Phase 3 Native App: baut (`assembleRelease`, 7,4 MB mit mitgeliefertem TV+-APK), 29 Unit-Tests grün.
- 🟡 Phase 4 Emulator: grösstenteils getestet, siehe Abschnitt «Emulator».
- 🟡 Phase 5 Echter Google TV: Release-APK (neuer Schlüssel) per `adb install -r` auf dem TCL (Smart TV Pro, Android 14) installiert, 2026-11-18. Start per `am start` ohne Fehler, `am start -W` TotalTime 393 ms (Einzelwert, keine Messreihe). Home und Wetterkarte sichtbar, bedient mit der echten Fernbedienung (Apps-Rubrik und App-Menü offen gesehen). Home-Rolle per `cmd role add-role-holder android.app.role.HOME ch.arjo.terebi 0` gesetzt (2026-11-18, mit Okay von Arijan; vorher `ch.arjo.tvlauncher`). Zurück: `adb shell cmd role add-role-holder android.app.role.HOME ch.arjo.tvlauncher 0`. Hinweis: ADB-Server nach «No route to host» neu starten (`adb kill-server`). Aufgeräumt auf Wunsch von Arijan (2026-11-18): deinstalliert `com.google.android.videos`, `com.tcl.browser`, `flar2.homebutton`, `ch.arjo.tvlauncher` (ARJO Home, vorheriger Home-Launcher; Rückweg über die Home-Rolle gilt damit nicht mehr); `ch.arjo.iptelevision` auf 1.2.0 aktualisiert; in terebi+ sichtbar nur NimiTV, TV+ (iptelevision 1.2.0) und YouTube, `ch.arjo.tv.android` 2.1.0 und die System-Apps sind ausgeblendet, nicht deinstalliert. WLAN-Karte: QR-Funktion an, SSID (privat, nicht im Repo), WPA/WPA2, **Passwort trägt Arijan selbst ein** (Einstellungen › WLAN › Passwort); die Karte erscheint nach der Eingabe. Fehler gefunden und behoben: Das WLAN-Passwort wurde gespeichert, aber die Zeile zeigte weiter den Platzhalter und die QR-Karte nutzte ein veraltetes Passwort (Zustand war nicht reaktiv; `secretVersion` in `LauncherState`). Im Emulator geprüft: Punkte in der Zeile, QR-Karte «Demo-Netz» erscheint; Release-Update auf dem TCL installiert. IP Television+ 1.2.0 geprüft: das installierte APK auf dem TCL ist byte-gleich mit dem gebündelten (SHA-256 `2e8ac3f6…`), Icon und Banner zeigen den neuen Bildschirm mit orangem Plus (Vergleich mit `AppIconMaster.png` im IP-Television+-Repo), die Rail zeigt es nach Neustart. Dabei gefunden: nach einem App-Update blieb das alte Icon im Cache; `refreshApps` leert jetzt den Icon-Cache. Zweiter Fund beim Passwort (Meldung von Arijan, TCL): die Fernbedienungs-Tastatur löst «Fertig» nicht zuverlässig aus. Das Passwort wird jetzt 0,4 s nach der letzten Eingabe und beim Schliessen gespeichert, mit Hinweis «Passwort gespeichert»; die Anzeige «••••••••» setzt das Eingabefeld nicht mehr zurück. Emulator geprüft (Eingabe ohne Fertig-Taste), auf dem TCL installiert, Eingabe dort durch Arijan offen. 20-Szenen-Bildset (2026-11-19): `scripts/gen-scenes.sh` erzeugt 20 Bilder (5 Gruppen × 4 Tageszeiten, zusammen 2,8 MB) aus dem lizenzierten Foto, die App wählt nach Wetter und Tageszeit (`SceneResolver.imageName`, Dämmerung ± 45 min, Guide-Scrimtabelle 9.5 als Grundwert der adaptiven Abdunklung), 4 neue Tests grün; im Emulator Szene «Regen, Nacht» gesehen. **Aktualisiert (2026-11-19):** die Bilder stammen jetzt aus acht von Arijan gelieferten Fotos (`design-src/scenes/`), fehlende Tageszeiten per Farbkorrektur; Regen, Sturm und Schnee haben Tropfen und Flocken schon im Bild, der Effekt liegt zusätzlich darüber; Herkunft und Rechte der Bilder sind offen (siehe `LICENSES.md`); die Aussage zur Einzelfoto-Ableitung gilt nur noch für nicht gelieferte Zeiten. Frühere Einschränkung: es waren Farbkorrekturen eines einzigen Fotos, keine echten Wetteraufnahmen (Nacht und Regen wirken grau statt tiefblau, Winter ohne Schnee am Ufer, kein Sternenhimmel); echte oder KI-Bilder mit gleicher Kamera-Position können unter denselben Dateinamen ersetzt werden. Die Vorschau (`preview/`) nutzt weiter das Einzelfoto. Realistischerer Regen (2026-11-19, auf Wunsch von Arijan): drei Tiefenebenen (nah, mittel, fern) mit eigener Länge, Dicke, Helligkeit und Fallgeschwindigkeit, jeder Tropfen mit eigener Phase (vorher fielen alle gleichzeitig), leichte Schräge durch Wind, blasser Schweif mit hellerem Kopf, Wasserringe auf dem See (nur bei Regen, nicht bei Schneeregen). Auf Wunsch danach transparenter und weicher (Tropfen nah 11 %, mittel 7 %, fern 3,5 % Deckkraft, Ringe 9 %, dreistufiger Verlauf mit runden Enden). Weiterhin reiner Compose-Canvas ohne Video (EffectCraft und FilmCraft waren nicht nötig: ein Videoloop würde APK-Grösse und Decoder-Last erhöhen). Emulator: 20 ms pro Frame bei Software-Rendering, Hardware-Messung auf dem TCL offen. Messreihe auf dem TCL (2026-11-19, Release-APK): Kaltstart 356 bis 379 ms (5 Messungen, `Displayed` im Log, Ziel unter 800 ms erreicht), Speicher PSS ca. 42 MB (Ziel unter 150 MB), Bildzeit 57 bis 77 ms im Mittel bei Fokuswechsel und im Ruhezustand mit Wettereffekt (Ziel 16 ms nicht erreicht; ohne Wettereffekt in Ruhe keine Frames, bei Fokuswechsel weiter 57 ms). Arijan bewertet das Bild als gut, daher keine Optimierung durchgeführt; Ansatz falls nötig: Hintergrund samt Scrims einmal vorrechnen, Animationswerte nur im Zeichnen lesen. Wettereffekte nach dem Test wieder eingeschaltet. Schlüssel-Backup: Release-Keystore zusätzlich als Schlüsselbund-Eintrag «terebi-release-keystore-file» (Base64, Wiederherstellung geprüft: gleiche SHA-256, Keystore öffnet); Wiederherstellen mit `security find-generic-password -a terebi -s terebi-release-keystore-file -w | base64 -d > terebi-release.jks`. Eine Kopie auf einem externen Laufwerk oder in einem Passwortmanager bleibt empfohlen (Regel J). Aufgefallen: das Manifest enthält zusätzlich `REQUEST_INSTALL_PACKAGES` (Vorgabe war nur INTERNET), Entscheid von Arijan 2026-11-19: bleibt, wegen Installation und Updates von TV+ (siehe `DECISIONS.md`). Offen: TV+-Installationsweg (TV+ ist dort bereits installiert), WLAN-QR.
- ✅ Phase 6 Finalisierung (2026-11-19): Release v0.1.0 im Projektelab unter `Releases/2026-11-19_v0.1.0/`. Frühere Notiz: Release-Schlüssel erzeugt (2026-11-18, RSA 4096, 25 Jahre, `~/.terebi-signing/terebi-release.jks`, Passwort im macOS-Schlüsselbund unter «terebi-release-keystore», nicht im Repo). Build: `TEREBI_KEYSTORE` und `TEREBI_KEYSTORE_PASSWORD` setzen, dann `./gradlew :app:assembleRelease`; ohne diese Variablen Debug-Signatur. Zertifikat SHA-256 `ce5552b2…eedd96`. **Offen: separate Sicherung des Schlüssels (iCloud ist kein Backup, Regel J)**; Release-Ordner im Projektelab erst nach Freigabe. Wichtig: ein mit Debug-Schlüssel installiertes terebi+ muss vor dem Release-APK deinstalliert werden.

## Emulator (2026-11-18, Google TV API 34)

Getestet und in Ordnung: Start (Kaltstart 119–150 ms), echte App-Banner, Home-Rolle per `cmd role add-role-holder`
(Home-Taste führt danach zu terebi+), Zurück auf Startseite ohne Wirkung, Einstellungen per Zahnrad und Menütaste,
Bildschirmtastatur und echte Open-Meteo-Stadtsuche, Wetteranzeige, Autostart (Countdown nach Aufwachen aus Standby,
Start nach 3 s, Abbruch per Taste, kein zweiter Countdown bei Rückkehr), Offline-Kaltstart ohne Absturz, Langdruck-Menü,
Verschieben, Ausblenden, Reihenfolge nach Neustart, Speicher 29 MB PSS.
Gefunden und behoben: Absturz beim ersten Speichern (`remove()` auf Double-Schlüssel, Regressionstest),
Textfeld-Fokussprung, veralteter App-Name, Langdruck löste Start aus (`isLongPress`), Einstellungen ohne Unschärfe.
Messung Bildrate beim Fokuswechsel im Emulator (Software-Rendering): 50 % 16 ms, 95 % 19 ms, 8 % «Janky» → auf echter Hardware neu messen.
Nicht getestet: eingebetteter WLAN-QR im Emulator und Kamera-Scan, App-Deinstallation live, Verschieben über Zeilen, mehr als 12 Apps, 4K.

D-Pad-Nachtest 2026-11-18 (Debug- und Release-APK): Startfokus auf Rail-Zeile 1; Langdruck öffnet das App-Menü;
Zahnrad öffnet die Einstellungen mit fokussierter linker Rubrik; Rubrik «System» zeigt «TV+ installieren» mit
Version 1.2.0; Wetterkarte öffnet die Zusammenfassung; der einmalige TV+-Hinweis ist fokussiert, «Später» schliesst
ihn und er erscheint nach einem Neustart nicht wieder; «Installieren» öffnet den Android-Installer (ohne Freigabe
zuerst die Systemeinstellung «Unbekannte Apps»), nach Bestätigung ist TV+ installiert, die Kachel erscheint und
startet `ch.arjo.iptelevision`. Noch offen: rechte Einstellungsrubriken vollständig durchklicken, WLAN-QR-Karte
sichtbar prüfen und mit Kamera scannen, frischer Offline-Start ohne Datei-Cache.

## TV+ (IP Television+) mitliefern

- ✅ Signiertes Release-APK 1.2.0 (`ch.arjo.iptelevision`, SHA-256 `2e8ac3f6…dea96`) liegt als `assets/bundled/iptelevision-1.2.0.apk` im Launcher; `TvPlusBundleTest` prüft Version und Prüfsumme, die Release-APK enthält das Asset.
- ✅ Einmaliger Hinweis beim ersten Start (auch «Später» gilt als Antwort, `tvPlusOfferSeen`), dauerhafte Zeile in Einstellungen › System («TV+ installieren» bzw. «Installiert»).
- ✅ Installation ausschliesslich über den Android-Installer mit Bestätigung; fehlt die Freigabe für unbekannte Apps, öffnet terebi+ nur die passende Systemeinstellung. Keine Systempakete, kein Root, keine stille Installation.
- ✅ Im Emulator durchgespielt: Hinweis → Installer → Installation → Kachel erscheint → TV+ startet (Debug- und Release-APK).
- ⏳ Auf TV-Hardware prüfen; neue TV+-Version ersetzt Asset **und** `TvPlus.VERSION`/`TvPlus.SHA256` gemeinsam.

## Offen

- ⏳ Bewölkten, bewegten Hintergrund und eingebetteten WLAN-QR im Emulator und auf TV prüfen; Bergfoto, einspaltige Apps und Wochenkarte im Emulator visuell geprüft.
- ✅ Geist (SIL OFL) liegt in `app/src/main/res/font/` und ist als `TerebiFont` gebunden (Brandguide § 4). Geist Mono (aus Launcher+, gleiche OFL, 145 KB) steht für Version, Paket und Android in Einstellungen › System; im Emulator gesehen.
- 🟡 Compose-UI-Tests (Robolectric, SDK 34): `HomeFocusTest` prüft die D-Pad-Karte der Startseite nach Guide § 6.2 (Startfokus, Rail ohne Umlauf, Wetterkarte, Zahnrad, Fokus-Gedächtnis), 4 Tests grün. `SettingsFocusTest` prüft die Rubrikenliste (ohne Umlauf), jede der sechs Rubriken öffnen (Zurück-Zeile fokussiert) und Rückkehr. Dabei gefunden und behoben: Fokus sprang nach «Zurück» immer auf «Apps», jetzt auf die zuletzt geöffnete Rubrik. Ein dritter Test prüft je Rubrik (ohne Apps) Down, Up und kein Umlauf nach oben. Offen: Textfelder und Schalterwirkung.
- ⏳ Messungen auf Zielgerät: Kaltstart, Bildrate bei Fokuswechsel, Speicher (Android Profiler / `dumpsys gfxinfo`).
- ⏳ Prüfen, ob RoleManager ROLE_HOME auf dem TCL verfügbar ist; sonst ADB-Weg (docs/installation.md).
- ⏳ Bei vielen Apps (> 12): Scrollverhalten auf dem Gerät prüfen.

## Definition of Done

| Kriterium | Implementiert | Automatisiert | Vorschau | Emulator | Hardware |
|---|---|---|---|---|---|
| Startet auf Android-TV-Hardware | ✅ | – | – | ✅ (Kaltstart 119–150 ms) | ⏳ |
| Originalmarke korrekt | ✅ | – | 🌐 | ⏳ | ⏳ |
| Apps erkannt und gestartet | ✅ | – | 🌐 (Mock) | ✅ | ⏳ |
| Apps ein-/ausblenden | ✅ | 🧪 AppConfigTest | 🌐 | ✅ | ⏳ |
| Reihenfolge bleibt nach Neustart | ✅ (DataStore) | 🧪 Logik | 🌐 (localStorage) | ✅ | ⏳ |
| Wetterdaten angezeigt | ✅ | 🧪 Parser | 🌐 (Mock) | ✅ | ⏳ |
| Wochenvorhersage (7 Tage) | ✅ | 🧪 URL und Mock | 🌐 (Mock) | ✅ | ⏳ |
| Temperaturfarben und gemeinsame Min/Max-Skala | ✅ | 🧪 Token-Endpunkte | 🌐 (Mock) | ✅ | ⏳ |
| Wochentage ausgeschrieben | ✅ | 🧪 beide Seiten | 🌐 | ✅ | ⏳ |
| Hintergrund nach Tageszeit und Wetter | ✅ | 🧪 gemeinsame Fixtures | 🌐 | ⏳ (Wolkenbewegung nicht gesehen) | ⏳ |
| Start ohne Internet | ✅ (Cache, nicht blockierend) | – | 🌐 (Offline-Schalter) | ✅ | ⏳ |
| WLAN-QR lokal | ✅ | 🧪 Payload | 🌐 | ⏳ (eingebettet nicht sichtbar, Scan offen) | ⏳ |
| Autostart 3 s, 1–5 s einstellbar, abbrechbar | ✅ | 🧪 AutoStartArm | 🌐 | ✅ | ⏳ |
| Keine Autostart-Schleifen | ✅ | 🧪 | 🌐 | ✅ | ⏳ |
| Alle Einstellungen per Fernbedienung | ✅ | ⏳ | 🌐 (Tastatur) | ⏳ (Rubriken noch nicht durchgeklickt) | ⏳ |
| TV+ mitgeliefert, angeboten, installierbar | ✅ (Asset, Hinweis, Einstellungen › System) | 🧪 TvPlusBundleTest | – | ✅ (Installer, Kachel, Start) | ⏳ |
| Zustände über localhost simulierbar | ✅ | 🧪 preview | 🌐 | – | – |
| Native APK baut | ✅ | ✅ Gradle | – | – | – |
| Installation und Wiederherstellung dokumentiert | ✅ docs/installation.md | – | – | ⏳ | ⏳ |

Emulator-Spalte: Stand 2026-11-18, Google TV API 34 (AVD `terebi_gtv34`); Hardware-Spalte bleibt leer, bis der TCL geprüft ist.

## Brandguide 1.1.0 (2026-11-18, laufend)

Neue verbindliche Vorlage: `Design/Brandguide/1.1.0/` im Projektelab und `assets/brandguide-tokens.json` im Repo. Die früheren Aussagen über rote App-Kacheln und farbige Temperaturzahlen sind durch die neue Vorlage überholt.

Umgesetzt: neue SVG-Marke als PNG in Android, Geist-Schrift, Farbwerte, Home-Zonen 58/112/634 dp, neutrale einspaltige App-Rail, WLAN-Karte 268 × 76 dp, Wetterkarte 268 × 400 dp mit aktueller Temperatur und sieben Tagen, Zürich als Standardort, Wettermodus Ein/Nur Bild/Aus, 15-Minuten-Abruf, Brandguide-Scrims. Native Debug-APK und Preview gebaut; **keine Tests auf Anweisung des Nutzers** für diesen neuen Stand.

Noch offen für «genau nach Vorlage»: visuelle Abnahme gegen alle fünf Referenzbilder, D-Pad-Karte nach Codeänderung auf dem Gerät prüfen; Rückkehr aus Apps und Fokus-Gedächtnis vollständig absichern, rechte Einstellungsansicht auf dem Gerät prüfen, 20 kuratierte/geeignete Wetterszenenbilder (bisher ein lizenziertes Foto mit Grading), volle WMO-/Intensitätslogik und Effekte auf Hardware prüfen (Partikel, Blitz und Schneeregen im Code ergänzt), 3h/12h-Offline-Stufen auf dem Gerät prüfen (Code-Grundlage vorhanden), adaptiver Kontrast/Glas, reduzierte Bewegung inkl. System-Animator-Skala, Font-Skalierung, Performance-Messungen und Hardwareprüfung. Nicht als abgeschlossen ausgeben.

### Abnahme Kapitel 13

Schritt 1 (Code gegen Guide, 2026-11-18, nur `assembleDebug` grün, noch keine Bildabnahme): Logo-Basislinie auf 58 dp (Bild 34 dp), Datum und Uhr mit gemeinsamer Basislinie 58 dp und rechtsbündig 842 dp, `tnum` für Uhr und Temperaturen, Favoritenpunkt nicht mehr rot (maximal zwei rote Elemente auf Home), Fokusring der Karten 2 dp mit 3 dp Abstand, Haarlinie um Rail-Icons, Schalterknopf und Cursor aus Tokens. Schritt 2 (2026-11-18, Unit-Tests und `assembleDebug` grün, Vorschau-Tests grün, nicht im Emulator gesehen): Fokusschatten der Rail-Zeile (55 % Schwarz), Pivot-Scroll auf Zeile 2 mit 24-dp-Ausblendmaske und angeschnittener 7. Zeile ab mehr als 6 Apps, Hex-Werte aus `Background.kt` und `Overlays.kt` in `assets/tokens.json` (`sceneTint`) bzw. `ink` verschoben. Schritt 3: System-Animator-Skala 0 wirkt wie «Animationen reduzieren» (Guide § 9.9), Schalter zeigen zusätzlich «Ein»/«Aus» (§ 7.5); Unit-Tests und `assembleDebug` grün, nicht im Emulator gesehen. Schritt 4 (Emulator API 34, Screenshots 1920 × 1080 von Home, Wetterkarte im Fokus und Einstellungen gesehen): Wochentag-Spalte auf 56 dp (Icon bei +56, «Heute» überlappte), Fokusring der Karten ausserhalb des Clippings sichtbar, Logo liegt ausserhalb der Unschärfe und bleibt im Einstellungs-Overlay scharf (Guide § 2.4). Home zeigt genau ein rotes Element (Logo-Punkt). Nicht gemessen: Zonen auf ± 2 dp gegen das Mockup, Kontrast, Rail-Pivot mit mehr als 6 Apps. Schritt 5 (Emulator gesehen, 3 neue Unit-Tests grün): Wetter-Detail als rechtes Overlay mit Stundenwerten der nächsten 24 Stunden (Open-Meteo `hourly`, `forecast_hours=26`; ältere Caches ohne Stundenwerte zeigen einen Hinweis). Logo bleibt über allen Overlays scharf. Fokus-Gedächtnis: nach dem Schliessen eines Overlays kehrt der Fokus auf das zuletzt fokussierte Element zurück (Wetterkarte im Emulator geprüft, Zahnrad, WLAN-Karte und Rail nur im Code). Regenwahrscheinlichkeit nur als Text. Schritt 6 (3 neue Unit-Tests grün, Emulator ohne Absturz, Bild unverändert): adaptive Abdunklung nach Guide § 7.6 (`AdaptiveScrim`: 95. Perzentil der Luminanz je Textzone auf 64 × 36 px, globaler Scrim in 0.04-Schritten bis 0.40, einmal pro Bild im Hintergrund-Thread). Mit den Guide-Scrims bleibt er bei 0.16, die Kontrasttabelle 3.4 ist im Test nachgerechnet. Glas fällt auf `isLowRamDevice()` deckend auf ink.800 92 % zurück (Code, nicht auf einem Low-RAM-Gerät gesehen). Nicht umgesetzt: Glasbild als weichgezeichnete Kopie (heute Tönung ohne Blur), Ken-Burns, Dither. Schritt 8: Geist Mono und «Über»-Zeilen in System, `SettingsFocusTest`, Fokus nach Zurück. Schritt 7: `HomeFocusTest` (siehe «Offen»). Schritt 9 (Unit-Tests grün, Emulator ohne Absturz): Hysterese nach Guide § 9.8 (`ConditionHysteresis`: neuer Zustand erst nach zwei gleichen Abfragen, Wechsel zu Starkregen und Gewitter sofort, Cache gilt sofort; `WeatherCondition` bildet alle WMO-Codes aus § 9.3 ab, getestet). Tageszeitwechsel blendet in 4000 ms, Wetterwechsel in 2000 ms, ohne Bewegung 200 ms. Nicht umgesetzt: getrennte Abrufe für `current` (15 min) und `daily` (3 h), Effekte-Überlappung 1500 ms, Übergang von Intensitäten in 3000 ms, Fallback-Bilder pro Tageszeit. Szenenwechsel warten auf der Startseite 3 s nach dem letzten Tastendruck (Code, Emulator ohne Absturz; in den Einstellungen sofort, damit die Hintergrundwahl live sichtbar bleibt). Schalter AN sind nach Entscheidung von Arijan warmweiss statt rot (Abweichung von § 7.5, in `DECISIONS.md`); im Emulator gesehen.

Referenz: `Design/Brandguide/1.1.0/terebi-plus_Brandguide.md` § 13.3 und die fünf Referenzbilder. Die Zonenwerte in `assets/tokens.json` stimmen mit § 5.2 überein (58/28 dp Keyline, 58/112 dp Rail 268 × 296, 634/112 dp Wetterkarte 268 × 400, Zeilen 46 dp, Abstand 4 dp); die Prüfung am Bild steht aus.

- [ ] Layout: Zonen ± 2 dp gegen `terebi-plus_home-mockup.png`; Logo Punkt-Kante x 58, Basislinie 58; WLAN-Karte endet auf 512; nichts ausserhalb 48/27 dp, auch im Fokus.
- [ ] Farbe/Typo: nur Tokens, maximal zwei rote Elemente, kein Text unter 12 sp, `tnum` für Uhr und Temperaturen, Kontrast ≥ 7 : 1 / 4,5 : 1 auch auf fast weissem Testbild.
- [ ] Fokus: D-Pad-Karte 6.2 vollständig, Startfokus Rail-Zeile 1, Rückkehr aus App auf die zuletzt geöffnete App, Fokus 150 ms, Skalierung 1.04/1.02/1.10, Zurück ohne Ausstiegsdialog, Langdruck-Menü und Verschieben abbrechbar.
- [ ] Zustände: offline mit und ohne Zwischenspeicher, App während des Laufs deinstalliert, WLAN-Karte standardmässig aus, Passwort nirgends im Klartext.
- [ ] Wetter: alle WMO-Codes (Unit-Test), Tageszeit-Grenzen 06:49/08:19/16:02/17:32, fünf Referenzszenen, Kontrast in 20 Szenen, 60 fps bei Starkregen mit Fokuswechsel, Effekt-Layer ≤ 2 ms GPU, Offline 3 h/12 h, «Nur Bild»/«Aus»/«Blitze» und Animator-Skala 0.
- [ ] Technik: 60 fps bei 20 Apps, Kaltstart < 800 ms, PSS < 150 MB, Manifest vollständig, TalkBack, `font_scale 1.3` ohne Abschneiden, Bewegungsreduktion.
