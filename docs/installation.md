# Installation und Wiederherstellung

## Unterstützte Geräte

- Android TV / Google TV ab Android 9 (API 28). Ziel- und Kompiliersdk 37.
- Erprobt wurde der Weg zum Standard-Launcher bisher nur mit Launcher+ auf dem TCL Smart TV Pro (Android 14).
  terebi+ selbst ist auf echter Hardware **noch nicht getestet** (TASKS.md).

## Installieren (ADB)

Am TV: Einstellungen › System › Info › Build 7× drücken → Entwickleroptionen › Netzwerk-Debugging an.

```bash
adb connect <TV-IP>:5555      # am TV bestätigen
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n ch.arjo.terebi/.MainActivity
```

## Als Standard-Launcher einrichten

In terebi+: Einstellungen › System › «Als Standard-Launcher einrichten». Reihenfolge der Wege:

1. **RoleManager** (`ROLE_HOME`): Android zeigt einen Auswahldialog, sofern das Gerät ihn anbietet.
2. **Home-Einstellungen** (`Settings.ACTION_HOME_SETTINGS`), falls vorhanden.
3. **ADB, umkehrbar, ohne Systempakete zu ändern** (Google TV bietet oft keine Auswahl):
   ```bash
   adb shell cmd package set-home-activity ch.arjo.terebi/.MainActivity
   ```
   Prüfen: `adb shell cmd package resolve-activity -c android.intent.category.HOME -a android.intent.action.MAIN`

terebi+ deaktiviert **niemals** selbst Systempakete. Manche Google-TV-Geräte springen trotz Schritt 3 beim
Home-Knopf in den Google-Launcher zurück. Das Deaktivieren von `com.google.android.apps.tv.launcherx` wäre ein
Eingriff ins System und erfolgt nur nach ausdrücklicher Zustimmung, mit dokumentiertem Rückweg (unten).

## Wiederherstellung (Original-Launcher)

```bash
# Standard zurücksetzen bzw. terebi+ entfernen
adb uninstall ch.arjo.terebi
# Nur falls früher bewusst deaktiviert:
adb shell pm enable com.google.android.apps.tv.launcherx
adb shell pm enable com.google.android.tungsten.setupwraith
```

Ohne ADB: Einstellungen › Apps › terebi+ › Standardeinstellungen löschen, oder deinstallieren.
Netzwerk-Debugging am TV eingeschaltet lassen, solange terebi+ getestet wird: das ist der Rettungsweg.

## TV+ (IP Television+) installieren

terebi+ liefert IP Television+ 1.2.0 als signiertes APK mit (`assets/bundled/iptelevision-1.2.0.apk`).
Es ist eine eigene App (`ch.arjo.iptelevision`), kein Systempaket.

1. terebi+ starten. Beim ersten Start erscheint einmalig «TV+ installieren»; später jederzeit über
   Einstellungen › System › «TV+ installieren».
2. Beim ersten Mal fragt Android nach der Freigabe «Unbekannte Apps installieren» für terebi+.
   terebi+ öffnet diese Einstellung automatisch; alternativ:
   `adb shell appops set ch.arjo.terebi REQUEST_INSTALL_PACKAGES allow`
   (auf dem TV: Einstellungen › Apps › terebi+ › Spezieller App-Zugriff).
3. Android zeigt den üblichen Installationsdialog. Erst «Installieren» bestätigt die Installation;
   terebi+ selbst installiert nichts still.
4. Danach erscheint «TV+» automatisch in der App-Rail und startet als eigenständige App.

Update auf eine neue TV+-Version: neues Release-APK als `assets/bundled/iptelevision-<Version>.apk`
ablegen und `TvPlus.VERSION` sowie `TvPlus.SHA256` anpassen (`TvPlusBundleTest` prüft beides).

## Autostart und Android-Grenzen

- Start der Ziel-App erfolgt nur aus dem sichtbaren Launcher (Vordergrund), nie aus dem Hintergrund.
- Sitzung beginnt bei Boot (Launcher-Start bis 5 min nach dem Booten) oder `SCREEN_ON`. Zurückkehren aus einer App startet nichts.
- Geht der Launcher in den Hintergrund oder der Bildschirm aus, wird der Countdown verworfen.
- Ist die Ziel-App deinstalliert, wird der Autostart abgeschaltet und eine Meldung gezeigt.
