# Localhost-Vorschau

`npm --prefix preview run dev` → http://localhost:5173 (nur localhost). Heimnetz nur bewusst: `npm --prefix preview run dev:lan`.

Bühne 1920 × 1080, skaliert ins Fenster. Rechts das Simulator-Panel (gehört nicht zum Produkt).
Design-Tokens, Szenenregeln, Wettercodes und Logo kommen aus `assets/`, wie in der App. Änderungen an CSS und
`assets/*.json` erscheinen sofort (Vite HMR); JS-Änderungen laden die Seite neu.

## Simuliert

Startseite, einspaltige App-Leiste mit Fokus, Langdruck-Menü, Verschieben, Favoriten, Ein-/Ausblenden, Mock-Installation und
-Deinstallation, alle Einstellungsrubriken, Stadtsuche (Mock-Liste), Wetterlagen, Tageszeiten, Bergfoto mit 11 Wetterszenen,
Offline-Kennzeichnung, Autostart-Countdown mit Abbruch, «Einschalten», WLAN-QR mit Auto-Schliessen, sichere Ränder.

## Nur auf Android prüfbar

Echte App-Icons und Starts, Home-Taste, Standard-Launcher-Rolle, Keystore-Verschlüsselung, Open-Meteo-Abruf,
Boot-/Standby-Verhalten des Autostarts, Bildschirmtastatur, Leistung und Speicher, Overscan des Geräts.

## Unterschiede

- Vorschau speichert in `localStorage` (ohne Passwort); App nutzt DataStore + Keystore.
- Rote Textkacheln statt der Original-App-Icons; die native App lädt echte Icons.
- Vorschau-Text in Inter, App-Text vorerst Systemschrift (TASKS.md).
