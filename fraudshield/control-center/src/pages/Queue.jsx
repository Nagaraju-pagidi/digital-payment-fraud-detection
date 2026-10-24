import { inr, timeAgo } from '../format.js'
import { scoreColor } from '../components/StatusPill.jsx'

export default function Queue({ cases, onOpen }) {
  if (cases.length === 0) {
    return <div className="card empty">🎉 No payments waiting for review. Held payments (score 40-69) will appear here.</div>
  }
  return (
    <div className="grid queue">
      {cases.map((c) => (
        <div className="card qcard" key={c.transactionId}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
            <span className="amount num">{inr(c.amount)}</span>
            <span className="muted">#{c.transactionId}</span>
            <span style={{ flex: 1 }} />
            <span className="num" style={{ fontWeight: 700, color: scoreColor(c.score) }}>score {c.score}</span>
          </div>
          <div className="ink2"><b>{c.senderName}</b> → {c.receiverName} <span className="muted">· {timeAgo(c.createdAt)}</span></div>
          <div className="muted" style={{ fontSize: 12 }}>📍 {c.locationCity || 'hidden'} (home {c.homeCity}) · {c.deviceLabel}{c.newDevice ? ' (new)' : ''}</div>
          <div className="hits">
            {c.ruleHits.map((h) => (
              <div className="hit" key={h.ruleId}><span className="rid">{h.ruleId}</span><span>{h.name}</span><span className="pts">+{h.points}</span></div>
            ))}
          </div>
          <button className="btn primary" onClick={() => onOpen(c.transactionId)} style={{ justifyContent: 'center' }}>Investigate & decide →</button>
        </div>
      ))}
    </div>
  )
}
