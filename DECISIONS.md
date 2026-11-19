# Entscheidungen

| Datum | Entscheidung | Begründung |
|---|---|---|
| 2026-11-18 | Repo in `~/Developer/Terebi-plus`, nicht in iCloud | Projektelab Regel E (iCloud beschädigt `.git`) |
| 2026-11-18 | Bewährte Bausteine aus Launcher+ übernommen (Gradle-Versionen, AutoStartArm, SecretStore, WifiQr, Open-Meteo-Abruf) | Auf demselben TCL-Gerät erprobt; kein Neubau ohne Nutzen |
| 2026-11-18 | Compose foundation statt TV-Material | Eigene, ruhige Fokusdarstellung; eine Abhängigkeit weniger |
| 2026-11-18 | Tokens zur Laufzeit aus `assets/tokens.json` statt Kotlin-Kopie | Vorschau und App können nicht auseinanderlaufen |
| 2026-11-18 | Masse in px bei 1920 Breite, Umrechnung über `screenWidthDp / 1920` | Gleiche Proportionen auf 1080p und 4K, unabhängig von Gerätedichte |
| 2026-11-18 | Kein ViewModel, `LauncherState` in der Activity | singleTask + configChanges: keine Neuerstellung; weniger Code |
| 2026-11-18 | DataStore (Preferences) für Einstellungen, Passwort separat im Keystore | Vorgabe; Passwort nie im Klartext |
| 2026-11-18 | Neu installierte Apps erscheinen sichtbar am Ende; Ausblenden bleibt über `known` erhalten | Erwartbares Verhalten, keine verlorenen Entscheidungen |
| 2026-11-18 | Favoriten stehen vorne, Verschieben nur innerhalb der Gruppe | Einfache, vorhersagbare Reihenfolge |
| 2026-11-18 | Autostart-Sitzung nur bei Boot (≤ 5 min) oder Bildschirm an; nie bei Rückkehr aus App; Countdown stoppt bei onPause | Keine Schleifen, keine Starts aus dem Hintergrund |
| 2026-11-18 | `dim` für Wetterszenen bei Dunkelheit | Wetterlage bleibt über dem Foto sichtbar, ohne die Nacht aufzuhellen |
| 2026-11-18 | Wetterabruf nur solange sichtbar, alle 45 min, Zeitzone = Gerät | Kein Hintergrunddienst, sparsam, Szenen passen zur Geräteuhr |
| 2026-11-18 | Wochenvorhersage ab heute standardmässig sichtbar; bewölkte Szene auch bei Dunkelheit mit ruhigem Wolkeneffekt | Die ganze Woche und die aktuelle Wetterlage bleiben auf dem Startbildschirm erkennbar |
| 2026-11-18 | Originalfoto «Morning Fog at Lake McDonald» als mitgeliefertes 1920 × 1080 WebP unter den Wetterszenen; App-Liste einspaltig links und Wochenkarte rechts | Brandguide-Referenz, keine Laufzeit-Bilddownloads; Fotoquelle und Lizenz in `assets/backgrounds/LICENSES.md` |
| 2026-11-18 | Logo/Banner/Icon als PNG aus den Original-SVGs gerendert (Inter 500, Originalgeometrie, Logo ohne Hintergrundfläche) | Inter-TTF fehlt im Projekt; SVG-`<text>` ist auf Android nicht darstellbar |
| 2026-11-18 | UI-Text vorerst Systemschrift | Inter nur als WOFF vorhanden; TTF-Download braucht Freigabe (TASKS.md) |
| 2026-11-18 | Vorschau in Vanilla-JS + Vite, nur `localhost`; `dev:lan` nur bewusst | Kein Framework nötig; Sicherheitsvorgabe |
| 2026-11-18 | Brandguide 1.1.0 aus `Downloads/terebi-plus_brandguide` ist die neue visuelle Vorgabe; Rot nur Akzent, App-Rail als neutrale Einzelzeilen | Neueste ausdrückliche Nutzeranweisung ersetzt die frühere Vorgabe roter Kacheln. Kanonische Tokens: `assets/brandguide-tokens.json`; `assets/tokens.json` bleibt abgeleitete Laufzeitstruktur für vorhandene App und Vorschau. |
| 2026-11-18 | Geist aus dem offiziellen Vercel-Repository lokal gebündelt; Logo, Banner und App-Icon aus den gelieferten SVGs | Kein Font- oder Bildabruf zur Laufzeit. OFL in `assets/fonts/OFL.txt`. |
| 2026-11-18 | Brandguide-Mockup-Foto nicht als Runtime-Bild verwendet | `LIZENZEN.txt` bezeichnet es als KI-Platzhalter und untersagt die App-Nutzung. Das lizenzierte Lake-McDonald-Foto bleibt vorläufiger Runtime-Hintergrund. |
| 2026-11-18 | IP Television+ 1.2.0 wird als signiertes APK in `assets/bundled/` mitgeliefert, einmalig angeboten und über Einstellungen › System installierbar; Installation nur über den Android-Installer mit Bestätigung | terebi+ soll TV+ mitbringen, ohne System-App, stilles Installieren oder Paketdeaktivierung. Der SHA-256 im Code bindet die geprüfte Release-Datei. |
| 2026-11-18 | Der einmalige TV+-Hinweis gilt nach «Später» als beantwortet (`tvPlusOfferSeen`) | Kein wiederholtes Aufdrängen; die dauerhafte Zeile in den Einstellungen bleibt der Weg zur Installation. |
| 2026-11-18 | Startfokus der Startseite nur, solange kein Overlay offen ist (`covered`) und mit Fokus-Wiederholung im Menü | Im Emulator verlor der TV+-Dialog den Fokus an die Startseite; Dialoge müssen die Fernbedienung allein bedienen. |
| 2026-11-18 | Schalter AN sind warmweiss statt rot (Abweichung von Brandguide § 7.5) | Arijan: Logo-Punkt bleibt das einzige rote Element, § 3.3 «maximal zwei rote Elemente» gilt auch in den Einstellungen. Zustand zusätzlich über Knopfposition und Text «Ein»/«Aus». |
| 2026-11-19 | `REQUEST_INSTALL_PACKAGES` bleibt im Manifest (Ausnahme von «nur INTERNET») | Arijan: TV+ soll über terebi+ installiert und aktualisiert werden können. Installation weiterhin nur über den Android-Installer mit Bestätigung, nie still. |
