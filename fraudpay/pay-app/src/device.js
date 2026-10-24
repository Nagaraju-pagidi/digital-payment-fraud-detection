// Device + location are captured automatically - the customer never types them.
import { api } from './api.js'

const DEVICE_KEY = 'fp_device_id'
let memoryDeviceId = null

/** A random id created once per browser/phone and remembered (like a device fingerprint). */
export function deviceId() {
  if (memoryDeviceId) return memoryDeviceId
  let id = null
  try { id = localStorage.getItem(DEVICE_KEY) } catch { /* private mode */ }
  if (!id) {
    id = (crypto.randomUUID ? crypto.randomUUID() : 'dev-' + Math.random().toString(36).slice(2) + Date.now().toString(36))
    try { localStorage.setItem(DEVICE_KEY, id) } catch { /* ignore */ }
  }
  memoryDeviceId = id
  return id
}

/** "Chrome on Windows", "Safari on iPhone", ... */
export function deviceLabel() {
  const ua = navigator.userAgent
  const os = /Android/i.test(ua) ? 'Android' : /iPhone|iPad/i.test(ua) ? 'iPhone' : /Windows/i.test(ua) ? 'Windows'
    : /Mac OS X/i.test(ua) ? 'Mac' : /Linux/i.test(ua) ? 'Linux' : 'Unknown OS'
  const browser = /Edg\//.test(ua) ? 'Edge' : /OPR\//.test(ua) ? 'Opera' : /Firefox\//.test(ua) ? 'Firefox'
    : /Chrome\//.test(ua) ? 'Chrome' : /Safari\//.test(ua) ? 'Safari' : 'Browser'
  return `${browser} on ${os}`
}

/**
 * Current GPS position from the browser. Resolves { lat, lon } or { error }.
 * Tip for the demo: Chrome DevTools -> More tools -> Sensors -> Location lets you pretend to be in Dubai.
 */
export function currentPosition() {
  return new Promise((resolve) => {
    if (!navigator.geolocation) return resolve({ error: 'unsupported' })
    if (!window.isSecureContext) return resolve({ error: 'insecure' })
    navigator.geolocation.getCurrentPosition(
      (p) => resolve({ lat: p.coords.latitude, lon: p.coords.longitude }),
      (e) => resolve({ error: e.code === 1 ? 'denied' : 'unavailable' }),
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 0 },
    )
  })
}

/** Position + readable place name ("Hyderabad, IN"). */
export async function locate() {
  const pos = await currentPosition()
  if (pos.error) return pos
  try {
    const place = await api(`/auth/locate?lat=${pos.lat}&lon=${pos.lon}`)
    return { ...pos, city: place.city, country: place.country }
  } catch {
    return { ...pos, city: `${pos.lat.toFixed(2)}, ${pos.lon.toFixed(2)}` }
  }
}
