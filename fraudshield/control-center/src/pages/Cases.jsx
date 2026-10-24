import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { inr, dateTime } from '../format.js'
import StatusPill, { scoreColor } from '../components/StatusPill.jsx'

const FILTERS = [
  { key: 'ALL', label: 'All' },
  { key: 'FLAGGED', label: 'Flagged (hold + blocked)' },
  { key: 'HOLD', label: 'On hold' },
  { key: 'BLOCKED', label: 'Blocked / rejected' },
  { key: 'APPROVED', label: 'Approved' },
]

export default function Cases({ onOpen, version }) {
  const [filter, setFilter] = useState('ALL')
  const [rows, setRows] = useState(null)
  const [error, setError] = useState('')
  const [hideSim, setHideSim] = useState(false)

  useEffect(() => {
    let alive = true
    api(`/fraud/cases?status=${filter}&limit=200`)
      .then((d) => alive && (setRows(d), setError('')))
      .catch((e) => alive && setError(e.message))
    return () => { alive = false }
  }, [filter, version])

  const visible = (rows || []).filter((r) => !(hideSim && r.simulated))
  return (
    <div className="card">
      <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap', marginBottom: 12 }}>
        <div className="tabs">
          {FILTERS.map((f) => (
            <button key={f.key} className={'tab' + (filter === f.key ? ' active' : '')} onClick={() => setFilter(f.key)}>{f.label}</button>
          ))}
        </div>
        <span style={{ flex: 1 }} />
        <label className="ink2" style={{ fontSize: 13, display: 'flex', gap: 6, alignItems: 'center' }}>
          <input type="checkbox" checked={hideSim} onChange={(e) => setHideSim(e.target.checked)} /> Hide simulator traffic
        </label>
      </div>
      {error && <div className="error">{error}</div>}
      {!rows && !error && <div className="muted"><span className="spinner" /> Loading…</div>}
      {rows && (
        <div style={{ overflowX: 'auto' }}>
          <table className="table">
            <thead>
              <tr><th>#</th><th>Channel</th><th>Time</th><th>From</th><th>To</th><th style={{ textAlign: 'right' }}>Amount</th><th>Paid from</th><th>Device</th><th style={{ textAlign: 'right' }}>Score</th><th>Status</th></tr>
            </thead>
            <tbody>
              {visible.map((c) => (
                <tr key={c.transactionId} className="click" onClick={() => onOpen(c.transactionId)}>
                  <td className="muted num">{c.transactionId}</td>
                  <td><span className="pill neutral">{c.channel || 'UPI'}</span></td>
                  <td className="ink2 num">{dateTime(c.createdAt)}</td>
                  <td>{c.senderName}{c.simulated && <span className="bot-tag">SIM</span>}</td>
                  <td className="ink2">{c.receiverName}</td>
                  <td className="num" style={{ textAlign: 'right' }}>{inr(c.amount)}</td>
                  <td className="ink2">{c.locationCity || 'hidden'}{c.locationCountry && c.locationCountry !== c.homeCountry ? ` (${c.locationCountry})` : ''}</td>
                  <td className="ink2">{c.deviceLabel}{c.newDevice && <span className="new-badge">NEW</span>}</td>
                  <td className="num" style={{ textAlign: 'right', fontWeight: 700, color: scoreColor(c.score) }}>{c.score}</td>
                  <td><StatusPill status={c.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
          {visible.length === 0 && <div className="empty">No payments in this view yet.</div>}
        </div>
      )}
    </div>
  )
}
