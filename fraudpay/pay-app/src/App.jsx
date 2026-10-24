import { useCallback, useEffect, useRef, useState } from 'react'
import { api, liveStream, session } from './api.js'
import { locate } from './device.js'
import { dateTime, inr } from './format.js'
import Logo from './components/Logo.jsx'
import Login from './pages/Login.jsx'
import OpenAccount from './pages/OpenAccount.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Transfer from './pages/Transfer.jsx'
import Upi from './pages/Upi.jsx'
import Payees from './pages/Payees.jsx'
import Transactions, { PaymentDrawer } from './pages/Transactions.jsx'
import Statement from './pages/Statement.jsx'
import Security from './pages/Security.jsx'

const NAV = [
  { key: 'dashboard', label: 'Dashboard', icon: '⌂' },
  { group: 'Payments' },
  { key: 'transfer', label: 'Transfer money', icon: '⇄' },
  { key: 'upi', label: 'Pay by UPI', icon: '₹' },
  { key: 'payees', label: 'Payees', icon: '👤' },
  { group: 'Accounts' },
  { key: 'transactions', label: 'Transactions', icon: '≡' },
  { key: 'statement', label: 'Statement', icon: '🗎' },
  { key: 'security', label: 'Profile & security', icon: '🔒' },
]
const PRIVATE = NAV.filter((n) => n.key).map((n) => n.key)
const IDLE_LIMIT = 5 * 60 // seconds of inactivity before automatic logout
const WARN_AT = 60

const route = () => window.location.hash.replace(/^#\//, '').split('?')[0] || ''

function readLastLogin() {
  try { return JSON.parse(sessionStorage.getItem('fp_last_login') || 'null') } catch { return null }
}

export default function App() {
  const [page, setPage] = useState(route())
  const [loggedIn, setLoggedIn] = useState(!!session.token)
  const [notice, setNotice] = useState('')

  useEffect(() => {
    const onHash = () => setPage(route())
    window.addEventListener('hashchange', onHash)
    return () => window.removeEventListener('hashchange', onHash)
  }, [])

  const logout = useCallback((message) => {
    session.clear()
    try { sessionStorage.removeItem('fp_last_login') } catch { /* ignore */ }
    setLoggedIn(false)
    setNotice(typeof message === 'string' ? message : 'You have logged out successfully.')
    window.location.hash = '#/login'
  }, [])

  useEffect(() => { session.onLogout(logout) }, [logout])

  if (!loggedIn) {
    if (page === 'open-account') return <OpenAccount />
    return (
      <Login notice={notice} onLoggedIn={(res) => {
        session.save(res.token, res.user.fullName)
        try { sessionStorage.setItem('fp_last_login', JSON.stringify(res.lastLogin)) } catch { /* ignore */ }
        setNotice('')
        setLoggedIn(true)
        window.location.hash = '#/dashboard'
      }} />
    )
  }
  return <Bank page={PRIVATE.includes(page) ? page : 'dashboard'} onLogout={logout} />
}

function Bank({ page, onLogout }) {
  const [user, setUser] = useState(null)
  const [payments, setPayments] = useState([])
  const [loc, setLoc] = useState({ loading: true })
  const [detailId, setDetailId] = useState(null)
  const [toast, setToast] = useState(null)
  const [idle, setIdle] = useState(0)
  const [version, setVersion] = useState(0)
  const [error, setError] = useState('')
  const toastTimer = useRef(null)
  const lastLogin = readLastLogin()

  const showToast = useCallback((t) => {
    setToast(t)
    clearTimeout(toastTimer.current)
    toastTimer.current = setTimeout(() => setToast(null), 4500)
  }, [])

  const refreshLocation = useCallback(() => {
    setLoc({ loading: true })
    locate().then(setLoc)
  }, [])

  const upsert = useCallback((p) => {
    setPayments((prev) => [p, ...prev.filter((x) => !(x.id === p.id && x.direction === p.direction))]
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)))
  }, [])

  const load = useCallback(() => {
    api('/users/me').then((u) => { setUser(u); setError('') }).catch((e) => setError(e.message))
    api('/payments').then((d) => setPayments(d || [])).catch(() => {})
  }, [])

  useEffect(() => { load(); refreshLocation() }, [load, refreshLocation])

  // live updates: payment status + balance
  useEffect(() => liveStream('/payments/stream', {
    payment: (p) => {
      upsert(p)
      setVersion((v) => v + 1)
      if (p.direction === 'RECEIVED' && p.status === 'SUCCESS') showToast(`${inr(p.amount)} received from ${p.counterpartyName}`)
    },
    account: (u) => setUser(u),
  }), [upsert, showToast])

  // automatic logout after 5 minutes without activity
  useEffect(() => {
    const reset = () => setIdle(0)
    const events = ['mousemove', 'keydown', 'mousedown', 'scroll', 'touchstart']
    events.forEach((e) => window.addEventListener(e, reset, { passive: true }))
    const t = setInterval(() => setIdle((s) => s + 1), 1000)
    return () => { events.forEach((e) => window.removeEventListener(e, reset)); clearInterval(t) }
  }, [])
  useEffect(() => {
    if (idle >= IDLE_LIMIT) onLogout('You were logged out after 5 minutes of inactivity.')
  }, [idle, onLogout])

  if (!user) {
    return (
      <div className="public-main" style={{ minHeight: '100vh' }}>
        {error ? <div className="form" style={{ width: 380 }}><div className="alert error">{error}</div><button className="btn" onClick={load}>Try again</button><button className="btn secondary" onClick={() => onLogout()}>Log out</button></div>
          : <div className="muted"><span className="spinner" /> Loading your accounts</div>}
      </div>
    )
  }

  const go = (key) => { window.location.hash = '#/' + key }
  const remaining = IDLE_LIMIT - idle
  const common = { user, payments, onLocated: setLoc, onSent: (p) => { upsert(p); setUser((u) => ({ ...u, balance: Number(u.balance) - Number(p.amount) })) }, onDone: () => { load(); go('dashboard') } }

  return (
    <>
      <header className="topbar">
        <a className="brand" href="#/dashboard"><Logo /><b>NexaPay</b><span>Net banking</span></a>
        <div className="grow" />
        <span className="session-pill" title="For your security you are logged out after 5 minutes without activity">
          {remaining <= WARN_AT ? `Logging out in ${remaining}s` : 'Secure session'}
        </span>
        <div className="who">
          <b>{user.fullName}</b>
          <small>{lastLogin ? `Last login ${dateTime(lastLogin.at)}` : 'First login'}</small>
        </div>
        <button className="btn-logout" onClick={() => onLogout()}>Log out</button>
      </header>
      <div className="shell">
        <nav className="sidenav" aria-label="Main">
          {NAV.map((n, i) => n.group
            ? <div key={i} className="group">{n.group}</div>
            : <a key={n.key} href={'#/' + n.key} className={page === n.key ? 'active' : ''}><span className="ic" aria-hidden="true">{n.icon}</span>{n.label}</a>)}
          <div className="help">Never share your password, PIN or OTP. Bank staff will never ask for them.</div>
        </nav>
        <main className="content">
          {page === 'dashboard' && <Dashboard user={user} payments={payments} loc={loc} lastLogin={lastLogin} onRefreshLoc={refreshLocation} onOpenPayment={setDetailId} />}
          {page === 'transfer' && <Transfer {...common} />}
          {page === 'upi' && <Upi {...common} />}
          {page === 'payees' && <Payees onToast={showToast} />}
          {page === 'transactions' && <Transactions payments={payments} onOpenPayment={setDetailId} />}
          {page === 'statement' && <Statement user={user} version={version} />}
          {page === 'security' && <Security user={user} onToast={showToast} />}
          <div className="footer">NexaPay (demo). Every payment is checked in real time by FraudShield.</div>
        </main>
      </div>

      {remaining <= WARN_AT && (
        <div className="overlay" role="alertdialog" aria-label="Session about to expire">
          <div className="modal form">
            <h3>Are you still there?</h3>
            <div className="text-2">For your security you will be logged out in <b className="num">{remaining}</b> seconds.</div>
            <div className="btn-row">
              <button className="btn" onClick={() => setIdle(0)}>Stay logged in</button>
              <button className="btn secondary" onClick={() => onLogout()}>Log out now</button>
            </div>
          </div>
        </div>
      )}
      {detailId != null && <PaymentDrawer p={payments.find((p) => p.id === detailId)} onClose={() => setDetailId(null)} />}
      {toast && <div className="toast" role="status">{toast}</div>}
    </>
  )
}
