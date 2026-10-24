import { STATUS } from '../format.js'

export default function StatusPill({ status }) {
  const s = STATUS[status] || { label: status, icon: '•', cls: 'neutral' }
  return (
    <span className={'pill ' + s.cls}>
      <span aria-hidden="true">{s.icon}</span>
      {s.label}
    </span>
  )
}

export function scoreColor(score) {
  if (score >= 70) return 'var(--critical)'
  if (score >= 40) return 'var(--warning)'
  return 'var(--good)'
}
