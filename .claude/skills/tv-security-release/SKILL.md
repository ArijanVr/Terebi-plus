---
name: tv-security-release
description: Sicherheits- und Release-Prüfung für terebi+ (Berechtigungen, Exporte, WLAN-Passwort, Signatur, Projektelab-Release). Verwenden vor jedem Release und bei Änderungen an Manifest, Speicherung oder Netzwerk.
---

# terebi+ Sicherheit und Release

1. Manifest: nur `INTERNET`; einzig exportiert ist `MainActivity` (HOME/LEANBACK). Kein `QUERY_ALL_PACKAGES`.
2. `network_security_config`: kein Klartext. Hosts im Code auf Open-Meteo begrenzt (`WeatherRepository.httpGet`).
3. WLAN-Passwort nur über `SecretStore` (Keystore, AES-GCM); nie in Logs, DataStore, Backups (`allowBackup=false`, data_extraction_rules).
4. Keine Systemeingriffe: kein `pm disable`, kein Root. ADB-Schritte am TV nur mit Zustimmung von Arijan.
5. Release: echte Signatur (Schlüssel nie im Repo), `versionCode`/`versionName` erhöhen, R8 an, `assembleRelease` ohne Warnungen.
6. Ablage nach Projektelab `MAIN.md` (Regel D/E/J): `Releases/YYYY-MM-DD_vX.Y.Z/` mit `Release.md`, `INDEX.md` und `Projekt.md` aktualisieren.

Referenzen: developer.android.com/privacy-and-security/security-tips · /training/articles/keystore · /guide/topics/manifest/activity-element#exported
