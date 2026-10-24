import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { deviceId, deviceLabel, locate } from '../device.js'
import { inr, maskAcc, timeOnly } from '../format.js'
import OtpDialog from '../components/OtpDialog.jsx'
import Tracker from '../components/Tracker.jsx'
import { ErrorBox, Field, PinInput } from '../components/Fields.jsx'

const MODES = [
  ['IMPS', 'IMPS', 'Instant, 24x7. Up to ₹5,00,000 per transfer'],
  ['NEFT', 'NEFT', 'Settles in batches. Up to ₹10,00,000 per transfer'],
]

/** Net-banking fund transfer: payee -> amount -> review -> PIN -> email OTP -> live fraud check. */
export default function Transfer({ user, payments, onLocated, onSent, onDone }) {
  const [payees, setPayees] = useState(null)
  const [payeeId, setPayeeId] = useState(null)
  const [amount, setAmount] = useState('')
  const [mode, setMode] = useState('IMPS')
  const [remarks, setRemarks] = useState('')
  const [stage, setStage] = useState('form') // form | review | tracking
  const [pin, setPin] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [challenge, setChallenge] = useState(null)
  const [trackId, setTrackId] = useState(null)

  const load = () => api('/users/payees').then((d) => setPayees(d || [])).catch((e) => setError(e.message))
  useEffect(() => { load() }, [])

  const payee = payees?.find((p) => p.id === payeeId)
  const value = Number(amount)
  const frozen = user.status === 'FROZEN'

  function review(e) {
    e.preventDefault()
    setError('')
    if (!payee) return setError('Choose a payee')
    if (!(value >= 1)) return setError('Enter an amount of at least ₹1')
    if (value > Number(user.balance)) return setError('Amount is more than your available balance')
    setStage('review')
  }

  async function authorise(e) {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      const here = await locate()
      onLocated(here)
      const ch = await api('/payments/transfer', {
        method: 'POST',
        body: {
          payeeId, amount: value, mode, remarks: remarks || null, pin, deviceId: deviceId(), deviceLabel: deviceLabel(),
          latitude: here.error ? null : here.lat, longitude: here.error ? null : here.lon,
        },
      })
      setChallenge(ch)
    } catch (err) {
      setError(err.message)
    } finally {
      setPin('')
      setBusy(false)
    }
  }

  if (stage === 'tracking') {
    const p = payments.find((x) => x.id === trackId && x.direction === 'SENT')
    return (
      <>
        <div className="page-head"><div className="grow"><h1>Transfer status</h1></div></div>
        <Tracker payment={p} onDone={onDone} onAnother={() => { setStage('form'); setAmount(''); setRemarks(''); setPayeeId(null) }} />
      </>
    )
  }

  return (
    <>
      <div className="page-head">
        <div className="grow">
          <h1>Transfer money</h1>
          <p>Send money to a saved payee by IMPS or NEFT.</p>
        </div>
        <a className="btn secondary" href="#/payees">Add a payee</a>
      </div>

      {frozen && <div className="alert error" style={{ marginBottom: 18 }}>Your account is frozen. Transfers are not allowed until the bank verifies you.</div>}

      {stage === 'form' && (
        <form className="grid-2" onSubmit={review}>
          <div className="panel form">
            <h3>1. Choose payee</h3>
            {!payees && <div className="muted"><span className="spinner" /> Loading payees</div>}
            {payees && payees.length === 0 && <div className="alert info">You have no payees yet. <a href="#/payees">Add a payee</a> first.</div>}
            {payees && payees.length > 0 && (
              <div className="payee-list" role="radiogroup" aria-label="Payees">
                {payees.map((p) => (
                  <label key={p.id} className={'payee-opt' + (payeeId === p.id ? ' on' : '') + (p.active ? '' : ' disabled')}>
                    <input type="radio" name="payee" disabled={!p.active} checked={payeeId === p.id} onChange={() => setPayeeId(p.id)} />
                    <span className="avatar">{p.name.split(' ').map((w) => w[0]).slice(0, 2).join('')}</span>
                    <span><b>{p.nickname ? `${p.nickname} (${p.name})` : p.name}</b><br /><span className="small muted">A/c {maskAcc(p.accountNumber)}, {p.ifsc}</span></span>
                    {p.active ? <span className="badge neutral">{p.branch}</span> : <span className="badge warning">Active from {timeOnly(p.activeFrom)}</span>}
                  </label>
                ))}
              </div>
            )}
          </div>
          <div className="panel form">
            <h3>2. Amount and mode</h3>
            <Field label="Amount (₹)" hint={`Available balance ${inr(user.balance)}`}>
              <input className="input big" inputMode="decimal" value={amount} placeholder="0.00"
                onChange={(e) => setAmount(e.target.value.replace(/[^0-9.]/g, ''))} />
            </Field>
            <Field label="Transfer mode" group>
              <div className="form" style={{ gap: 8 }}>
                {MODES.map(([v, t, d]) => (
                  <label key={v} className={'radio-card' + (mode === v ? ' on' : '')}>
                    <input type="radio" name="mode" checked={mode === v} onChange={() => setMode(v)} />
                    <div><b>{t}</b><span>{d}</span></div>
                  </label>
                ))}
              </div>
            </Field>
            <Field label="Remarks (optional)">
              <input className="input" maxLength={140} value={remarks} onChange={(e) => setRemarks(e.target.value)} />
            </Field>
            <ErrorBox message={error} />
            <button className="btn" disabled={frozen || !payee || !amount}>Review transfer</button>
          </div>
        </form>
      )}

      {stage === 'review' && payee && (
        <form className="grid-2" onSubmit={authorise}>
          <div className="panel">
            <h3 style={{ marginBottom: 14 }}>Review and confirm</h3>
            <div className="review">
              <div className="row"><span>From</span><span>{user.fullName}, A/c {maskAcc(user.accountNumber)}</span></div>
              <div className="row"><span>To</span><span>{payee.name}</span></div>
              <div className="row"><span>Payee account</span><span className="num">{payee.accountNumber}</span></div>
              <div className="row"><span>IFSC</span><span className="num">{payee.ifsc}</span></div>
              <div className="row"><span>Mode</span><span>{mode}</span></div>
              {remarks && <div className="row"><span>Remarks</span><span>{remarks}</span></div>}
              <div className="row total"><span>Amount</span><span className="num">{inr(value)}</span></div>
            </div>
          </div>
          <div className="panel form">
            <h3>Authorise</h3>
            <Field label="Transaction PIN" hint="Your 6-digit PIN. An OTP will also be sent to your email.">
              <PinInput value={pin} onChange={setPin} autoFocus />
            </Field>
            <ErrorBox message={error} />
            <div className="btn-row">
              <button className="btn" disabled={busy || pin.length !== 6}>{busy ? <span className="spinner" /> : 'Confirm and get OTP'}</button>
              <button type="button" className="btn secondary" onClick={() => { setStage('form'); setError('') }}>Edit</button>
            </div>
          </div>
        </form>
      )}

      {challenge && (
        <OtpDialog
          challenge={challenge}
          title="Confirm transfer"
          description={`${mode} of ${inr(value)} to ${payee?.name}`}
          confirmLabel={`Pay ${inr(value)}`}
          onCancel={() => setChallenge(null)}
          onVerify={async (otp, _t, challengeId) => {
            const p = await api('/payments/transfer/confirm', { method: 'POST', body: { challengeId, otp } })
            setChallenge(null)
            onSent(p)
            setTrackId(p.id)
            setStage('tracking')
          }}
        />
      )}
    </>
  )
}
