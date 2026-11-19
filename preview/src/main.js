// terebi+ Vorschau: simuliert Launcher, Einstellungen, Wetter, Hintergründe, Autostart und WLAN-QR.
// Nicht das Produkt. Massgebend ist die native App in app/. Zustandsmodell: docs/state-model.md.
import './style.css'
import QRCode from 'qrcode'
import tokens from '../../assets/tokens.json'
import codes from '../../assets/weather-codes.json'
import logoRaw from '../../assets/brand/logo-header.svg?raw'
import { resolveScene, wifiPayload, weekday, formatTemp, clampCountdown, createAutostart } from './logic.js'
import { ICONS, iconFor } from './icons.js'
import { CATALOG, CITIES, DEFAULT_VISIBLE, WEATHER_PRESETS, mockForecast } from './mock.js'

const $ = (id) => document.getElementById(id)
const stage = $('stage')
const esc = (s) => String(s).replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`)

// ---------- Tokens -> CSS-Variablen ----------
const T = tokens
const css = {
  '--ink': T.color.ink, '--text': T.color.text, '--text-secondary': T.color.textSecondary, '--accent': T.color.accent,
  '--focus-ring': T.color.focusRing, '--scrim': T.color.scrim, '--font': T.type.family,
  '--display': T.type.display + 'px', '--title': T.type.title + 'px', '--body': T.type.body + 'px', '--caption': T.type.caption + 'px',
  '--tile-label': T.type.tileLabel + 'px',
  '--w-regular': T.type.weightRegular, '--w-medium': T.type.weightMedium,
  '--safe-x': T.layout.safeX + 'px', '--safe-y': T.layout.safeY + 'px', '--logo-h': T.layout.logoHeight + 'px',
  '--app-w': T.layout.appWidth + 'px', '--app-h': T.layout.appHeight + 'px', '--app-gap': T.layout.appGap + 'px', '--app-r': T.layout.appRadius + 'px',
  '--sidebar-w': T.layout.sidebarWidth + 'px', '--sidebar-top': T.layout.sidebarTop + 'px', '--sidebar-h': T.layout.sidebarHeight + 'px',
  '--tile-w': T.layout.sidebarTileWidth + 'px', '--tile-h': T.layout.sidebarTileHeight + 'px', '--tile-gap': T.layout.sidebarGap + 'px', '--sidebar-qr': T.layout.sidebarQrSize + 'px',
  '--focus-scale': T.focus.scale, '--ring-w': T.focus.ringWidth + 'px', '--ring-off': T.focus.ringOffset + 'px', '--dim': T.focus.dimUnfocused,
  '--focus-ms': T.motion.focusMs + 'ms', '--overlay-ms': T.motion.overlayMs + 'ms', '--scene-ms': T.motion.sceneCrossfadeMs + 'ms',
  '--ease': 'cubic-bezier(.2,0,0,1)', '--sim-w': '340px',
}
for (const [k, v] of Object.entries(css)) document.documentElement.style.setProperty(k, v)

// ---------- Zustand ----------
const STORE = 'terebi.preview.v1'
const defaults = () => ({
  installed: CATALOG.map((a) => a.id),
  order: CATALOG.map((a) => a.id),
  visible: CATALOG.slice(0, DEFAULT_VISIBLE).map((a) => a.id),
  favorites: [],
  cityIdx: 0,
  forecastWeekMigrated: true,
  settings: {
    background: 'auto', effects: true, activeWeatherMode: 'ON', lightningEnabled: true, reduceMotion: false, iconSize: 'M', showClock: true, showForecast: true,
    showWeather: true, unit: 'C',
    autostart: { enabled: false, pkg: CATALOG[0].id, seconds: 3 },
    wifi: { enabled: false, ssid: '', security: 'WPA', hidden: false, autoClose: 120 },
  },
})
const load = () => {
  try {
    const s = JSON.parse(localStorage.getItem(STORE))
    if (s) {
      const d = defaults()
      const settings = { ...d.settings, ...s.settings }
      if (!s.forecastWeekMigrated) settings.showForecast = true
      return { ...d, ...s, forecastWeekMigrated: true, settings }
    }
  } catch {}
  return defaults()
}
const st = load()
let wifiPassword = '' // nur im Speicher, nie in localStorage (Vorschau). Nativ: Android Keystore.
const save = () => {
  try { localStorage.setItem(STORE, JSON.stringify({ installed: st.installed, order: st.order, visible: st.visible, favorites: st.favorites, cityIdx: st.cityIdx, forecastWeekMigrated: true, settings: st.settings })) } catch {}
}

const sim = { realtime: false, minute: 20 * 60 + 49, code: 45, temp: 7, offline: false, fetchedAt: new Date(), guides: false }
const ui = { layers: [], focus: null, prevFocus: [], section: 'apps', settingsDetail: false, moving: null, menu: null, search: null, confirm: null, countdown: null, running: null, toastTimer: 0, lastKey: Date.now(), qrTimer: 0 }
const autostart = createAutostart()

const app = (id) => CATALOG.find((a) => a.id === id)
const city = () => CITIES[st.cityIdx] || CITIES[0]
const minuteNow = () => (sim.realtime ? (() => { const d = new Date(); return d.getHours() * 60 + d.getMinutes() })() : sim.minute)
const homeApps = () => {
  const vis = st.order.filter((id) => st.installed.includes(id) && st.visible.includes(id))
  return [...vis.filter((id) => st.favorites.includes(id)), ...vis.filter((id) => !st.favorites.includes(id))]
}

// ---------- Logo (Original-SVG, nur Hintergrundfläche entfernt, Proportionen unverändert) ----------
function renderLogo() {
  $('logo').innerHTML = logoRaw.replace(/<rect[^>]*\/>/, '').replace(/<title>.*?<\/title>/, '')
  const svg = $('logo').querySelector('svg')
  svg.removeAttribute('width'); svg.removeAttribute('height')
  document.fonts.ready.then(() => {
    const b = svg.getBBox()
    svg.setAttribute('viewBox', `${b.x} ${b.y} ${b.width} ${b.height}`)
  })
}

// ---------- Hintergrund ----------
let sceneOn = 'sceneA', sceneKey = ''
const rgba = (hex, a) => { const n = parseInt(hex.slice(1), 16); return `rgba(${n >> 16},${(n >> 8) & 255},${n & 255},${a})` }
function currentScene() {
  if (st.settings.activeWeatherMode === 'OFF') return { scene: 'afternoon', dim: false }
  if (st.settings.background !== 'auto') return { scene: st.settings.background, dim: false }
  const c = city()
  return resolveScene(minuteNow(), c.sunrise, c.sunset, sim.code)
}
function renderBackground() {
  const { scene, dim } = currentScene()
  const s = T.scenes[scene]
  const fxOn = st.settings.activeWeatherMode === 'ON' && st.settings.effects && !st.settings.reduceMotion ? s.effect : 'none'
  $('fx').className = 'fx ' + fxOn + (scene === 'storm' && st.settings.lightningEnabled && fxOn !== 'none' ? ' storm' : '')
  $('dimmer').style.opacity = 0
  const key = scene
  if (key === sceneKey) return
  sceneKey = key
  const next = sceneOn === 'sceneA' ? 'sceneB' : 'sceneA'
  const tint = .05
  $(next).style.background = `radial-gradient(circle at ${s.glowX * 100}% ${s.glowY * 100}%, ${rgba(s.glow, s.glowAlpha * tint)}, transparent 55%), linear-gradient(180deg, ${rgba(s.stops[0], tint)}, ${rgba(s.stops[1], tint)} 55%, ${rgba(s.stops[2], tint)})`
  $(next).classList.add('on'); $(sceneOn).classList.remove('on')
  sceneOn = next
}

// ---------- Kopfzeile ----------
function renderWeather() {
  const el = $('weather'), card = $('weatherCard')
  const date = new Intl.DateTimeFormat('de-CH', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date())
  const compact = !st.settings.showWeather ? `<span class="header-compact">${formatTemp(sim.temp, st.settings.unit)} ${esc((codes.codes[sim.code] || codes.codes[3]).label)}</span>` : ''
  el.innerHTML = `${compact}<span class="header-date">${esc(date)}</span><span class="header-clock">${clock()}</span>`
  card.hidden = !st.settings.showWeather
  if (card.hidden) return
  const c = city(), info = codes.codes[sim.code] || codes.codes[3]
  const isDay = minuteNow() > c.sunrise && minuteNow() < c.sunset
  const days = mockForecast(sim.code, sim.temp)
  const scaleMin = Math.min(...days.map((f) => f.min)) - 1
  const scaleMax = Math.max(...days.map((f) => f.max)) + 1
  const pct = (v) => Math.max(0, Math.min(100, (v - scaleMin) / (scaleMax - scaleMin) * 100))
  const range = (f, i) => {
    const date = new Date(); date.setDate(date.getDate() + i)
    const ic = (codes.codes[f.code] || codes.codes[3]).icon
    const dot = i === 0 ? `<i class="current-dot" style="left:${pct(sim.temp)}%"></i>` : ''
    return `<div class="forecast-row"><span class="fc-day">${i === 0 ? 'Heute' : weekday(date).slice(0, 2)}</span><span class="fc-icon">${ICONS[ic]}</span><span class="fc-min">${formatTemp(f.min, st.settings.unit)}</span><span class="fc-track"><i class="fc-range" style="left:${pct(f.min)}%;width:${pct(f.max) - pct(f.min)}%"></i>${dot}</span><span class="fc-max">${formatTemp(f.max, st.settings.unit)}</span></div>`
  }
  const conditionLabel = !isDay && sim.code === 0 ? 'Klar' : !isDay && sim.code === 1 ? 'Meist klar' : info.label
  card.innerHTML = `<div class="forecast-title">WETTER · ${esc(c.name.toLocaleUpperCase('de-CH'))}</div>
    <div class="weather-current"><span class="current-icon">${iconFor(info.icon, isDay)}</span><span class="current-temp">${formatTemp(sim.temp, st.settings.unit)}</span><span class="current-hilo">H ${formatTemp(days[0].max, st.settings.unit)}<br>T ${formatTemp(days[0].min, st.settings.unit)}</span></div>
    <div class="current-condition">${esc(conditionLabel)}</div><div class="weather-divider"></div>
    <div class="forecast-list">${days.map(range).join('')}</div>
    <div class="weather-source">${sim.offline ? 'Stand ' + sim.fetchedAt.toTimeString().slice(0, 5) + ' · offline' : 'Open-Meteo · ' + sim.fetchedAt.toTimeString().slice(0, 5)}</div>`
}
const clock = () => { const m = minuteNow(); return `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}` }
function renderTopActions() {
  $('topActions').innerHTML = `<button class="icon-btn" data-key="top:settings" aria-label="Einstellungen">${ICONS.gear}</button>`
}

// ---------- Apps ----------
function renderApps() {
  const list = homeApps()
  const w = st.settings.wifi
  const qrReady = w.enabled && w.ssid && (w.security === 'nopass' || wifiPassword)
  const qr = qrReady ? `<div class="sidebar-qr" data-key="top:wifi" aria-label="WLAN QR-Code für ${esc(w.ssid)} gross anzeigen"><div class="qr-card"><canvas id="sidebarQr"></canvas></div><div class="qr-copy"><b>WLAN · GÄSTE</b><strong>${esc(w.ssid)}</strong><small>Mit dem Handy scannen</small></div></div>` : ''
  if (!list.length) {
    $('apps').innerHTML = `<div class="app-scroll"><div class="empty">Keine Apps eingeblendet.<br>Öffne <b>Einstellungen › Apps</b>.</div></div>${qr}`
    if (qrReady) renderSidebarQr(w)
    return
  }
  const cols = Math.min(list.length, T.layout.appColumns)
  $('apps').innerHTML = `<div class="app-scroll"><div class="grid" style="--cols:${cols}">` + list.map((id) => {
    const a = app(id)
    return `<div class="app${ui.moving === id ? ' moving' : ''}" data-key="app:${id}" aria-label="${esc(a.label)}"><div class="preview-icon" style="--hue:${a.hue}">${esc(a.label === "YouTube" ? "▶" : a.label === "Netflix" ? "N" : a.label === "Disney+" ? "D+" : a.label === "Prime Video" ? "pv" : a.label === "Play SRF" ? "SRF" : a.label.slice(0,1))}</div><div class="banner">${esc(a.label)}</div>${st.favorites.includes(id) ? '<span class="fav" aria-label="Favorit"></span>' : ''}</div>`
  }).join('') + `</div></div>${qr}`
  if (qrReady) renderSidebarQr(w)
}
function renderSidebarQr(w) {
  const canvas = $('sidebarQr')
  QRCode.toCanvas(canvas, wifiPayload({ ssid: w.ssid, password: wifiPassword, security: w.security, hidden: w.hidden }),
    { errorCorrectionLevel: 'M', margin: 4, width: 120, color: { dark: '#07080B', light: '#F2F0EB' } })
}
function renderAppName() {
  const el = $('appName')
  const id = ui.layers.length === 0 && ui.focus?.startsWith('app:') ? ui.focus.slice(4) : null
  el.classList.toggle('on', Boolean(id))
  if (id) el.innerHTML = `${esc(app(id).label)}<small>${ui.moving ? 'Pfeiltasten verschieben · OK fertig' : 'OK öffnen · Halten für Optionen'}</small>`
}

// ---------- Einstellungen ----------
const SECTIONS = [['apps', 'Apps'], ['look', 'Darstellung'], ['weather', 'Wetter'], ['autostart', 'Autostart'], ['wifi', 'WLAN'], ['system', 'System']]
const SCENE_LABELS = { auto: 'Automatisch', sunrise: 'Sonnenaufgang', morning: 'Sonniger Morgen', afternoon: 'Sonniger Nachmittag', cloudy: 'Bewölkter Tag', sunset: 'Sonnenuntergang', evening: 'Abend', night: 'Nacht', rain: 'Regen', snow: 'Schneefall', fog: 'Nebel', storm: 'Gewitter' }
const SECURITY = [['WPA', 'WPA/WPA2'], ['SAE', 'WPA3'], ['WEP', 'WEP'], ['nopass', 'Offen']]
const cycle = (list, v) => list[(list.indexOf(v) + 1) % list.length]
const tog = (on) => `<span class="toggle${on ? ' on' : ''}"></span>`

// Jede Zeile: key, label, value-HTML, action
function rows(section) {
  const s = st.settings, r = []
  const add = (key, label, val, act, extra = '') => r.push({ key, label, val, act, extra })
  if (section === 'apps') {
    for (const id of st.order.filter((i) => st.installed.includes(i))) {
      const a = app(id), on = st.visible.includes(id)
      add(`appRow:${id}`, (st.favorites.includes(id) ? '<span class="chip"></span>' : '') + esc(a.label), on ? 'Angezeigt' : 'Ausgeblendet', () => openMenu(id), on ? '' : ' muted')
    }
  }
  if (section === 'look') {
    add('bg', 'Hintergrund', SCENE_LABELS[s.background], () => { s.background = cycle(Object.keys(SCENE_LABELS), s.background) })
    add('effects', 'Wettereffekte', tog(s.effects), () => { s.effects = !s.effects })
    add('reduce', 'Animationen reduzieren', tog(s.reduceMotion), () => { s.reduceMotion = !s.reduceMotion })
    add('iconSize', 'Icon-Grösse', { S: 'Klein', M: 'Mittel', L: 'Gross' }[s.iconSize], () => { s.iconSize = cycle(['S', 'M', 'L'], s.iconSize) })
  }
  if (section === 'weather') {
    add('activeWeather', 'Aktives Wetter', { ON: 'Ein', IMAGE_ONLY: 'Nur Bild', OFF: 'Aus' }[s.activeWeatherMode], () => { s.activeWeatherMode = cycle(['ON', 'IMAGE_ONLY', 'OFF'], s.activeWeatherMode) })
    if (s.activeWeatherMode === 'ON') add('lightning', 'Blitze anzeigen', tog(s.lightningEnabled), () => { s.lightningEnabled = !s.lightningEnabled })
    add('showWeather', 'Wetteranzeige', tog(s.showWeather), () => { s.showWeather = !s.showWeather })
    add('city', 'Standort', esc(city().name), () => {})
    add('search', 'Stadt suchen', `<input data-input="search" placeholder="Name eingeben" value="${esc(ui.search?.q || '')}">`, () => editInput('search'))
    if (ui.search?.results) {
      if (!ui.search.results.length) add('noResult', 'Keine Treffer', '', () => {})
      for (const i of ui.search.results) add(`cityPick:${i}`, esc(CITIES[i].name), esc(CITIES[i].admin), () => { st.cityIdx = i; ui.search = null; ui.focus = 'city'; toast(`Standort: ${CITIES[i].name}`) })
    }
    add('refresh', 'Wetter aktualisieren', sim.offline ? 'offline' : '', () => {
      if (sim.offline) toast('Keine Verbindung. Letzte Daten bleiben sichtbar.')
      else { sim.fetchedAt = new Date(); toast('Wetter aktualisiert (Mock)') }
    })
    add('unit', 'Temperatureinheit', s.unit === 'C' ? '°C' : '°F', () => { s.unit = s.unit === 'C' ? 'F' : 'C' })
  }
  if (section === 'autostart') {
    const a = s.autostart
    add('asOn', 'Autostart', tog(a.enabled), () => { a.enabled = !a.enabled })
    const cands = st.order.filter((i) => st.installed.includes(i))
    add('asApp', 'App', esc(app(a.pkg)?.label || 'Keine'), () => { a.pkg = cycle(cands, a.pkg) })
    add('asSec', 'Countdown', `${a.seconds} s`, () => { a.seconds = a.seconds >= 5 ? 1 : clampCountdown(a.seconds + 1) })
    add('asTest', 'Jetzt testen', 'simuliert Einschalten', () => { closeAll(); powerOn() })
  }
  if (section === 'wifi') {
    const w = s.wifi
    add('wOn', 'QR-Code-Funktion', tog(w.enabled), () => { w.enabled = !w.enabled })
    add('ssid', 'WLAN-Name (SSID)', `<input data-input="ssid" placeholder="z. B. Zuhause" value="${esc(w.ssid)}">`, () => editInput('ssid'))
    add('sec', 'Sicherheit', SECURITY.find((x) => x[0] === w.security)[1], () => { w.security = cycle(SECURITY.map((x) => x[0]), w.security) })
    if (w.security !== 'nopass') add('pw', 'Passwort', `<input data-input="pw" type="password" placeholder="Passwort" value="${esc(wifiPassword)}">`, () => editInput('pw'))
    add('hidden', 'Verstecktes Netz', tog(w.hidden), () => { w.hidden = !w.hidden })
    add('autoClose', 'Automatisch schliessen', w.autoClose ? `${w.autoClose / 60} min` : 'Nie', () => { w.autoClose = cycle([60, 120, 300, 0], w.autoClose) })
    add('showQr', 'QR-Code anzeigen', '', () => openQr())
    add('wDel', ui.confirm === 'wDel' ? 'Wirklich löschen? OK bestätigt' : 'Zugangsdaten löschen', '', () => confirmThen('wDel', () => { w.ssid = ''; wifiPassword = ''; w.enabled = false; toast('WLAN-Zugangsdaten gelöscht') }))
  }
  if (section === 'system') {
    add('android', 'Android-Einstellungen öffnen', '', () => toast('Nativ: öffnet Settings.ACTION_SETTINGS'))
    add('home', 'Als Standard-Launcher einrichten', '', () => toast('Nativ: RoleManager.ROLE_HOME, sonst Home-Einstellungen. Auf manchen Google-TV-Geräten nur per ADB (siehe docs/installation.md).'))
    add('cache', 'Cache leeren', 'Wetterdaten', () => toast('Wetter-Cache geleert'))
    add('reset', ui.confirm === 'reset' ? 'Wirklich zurücksetzen? OK bestätigt' : 'Konfiguration zurücksetzen', '', () => confirmThen('reset', () => { Object.assign(st, defaults()); wifiPassword = ''; toast('Konfiguration zurückgesetzt') }))
    add('restore', 'Original-Launcher wiederherstellen', 'Anleitung', () => openInfo('Original-Launcher', 'terebi+ deaktiviert keine Systempakete. Zurück zum Google-TV-Startbildschirm: Einstellungen › Apps › terebi+ › «Als Standard entfernen» oder terebi+ deinstallieren. ADB-Weg: docs/installation.md.'))
    add('version', 'Version', '0.1.0 · Vorschau', () => {})
  }
  return r
}
function confirmThen(key, fn) {
  if (ui.confirm === key) { ui.confirm = null; fn() } else ui.confirm = key
}
function renderSettings() {
  const el = $('settings')
  el.hidden = !ui.layers.includes('settings')
  if (el.hidden) return
  const nav = SECTIONS.map(([k, l]) => `<div class="item${ui.section === k ? ' sel' : ''}" data-key="sec:${k}">${l}</div>`).join('')
  const rs = rows(ui.section)
  const note = {
    apps: 'OK öffnet Optionen. «Ausblenden» entfernt eine App nur aus terebi+. Sie bleibt auf dem Fernseher installiert.',
    look: 'Der Hintergrund ist live sichtbar. Automatisch folgt er Tageszeit und Wetter.',
    weather: 'Wetterdaten: Open-Meteo.com (CC BY 4.0). Aktualisierung alle 45 Minuten, ohne Verbindung bleiben die letzten Daten.',
    autostart: 'Startet die App nur nach dem Einschalten des Fernsehers, nie bei Rückkehr aus einer App. Jede Taste bricht ab.',
    wifi: 'Der QR-Code entsteht nur auf dem Gerät. Das Passwort wird verschlüsselt gespeichert und nie gesendet.',
    system: 'terebi+ verändert keine Systempakete und braucht kein Root.',
  }[ui.section]
  el.innerHTML = ui.settingsDetail
    ? `<div class="s-pane"><h2>${SECTIONS.find((x) => x[0] === ui.section)[1]}</h2><div class="item" data-key="settings:back">Zurück</div><div class="scroll"><div class="row-list">${rs.map((x) => `<div class="item${x.extra}" data-key="${x.key}"><span>${x.label}</span><span class="val">${x.val}</span></div>`).join('')}</div></div><p class="note">${note}</p></div>`
    : `<div class="s-pane"><h2>Einstellungen</h2><div class="row-list">${nav}</div></div>`
}

// ---------- Menüs, QR, Info ----------
function openMenu(id) {
  ui.menu = { type: 'app', id }
  pushLayer('menu', 'm:toggle')
}
function openInfo(title, text) { ui.menu = { type: 'info', title, text }; pushLayer('menu', 'm:ok') }
function menuItems() {
  const m = ui.menu
  if (m.type === 'info') return [['m:ok', 'Verstanden', () => popLayer()]]
  const id = m.id, vis = st.visible.includes(id), fav = st.favorites.includes(id)
  return [
    ['m:toggle', vis ? 'Aus terebi+ ausblenden' : 'In terebi+ anzeigen', () => { st.visible = vis ? st.visible.filter((x) => x !== id) : [...st.visible, id]; popLayer() }],
    ['m:fav', fav ? 'Kein Favorit mehr' : 'Als Favorit', () => { st.favorites = fav ? st.favorites.filter((x) => x !== id) : [...st.favorites, id]; popLayer() }],
    ...(vis ? [['m:move', 'Verschieben', () => { closeAll(); ui.moving = id; ui.focus = 'app:' + id }]] : []),
    ['m:up', 'In Liste nach oben', () => moveInOrder(id, -1)],
    ['m:down', 'In Liste nach unten', () => moveInOrder(id, 1)],
    ['m:info', 'App-Info (Android)', () => { popLayer(); toast('Nativ: öffnet die Android-App-Einstellungen dieser App') }],
  ]
}
function moveInOrder(id, d) {
  const o = st.order, i = o.indexOf(id), j = i + d
  if (j >= 0 && j < o.length) [o[i], o[j]] = [o[j], o[i]]
}
function renderMenu() {
  const el = $('menu')
  el.hidden = !ui.layers.includes('menu')
  if (el.hidden) return
  const m = ui.menu
  const head = m.type === 'info' ? `<h4>${esc(m.title)}</h4><p>${esc(m.text)}</p>` : `<h4>${esc(app(m.id).label)}</h4>${st.visible.includes(m.id) ? '' : '<p>Ausgeblendet. Bleibt auf dem Fernseher installiert.</p>'}`
  el.innerHTML = `<div class="card">${head}<div class="row-list">${menuItems().map(([k, l]) => `<div class="item" data-key="${k}">${l}</div>`).join('')}</div></div>`
}
async function openQr() {
  const w = st.settings.wifi
  if (!w.ssid) { toast('Zuerst WLAN-Namen eintragen'); return }
  if (w.security !== 'nopass' && !wifiPassword) { toast('Zuerst Passwort eintragen'); return }
  pushLayer('qr', 'qr:close')
  const el = $('qr')
  el.innerHTML = `<div class="card"><div class="code"><canvas id="qrCanvas"></canvas></div><div><h4>WLAN für Gäste</h4>
    <p>Kamera auf den Code richten und verbinden.</p><p>Netz <b>${esc(w.ssid)}</b></p>
    <p>${w.autoClose ? `Schliesst nach ${w.autoClose / 60} min. ` : ''}Zurück schliesst.</p><span data-key="qr:close"></span></div></div>`
  await QRCode.toCanvas($('qrCanvas'), wifiPayload({ ssid: w.ssid, password: wifiPassword, security: w.security, hidden: w.hidden }), { errorCorrectionLevel: 'M', margin: 0, width: 520, color: { dark: '#1B1B1D', light: '#FFFFFF' } })
  clearTimeout(ui.qrTimer)
  if (w.autoClose) ui.qrTimer = setTimeout(() => { if (ui.layers.at(-1) === 'qr') { popLayer(); render() } }, w.autoClose * 1000)
}

// ---------- Ebenen und Fokus ----------
function pushLayer(name, focus) { ui.prevFocus.push(ui.focus); ui.layers.push(name); ui.focus = focus }
function popLayer() {
  const name = ui.layers.pop()
  if (name === 'qr') { clearTimeout(ui.qrTimer); $('qr').hidden = true }
  ui.focus = ui.prevFocus.pop() ?? null
  ui.confirm = null
}
function closeAll() { while (ui.layers.length) popLayer() }
function scope() {
  const top = ui.layers.at(-1)
  return top ? $(top) : stage
}
function focusables() {
  const root = scope()
  return [...root.querySelectorAll('[data-key]')].filter((el) => !el.closest('.layer[hidden]') && (root !== stage || !el.closest('.layer')))
}
function ensureFocus() {
  const els = focusables()
  if (!els.find((e) => e.dataset.key === ui.focus)) {
    const first = els.find((e) => e.dataset.key.startsWith('app:')) || els.find((e) => e.dataset.key.startsWith('sec:' + ui.section)) || els[0]
    ui.focus = first?.dataset.key ?? null
  }
  for (const e of stage.querySelectorAll('.focused')) e.classList.remove('focused')
  focusables().find((e) => e.dataset.key === ui.focus)?.classList.add('focused')
}
function move(dir) {
  const els = focusables(), cur = els.find((e) => e.dataset.key === ui.focus)
  if (!cur) return ensureFocus()
  // Einstellungen: links aus dem Inhalt immer zur aktiven Rubrik
  if (ui.layers.at(-1) === 'settings' && dir === 'left' && ui.settingsDetail) { ui.focus = 'settings:back'; return }
  const a = cur.getBoundingClientRect(), ax = a.left + a.width / 2, ay = a.top + a.height / 2
  let best = null, bestScore = Infinity
  for (const e of els) {
    if (e === cur) continue
    const b = e.getBoundingClientRect(), bx = b.left + b.width / 2, by = b.top + b.height / 2
    const dx = bx - ax, dy = by - ay
    const primary = { left: -dx, right: dx, up: -dy, down: dy }[dir]
    if (primary <= 1) continue
    const ortho = dir === 'left' || dir === 'right' ? Math.abs(dy) : Math.abs(dx)
    const score = primary + ortho * 2.5
    if (score < bestScore) { bestScore = score; best = e }
  }
  if (best) ui.focus = best.dataset.key
}
function moveApp(dir) {
  const list = homeApps(), i = list.indexOf(ui.moving)
  const cols = Math.min(list.length, T.layout.appColumns)
  const j = { left: i - 1, right: i + 1, up: i - cols, down: i + cols }[dir]
  if (j < 0 || j >= list.length) return
  const a = ui.moving, b = list[j]
  if (st.favorites.includes(a) !== st.favorites.includes(b)) return // Favoriten bleiben vorne
  const o = st.order, ia = o.indexOf(a), ib = o.indexOf(b)
  ;[o[ia], o[ib]] = [o[ib], o[ia]]
}

// ---------- Aktionen ----------
function activate() {
  const k = ui.focus
  if (!k) return
  if (k === 'top:settings') return openSettings()
  if (k === 'top:wifi') return openQr()
  if (k.startsWith('app:')) {
    if (ui.moving) { ui.moving = null; toast('Reihenfolge gespeichert'); return }
    return launch(k.slice(4))
  }
  if (k.startsWith('sec:')) { ui.section = k.slice(4); ui.settingsDetail = true; ui.search = null; ui.focus = 'settings:back'; return }
  if (k === 'settings:back') { ui.settingsDetail = false; ui.focus = 'sec:' + ui.section; return }
  if (k === 'qr:close') return popLayer()
  if (ui.layers.at(-1) === 'menu') return menuItems().find((x) => x[0] === k)?.[2]()
  const row = rows(ui.section).find((x) => x.key === k)
  if (row) { if (ui.confirm && ui.confirm !== k) ui.confirm = null; row.act() }
}
function longPress() {
  const k = ui.focus
  if (k?.startsWith('app:') && !ui.moving) openMenu(k.slice(4))
  else if (k?.startsWith('appRow:')) openMenu(k.slice(7))
  else activate()
}
function openSettings() { if (!ui.layers.includes('settings')) { ui.settingsDetail = false; pushLayer('settings', 'sec:' + ui.section) } }
function launch(id) {
  ui.running = id
  $('running').hidden = false
  $('running').innerHTML = `<div><h4>${esc(app(id).label)}</h4><p>läuft (simuliert) · Zurück oder Home kehrt zu terebi+ zurück</p></div>`
}
function back() {
  if (ui.running) return endRunning()
  if (ui.moving) { ui.moving = null; return }
  if (ui.confirm) { ui.confirm = null; return }
  if (ui.layers.at(-1) === 'settings' && ui.settingsDetail) { ui.settingsDetail = false; ui.focus = 'sec:' + ui.section; return }
  if (ui.layers.length) return popLayer()
  // Startseite: Zurück verlässt den Launcher nie.
}
function home() {
  if (ui.running) endRunning()
  ui.moving = null; closeAll(); ui.focus = homeApps()[0] ? 'app:' + homeApps()[0] : 'top:settings'
}
function endRunning() { ui.running = null; $('running').hidden = true } // Rückkehr: kein neuer Autostart

// ---------- Autostart ----------
function powerOn() {
  if (ui.running) endRunning()
  autostart.powerOn()
  const cfg = st.settings.autostart
  if (!autostart.consume(cfg, st.installed)) {
    if (cfg.enabled) toast('Autostart-App nicht installiert. terebi+ bleibt offen.')
    return render()
  }
  if (ui.countdown) return // nie zwei Countdowns
  const total = clampCountdown(cfg.seconds)
  ui.countdown = { pkg: cfg.pkg, total, left: total, t0: performance.now() }
  tickCountdown()
}
function tickCountdown() {
  const c = ui.countdown
  if (!c) return
  const elapsed = (performance.now() - c.t0) / 1000
  c.left = Math.max(0, c.total - elapsed)
  const el = $('countdown')
  el.hidden = false
  el.innerHTML = `<div class="num" style="--p:${(elapsed / c.total) * 100}%"><span>${Math.ceil(c.left)}</span></div><div>${esc(app(c.pkg).label)} startet<br><small>Beliebige Taste: abbrechen</small></div>`
  if (c.left <= 0) { cancelCountdown(); launch(c.pkg); return }
  c.raf = requestAnimationFrame(tickCountdown)
}
function cancelCountdown(msg) {
  if (!ui.countdown) return
  cancelAnimationFrame(ui.countdown.raf)
  ui.countdown = null
  $('countdown').hidden = true
  if (msg) toast(msg)
}

// ---------- Eingabefelder ----------
function editInput(name) {
  // nach dem folgenden render(), das die Zeilen neu aufbaut
  setTimeout(() => {
    const input = $('settings').querySelector(`[data-input="${name}"]`)
    if (input) { input.focus(); input.select() }
  })
}
function commitInput(input) {
  const v = input.value.trim()
  const name = input.dataset.input
  if (name === 'ssid') st.settings.wifi.ssid = v
  if (name === 'pw') wifiPassword = input.value
  if (name === 'search') {
    const n = (s) => s.toLowerCase().normalize('NFD').replace(/\p{M}/gu, '')
    ui.search = v ? { q: v, results: CITIES.map((c, i) => [c, i]).filter(([c]) => n(c.name).includes(n(v))).map(([, i]) => i) } : null
  }
  input.blur()
  render()
}

function toast(text) {
  const el = $('toast'); el.textContent = text; el.hidden = false
  clearTimeout(ui.toastTimer); ui.toastTimer = setTimeout(() => (el.hidden = true), 3600)
}

// ---------- Tasten ----------
let enterTimer = 0, enterLong = false
window.addEventListener('keydown', (e) => {
  if (e.target.closest?.('#sim')) return
  if (e.target.tagName === 'INPUT') {
    if (e.key === 'Enter' || e.key === 'Escape') { e.preventDefault(); commitInput(e.target) }
    return
  }
  ui.lastKey = Date.now(); stage.classList.remove('idle')
  const nav = { ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right' }[e.key]
  if (nav || ['Enter', 'Escape', 'Backspace', 'h', 'Home', 'm', 'ContextMenu'].includes(e.key)) e.preventDefault()
  if (ui.countdown) { if (!e.repeat) cancelCountdown('Autostart abgebrochen'); return }
  if (ui.running) { if (['Escape', 'Backspace', 'h', 'Home'].includes(e.key)) { endRunning(); render() } return }
  if (nav) { ui.moving ? moveApp(nav) : move(nav) }
  else if (e.key === 'Enter') {
    if (e.repeat) return
    enterLong = false
    enterTimer = setTimeout(() => { enterLong = true; longPress(); render() }, T.motion.longPressMs)
    return
  }
  else if (e.key === 'Escape' || e.key === 'Backspace') back()
  else if (e.key === 'h' || e.key === 'Home') home()
  else if (e.key === 'm' || e.key === 'ContextMenu') openSettings()
  else return
  render()
})
window.addEventListener('keyup', (e) => {
  if (e.key !== 'Enter' || e.target.tagName === 'INPUT' || e.target.closest?.('#sim')) return
  if (!enterTimer) return
  clearTimeout(enterTimer); enterTimer = 0
  if (!enterLong) { activate(); render() }
})

// ---------- Render ----------
function render() {
  save()
  stage.classList.toggle('still', st.settings.reduceMotion)
  stage.classList.toggle('guides', sim.guides)
  renderBackground(); renderWeather(); renderTopActions(); renderApps(); renderSettings(); renderMenu()
  if (!ui.layers.includes('qr')) $('qr').hidden = true
  else $('qr').hidden = false
  ensureFocus(); renderAppName(); syncSim()
}

// ---------- Skalierung 1920x1080 ----------
function fit() {
  const simW = $('sim').hidden ? 0 : 340
  document.documentElement.style.setProperty('--sim-w', simW + 'px')
  const s = Math.min((innerWidth - simW - 24) / 1920, (innerHeight - 24) / 1080)
  document.documentElement.style.setProperty('--scale', Math.max(0.1, s))
}
addEventListener('resize', fit)

// ---------- Simulator-Panel ----------
function buildSim() {
  const opt = (v, l, sel) => `<option value="${v}"${sel ? ' selected' : ''}>${l}</option>`
  $('sim').innerHTML = `<h1>terebi+ Simulator</h1><div class="hint">Nicht Teil des Launchers. Bedienung im Bild: <kbd>←↑→↓</kbd> <kbd>Enter</kbd> (lang = Optionen) <kbd>Esc</kbd> Zurück <kbd>H</kbd> Home <kbd>M</kbd> Menü</div>
  <h2>Tageszeit</h2>
  <label><input type="checkbox" id="simReal"> Echtzeit (Gerätezeit)</label>
  <label><input type="range" id="simMin" min="0" max="1439" step="5"><span id="simMinL"></span></label>
  <div class="btns">${[['Sonnenaufgang', 'sr'], ['Morgen', 540], ['Mittag', 750], ['Sonnenuntergang', 'ss'], ['Abend', 1140], ['Nacht', 1410]].map(([l, v]) => `<button data-min="${v}">${l}</button>`).join('')}</div>
  <h2>Wetter</h2>
  <select id="simCode">${WEATHER_PRESETS.map((c) => opt(c, `${c} · ${codes.codes[c].label}`)).join('')}</select>
  <label><input type="range" id="simTemp" min="-15" max="35"><span id="simTempL"></span></label>
  <label><input type="checkbox" id="simOff"> Offline (letzte Daten, als veraltet markiert)</label>
  <h2>Standort</h2>
  <select id="simCity">${CITIES.map((c, i) => opt(i, c.name)).join('')}</select>
  <h2>Apps (Mock-Katalog)</h2>
  <div class="btns"><select id="simInst"></select><button id="simInstB">Installieren</button></div>
  <div class="btns" style="margin-top:6px"><select id="simUn"></select><button id="simUnB">Deinstallieren</button></div>
  <h2>Gerät</h2>
  <div class="btns"><button id="simPower">Einschalten (Autostart-Sitzung)</button></div>
  <label><input type="checkbox" id="simGuides"> Sichere Ränder anzeigen</label>
  <div class="btns"><button id="simReset">Vorschau zurücksetzen</button></div>
  <h2>Nur auf Android prüfbar</h2>
  <div class="hint">Echte App-Icons und Starts, Home-Taste, Standard-Launcher-Rolle, Keystore, Open-Meteo-Abruf, Bootverhalten, Leistung. Siehe docs/preview.md.</div>`
  const on = (id, ev, fn) => $(id).addEventListener(ev, (e) => { fn(e); e.target.blur?.(); render() })
  on('simReal', 'change', (e) => { sim.realtime = e.target.checked })
  on('simMin', 'input', (e) => { sim.realtime = false; sim.minute = +e.target.value })
  for (const b of $('sim').querySelectorAll('[data-min]')) b.addEventListener('click', () => {
    const v = b.dataset.min; sim.realtime = false
    sim.minute = v === 'sr' ? city().sunrise : v === 'ss' ? city().sunset : +v
    b.blur(); render()
  })
  on('simCode', 'change', (e) => { sim.code = +e.target.value })
  on('simTemp', 'input', (e) => { sim.temp = +e.target.value })
  on('simOff', 'change', (e) => { sim.offline = e.target.checked; if (!sim.offline) sim.fetchedAt = new Date() })
  on('simCity', 'change', (e) => { st.cityIdx = +e.target.value })
  // wie nativ (AppConfig.sync): neue Apps erscheinen sichtbar am Ende
  on('simInstB', 'click', () => { const id = $('simInst').value; if (id) { st.installed.push(id); st.visible.push(id); st.order = [...st.order.filter((x) => x !== id), id]; toast(`${app(id).label} installiert und am Ende eingefügt`) } })
  on('simUnB', 'click', () => {
    const id = $('simUn').value; if (!id) return
    // Bereinigung wie nativ bei PACKAGE_REMOVED
    st.installed = st.installed.filter((x) => x !== id); st.visible = st.visible.filter((x) => x !== id); st.favorites = st.favorites.filter((x) => x !== id)
    toast(`${app(id).label} deinstalliert und aus terebi+ entfernt`)
  })
  on('simPower', 'click', () => { closeAll(); powerOn() })
  on('simGuides', 'change', (e) => { sim.guides = e.target.checked })
  on('simReset', 'click', () => { try { localStorage.removeItem(STORE) } catch {} location.reload() })
  const t = document.createElement('button'); t.className = 'sim-toggle'; t.textContent = 'Panel'
  t.onclick = () => { $('sim').hidden = !$('sim').hidden; t.blur(); fit() }
  document.body.append(t)
}
function syncSim() {
  $('simReal').checked = sim.realtime
  $('simMin').value = minuteNow(); $('simMinL').textContent = clock()
  $('simCode').value = sim.code; $('simTemp').value = sim.temp; $('simTempL').textContent = sim.temp + ' °C'
  $('simOff').checked = sim.offline; $('simCity').value = st.cityIdx
  const opts = (ids) => ids.map((id) => `<option value="${id}">${esc(app(id).label)}</option>`).join('') || '<option value="">–</option>'
  $('simInst').innerHTML = opts(CATALOG.map((a) => a.id).filter((id) => !st.installed.includes(id)))
  $('simUn').innerHTML = opts(st.installed)
}

// ---------- Start ----------
renderLogo(); buildSim(); fit(); render()
setInterval(() => {
  if (Date.now() - ui.lastKey > T.motion.idleReduceAfterMs) stage.classList.add('idle')
  if (sim.realtime) { renderBackground(); renderWeather(); syncSim() }
}, 15000)
// Erster Start der Vorschau = Einschalten
powerOn()
