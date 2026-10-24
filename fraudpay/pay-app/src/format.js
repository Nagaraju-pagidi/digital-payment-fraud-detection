export const inr = (v, digits = 2) =>
  v == null || v === '' ? '-' : '₹' + Number(v).toLocaleString('en-IN', { minimumFractionDigits: digits, maximumFractionDigits: 2 })

export const dateTime = (iso) =>
  iso ? new Date(iso).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '-'

export const dateOnly = (iso) =>
  iso ? new Date(iso).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-'

export const timeOnly = (iso) =>
  iso ? new Date(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : ''

export const maskAcc = (a) => (a && a.length > 4 ? 'XXXX ' + a.slice(-4) : a)

export const groupAcc = (a) => (a ? a.replace(/(\d{4})(?=\d)/g, '$1 ') : '')

export const label = (code) =>
  code ? code.replaceAll('_', ' ').toLowerCase().replace(/^./, (c) => c.toUpperCase()) : ''

export const greeting = () => {
  const h = new Date().getHours()
  return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'
}

// Payment status: fixed meaning, always shown with an icon + words (never colour alone)
export const STATUS = {
  PROCESSING: { label: 'Security check', icon: '⏳', cls: 'info' },
  SUCCESS: { label: 'Successful', icon: '✓', cls: 'good' },
  ON_HOLD: { label: 'On hold', icon: '⏳', cls: 'warning' },
  BLOCKED: { label: 'Blocked, refunded', icon: '⛔', cls: 'critical' },
  REJECTED: { label: 'Rejected, refunded', icon: '✕', cls: 'critical' },
}

// ---- client-side checks (the server checks everything again) ----
const D = [[0,1,2,3,4,5,6,7,8,9],[1,2,3,4,0,6,7,8,9,5],[2,3,4,0,1,7,8,9,5,6],[3,4,0,1,2,8,9,5,6,7],[4,0,1,2,3,9,5,6,7,8],[5,9,8,7,6,0,4,3,2,1],[6,5,9,8,7,1,0,4,3,2],[7,6,5,9,8,2,1,0,4,3],[8,7,6,5,9,3,2,1,0,4],[9,8,7,6,5,4,3,2,1,0]]
const P = [[0,1,2,3,4,5,6,7,8,9],[1,5,7,6,2,8,3,0,9,4],[5,8,0,3,7,9,6,1,4,2],[8,9,1,6,0,4,3,5,2,7],[9,4,5,3,1,2,6,8,7,0],[4,2,8,6,5,7,3,9,0,1],[2,7,9,3,8,0,6,4,1,5],[7,0,4,6,9,1,3,2,5,8]]

export function aadhaarValid(v) {
  if (!/^[2-9][0-9]{11}$/.test(v)) return false
  let c = 0
  for (let i = 0; i < 12; i++) c = D[c][P[i % 8][Number(v[11 - i])]]
  return c === 0
}

export function passwordChecks(pw, username) {
  return [
    { ok: pw.length >= 8 && pw.length <= 20, text: '8 to 20 characters' },
    { ok: /[A-Z]/.test(pw), text: 'An uppercase letter' },
    { ok: /[a-z]/.test(pw), text: 'A lowercase letter' },
    { ok: /[0-9]/.test(pw), text: 'A number' },
    { ok: /[^A-Za-z0-9]/.test(pw), text: 'A special character (@ # $ ! ...)' },
    { ok: !username || !pw.toLowerCase().includes(username.toLowerCase()), text: 'Does not contain your User ID' },
  ]
}

export function ageOf(dob) {
  if (!dob) return null
  const d = new Date(dob), n = new Date()
  let a = n.getFullYear() - d.getFullYear()
  if (n < new Date(n.getFullYear(), d.getMonth(), d.getDate())) a--
  return a
}
