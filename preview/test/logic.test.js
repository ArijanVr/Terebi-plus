import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolveScene, wifiPayload, escapeWifi, weekday, clampCountdown, createAutostart, formatTemp } from '../src/logic.js'
import { mockForecast } from '../src/mock.js'

const read = (p) => JSON.parse(readFileSync(new URL(p, import.meta.url)))

test('Szenen entsprechen den gemeinsamen Fixtures', () => {
  for (const c of read('../../assets/scene-cases.json').cases) {
    assert.deepEqual(resolveScene(c.minute, c.sunrise, c.sunset, c.code), { scene: c.scene, dim: c.dim }, JSON.stringify(c))
  }
})

test('jede Szene aus den Fixtures hat Tokens', () => {
  const scenes = read('../../assets/tokens.json').scenes
  for (const c of read('../../assets/scene-cases.json').cases) assert.ok(scenes[c.scene], c.scene)
  assert.equal(Object.keys(scenes).length, 11)
})

test('Wochenvorschau enthält heute und sechs weitere Tage', () => {
  assert.equal(mockForecast(61, 7).length, 7)
})

test('WLAN-Payload maskiert Sonderzeichen', () => {
  assert.equal(escapeWifi('a;b,c:d"e\\f'), 'a\\;b\\,c\\:d\\"e\\\\f')
  assert.equal(wifiPayload({ ssid: 'Heim', password: 'p;w', security: 'WPA', hidden: false }), 'WIFI:T:WPA;S:Heim;P:p\\;w;;')
  assert.equal(wifiPayload({ ssid: 'Gast', password: 'x', security: 'nopass', hidden: true }), 'WIFI:T:nopass;S:Gast;H:true;;')
})

test('Wochentag ausgeschrieben, Countdown 1–5, Temperatur', () => {
  assert.equal(weekday(new Date(2026, 10, 18)), 'Mittwoch')
  assert.deepEqual([0, 1, 3, 5, 9].map(clampCountdown), [1, 1, 3, 5, 5])
  assert.equal(formatTemp(20, 'F'), '68°')
  assert.equal(formatTemp(-0.4, 'C'), '0°')
})

test('Autostart: genau ein Countdown pro Einschalten, keiner bei Rückkehr', () => {
  const a = createAutostart()
  const cfg = { enabled: true, pkg: 'yt' }
  assert.equal(a.consume(cfg, ['yt']), false)
  a.powerOn()
  assert.equal(a.consume(cfg, ['yt']), true)
  assert.equal(a.consume(cfg, ['yt']), false)
  a.powerOn()
  assert.equal(a.consume(cfg, []), false)
})
