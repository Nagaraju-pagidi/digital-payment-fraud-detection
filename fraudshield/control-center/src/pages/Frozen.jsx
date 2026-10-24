import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { dateTime } from '../format.js'

export default function Frozen({ version, onToast }) {
  const [rows, setRows] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(null)

  const load = () => api('/fraud/accounts/frozen').then((d) => { setRows(d); setError('') }).catch((e) => setError(e.message))
  useEffect(() => { load() }, [version])

  async function unfreeze(userId, name) {
    setBusy(userId)
    try {
      await api(`/fraud/accounts/${userId}/unfreeze`, { method: 'POST' })
      onToast(`${name}'s account is active again`)
      load()
    } catch (e) {
      onToast(e.message)
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className="card">
      <h3>Frozen accounts</h3>
      <div className="sub">Frozen automatically when a payment scores 120+ or 3 payments are blocked in 24 hours. Frozen customers cannot send money.</div>
      {error && <div className="error">{error}</div>}
      {rows && rows.length === 0 && <div className="empty">No frozen accounts 👍</div>}
      {rows && rows.length > 0 && (
        <table className="table">
          <thead><tr><th>Customer</th><th>UPI ID</th><th>Reason</th><th>Frozen at</th><th /></tr></thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.userId}>
                <td><b>{r.userName}</b></td>
                <td className="ink2">{r.userUpi}</td>
                <td className="ink2">{r.reason}</td>
                <td className="ink2 num">{dateTime(r.frozenAt)}</td>
                <td style={{ textAlign: 'right' }}>
                  <button className="btn small" disabled={busy === r.userId} onClick={() => unfreeze(r.userId, r.userName)}>🔓 Verify & unfreeze</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
