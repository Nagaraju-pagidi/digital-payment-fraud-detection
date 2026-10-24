import { useCallback, useEffect, useRef, useState } from 'react'
import { api, liveStream, session } from './api.js'
import ShieldLogo from './components/Logo.jsx'
import CaseDrawer from './components/CaseDrawer.jsx'
import Login from './pages/Login.jsx'
import Monitor from './pages/Monitor.jsx'
import Queue from './pages/Queue.jsx'
import Cases from './pages/Cases.jsx'
import Frozen from './pages/Frozen.jsx'
import Rules from './pages/Rules.jsx'
import System from './pages/System.jsx'

const PAGES = [
  { key: 'monitor', label: 'Live monitor', icon: '📡' },
  { key: 'queue', label: 'Review queue', icon: '⏳' },
  { key: 'cases', label: 'All payments', icon: '🧾' },
  { key: 'frozen', label: 'Frozen accounts', icon: '🔒' },
  { key: 'rules', label: 'Rule book', icon: '📘' },
  { key: 'system', label: 'System & tools', icon: '⚙️' },
]

function currentPage() {
  const key = window.location.hash.replace('#/', '')
  return PAGES.some((p) => p.key === key) ? key : 'monitor'
}

export default function App() {
  const [loggedIn, setLoggedIn] = useState(!!session.token)
  useEffect(() => { session.onLogout(() => setLoggedIn(false)) }, [])
  if (!loggedIn) return <Login onLogin={() => setLoggedIn(true)} />
  return <Dashboard onLogout={() => { session.clear(); setLoggedIn(false) }} />
}

function Dashboard({ onLogout }) {
  const [page, setPage] = useState(currentPage())
  const [stats, setStats] = useState(null)
  const [feed, setFeed] = useState([])
  const [queue, setQueue] = useState([])
  const [freshIds, setFreshIds] = useState(new Set())
  const [openId, setOpenId] = useState(null)
  const [lastCase, setLastCase] = useState(null)
  const [version, setVersion] = useState(0)
  const [live, setLive] = useState(false)
  const [sim, setSim] = useState(null)
  const [toast, setToast] = useState(null)
  const statsTimer = useRef(null)
  const toastTimer = useRef(null)

  useEffect(() => {
    const onHash = () => setPage(currentPage())
    window.addEventListener('hashchange', onHash)
    return () => window.removeEventListener('hashchange', onHash)
  }, [])

  const showToast = useCallback((text) => {
    setToast(text)
    clearTimeout(toastTimer.current)
    toastTimer.current = setTimeout(() => setToast(null), 4500)
  }, [])

  const loadStats = useCallback(() => {
    api('/fraud/stats').then(setStats).catch(() => {})
  }, [])

  // refresh the numbers at most every 1.5s while events stream in
  const scheduleStats = useCallback(() => {
    if (statsTimer.current) return
    statsTimer.current = setTimeout(() => { statsTimer.current = null; loadStats() }, 1500)
  }, [loadStats])

  useEffect(() => {
    loadStats()
    api('/fraud/cases?limit=40').then((d) => setFeed(d || [])).catch(() => {})
    api('/fraud/cases?status=HOLD&limit=100').then((d) => setQueue(d || [])).catch(() => {})
    api('/simulator/status').then(setSim).catch(() => setSim(null))
    const t = setInterval(loadStats, 30000) // keeps the per-minute chart moving
    return () => clearInterval(t)
  }, [loadStats])

  // live updates from fraud-service
  useEffect(() => {
    const close = liveStream('/fraud/stream', {
      case: (c) => {
        setLastCase(c)
        setFeed((prev) => [c, ...prev.filter((x) => x.transactionId !== c.transactionId)].slice(0, 40))
        setQueue((prev) => {
          const rest = prev.filter((x) => x.transactionId !== c.transactionId)
          return c.status === 'HOLD' ? [c, ...rest] : rest
        })
        setFreshIds((prev) => new Set(prev).add(c.transactionId))
        setTimeout(() => setFreshIds((prev) => { const n = new Set(prev); n.delete(c.transactionId); return n }), 2300)
        setVersion((v) => v + 1)
        scheduleStats()
      },
      account: (a) => {
        setVersion((v) => v + 1)
        scheduleStats()
        showToast(a.frozen ? `🔒 Account frozen: ${a.userName} - ${a.reason}` : `🔓 ${a.userName}'s account is active again`)
      },
    }, setLive)
    return close
  }, [scheduleStats, showToast])

  async function toggleSim() {
    try {
      const next = await api(sim && sim.running ? '/simulator/stop' : '/simulator/start', { method: 'POST' })
      setSim(next)
      showToast(next.running ? 'Demo traffic started - simulator bots are paying each other' : 'Demo traffic stopped')
    } catch (e) {
      showToast(e.message)
    }
  }

  const go = (key) => { window.location.hash = '#/' + key }
  const title = PAGES.find((p) => p.key === page)?.label

  return (
    <div className="shell">
      <nav className="sidebar" aria-label="Main">
        <div className="brand">
          <ShieldLogo />
          <div><b>FraudShield</b><small>Control Center</small></div>
        </div>
        {PAGES.map((p) => (
          <button key={p.key} className={'nav-item' + (page === p.key ? ' active' : '')} onClick={() => go(p.key)}>
            <span aria-hidden="true">{p.icon}</span>{p.label}
            {p.key === 'queue' && queue.length > 0 && <span className="count">{queue.length}</span>}
          </button>
        ))}
        <div className="spacer" />
        <div className="who">
          Signed in as<br /><b style={{ color: 'var(--ink)' }}>{session.name}</b>
          <div><button className="btn small" style={{ marginTop: 8 }} onClick={onLogout}>Sign out</button></div>
        </div>
      </nav>

      <main className="main">
        <header className="topbar">
          <h1>{title}</h1>
          <span className={'live' + (live ? ' on' : '')}><i />{live ? 'LIVE' : 'connecting…'}</span>
          <div className="grow" />
          {sim && (
            <button className={'btn ' + (sim.running ? '' : 'primary')} onClick={toggleSim}>
              {sim.running ? '■ Stop demo traffic' : '▶ Start demo traffic'}
            </button>
          )}
        </header>
        <div className="content">
          {page === 'monitor' && <Monitor stats={stats} feed={feed} freshIds={freshIds} onOpen={setOpenId} />}
          {page === 'queue' && <Queue cases={queue} onOpen={setOpenId} />}
          {page === 'cases' && <Cases onOpen={setOpenId} version={version} />}
          {page === 'frozen' && <Frozen version={version} onToast={showToast} />}
          {page === 'rules' && <Rules />}
          {page === 'system' && <System />}
        </div>
      </main>

      {openId != null && (
        <CaseDrawer caseId={openId} liveCase={lastCase} onClose={() => setOpenId(null)} onToast={showToast} />
      )}
      {toast && <div className="toast" role="status">{toast}</div>}
    </div>
  )
}
