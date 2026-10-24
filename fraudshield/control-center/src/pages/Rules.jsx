import { useEffect, useState } from 'react'
import { api } from '../api.js'

export default function Rules() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { api('/fraud/rules').then(setData).catch((e) => setError(e.message)) }, [])

  return (
    <>
      <div className="grid three">
        <div className="card"><span className="pill good">✓ 0 - {data ? data.holdThreshold - 1 : 39}</span><h3 style={{ marginTop: 10 }}>Approved instantly</h3><div className="sub">Matches the customer's normal behaviour. Money moves immediately and the profile learns from it.</div></div>
        <div className="card"><span className="pill warning">⏳ {data ? data.holdThreshold : 40} - {data ? data.blockThreshold - 1 : 69}</span><h3 style={{ marginTop: 10 }}>On hold</h3><div className="sub">Money is held. An analyst approves (deliver) or rejects (refund). Customer gets an email.</div></div>
        <div className="card"><span className="pill critical">⛔ {data ? data.blockThreshold : 70}+</span><h3 style={{ marginTop: 10 }}>Blocked</h3><div className="sub">Refunded automatically (saga compensation). 120+ or 3 blocks in 24h also freezes the account.</div></div>
      </div>
      <div className="card">
        <h3>Rule book</h3>
        <div className="sub">Every rule compares the payment with THIS customer's own profile - home city, devices, usual amount, usual receivers - not with fixed limits.</div>
        {error && <div className="error">{error}</div>}
        {data && (
          <table className="table">
            <thead><tr><th>ID</th><th>Rule</th><th>What it checks</th><th style={{ textAlign: 'right' }}>Points</th></tr></thead>
            <tbody>
              {data.rules.map((r) => (
                <tr key={r.id}>
                  <td className="muted num">{r.id}</td>
                  <td><b>{r.name}</b></td>
                  <td className="ink2">{r.description}</td>
                  <td className="num" style={{ textAlign: 'right', fontWeight: 700 }}>+{r.maxPoints}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
