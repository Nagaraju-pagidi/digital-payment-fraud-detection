import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { dateOnly, dateTime, label, passwordChecks } from '../format.js'
import { ErrorBox, Field, PasswordInput, PinInput } from '../components/Fields.jsx'

export default function Security({ user, onToast }) {
  const [profile, setProfile] = useState(null)
  const [devices, setDevices] = useState([])
  const [logins, setLogins] = useState([])
  const [tab, setTab] = useState('profile')
  const [error, setError] = useState('')

  const load = () => {
    api('/users/me/profile').then(setProfile).catch((e) => setError(e.message))
    api('/users/me/devices').then(setDevices).catch(() => {})
    api('/users/me/logins').then(setLogins).catch(() => {})
  }
  useEffect(() => { load() }, [])

  async function removeDevice(d) {
    if (!window.confirm(`Remove ${d.deviceLabel}? It will need an OTP to log in again.`)) return
    try {
      const r = await api(`/users/me/devices/${d.id}`, { method: 'DELETE' })
      onToast(r.message)
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <>
      <div className="page-head">
        <div className="grow"><h1>Profile & security</h1><p>Your KYC details, password, transaction PIN and devices.</p></div>
      </div>
      <div className="tabs" role="tablist">
        {[['profile', 'Profile'], ['password', 'Change password'], ['pin', 'Change transaction PIN'], ['devices', 'Devices and logins']].map(([k, l]) =>
          <button key={k} role="tab" aria-selected={tab === k} className={tab === k ? 'on' : ''} onClick={() => { setTab(k); setError('') }}>{l}</button>)}
      </div>
      <ErrorBox message={error} />

      {tab === 'profile' && profile && (
        <div className="grid-even">
          <div className="panel">
            <h3 style={{ marginBottom: 12 }}>Personal and KYC</h3>
            <div className="review">
              <div className="row"><span>Name</span><span>{profile.fullName}</span></div>
              <div className="row"><span>Date of birth</span><span>{dateOnly(profile.dateOfBirth)}</span></div>
              <div className="row"><span>Gender</span><span>{label(profile.gender)}</span></div>
              <div className="row"><span>Mobile</span><span className="num">{profile.phone}</span></div>
              <div className="row"><span>Email</span><span>{profile.email}</span></div>
              <div className="row"><span>PAN</span><span className="num">{profile.panMasked}</span></div>
              <div className="row"><span>Aadhaar</span><span className="num">{profile.aadhaarMasked}</span></div>
              <div className="row"><span>Address</span><span>{profile.addressLine}, {profile.city}, {profile.state} {profile.pincode}</span></div>
              <div className="row"><span>Occupation</span><span>{label(profile.occupation)}</span></div>
              <div className="row"><span>Nominee</span><span>{profile.nomineeName ? `${profile.nomineeName} (${label(profile.nomineeRelation)})` : 'Not registered'}</span></div>
            </div>
          </div>
          <div className="panel">
            <h3 style={{ marginBottom: 12 }}>Account</h3>
            <div className="review">
              <div className="row"><span>Customer ID</span><span className="num">{profile.customerId}</span></div>
              <div className="row"><span>User ID</span><span>{profile.username}</span></div>
              <div className="row"><span>Account type</span><span>{label(profile.accountType)}</span></div>
              <div className="row"><span>Account number</span><span className="num">{profile.accountNumber}</span></div>
              <div className="row"><span>IFSC</span><span className="num">{profile.ifsc}</span></div>
              <div className="row"><span>Branch</span><span>{profile.branch}</span></div>
              <div className="row"><span>UPI ID</span><span>{profile.upiId}</span></div>
              <div className="row"><span>Registered home location</span><span>{profile.homeCity || 'Set at first login'}</span></div>
              <div className="row"><span>Customer since</span><span>{dateOnly(profile.createdAt)}</span></div>
            </div>
            <div className="small muted" style={{ marginTop: 10 }}>To change KYC details, visit your home branch with original documents.</div>
          </div>
        </div>
      )}

      {tab === 'password' && <ChangePassword username={user.username} onToast={onToast} />}
      {tab === 'pin' && <ChangePin onToast={onToast} />}

      {tab === 'devices' && (
        <div className="stack">
          <div className="panel">
            <h3 style={{ marginBottom: 12 }}>Trusted devices</h3>
            <table className="table">
              <thead><tr><th>Device</th><th>Added</th><th>Last used</th><th /></tr></thead>
              <tbody>
                {devices.map((d) => (
                  <tr key={d.id}>
                    <td><b>{d.deviceLabel}</b> {d.current && <span className="badge good">This device</span>} {d.registration && <span className="badge neutral">Registered</span>}</td>
                    <td className="text-2">{dateTime(d.addedAt)}</td>
                    <td className="text-2">{dateTime(d.lastUsedAt)}</td>
                    <td className="r">{!d.current && <button className="btn danger small" onClick={() => removeDevice(d)}>Remove</button>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="panel">
            <h3 style={{ marginBottom: 12 }}>Recent login activity</h3>
            <table className="table">
              <thead><tr><th>Date</th><th>Result</th><th>Device</th><th>Location</th><th>IP address</th></tr></thead>
              <tbody>
                {logins.map((l, i) => (
                  <tr key={i}>
                    <td className="text-2 num">{dateTime(l.at)}</td>
                    <td>{l.success ? <span className="badge good">✓ {l.detail}</span> : <span className="badge warning">! {l.detail}</span>}</td>
                    <td className="text-2">{l.deviceLabel}</td>
                    <td className="text-2">{l.location}</td>
                    <td className="text-2 num">{l.ipAddress}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  )
}

function ChangePassword({ username, onToast }) {
  const [f, setF] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (v) => setF({ ...f, [k]: v })
  const checks = passwordChecks(f.newPassword, username)
  async function submit(e) {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      const r = await api('/users/me/password', { method: 'POST', body: f })
      onToast(r.message)
      setF({ currentPassword: '', newPassword: '', confirmPassword: '' })
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <form className="panel form" style={{ maxWidth: 560 }} onSubmit={submit}>
      <Field label="Current password"><PasswordInput value={f.currentPassword} onChange={set('currentPassword')} autoComplete="current-password" /></Field>
      <Field label="New password"><PasswordInput value={f.newPassword} onChange={set('newPassword')} autoComplete="new-password" /></Field>
      <ul className="checklist">{checks.map((c) => <li key={c.text} className={c.ok ? 'met' : ''}>{c.ok ? '✓' : '○'} {c.text}</li>)}</ul>
      <Field label="Confirm new password"><PasswordInput value={f.confirmPassword} onChange={set('confirmPassword')} autoComplete="new-password" /></Field>
      <ErrorBox message={error} />
      <div><button className="btn" disabled={busy || checks.some((c) => !c.ok) || f.newPassword !== f.confirmPassword}>Change password</button></div>
    </form>
  )
}

function ChangePin({ onToast }) {
  const [f, setF] = useState({ currentPin: '', newPin: '', confirmPin: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (v) => setF({ ...f, [k]: v })
  async function submit(e) {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      const r = await api('/users/me/pin', { method: 'POST', body: f })
      onToast(r.message)
      setF({ currentPin: '', newPin: '', confirmPin: '' })
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <form className="panel form" style={{ maxWidth: 560 }} onSubmit={submit}>
      <Field label="Current transaction PIN"><PinInput value={f.currentPin} onChange={set('currentPin')} /></Field>
      <Field label="New transaction PIN" hint="6 digits. Not repeated or sequential digits, not your birth date or mobile number."><PinInput value={f.newPin} onChange={set('newPin')} /></Field>
      <Field label="Confirm new PIN"><PinInput value={f.confirmPin} onChange={set('confirmPin')} /></Field>
      <ErrorBox message={error} />
      <div><button className="btn" disabled={busy || f.newPin.length !== 6 || f.newPin !== f.confirmPin || f.currentPin.length !== 6}>Change PIN</button></div>
    </form>
  )
}
