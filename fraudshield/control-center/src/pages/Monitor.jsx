import { inr, inrCompact, timeAgo, clock } from '../format.js'
import StatusPill, { scoreColor } from '../components/StatusPill.jsx'
import { DecisionTimeline, HBars, ScoreBands } from '../components/Charts.jsx'

export default function Monitor({ stats, feed, freshIds, onOpen }) {
  const s = stats || {}
  const tiles = [
    { label: 'Payments checked', value: (s.total ?? 0).toLocaleString('en-IN'), hint: `avg score ${s.averageScore ?? 0}` },
    { label: 'Approved', icon: '✓', color: 'var(--good)', value: (s.approved ?? 0).toLocaleString('en-IN'), hint: 'money delivered' },
    { label: 'On hold', icon: '⏳', color: 'var(--warning)', value: (s.onHold ?? 0).toLocaleString('en-IN'), hint: 'waiting for analyst' },
    { label: 'Blocked', icon: '⛔', color: 'var(--critical)', value: (s.blocked ?? 0).toLocaleString('en-IN'), hint: `${s.fraudRate ?? 0}% flagged overall` },
    { label: 'Frozen accounts', icon: '🔒', value: (s.frozenAccounts ?? 0).toLocaleString('en-IN'), hint: 'cannot send money' },
    { label: 'Money protected', value: inrCompact(s.moneyProtected), hint: 'blocked + rejected' },
  ]
  return (
    <>
      <div className="grid kpis">
        {tiles.map((t) => (
          <div className="card kpi" key={t.label}>
            <div className="label">{t.color && <i className="dot" style={{ background: t.color }} />}{t.icon && <span aria-hidden="true">{t.icon}</span>}{t.label}</div>
            <div className="value num">{t.value}</div>
            <div className="hint">{t.hint}</div>
          </div>
        ))}
      </div>

      <div className="grid two">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16, minWidth: 0 }}>
          <div className="card">
            <h3>Decisions per minute</h3>
            <div className="sub">Every payment checked by FraudShield in the last 30 minutes</div>
            <DecisionTimeline data={s.timeline} />
          </div>
          <div className="grid" style={{ gridTemplateColumns: 'minmax(0,1.2fr) minmax(0,1fr)' }}>
            <div className="card">
              <h3>Most triggered rules</h3>
              <div className="sub">How often each rule fired (last 1,000 payments)</div>
              <HBars rows={(s.topRules || []).map((r) => ({ ...r, label: `${r.ruleId} ${r.name}` }))} labelKey="label" valueKey="count" />
            </div>
            <div className="card">
              <h3>Risk score distribution</h3>
              <div className="sub">Payments per score band</div>
              <ScoreBands rows={s.scoreBands} />
            </div>
          </div>
        </div>

        <div className="card">
          <h3>Live feed</h3>
          <div className="sub">New payments appear here instantly (Server-Sent Events)</div>
          <div className="feed">
            {feed.length === 0 && <div className="empty">Waiting for payments… make one in the Pay App or start demo traffic.</div>}
            {feed.map((c) => (
              <div key={c.transactionId} className={'feed-row' + (freshIds.has(c.transactionId) ? ' fresh' : '')} onClick={() => onOpen(c.transactionId)}>
                <div>
                  <span className="who">{c.senderName}</span>
                  <span className="muted"> → {c.receiverName}</span>
                  {c.simulated && <span className="bot-tag">SIM</span>}
                </div>
                <div style={{ textAlign: 'right' }}><b className="num">{inr(c.amount)}</b></div>
                <div className="meta">
                  <StatusPill status={c.status} />
                  <span>{c.channel || 'UPI'}</span>
                  <span>score <b className="score num" style={{ color: scoreColor(c.score) }}>{c.score}</b></span>
                  <span>📍 {c.locationCity || 'hidden'}</span>
                  {c.newDevice && <span style={{ color: 'var(--warning)' }}>new device</span>}
                  <span title={c.createdAt}>{clock(c.createdAt)} · {timeAgo(c.createdAt)}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </>
  )
}
