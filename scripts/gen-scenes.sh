#!/usr/bin/env bash
# Erzeugt die 20 Szenenbilder (5 Himmelsgruppen × 4 Tageszeiten, Brandguide 9.5) in assets/backgrounds aus den
# gelieferten Fotos in design-src/scenes/ (nicht im APK). Fehlende Kombinationen entstehen per Farbkorrektur
# (PhotoCraft) aus dem nächstpassenden Foto. Aufruf: scripts/gen-scenes.sh   (braucht PhotoCraft und cwebp)
set -euo pipefail
cd "$(dirname "$0")/.."
PC=/Applications/PhotoCraft.app/Contents/MacOS/photocraft-cli
SRC=design-src/scenes
OUT=assets/backgrounds
TMP=$(mktemp -d)

# Platz: Quelle, Belichtung, Sättigung, Filterfarbe, Dichte  (Filterfarbe «-» = keine Korrektur)
slot() { case "$1" in
  clear_dawn)   echo "dawn-blue 0 0 - 0" ;;
  clear_day)    echo "day 0 0 - 0" ;;
  clear_dusk)   echo "sunset 0 0 - 0" ;;
  clear_night)  echo "sunset -2.2 -10 #1B2A4A 55" ;;
  cloudy_dawn)  echo "pink 0 0 - 0" ;;
  cloudy_day)   echo "pink -0.15 -65 #8DB3E2 14" ;;
  cloudy_dusk)  echo "pink -0.6 -20 #6E5A8A 38" ;;
  cloudy_night) echo "pink -2.2 -45 #1B2A4A 60" ;;
  fog_dawn)     echo "fog -0.1 0 #F0C27B 22" ;;
  fog_day)      echo "fog 0 0 - 0" ;;
  fog_dusk)     echo "fog -0.5 0 #6E5A8A 32" ;;
  fog_night)    echo "fog -2.0 0 #1B2A4A 60" ;;
  wet_dawn)     echo "rain -0.1 0 #F0C27B 18" ;;
  wet_day)      echo "rain 0 0 - 0" ;;
  wet_dusk)     echo "storm 0 0 #6E5A8A 18" ;;
  wet_night)    echo "storm -1.6 0 #1B2A4A 55" ;;
  winter_dawn)  echo "snow -0.1 0 #F0C27B 20" ;;
  winter_day)   echo "snow 0 0 - 0" ;;
  winter_dusk)  echo "snow -0.6 0 #6E5A8A 34" ;;
  winter_night) echo "snow -2.0 0 #1B2A4A 60" ;;
esac; }

for g in clear cloudy fog wet winter; do
  for p in dawn day dusk night; do
    read -r src ex sat col dens <<<"$(slot ${g}_${p})"
    cmds=(--cmd image.adjustments.exposure --params "{\"exposure\":$ex}")
    [ "$sat" != 0 ] && cmds+=(--cmd image.adjustments.hueSaturation --params "{\"saturation\":$sat}")
    [ "$col" != "-" ] && cmds+=(--cmd image.adjustments.photoFilter --params "{\"color\":\"$col\",\"density\":$dens}")
    $PC run "$SRC/$src.jpg" "${cmds[@]}" --out "$TMP/s.png" >/dev/null
    cwebp -quiet -q 82 -resize 1920 1080 "$TMP/s.png" -o "$OUT/scene_${g}_${p}_1.webp"
  done
done
rm -rf "$TMP"
du -ch "$OUT"/scene_*.webp | tail -1
