# Zustandsmodell (gemeinsam für App und Vorschau)

| Bereich | App (massgebend) | Vorschau |
|---|---|---|
| Einstellungen | `data/Settings` (DataStore) | `st.settings` (localStorage) |
| Apps | `AppConfig(order, hidden, favorites, known)` | `order`, `visible`, `favorites`, `installed` |
| Startseite | `AppConfig.home()`: Favoriten zuerst, dann Reihenfolge | `homeApps()` |
| Szene | `SceneResolver.resolve(minute, sunrise, sunset, code)` | `resolveScene(...)` |
| Autostart | `AutoStartArm` + `countdownToken` (höchstens einer) | `createAutostart()` + `ui.countdown` |
| WLAN | `Settings.ssid/security/wifiHidden` + `SecretStore` | `st.settings.wifi` + Passwort nur im Speicher |
| Ebenen | Einstellungen, ein Overlay (`AppMenu`, `Info`, `Qr`), Verschiebemodus | `ui.layers`, `ui.moving` |

Zurück-Reihenfolge: Overlay → Einstellungen → Verschiebemodus → (Startseite: keine Wirkung).
Gemeinsame Testdaten: `assets/scene-cases.json`. Gemeinsame Werte: `assets/tokens.json`, `assets/weather-codes.json`.
