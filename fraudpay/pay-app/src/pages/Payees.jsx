import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { dateOnly, timeOnly } from '../format.js'
import OtpDialog from '../components/OtpDialog.jsx'
import { ErrorBox, Field } from '../components/Fields.jsx'

/** Manage payees: verify account + IFSC, confirm with OTP, then a short cooling period. */
export default function Payees({ onToast }) {
  const [payees, setPayees] = useState(null)
  const [adding, setAdding] = useState(false)
  const [form, setForm] = useState({ accountNumber: '', confirmAccountNumber: '', ifsc: '', nickname: '' })
  const [verified, setVerified] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [challenge, setChallenge] = useState(null)

  const load = () => api('/users/payees').then(setPayees).catch((e) => setError(e.message))
  useEffect(() => {
    load()
    const t = setInterval(load, 20000) // cooling periods end while the page is open
    return () => clearInterval(t)
  }, [])

  const set = (k) => (e) => {
    const v = k === 'ifsc' ? e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '') : k.includes('ccount') ? e.target.value.replace(/\D/g, '') : e.target.value
    setForm({ ...form, [k]: v })
    if (k !== 'nickname') setVerified(null)
  }

  async function verify() {
    setError(''); setBusy(true)
    try {
      if (form.accountNumber !== form.confirmAccountNumber) throw new Error('Account numbers do not match')
      const r = await api('/users/payees/lookup', { method: 'POST', body: { accountNumber: form.accountNumber, ifsc: form.ifsc } })
      setVerified(r)
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  async function add(e) {
    e.preventDefault()
    setError(''); setBusy(true)
    try {
      setChallenge(await api('/users/payees', { method: 'POST', body: form }))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function remove(p) {
    if (!window.confirm(`Delete payee ${p.name}?`)) return
    try {
      await api(`/users/payees/${p.id}`, { method: 'DELETE' })
      onToast(`${p.name} deleted from your payees`)
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <>
      <div className="page-head">
        <div className="grow">
          <h1>Payees</h1>
          <p>People you can send money to by IMPS or NEFT.</p>
        </div>
        {!adding && <button className="btn" onClick={() => { setAdding(true); setError('') }}>Add payee</button>}
      </div>

      {adding && (
        <form className="panel form" onSubmit={add} style={{ marginBottom: 18 }}>
          <div className="panel-head" style={{ marginBottom: 0 }}>
            <h3 className="grow">Add a payee</h3>
            <button type="button" className="btn link" onClick={() => { setAdding(false); setVerified(null) }}>Cancel</button>
          </div>
          <div className="text-2 small">For your safety, a new payee can receive money only a short while after being added, and you will get an email.</div>
          <div className="form-grid">
            <Field label="Account number"><input className="input num" inputMode="numeric" maxLength={18} value={form.accountNumber} onChange={set('accountNumber')} autoComplete="off" /></Field>
            <Field label="Re-enter account number"><input className="input num" inputMode="numeric" maxLength={18} value={form.confirmAccountNumber} onChange={set('confirmAccountNumber')} autoComplete="off" onPaste={(e) => e.preventDefault()} /></Field>
            <Field label="IFSC" hint="11 characters, e.g. FRPY0HYD001"><input className="input num" maxLength={11} value={form.ifsc} onChange={set('ifsc')} /></Field>
            <Field label="Nickname (optional)"><input className="input" maxLength={40} value={form.nickname} onChange={set('nickname')} /></Field>
          </div>
          {verified && (
            <div className="alert success"><span aria-hidden="true">✓</span>
              <span>Account holder name: <b>{verified.name}</b>, {verified.branch}. Check this is the person you want to pay.</span></div>
          )}
          <ErrorBox message={error} />
          <div className="btn-row">
            {!verified
              ? <button type="button" className="btn secondary" disabled={busy || form.accountNumber.length < 9 || form.ifsc.length !== 11} onClick={verify}>{busy ? <span className="spinner" /> : 'Verify account'}</button>
              : <button className="btn" disabled={busy}>{busy ? <span className="spinner" /> : 'Add payee'}</button>}
          </div>
        </form>
      )}

      {!adding && <ErrorBox message={error} />}
      <div className="table-wrap">
        <table className="table">
          <thead><tr><th>Payee</th><th>Account number</th><th>IFSC</th><th>Branch</th><th>Status</th><th>Added on</th><th /></tr></thead>
          <tbody>
            {payees && payees.map((p) => (
              <tr key={p.id}>
                <td><b>{p.name}</b>{p.nickname && <div className="small muted">{p.nickname}</div>}</td>
                <td className="num">{p.accountNumber}</td>
                <td className="num">{p.ifsc}</td>
                <td className="text-2">{p.branch}</td>
                <td>{p.active ? <span className="badge good">✓ Active</span> : <span className="badge warning">⏳ Active from {timeOnly(p.activeFrom)}</span>}</td>
                <td className="text-2">{dateOnly(p.addedAt)}</td>
                <td className="r"><button className="btn danger small" onClick={() => remove(p)}>Delete</button></td>
              </tr>
            ))}
          </tbody>
        </table>
        {payees && payees.length === 0 && <div className="text-2" style={{ padding: 16 }}>No payees yet. Add one to transfer money by IMPS or NEFT.</div>}
      </div>

      {challenge && (
        <OtpDialog
          challenge={challenge}
          title="Confirm new payee"
          description={`Adding ${verified?.name} as a payee`}
          confirmLabel="Add payee"
          onCancel={() => setChallenge(null)}
          onVerify={async (otp, _t, challengeId) => {
            const p = await api('/users/payees/verify', { method: 'POST', body: { challengeId, otp } })
            setChallenge(null)
            setAdding(false)
            setVerified(null)
            setForm({ accountNumber: '', confirmAccountNumber: '', ifsc: '', nickname: '' })
            onToast(`${p.name} added. You can send money from ${timeOnly(p.activeFrom)}.`)
            load()
          }}
        />
      )}
    </>
  )
}
