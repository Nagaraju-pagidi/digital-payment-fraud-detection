import { useState } from 'react'
import { api } from '../api.js'
import { deviceId, deviceLabel, currentPosition } from '../device.js'
import Logo from '../components/Logo.jsx'
import OtpDialog from '../components/OtpDialog.jsx'
import { ErrorBox, Field, PasswordInput } from '../components/Fields.jsx'

export function PublicSide() {
  return (
    <aside className="public-side">
      <div className="brand" style={{ color: '#fff' }}><Logo size={36} /><b>NexaPay</b></div>
      <div>
        <h1>Net banking that checks every payment before the money moves.</h1>
        <p style={{ marginTop: 12 }}>Each transfer is screened in real time by FraudShield, our fraud detection system.</p>
      </div>
      <div className="tips">
        <div className="tip"><i aria-hidden="true">◆</i><span>We will never ask for your password, transaction PIN or OTP by phone, SMS or email.</span></div>
        <div className="tip"><i aria-hidden="true">◆</i><span>Logging in from a new device needs a one-time password sent to your registered email.</span></div>
        <div className="tip"><i aria-hidden="true">◆</i><span>Five wrong passwords lock your login for 15 minutes.</span></div>
        <div className="tip"><i aria-hidden="true">◆</i><span>We use your device and location only to protect your payments.</span></div>
      </div>
      <div className="foot">NexaPay is a demonstration bank built for an academic project. No real money is moved.</div>
    </aside>
  )
}

export default function Login({ onLoggedIn, notice }) {
  const [userId, setUserId] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [challenge, setChallenge] = useState(null)

  async function submit(e) {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      const pos = await currentPosition()
      const res = await api('/auth/login', {
        method: 'POST',
        body: {
          userId, password, deviceId: deviceId(), deviceLabel: deviceLabel(),
          latitude: pos.error ? null : pos.lat, longitude: pos.error ? null : pos.lon,
        },
      })
      setPassword('')
      if (res.otpRequired) setChallenge(res.challenge)
      else onLoggedIn(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="public">
      <PublicSide />
      <main className="public-main">
        <form className="login-card form" onSubmit={submit} autoComplete="off">
          <div>
            <h2>Log in to net banking</h2>
            <div className="text-2">Use the User ID you chose when you opened your account, or your Customer ID.</div>
          </div>
          {notice && <div className="alert info">{notice}</div>}
          <Field label="User ID or Customer ID">
            <input className="input" value={userId} onChange={(e) => setUserId(e.target.value)} autoComplete="username" autoFocus />
          </Field>
          <Field label="Password">
            <PasswordInput value={password} onChange={setPassword} autoComplete="current-password" />
          </Field>
          <ErrorBox message={error} />
          <button className="btn" disabled={busy || !userId || !password}>{busy ? <span className="spinner" /> : 'Log in'}</button>
          <div className="small text-2">
            New to NexaPay? <a href="#/open-account">Open a savings or current account</a>
          </div>
          <div className="small muted">
            Your browser may ask to share your location. Allowing it helps us recognise you and stop fraudsters.
          </div>
        </form>
      </main>
      {challenge && (
        <OtpDialog
          challenge={challenge}
          title="Verify it's you"
          description="You are logging in from a device we have not seen before."
          confirmLabel="Verify and log in"
          showTrust
          onCancel={() => setChallenge(null)}
          onVerify={async (otp, trust, challengeId) => {
            const res = await api('/auth/login/verify', { method: 'POST', body: { challengeId, otp, trustDevice: trust } })
            setChallenge(null)
            onLoggedIn(res)
          }}
        />
      )}
    </div>
  )
}
