import { useState } from 'react'
import { deviceLabel } from '../device.js'
import { dateTime, greeting, groupAcc, inr, label } from '../format.js'
import StatusBadge from '../components/StatusBadge.jsx'

export function SecurityPanel({ user, loc, lastLogin, onRefreshLoc }) {
  const frozen = user.status === 'FROZEN'
  const locText = !loc || loc.loading ? 'Checking...' : loc.error ? (loc.error === 'denied' ? 'Location blocked in browser' : 'Location unavailable') : `${loc.city}${loc.country ? ', ' + loc.country : ''}`
  return (
    <div className="panel security">
      <div className="panel-head"><h3 className="grow">Security check</h3></div>
      <div className="sec-row">
        <span className={frozen ? 'bad' : 'ok'} aria-hidden="true">{frozen ? '⛔' : '✓'}</span>
        <div><div className="k">Account status</div><div className={'v ' + (frozen ? 'bad' : '')}>{frozen ? 'Frozen by fraud team' : 'Active'}</div></div>
      </div>
      <div className="sec-row">
        <span className="ok" aria-hidden="true">✓</span>
        <div><div className="k">This device</div><div className="v">{deviceLabel()}</div></div>
      </div>
      <div className="sec-row">
        <span className={loc?.error ? 'warn' : 'ok'} aria-hidden="true">{loc?.error ? '!' : '✓'}</span>
        <div>
          <div className="k">Current location <button className="btn link small" onClick={onRefreshLoc}>Refresh</button></div>
          <div className="v">{locText}</div>
          <div className="small muted">Registered home: {user.homeCity || 'set at your first login'}</div>
        </div>
      </div>
      <div className="sec-row">
        <span className="ok" aria-hidden="true">✓</span>
        <div>
          <div className="k">Previous login</div>
          <div className="v">{lastLogin ? dateTime(lastLogin.at) : 'This is your first login'}</div>
          {lastLogin && <div className="small muted">{lastLogin.deviceLabel}, {lastLogin.location}</div>}
        </div>
      </div>
      <div className="small muted" style={{ marginTop: 8 }}>Not you? Change your password in <a href="#/security">Profile & security</a>.</div>
    </div>
  )
}

export default function Dashboard({ user, payments, loc, lastLogin, onRefreshLoc, onOpenPayment }) {
  const [hidden, setHidden] = useState(false)
  const frozen = user.status === 'FROZEN'
  const firstName = user.fullName.split(' ')[0]
  return (
    <>
      <div className="page-head">
        <div className="grow">
          <h1>{greeting()}, {firstName}</h1>
          <p>Customer ID {user.customerId}</p>
        </div>
      </div>

      {frozen && (
        <div className="alert error" style={{ marginBottom: 18 }}>
          <span aria-hidden="true">⛔</span>
          <span><b>Your account is frozen for your safety.</b> {user.statusReason} Payments are paused until the bank verifies your identity. Please contact your branch.</span>
        </div>
      )}

      <div className="grid-2">
        <div className="stack">
          <section className="passbook" aria-label="Account summary">
            <div className="type">{label(user.accountType)} account</div>
            <div className="k">Account number</div>
            <div className="acc">{groupAcc(user.accountNumber)}</div>
            <div className="row">
              <div>
                <div className="k">Available balance <button className="toggle" onClick={() => setHidden(!hidden)}>{hidden ? 'Show' : 'Hide'}</button></div>
                <div className="bal">{hidden ? '₹ ••••••' : inr(user.balance)}</div>
              </div>
            </div>
            <div className="row" style={{ marginTop: 18 }}>
              <div><div className="k">IFSC</div><div className="v num">{user.ifsc}</div></div>
              <div><div className="k">Branch</div><div className="v">{user.branch}</div></div>
              <div><div className="k">UPI ID</div><div className="v">{user.upiId}</div></div>
            </div>
          </section>

          <nav className="quick" aria-label="Quick actions">
            <a href="#/transfer"><b>Transfer money</b><span>IMPS or NEFT to a payee</span></a>
            <a href="#/upi"><b>Pay by UPI</b><span>Send to any UPI ID</span></a>
            <a href="#/payees"><b>Add payee</b><span>Account number and IFSC</span></a>
            <a href="#/statement"><b>Statement</b><span>View or download</span></a>
          </nav>

          <div className="panel">
            <div className="panel-head">
              <h3 className="grow">Recent transactions</h3>
              <a href="#/transactions" className="small">View all</a>
            </div>
            {payments.length === 0 ? (
              <div className="text-2">No transactions yet. Add a payee or pay someone by UPI to get started.</div>
            ) : (
              <table className="table">
                <thead><tr><th>Date</th><th>Details</th><th>Mode</th><th className="r">Amount</th><th>Status</th></tr></thead>
                <tbody>
                  {payments.slice(0, 6).map((p) => (
                    <tr key={p.id + p.direction} className="click" onClick={() => onOpenPayment(p.id)}>
                      <td className="text-2 num">{dateTime(p.createdAt)}</td>
                      <td>{p.direction === 'SENT' ? 'To ' : 'From '}<b>{p.counterpartyName}</b></td>
                      <td className="text-2">{p.channel}</td>
                      <td className={'r num ' + (p.direction === 'SENT' ? 'debit' : 'credit')}>{p.direction === 'SENT' ? '-' : '+'}{inr(p.amount)}</td>
                      <td>{p.direction === 'SENT' ? <StatusBadge status={p.status} /> : <span className="badge good">✓ Received</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        <SecurityPanel user={user} loc={loc} lastLogin={lastLogin} onRefreshLoc={onRefreshLoc} />
      </div>
    </>
  )
}
