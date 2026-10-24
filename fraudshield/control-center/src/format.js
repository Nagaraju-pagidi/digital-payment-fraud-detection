export const inr = (v) =>
  v == null ? '-' : '₹' + Number(v).toLocaleString('en-IN', { maximumFractionDigits: 0 })

export const inrCompact = (v) => {
  const n = Number(v || 0)
  if (n >= 1e7) return '₹' + (n / 1e7).toFixed(1) + ' Cr'
  if (n >= 1e5) return '₹' + (n / 1e5).toFixed(1) + ' L'
  return inr(n)
}

export function timeAgo(iso) {
  if (!iso) return ''
  const s = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000))
  if (s < 60) return s + 's ago'
  if (s < 3600) return Math.floor(s / 60) + 'm ago'
  if (s < 86400) return Math.floor(s / 3600) + 'h ago'
  return new Date(iso).toLocaleDateString('en-IN')
}

export const dateTime = (iso) =>
  iso ? new Date(iso).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '-'

export const clock = (iso) =>
  iso ? new Date(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : ''

// Status = fixed meaning, never a series colour. Always shown with an icon + label.
export const STATUS = {
  APPROVED: { label: 'Approved', icon: '✓', cls: 'good' },
  ANALYST_APPROVED: { label: 'Released by analyst', icon: '✓', cls: 'good' },
  HOLD: { label: 'On hold', icon: '⏳', cls: 'warning' },
  BLOCKED: { label: 'Blocked', icon: '⛔', cls: 'critical' },
  ANALYST_REJECTED: { label: 'Rejected by analyst', icon: '✕', cls: 'critical' },
}
