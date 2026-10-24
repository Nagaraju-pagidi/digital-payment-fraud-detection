import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { dateTime, groupAcc, inr, label } from '../format.js'
import { ErrorBox } from '../components/Fields.jsx'

const iso = (d) => d.toISOString().slice(0, 10)
const daysAgo = (n) => { const d = new Date(); d.setDate(d.getDate() - n); return iso(d) }

export default function Statement({ user, version }) {
  const [from, setFrom] = useState(daysAgo(30))
  const [to, setTo] = useState(iso(new Date()))
  const [rows, setRows] = useState(null)
  const [error, setError] = useState('')

  const load = (f = from, t = to) => {
    setError('')
    api(`/users/me/statement?from=${f}&to=${t}`).then(setRows).catch((e) => setError(e.message))
  }
  useEffect(() => { load() }, [version]) // eslint-disable-line react-hooks/exhaustive-deps

  function preset(n) {
    const f = daysAgo(n), t = iso(new Date())
    setFrom(f); setTo(t); load(f, t)
  }

  function download() {
    const head = ['Date', 'Description', 'Reference', 'Debit', 'Credit', 'Balance']
    const lines = (rows || []).slice().reverse().map((r) => [new Date(r.date).toLocaleString('en-IN'), r.description, r.referenceNo || '',
      r.debit ?? '', r.credit ?? '', r.balance].map((v) => `"${String(v).replaceAll('"', '""')}"`).join(','))
    const csv = [`"NexaPay statement","A/c ${user.accountNumber}","${from} to ${to}"`, head.join(','), ...lines].join('\n')
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
    const a = document.createElement('a')
    a.href = url; a.download = `statement-${user.accountNumber}-${from}-to-${to}.csv`; a.click()
    URL.revokeObjectURL(url)
  }

  const ordered = rows || []
  const closing = ordered.length ? ordered[0].balance : null
  const totalDebit = ordered.reduce((s, r) => s + Number(r.debit || 0), 0)
  const totalCredit = ordered.reduce((s, r) => s + Number(r.credit || 0), 0)

  return (
    <>
      <div className="page-head">
        <div className="grow">
          <h1>Account statement</h1>
          <p>{label(user.accountType)} account {groupAcc(user.accountNumber)}, {user.branch}</p>
        </div>
        <button className="btn secondary" onClick={download} disabled={!rows || rows.length === 0}>Download CSV</button>
        <button className="btn secondary" onClick={() => window.print()}>Print</button>
      </div>

      <div className="panel" style={{ marginBottom: 18 }}>
        <form className="btn-row" onSubmit={(e) => { e.preventDefault(); load() }}>
          <label className="field" style={{ width: 180 }}><span className="lbl">From</span><input className="input" type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} /></label>
          <label className="field" style={{ width: 180 }}><span className="lbl">To</span><input className="input" type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} /></label>
          <button className="btn" style={{ alignSelf: 'flex-end' }}>Show statement</button>
          <span style={{ flex: 1 }} />
          <span className="small muted" style={{ alignSelf: 'flex-end' }}>Quick:</span>
          {[7, 30, 90].map((n) => <button type="button" key={n} className="btn secondary small" style={{ alignSelf: 'flex-end' }} onClick={() => preset(n)}>Last {n} days</button>)}
        </form>
      </div>

      <ErrorBox message={error} />
      {rows && (
        <>
          <div className="btn-row" style={{ marginBottom: 12, gap: 28 }}>
            <div><div className="small muted">Total debits</div><b className="num debit">{inr(totalDebit)}</b></div>
            <div><div className="small muted">Total credits</div><b className="num credit">{inr(totalCredit)}</b></div>
            <div><div className="small muted">Closing balance</div><b className="num">{closing == null ? '-' : inr(closing)}</b></div>
          </div>
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Date</th><th>Description</th><th>Reference</th><th className="r">Debit</th><th className="r">Credit</th><th className="r">Balance</th></tr></thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    <td className="text-2 num">{dateTime(r.date)}</td>
                    <td>{r.description}</td>
                    <td className="text-2 num small">{r.referenceNo || '-'}</td>
                    <td className="r num debit">{r.debit != null ? inr(r.debit) : ''}</td>
                    <td className="r num credit">{r.credit != null ? inr(r.credit) : ''}</td>
                    <td className="r num">{inr(r.balance)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {rows.length === 0 && <div className="text-2" style={{ padding: 16 }}>No entries in this period.</div>}
          </div>
        </>
      )}
    </>
  )
}
