---
name: tv-premium-design
description: Gestaltungsregeln für terebi+ (TV-first, ruhig, Markentreue). Verwenden bei Layout-, Farb-, Typo-, Fokus- oder Hintergrundänderungen in app/ oder preview/.
---

# terebi+ Design

1. Eine Quelle: `assets/tokens.json`. Änderung dort, dann Vorschau prüfen (`npm --prefix preview run dev`) und App bauen.
2. Logo nie neu zeichnen oder verzerren. Quelle `assets/brand/*.svg`; abgeleitete PNGs nur mit Originalgeometrie (Inter 500, Rot #E0302A).
3. Apps stehen im Zentrum; Wetter und Logo bleiben klein. Keine Kachelhintergründe ausser dem Original-Banner der App.
4. Fokus: Skalierung `focus.scale` + Ring `focusRing`; nichts blinkt, nichts pulsiert.
5. Sichere Ränder `safeX/safeY` (5 % Overscan) einhalten; mit «Sichere Ränder anzeigen» in der Vorschau prüfen.
6. Text mindestens `caption` (22 px bei 1080p), für 3 m Abstand.
7. Hintergründe: nur Verläufe/leichte Effekte; Szenenregeln in `scene-cases.json` testen, beide Seiten.
8. Prüfen mit 4 und mit 12 sichtbaren Apps.

Referenzen: developer.android.com/design/ui/tv · developer.apple.com/design/human-interface-guidelines (nur visuelle Zurückhaltung)
