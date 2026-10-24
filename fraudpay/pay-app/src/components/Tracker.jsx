import { useEffect, useState } from 'react'
import { inr, maskAcc } from '../format.js'

/** Live status of a payment: held -> sent to FraudShield (Kafka) -> security check -> delivered / held / refunded. */
export default function Tracker({ payment, onDone, onAnother }) {
  const [sent, setSent] = useState(false)
  useEffect(() => {
    const t = setTimeout(() => setSent(true), 700)
    return () => clearTimeout(t)
  }, [])
  if (!payment) return null

  const st = payment.status
  const finished = st !== 'PROCESSING'
  const view = {
    PROCESSING: { cls: 'info', icon: <span className="spinner" />, title: 'Checking your payment', sub: 'This usually takes a few seconds. You can stay on this page.' },
    SUCCESS: { cls: 'good', icon: '✓', title: `${inr(payment.amount)} sent to ${payment.counterpartyName}`, sub: payment.statusMessage },
    ON_HOLD: { cls: 'warning', icon: '⏳', title: 'Payment on hold for verification', sub: 'Our fraud team is checking this payment. Your money is safe and has not left the bank.' },
    BLOCKED: { cls: 'critical', icon: '⛔', title: 'Payment blocked', sub: `We stopped this payment to protect you. ${inr(payment.amount)} is back in your account.` },
    REJECTED: { cls: 'critical', icon: '✕', title: 'Payment rejected', sub: `Our fraud team rejected this payment. ${inr(payment.amount)} is back in your account.` },
  }[st] || { cls: 'info', icon: '•', title: st, sub: '' }

  const step3 = !finished
    ? { cls: 'active', mark: <span className="spinner" />, b: 'Security check in progress', s: 'Location, device, amount and payee compared with your usual activity' }
    : { cls: 'done', mark: '✓', b: 'Security check complete', s: payment.fraudScore != null ? `Risk score ${payment.fraudScore} (below 40 is approved)` : '' }
  const step4 = st === 'SUCCESS' ? { cls: 'done', mark: '✓', b: `Credited to ${payment.counterpartyName}`, s: payment.counterpartyAccount ? `A/c ${payment.counterpartyAccount}` : '' }
    : st === 'ON_HOLD' ? { cls: 'hold', mark: '⏳', b: 'Waiting for fraud team review', s: 'You will get an email once it is decided' }
    : st === 'BLOCKED' || st === 'REJECTED' ? { cls: 'bad', mark: '↩', b: 'Amount reversed to your account', s: payment.statusMessage }
    : { cls: '', mark: '4', b: 'Credit to payee', s: '' }

  return (
    <div className="panel">
      <div className="tracker">
        <div className="outcome">
          <div className={'icon ' + view.cls}>{view.icon}</div>
          <h2>{view.title}</h2>
          <p className="text-2" style={{ marginTop: 6 }}>{view.sub}</p>
          <div className="review" style={{ marginTop: 16 }}>
            <div className="row"><span>Reference number</span><span className="num">{payment.referenceNo}</span></div>
            <div className="row"><span>Mode</span><span>{payment.channel}</span></div>
            <div className="row"><span>To</span><span>{payment.counterpartyName}{payment.counterpartyAccount ? ` (${maskAcc(payment.counterpartyAccount)})` : ` (${payment.counterpartyUpi})`}</span></div>
            <div className="row total"><span>Amount</span><span className="num">{inr(payment.amount)}</span></div>
            {payment.note && <div className="row"><span>Remarks</span><span>{payment.note}</span></div>}
          </div>
          {(st === 'ON_HOLD' || st === 'BLOCKED' || st === 'REJECTED') && payment.reasons?.length > 0 && (
            <div className={'alert ' + (st === 'ON_HOLD' ? 'warning' : 'error')} style={{ marginTop: 14, display: 'block' }}>
              <b>Why we stopped it</b>
              <ul style={{ margin: '6px 0 0', paddingLeft: 18 }}>{payment.reasons.map((r, i) => <li key={i}>{r}</li>)}</ul>
            </div>
          )}
        </div>
        <div>
          <h3 style={{ marginBottom: 14 }}>Payment progress</h3>
          <div className="steps" aria-live="polite">
            <div className="step done"><div className="mark">✓</div><div><b>Amount held from your account</b><small>Authorised with your transaction PIN{payment.channel !== 'UPI' ? ' and OTP' : ''}</small></div></div>
            <div className={'step ' + (sent || finished ? 'done' : 'active')}>
              <div className="mark">{sent || finished ? '✓' : <span className="spinner" />}</div>
              <div><b>Sent to FraudShield</b><small>Real-time fraud check service</small></div>
            </div>
            <div className={'step ' + step3.cls}><div className="mark">{step3.mark}</div><div><b>{step3.b}</b><small>{step3.s}</small></div></div>
            <div className={'step ' + step4.cls}><div className="mark">{step4.mark}</div><div><b>{step4.b}</b><small>{step4.s}</small></div></div>
          </div>
          <div className="small muted">From {payment.locationCity || 'location not shared'}, {payment.deviceLabel}</div>
          <div className="btn-row" style={{ marginTop: 18 }}>
            <button className="btn" onClick={onDone}>Go to dashboard</button>
            {onAnother && <button className="btn secondary" onClick={onAnother}>Make another payment</button>}
          </div>
        </div>
      </div>
    </div>
  )
}
