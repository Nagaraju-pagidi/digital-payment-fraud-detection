import { useState } from 'react'
import { api, session } from '../api.js'
import ShieldLogo from '../components/Logo.jsx'

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('analyst')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      const res = await api('/fraud/auth/login', { method: 'POST', body: { username, password } })
      session.save(res.token, res.fullName)
      onLogin()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-page">
      <form className="card login-card" onSubmit={submit}>
        <div className="brand" style={{ padding: 0 }}>
          <ShieldLogo size={36} />
          <div><b>FraudShield</b><small>Control Center</small></div>
        </div>
        <h2>Analyst sign in</h2>
        <div className="field">
          <label htmlFor="u">Username</label>
          <input id="u" className="input" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
        </div>
        <div className="field">
          <label htmlFor="p">Password</label>
          <input id="p" className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
        </div>
        {error && <div className="error">{error}</div>}
        <button className="btn primary" disabled={busy} type="submit" style={{ justifyContent: 'center' }}>
          {busy ? <span className="spinner" /> : 'Sign in'}
        </button>
        <div className="hint-box">Demo analyst: <b>analyst</b> / <b>password123</b></div>
      </form>
    </div>
  )
}
