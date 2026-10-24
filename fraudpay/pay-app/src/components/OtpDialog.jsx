import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { ErrorBox } from './Fields.jsx'

/**
 * Email OTP step used everywhere (account opening, new-device login, add payee, transfer).
 * onVerify(otp, trustDevice) must return a promise; errors are shown inside the dialog.
 */
export default function OtpDialog({ challenge, title, description, confirmLabel = 'Verify', showTrust, onVerify, onCancel }) {
  const [current, setCurrent] = useState(challenge)
  const [otp, setOtp] = useState('')
  const [trust, setTrust] = useState(true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [left, setLeft] = useState(challenge.expiresInSeconds)
  const [info, setInfo] = useState('')

  useEffect(() => {
    setLeft(current.expiresInSeconds)
    const t = setInterval(() => setLeft((s) => Math.max(0, s - 1)), 1000)
    return () => clearInterval(t)
  }, [current])

  async function submit(e) {
    e.preventDefault()
    setBusy(true); setError(''); setInfo('')
    try {
      await onVerify(otp, trust, current.challengeId)
    } catch (err) {
      setError(err.message)
      setOtp('')
    } finally {
      setBusy(false)
    }
  }

  async function resend() {
    setError(''); setInfo('')
    try {
      const next = await api('/auth/otp/resend', { method: 'POST', body: { challengeId: current.challengeId } })
      setCurrent(next)
      setInfo('A new OTP has been sent.')
    } catch (err) {
      setError(err.message)
    }
  }

  const mm = String(Math.floor(left / 60)).padStart(2, '0')
  const ss = String(left % 60).padStart(2, '0')
  return (
    <div className="overlay" role="dialog" aria-modal="true" aria-label={title}>
      <form className="modal form" onSubmit={submit}>
        <div>
          <h3>{title}</h3>
          <div className="text-2 small">{description}</div>
        </div>
        <div className="alert info">
          <span aria-hidden="true">✉</span>
          <span>We have sent a 6-digit OTP to <b>{current.maskedEmail}</b>. It expires in <b className="num">{mm}:{ss}</b>.</span>
        </div>
        <input className="input otp-input" inputMode="numeric" autoComplete="one-time-code" autoFocus maxLength={6}
          placeholder="000000" value={otp} aria-label="One time password"
          onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))} />
        {showTrust && (
          <label className="check"><input type="checkbox" checked={trust} onChange={(e) => setTrust(e.target.checked)} />
            <span>Remember this device. Next time you will not need an OTP to log in from here.</span></label>
        )}
        <ErrorBox message={error} />
        {info && <div className="alert success">{info}</div>}
        <div className="btn-row">
          <button className="btn" disabled={busy || otp.length !== 6 || left === 0}>{busy ? <span className="spinner" /> : confirmLabel}</button>
          <button type="button" className="btn secondary" onClick={onCancel} disabled={busy}>Cancel</button>
          <span className="grow" style={{ flex: 1 }} />
          <button type="button" className="btn link" onClick={resend} disabled={busy}>Resend OTP</button>
        </div>
      </form>
    </div>
  )
}
