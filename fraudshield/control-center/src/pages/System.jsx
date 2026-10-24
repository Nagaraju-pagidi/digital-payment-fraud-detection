import { useEffect, useState } from 'react'
import { api } from '../api.js'

const host = typeof window !== 'undefined' ? window.location.hostname : 'localhost'
const TOOLS = [
  { name: 'Eureka service registry', url: `http://${host}:8761`, what: 'Registered application services' },
  { name: 'NexaPay customer app', url: `http://${host}:5173`, what: 'Customer payment application' },
  { name: 'Mailpit test inbox', url: `http://${host}:8025`, what: 'OTP emails for local demo accounts' },
]
export default function System() {
  const [rules, setRules] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { api('/fraud/rules').then(setRules).catch(e => setError(e.message)) }, [])
  return <>
    <div className="grid three">
      <div className="card"><h3>ML fraud scoring</h3><div className="sub">Random Forest model served by the local ML scoring API.</div><span className="pill info">{rules ? 'Fraud engine connected' : 'Checking connection'}</span><p className="muted" style={{fontSize:12}}>Model training data is synthetic and intended for demonstration. Validate against a labelled dataset before real-world use.</p>{error && <div className="error">{error}</div>}</div>
      <div className="card"><h3>Behavioral rules</h3><div className="sub">Rules remain as security signals and a comparison baseline.</div>{rules && <p className="ink2">Configured rules: <b>{rules.rules?.length ?? '—'}</b><br/>Hold threshold: <b>{rules.holdThreshold}</b><br/>Block threshold: <b>{rules.blockThreshold}</b></p>}</div>
      <div className="card"><h3>Runtime</h3><div className="sub">Kafka and Ollama are not required in the student demo profile.</div><span className="pill good">Lightweight Compose setup</span></div>
    </div>
    <div className="card"><h3>Demo tools</h3><div className="sub">Open these while testing the application.</div>{TOOLS.map(t => <div key={t.name} style={{marginBottom:10}}><a href={t.url} target="_blank" rel="noreferrer">{t.name} ↗</a><div className="muted" style={{fontSize:12}}>{t.what}</div></div>)}</div>
  </>
}
