// Small fetch wrapper: adds the analyst's JWT and turns errors into readable messages.
const TOKEN_KEY = 'fs_token'
const NAME_KEY = 'fs_name'

function read(key) {
  try { return localStorage.getItem(key) } catch { return null }
}
function write(key, value) {
  try { value == null ? localStorage.removeItem(key) : localStorage.setItem(key, value) } catch { /* ignore */ }
}

let memoryToken = read(TOKEN_KEY)
let onLogout = () => {}

export const session = {
  get token() { return memoryToken },
  get name() { return read(NAME_KEY) || 'Analyst' },
  save(token, name) { memoryToken = token; write(TOKEN_KEY, token); write(NAME_KEY, name) },
  clear() { memoryToken = null; write(TOKEN_KEY, null); write(NAME_KEY, null) },
  onLogout(fn) { onLogout = fn },
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (memoryToken) headers.Authorization = 'Bearer ' + memoryToken
  let res
  try {
    res = await fetch('/api' + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  } catch {
    throw new Error('Cannot reach the server. Is the API Gateway running?')
  }
  const text = await res.text()
  let data = null
  try { data = text ? JSON.parse(text) : null } catch { data = null }
  if (res.status === 401 && !path.includes('/auth/')) {
    session.clear()
    onLogout()
  }
  if (!res.ok && res.status !== 202) {
    const msg = data && data.message ? data.message
      : res.status === 503 ? 'Service is starting up - try again in a few seconds'
      : `Request failed (${res.status})`
    const err = new Error(msg)
    err.status = res.status
    throw err
  }
  return data
}

/** Live updates via Server-Sent Events. handlers = { eventName: fn(data) }. Returns a close() function. */
export function liveStream(path, handlers, onStatus) {
  if (!memoryToken) return () => {}
  const es = new EventSource('/api' + path + '?access_token=' + encodeURIComponent(memoryToken))
  es.onopen = () => onStatus && onStatus(true)
  es.onerror = () => onStatus && onStatus(false)
  Object.entries(handlers).forEach(([name, fn]) => {
    es.addEventListener(name, (e) => {
      try { fn(JSON.parse(e.data)) } catch { /* ignore malformed */ }
    })
  })
  return () => es.close()
}
