// Mock-Daten der Vorschau. Platzhalter-Banner statt Original-App-Icons (die liefert erst Android).
export const CATALOG = [
  { id: 'com.google.android.youtube.tv', label: 'YouTube', hue: 0 },
  { id: 'com.netflix.ninja', label: 'Netflix', hue: 355 },
  { id: 'com.disney.disneyplus', label: 'Disney+', hue: 220 },
  { id: 'com.amazon.amazonvideo.livingroom', label: 'Prime Video', hue: 200 },
  { id: 'ch.srf.mobile.srfplayer.tv', label: 'Play SRF', hue: 5 },
  { id: 'com.zattoo.player', label: 'Zattoo', hue: 30 },
  { id: 'com.spotify.tv.android', label: 'Spotify', hue: 140 },
  { id: 'com.plexapp.android', label: 'Plex', hue: 45 },
  { id: 'org.xbmc.kodi', label: 'Kodi', hue: 195 },
  { id: 'com.apple.atve.androidtv.appletv', label: 'Apple TV', hue: 0, gray: true },
  { id: 'com.android.vending', label: 'Play Store', hue: 160 },
  { id: 'com.android.tv.settings', label: 'Einstellungen', hue: 0, gray: true },
  { id: 'com.google.android.apps.youtube.music', label: 'YouTube Music', hue: 350 },
  { id: 'tv.twitch.android.app', label: 'Twitch', hue: 265 },
]

// Erste Installation: diese sind sichtbar, der Rest ist installiert, aber ausgeblendet.
export const DEFAULT_VISIBLE = 6

// Mock-Geocoding (Open-Meteo-Format reduziert). Sonnenzeiten als Minuten, grob für November.
export const CITIES = [
  { name: 'Zürich', admin: 'Zürich, Schweiz', lat: 47.37, lon: 8.55, sunrise: 450, sunset: 1000 },
  { name: 'Bern', admin: 'Bern, Schweiz', lat: 46.95, lon: 7.45, sunrise: 455, sunset: 1003 },
  { name: 'Basel', admin: 'Basel-Stadt, Schweiz', lat: 47.56, lon: 7.59, sunrise: 456, sunset: 1001 },
  { name: 'Genf', admin: 'Genf, Schweiz', lat: 46.2, lon: 6.14, sunrise: 459, sunset: 1010 },
  { name: 'Lugano', admin: 'Tessin, Schweiz', lat: 46.0, lon: 8.95, sunrise: 448, sunset: 1005 },
  { name: 'St. Moritz', admin: 'Graubünden, Schweiz', lat: 46.5, lon: 9.84, sunrise: 444, sunset: 1000 },
  { name: 'Luzern', admin: 'Luzern, Schweiz', lat: 47.05, lon: 8.31, sunrise: 451, sunset: 1001 },
  { name: 'Wien', admin: 'Wien, Österreich', lat: 48.21, lon: 16.37, sunrise: 425, sunset: 975 },
  { name: 'Berlin', admin: 'Berlin, Deutschland', lat: 52.52, lon: 13.4, sunrise: 450, sunset: 960 },
  { name: 'Tokio', admin: 'Tokio, Japan', lat: 35.68, lon: 139.69, sunrise: 375, sunset: 990 },
]

export const WEATHER_PRESETS = [0, 1, 2, 3, 45, 61, 65, 73, 81, 95]

/** Mock-Vorhersage: heute und sechs weitere Tage. */
export function mockForecast(code, temp) {
  if (code === 45 && temp === 7) {
    const codes = [45, 3, 61, 2, 0, 3, 73]
    const lows = [3, 2, 4, 1, -1, 0, -2]
    const highs = [9, 8, 9, 7, 6, 5, 3]
    return codes.map((c, i) => ({ code: c, min: lows[i], max: highs[i] }))
  }
  const next = [code, code === 0 ? 2 : 0, code >= 61 ? 3 : 61, 2, code, 3, 0]
  return next.map((c, i) => ({ code: c, max: Math.round(temp + 2 - i), min: Math.round(temp - 5 - i) }))
}
