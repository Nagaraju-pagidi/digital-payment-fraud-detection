import { STATUS } from '../format.js'

export default function StatusBadge({ status }) {
  const s = STATUS[status] || { label: status, icon: '•', cls: 'neutral' }
  return <span className={'badge ' + s.cls}><span aria-hidden="true">{s.icon}</span>{s.label}</span>
}
