import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { inr, dateTime, timeAgo } from '../format.js'
import StatusPill, { scoreColor } from './StatusPill.jsx'

/** Full investigation view of one payment: facts, location, rules, review actions and AI explanation. */
export default function CaseDrawer({ caseId, liveCase, onClose, onToast }) {
  const [c, setC] = useState(null)
  const [history, setHistory] = useState([])
  const [error, setError] = useState('')
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)
  const [ai, setAi] = useState(null)
  const [aiBusy, setAiBusy] = useState(false)
  const [aiError, setAiError] = useState('')

  useEffect(() => {
    let alive = true
    setC(null); setAi(null); setAiError(''); setError(''); setNote('')
    api(`/fraud/cases/${caseId}`)
      .then((d) => {
        if (!alive) return
        setC(d)
        return api(`/fraud/customers/${d.senderId}/cases`).then((h) => alive && setHistory(h || []))
      })
      .catch((e) => alive && setError(e.message))
    return () => { alive = false }
  }, [caseId])

  // keep in sync with live updates (e.g. another analyst reviewed it)
  useEffect(() => {
    if (liveCase && liveCase.transactionId === caseId) setC(liveCase)
  }, [liveCase, caseId])

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  async function review(action) {
    setBusy(true)
    try {
      const updated = await api(`/fraud/cases/${caseId}/${action}`, { method: 'POST', body: { note: note || null } })
      setC(updated)
      onToast(action === 'approve'
        ? `Payment #${caseId} released - money sent to the receiver`
        : `Payment #${caseId} rejected - money refunded to the sender`)
    } catch (e) {
      onToast(e.message)
    } finally {
      setBusy(false)
    }
  }

  async function explain(refresh = false) {
    setAiBusy(true); setAiError('')
    try {
      const res = await api(`/insights/transactions/${caseId}${refresh ? '?refresh=true' : ''}`)
      setAi(res)
    } catch (e) {
      setAiError(e.message)
    } finally {
      setAiBusy(false)
    }
  }

  return (
    <div className="overlay" onClick={onClose}>
      <aside className="drawer" onClick={(e) => e.stopPropagation()} aria-label="Payment investigation">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span className="muted">Payment #{caseId}</span>
          {c && <span className="pill neutral">{c.channel || 'UPI'}</span>}
          <span style={{ flex: 1 }} />
          <button className="btn small" onClick={onClose}>Close ✕</button>
        </div>
        {error && <div className="error">{error}</div>}
        {!c && !error && <div className="muted"><span className="spinner" /> Loading…</div>}
        {c && (
          <>
            <div>
              <h2 className="num">{inr(c.amount)}</h2>
              <div className="ink2" style={{ marginTop: 4 }}>
                <b>{c.senderName}</b> <span className="muted">({c.senderUpi})</span> → <b>{c.receiverName}</b>{' '}
                <span className="muted">({c.receiverUpi})</span>
              </div>
              <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap', alignItems: 'center' }}>
                <StatusPill status={c.status} />
                {c.accountFrozen && <span className="pill critical">🔒 Account frozen</span>}
                {c.simulated && <span className="pill neutral">Simulator traffic</span>}
                <span className="muted" style={{ fontSize: 12 }}>{dateTime(c.createdAt)}</span>
              </div>
            </div>

            {/* risk score gauge */}
            <div className="card" style={{ padding: 14 }}>
              <div className="gauge">
                <div style={{ fontSize: 30, fontWeight: 700, color: scoreColor(c.score) }} className="num">{c.score}</div>
                <div style={{ flex: 1 }}>
                  <div className="gauge-bar">
                    <b style={{ width: `${Math.min(100, (c.score / 150) * 100)}%`, background: scoreColor(c.score) }} />
                  </div>
                  <div className="gauge-scale"><span>0 approve</span><span>40 hold</span><span>70 block</span><span>120 freeze</span></div>
                </div>
              </div>
            </div>

            {/* where */}
            <div>
              <div className="muted" style={{ fontSize: 12, marginBottom: 6 }}>Where was the money sent from?</div>
              <div className="route">
                <div><div className="muted" style={{ fontSize: 11 }}>HOME CITY</div><b>{c.homeCity}</b> <span className="muted">{c.homeCountry}</span></div>
                <div className="line">{c.distanceFromHomeKm != null && <span>{Math.round(c.distanceFromHomeKm).toLocaleString('en-IN')} km</span>}</div>
                <div style={{ textAlign: 'right' }}>
                  <div className="muted" style={{ fontSize: 11 }}>PAID FROM</div>
                  <b>{c.locationCity || 'Location hidden'}</b> <span className="muted">{c.locationCountry || ''}</span>
                </div>
              </div>
            </div>

            <div className="facts">
              <div className="fact"><div className="k">Device</div><div className="v">{c.deviceLabel || 'Unknown'}{c.newDevice && <span className="new-badge">NEW</span>}</div></div>
              <div className="fact"><div className="k">Note</div><div className="v">{c.note || '-'}</div></div>
              <div className="fact"><div className="k">Automatic decision</div><div className="v">{c.decision}</div></div>
              <div className="fact"><div className="k">Reviewed by</div><div className="v">{c.reviewedBy ? `${c.reviewedBy} · ${timeAgo(c.reviewedAt)}` : '-'}</div></div>
            </div>

            {/* rules */}
            <div>
              <h3 style={{ margin: '0 0 8px', fontSize: 14 }}>Why this score? ({c.ruleHits.length} rule{c.ruleHits.length === 1 ? '' : 's'} triggered)</h3>
              {c.ruleHits.length === 0 && <div className="muted">Nothing unusual - matches this customer's normal behaviour.</div>}
              <div className="hits">
                {c.ruleHits.map((h) => (
                  <div className="hit" key={h.ruleId}>
                    <span className="rid">{h.ruleId}</span>
                    <span><b>{h.name}</b><br /><span className="ink2">{h.detail}</span></span>
                    <span className="pts">+{h.points}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* human review */}
            {c.status === 'HOLD' && (
              <div className="card" style={{ borderColor: 'rgba(250,178,25,.4)' }}>
                <h3>Analyst decision</h3>
                <div className="sub">The money is held. Approve to deliver it, or reject to refund the sender.</div>
                <textarea className="input" rows={2} placeholder="Note (e.g. 'Customer confirmed on phone')"
                  value={note} onChange={(e) => setNote(e.target.value)} />
                <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
                  <button className="btn good" disabled={busy} onClick={() => review('approve')}>✓ Approve & release</button>
                  <button className="btn bad" disabled={busy} onClick={() => review('reject')}>✕ Reject & refund</button>
                </div>
              </div>
            )}
            {c.reviewNote && <div className="hint-box">Analyst note: {c.reviewNote}</div>}

            {/* ML evidence */}
            <div className="ai-box">
              <h4 style={{ margin: '0 0 8px' }}>ML fraud assessment</h4>
              <div className="muted" style={{ fontSize: 13 }}>
                Risk score: <b>{c.score}/100</b>. The score combines the trained demo ML model with behavioral rules.
                Review the evidence below before making a final analyst decision. The bundled model is trained on synthetic teaching data and is not validated for real banking use.
              </div>
              {Array.isArray(c.ruleHits) && c.ruleHits.length > 0 && (
                <ul style={{ marginTop: 8 }}>{c.ruleHits.map((h, i) => <li key={i}><b>{h.name}</b>: {h.detail}</li>)}</ul>
              )}
            </div>

            {/* customer history */}
            <div>
              <h3 style={{ margin: '0 0 8px', fontSize: 14 }}>{c.senderName}'s recent payments</h3>
              <table className="table">
                <tbody>
                  {history.map((h) => (
                    <tr key={h.transactionId}>
                      <td className="muted num">#{h.transactionId}</td>
                      <td className="num">{inr(h.amount)}</td>
                      <td className="ink2">{h.locationCity || '—'}</td>
                      <td className="num" style={{ color: scoreColor(h.score) }}>{h.score}</td>
                      <td><StatusPill status={h.status} /></td>
                      <td className="muted">{timeAgo(h.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </aside>
    </div>
  )
}

function Explanation({ ai }) {
  if (ai.source === 'PENDING') return <div className="muted" style={{ marginTop: 8 }}>{ai.summary}</div>
  const fallback = ai.source !== 'AI'
  return (
    <div style={{ marginTop: 10, display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        <span className={'pill ' + (fallback ? 'neutral' : 'info')}>{fallback ? 'Rule-based fallback' : `Generated by ${ai.model}`}</span>
        {ai.riskLevel && <span className="pill neutral">Risk: {ai.riskLevel}</span>}
        <span className="pill neutral">{(ai.durationMs / 1000).toFixed(1)}s</span>
      </div>
      <div>{ai.summary}</div>
      {ai.keyFactors && ai.keyFactors.length > 0 && (
        <ul style={{ margin: 0, paddingLeft: 18 }} className="ink2">
          {ai.keyFactors.map((f, i) => <li key={i}><b>{f.rule}</b> {f.explanation}</li>)}
        </ul>
      )}
      {ai.recommendedAction && <div><span className="muted">Recommended action: </span>{ai.recommendedAction}</div>}
      {ai.customerMessage && <div><span className="muted">Message for customer: </span>“{ai.customerMessage}”</div>}
      {ai.knowledgeUsed && ai.knowledgeUsed.length > 0 && (
        <div>
          <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>Handbook entries retrieved from pgvector:</div>
          <div className="chips">{ai.knowledgeUsed.map((k) => <span className="chip" key={k.docId}>{k.docId} · {k.title}</span>)}</div>
        </div>
      )}
    </div>
  )
}
