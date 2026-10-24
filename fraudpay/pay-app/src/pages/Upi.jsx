import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { deviceId, deviceLabel, locate } from '../device.js'
import { inr } from '../format.js'
import Tracker from '../components/Tracker.jsx'
import { ErrorBox, Field, PinInput } from '../components/Fields.jsx'

/** UPI payment: UPI ID (name is verified) + amount + transaction PIN. */
export default function Upi({ user, payments, onLocated, onSent, onDone }) {
  const [upi, setUpi] = useState('')
  const [receiver, setReceiver] = useState(null)
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [pin, setPin] = useState('')
  const [error, setError] = useState('')
  const [lookupError, setLookupError] = useState('')
  const [busy, setBusy] = useState(false)
  const [trackId, setTrackId] = useState(null)

  useEffect(() => {
    const id = upi.trim().toLowerCase()
    setReceiver(null); setLookupError('')
    if (!/^[a-z0-9._-]+@[a-z]+$/.test(id)) return
    const t = setTimeout(() => {
      api(`/users/upi/lookup?upiId=${encodeURIComponent(id)}`).then(setReceiver).catch((e) => setLookupError(e.message))
    }, 400)
    return () => clearTimeout(t)
  }, [upi])

  const value = Number(amount)
  const frozen = user.status === 'FROZEN'

  async function pay(e) {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      const here = await locate()
      onLocated(here)
      const p = await api('/payments/upi', {
        method: 'POST',
        body: {
          upiId: receiver.upiId, amount: value, note: note || null, pin, deviceId: deviceId(), deviceLabel: deviceLabel(),
          latitude: here.error ? null : here.lat, longitude: here.error ? null : here.lon,
        },
      })
      onSent(p)
      setTrackId(p.id)
    } catch (err) {
      setError(err.message)
    } finally {
      setPin('')
      setBusy(false)
    }
  }

  if (trackId != null) {
    const p = payments.find((x) => x.id === trackId && x.direction === 'SENT')
    return (
      <>
        <div className="page-head"><div className="grow"><h1>UPI payment status</h1></div></div>
        <Tracker payment={p} onDone={onDone} onAnother={() => { setTrackId(null); setAmount(''); setNote(''); setUpi('') }} />
      </>
    )
  }

  return (
    <>
      <div className="page-head">
        <div className="grow">
          <h1>Pay by UPI</h1>
          <p>Send up to ₹1,00,000 a day to any NexaPay UPI ID. Your UPI ID is {user.upiId}.</p>
        </div>
      </div>
      {frozen && <div className="alert error" style={{ marginBottom: 18 }}>Your account is frozen. Payments are not allowed until the bank verifies you.</div>}
      <form className="grid-2" onSubmit={pay}>
        <div className="panel form">
          <Field label="UPI ID" hint="For example priya.sharma@fraudpay" error={lookupError}>
            <input className="input" value={upi} onChange={(e) => setUpi(e.target.value)} autoCapitalize="none" autoComplete="off" />
          </Field>
          {receiver && <div className="alert success"><span aria-hidden="true">✓</span><span>Paying <b>{receiver.fullName}</b></span></div>}
          <Field label="Amount (₹)" hint={`Available balance ${inr(user.balance)}`}>
            <input className="input big" inputMode="decimal" value={amount} placeholder="0.00"
              onChange={(e) => setAmount(e.target.value.replace(/[^0-9.]/g, ''))} />
          </Field>
          <Field label="Note (optional)">
            <input className="input" maxLength={140} value={note} onChange={(e) => setNote(e.target.value)} />
          </Field>
        </div>
        <div className="panel form">
          <h3>Authorise</h3>
          <div className="review">
            <div className="row"><span>To</span><span>{receiver ? receiver.fullName : '-'}</span></div>
            <div className="row total"><span>Amount</span><span className="num">{value >= 1 ? inr(value) : '-'}</span></div>
          </div>
          <Field label="Transaction PIN">
            <PinInput value={pin} onChange={setPin} />
          </Field>
          <ErrorBox message={error} />
          <button className="btn" disabled={frozen || busy || !receiver || !(value >= 1) || pin.length !== 6}>
            {busy ? <span className="spinner" /> : value >= 1 ? `Pay ${inr(value)}` : 'Pay'}
          </button>
        </div>
      </form>
    </>
  )
}
