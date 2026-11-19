// Erzeugt app/src/main/res/drawable/ic_w_*.xml aus preview/src/icons.js (eine Quelle für beide).
// Aufruf: node scripts/gen-icons.mjs
import { writeFileSync } from 'node:fs'
import { ICONS } from '../preview/src/icons.js'

const circle = (cx, cy, r) => `M${cx - r},${cy}a${r},${r} 0 1,0 ${2 * r},0a${r},${r} 0 1,0 ${-2 * r},0`
for (const [name, svg] of Object.entries(ICONS)) {
  const paths = []
  for (const m of svg.matchAll(/<(path|circle)([^>]*)\/>/g)) {
    const attr = (k) => m[2].match(new RegExp(`${k}="([^"]*)"`))?.[1]
    const d = m[1] === 'circle' ? circle(+attr('cx'), +attr('cy'), +attr('r')) : attr('d')
    const w = attr('stroke-width') || '2'
    paths.push(`    <path android:pathData="${d}" android:strokeColor="#FFFFFFFF" android:strokeWidth="${w}" android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000" />`)
  }
  writeFileSync(new URL(`../app/src/main/res/drawable/ic_w_${name}.xml`, import.meta.url),
    `<?xml version="1.0" encoding="utf-8"?>\n<!-- Generiert von scripts/gen-icons.mjs aus preview/src/icons.js. Nicht von Hand ändern. -->\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n${paths.join('\n')}\n</vector>\n`)
}
console.log('Icons:', Object.keys(ICONS).join(', '))
