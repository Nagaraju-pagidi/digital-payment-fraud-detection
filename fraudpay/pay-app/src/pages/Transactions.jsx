import { useState } from 'react'
import { dateTime, inr, maskAcc } from '../format.js'
import StatusBadge from '../components/StatusBadge.jsx'

const FILTERS = [['ALL', 'All'], ['SUCCESS', 'Successful'], ['ON_HOLD', 'On hold'], ['STOPPED', 'Blocked or rejected'], ['RECEIVED', 'Received']]

export default function Transactions({ payments, onOpenPayment }) {
  const [filter, setFilter] = useState('ALL')
  const rows = payments.filter((p) =>
    filter === 'ALL' ? true
      : filter === 'RECEIVED' ? p.direction === 'RECEIVED'
      : filter === 'STOPPED' ? p.direction === 'SENT' && (p.status === 'BLOCKED' || p.status === 'REJECTED')
      : p.direction === 'SENT' && p.status === filter)
  return (
    <>
      <div className="page-head">
        <div className="grow">
          <h1>Transactions</h1>
          <p>Every transfer and UPI payment, with its fraud-check result. Select a row for details.</p>
        </div>
      </div>
      <div className="tabs" role="tablist">
        {FILTERS.map(([k, l]) => <button key={k} role="tab" aria-selected={filter === k} className={filter === k ? 'on' : ''} onClick={() => setFilter(k)}>{l}</button>)}
      </div>
      <div className="table-wrap">
        <table className="table">
          <thead><tr><th>Date</th><th>Reference</th><th>Details</th><th>Mode</th><th className="r">Amount</th><th>Status</th></tr></thead>
          <tbody>
            {rows.map((p) => (
              <tr key={p.id + p.direction} className="click" onClick={() => onOpenPayment(p.id)}>
                <td className="text-2 num">{dateTime(p.createdAt)}</td>
                <td className="text-2 num small">{p.referenceNo}</td>
                <td>{p.direction === 'SENT' ? 'To ' : 'From '}<b>{p.counterpartyName}</b>
                  <div className="small muted">{p.counterpartyAccount ? 'A/c ' + maskAcc(p.counterpartyAccount) : p.counterpartyUpi}{p.note ? `, ${p.note}` : ''}</div></td>
                <td className="text-2">{p.channel}</td>
                <td className={'r num ' + (p.direction === 'SENT' ? 'debit' : 'credit')}>{p.direction === 'SENT' ? '-' : '+'}{inr(p.amount)}</td>
                <td>{p.direction === 'SENT' ? <StatusBadge status={p.status} /> : <span className="badge good">✓ Received</span>}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {rows.length === 0 && <div className="text-2" style={{ padding: 16 }}>No transactions in this view.</div>}
      </div>
    </>
  )
}

export function PaymentDrawer({ p, onClose }) {
  if (!p) return null
  const sent = p.direction === 'SENT'
  return (
    <div className="drawer-overlay" onClick={onClose}>
      <aside className="drawer" onClick={(e) => e.stopPropagation()} aria-label="Transaction details">
        <div className="panel-head">
          <h2 className="grow">{sent ? 'Payment details' : 'Money received'}</h2>
          <button className="btn secondary small" onClick={onClose}>Close</button>
        </div>
        <div style={{ fontSize: 30, fontWeight: 600, color: 'var(--ink)' }} className="num">{inr(p.amount)}</div>
        <div style={{ margin: '8px 0 16px' }}>{sent ? <StatusBadge status={p.status} /> : <span className="badge good">✓ Received</span>}</div>
        <div className="review">
          <div className="row"><span>Reference number</span><span className="num">{p.referenceNo}</span></div>
          <div className="row"><span>Mode</span><span>{p.channel}</span></div>
          <div className="row"><span>{sent ? 'To' : 'From'}</span><span>{p.counterpartyName}</span></div>
          {p.counterpartyAccount && <div className="row"><span>Account</span><span className="num">{p.counterpartyAccount}</span></div>}
          <div className="row"><span>UPI ID</span><span>{p.counterpartyUpi}</span></div>
          <div className="row"><span>Date</span><span>{dateTime(p.createdAt)}</span></div>
          {p.note && <div className="row"><span>Remarks</span><span>{p.note}</span></div>}
          {sent && <div className="row"><span>Status</span><span>{p.statusMessage}</span></div>}
          {sent && <div className="row"><span>Paid from</span><span>{p.locationCity || 'Location not shared'}</span></div>}
          {sent && <div className="row"><span>Device</span><span>{p.deviceLabel}</span></div>}
          {sent && p.fraudScore != null && <div className="row"><span>Fraud risk score</span><span className="num">{p.fraudScore}</span></div>}
        </div>
        {sent && p.reasons?.length > 0 && (
          <div className={'alert ' + (p.status === 'SUCCESS' ? 'info' : p.status === 'ON_HOLD' ? 'warning' : 'error')} style={{ marginTop: 14, display: 'block' }}>
            <b>Security notes</b>
            <ul style={{ margin: '6px 0 0', paddingLeft: 18 }}>{p.reasons.map((r, i) => <li key={i}>{r}</li>)}</ul>
          </div>
        )}
      </aside>
    </div>
  )
}
