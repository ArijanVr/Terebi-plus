// Reine Logik der Vorschau. Spiegelt die native App (SceneResolver.kt, WifiQr.kt, AutoStart.kt).
// Die native App ist massgebend; beide Seiten prüfen dieselben Fixtures aus assets/.

export const WINDOW_MIN = 45

export function weatherCategory(code) {
  if ([95, 96, 97, 99].includes(code)) return 'storm'
  if ([56, 57, 66, 67].includes(code) || (code >= 71 && code <= 77) || code === 85 || code === 86) return 'snow'
  if ((code >= 51 && code <= 55) || (code >= 61 && code <= 65) || (code >= 80 && code <= 82)) return 'rain'
  if (code === 45 || code === 48) return 'fog'
  if (code >= 0 && code <= 2) return 'clear'
  return 'cloudy'
}

/** Szene aus Minute des Tages, Sonnenauf-/-untergang (Minuten) und WMO-Code. */
export function resolveScene(minute, sunrise, sunset, code) {
  const cat = weatherCategory(code)
  // Wetterlagen bleiben auch abends sichtbar; dunkle Wetterszenen werden sanft abgedunkelt.
  if (cat !== 'clear') {
    return { scene: cat, dim: minute < sunrise - WINDOW_MIN || minute > sunset + WINDOW_MIN }
  }
  const dim = false
  if (Math.abs(minute - sunrise) <= WINDOW_MIN) return { scene: 'sunrise', dim }
  if (Math.abs(minute - sunset) <= WINDOW_MIN) return { scene: 'sunset', dim }
  if (minute > sunrise && minute < sunset) {
    return { scene: minute < 12 * 60 ? 'morning' : 'afternoon', dim }
  }
  if (minute > sunset && minute < 22 * 60) return { scene: 'evening', dim }
  return { scene: 'night', dim }
}

const SPECIAL = new Set(['\\', ';', ',', ':', '"'])
export const escapeWifi = (s) => [...s].map((c) => (SPECIAL.has(c) ? '\\' + c : c)).join('')

/** WIFI:T:<WPA|SAE|WEP|nopass>;S:<ssid>;P:<pw>;H:true;; */
export function wifiPayload({ ssid, password, security, hidden }) {
  let p = `WIFI:T:${security};S:${escapeWifi(ssid)};`
  if (security !== 'nopass') p += `P:${escapeWifi(password || '')};`
  if (hidden) p += 'H:true;'
  return p + ';'
}

export const weekday = (date, locale = 'de-CH') =>
  new Intl.DateTimeFormat(locale, { weekday: 'long' }).format(date)

export const formatTemp = (c, unit) => `${Math.round(unit === 'F' ? c * 9 / 5 + 32 : c)}°`

export const clampCountdown = (s) => Math.min(5, Math.max(1, Math.round(s)))

/**
 * Autostart-Sitzung. Eine Sitzung beginnt nur bei «Einschalten» (Boot oder Aufwachen aus Standby),
 * nie bei Rückkehr aus einer App oder Neuzeichnen. Pro Sitzung höchstens ein Countdown.
 */
export function createAutostart() {
  let armed = false
  return {
    powerOn() { armed = true },
    /** Liefert true genau einmal pro Sitzung, wenn der Countdown starten darf. */
    consume(cfg, installed) {
      if (!armed) return false
      armed = false
      return Boolean(cfg.enabled && cfg.pkg && installed.includes(cfg.pkg))
    },
  }
}
