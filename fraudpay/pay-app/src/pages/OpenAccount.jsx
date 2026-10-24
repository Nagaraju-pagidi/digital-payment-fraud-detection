import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { deviceId, deviceLabel, currentPosition } from '../device.js'
import { aadhaarValid, ageOf, groupAcc, inr, label, passwordChecks } from '../format.js'
import OtpDialog from '../components/OtpDialog.jsx'
import { ErrorBox, Field, PasswordInput, PinInput } from '../components/Fields.jsx'
import { PublicSide } from './Login.jsx'

const STEPS = ['Personal details', 'KYC documents', 'Address', 'Account', 'Login & security']

const EMPTY = {
  fullName: '', dateOfBirth: '', gender: '', email: '', phone: '',
  pan: '', aadhaar: '', kycConsent: false,
  addressLine: '', city: '', pincode: '',
  accountType: 'SAVINGS', occupation: '', annualIncome: '', nomineeName: '', nomineeRelation: '',
  username: '', password: '', confirmPassword: '', pin: '', confirmPin: '', acceptTerms: false,
}

function validate(step, f) {
  const e = {}
  if (step === 0) {
    if (!/^[A-Za-z][A-Za-z .']{1,98}[A-Za-z]$/.test(f.fullName.trim())) e.fullName = 'Enter your full name as on PAN (letters and spaces only)'
    else if (f.fullName.trim().split(/\s+/).length < 2) e.fullName = 'Enter first name and surname'
    const age = ageOf(f.dateOfBirth)
    if (!f.dateOfBirth) e.dateOfBirth = 'Enter your date of birth'
    else if (age < 18) e.dateOfBirth = 'You must be at least 18 years old'
    else if (age > 100) e.dateOfBirth = 'Please check the date of birth'
    if (!f.gender) e.gender = 'Select gender'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email.trim())) e.email = 'Enter a valid email address'
    if (!/^[6-9][0-9]{9}$/.test(f.phone)) e.phone = 'Enter a 10-digit mobile number starting with 6, 7, 8 or 9'
  }
  if (step === 1) {
    const pan = f.pan.toUpperCase()
    const surname = f.fullName.trim().split(/\s+/).pop() || ''
    if (!/^[A-Z]{3}P[A-Z][0-9]{4}[A-Z]$/.test(pan)) e.pan = 'Format: ABCPD1234E (4th letter is P for individuals)'
    else if (pan[4] !== surname[0]?.toUpperCase()) e.pan = `5th letter of PAN should be '${surname[0]?.toUpperCase()}' (first letter of your surname)`
    if (!aadhaarValid(f.aadhaar)) e.aadhaar = 'Enter a valid 12-digit Aadhaar number'
    if (!f.kycConsent) e.kycConsent = 'Please give consent for KYC verification'
  }
  if (step === 2) {
    if (f.addressLine.trim().length < 10) e.addressLine = 'Enter house number, street and area'
    if (!f.city) e.city = 'Select your city'
    if (!/^[1-9][0-9]{5}$/.test(f.pincode)) e.pincode = 'Enter a 6-digit PIN code'
  }
  if (step === 3) {
    if (!f.occupation) e.occupation = 'Select occupation'
    if (!f.annualIncome) e.annualIncome = 'Select annual income'
    if (f.nomineeName.trim() && !f.nomineeRelation) e.nomineeRelation = 'Select relation with nominee'
  }
  if (step === 4) {
    if (!/^[a-z][a-z0-9._]{4,19}$/.test(f.username)) e.username = '5 to 20 characters: start with a letter; letters, numbers, . or _'
    if (passwordChecks(f.password, f.username).some((c) => !c.ok)) e.password = 'Password does not meet all the rules below'
    if (f.password !== f.confirmPassword) e.confirmPassword = 'Passwords do not match'
    if (!/^[0-9]{6}$/.test(f.pin)) e.pin = 'Enter 6 digits'
    else if (new Set(f.pin).size === 1 || '0123456789'.includes(f.pin) || '9876543210'.includes(f.pin)) e.pin = 'Too easy to guess. Avoid repeated or sequential digits'
    if (f.pin !== f.confirmPin) e.confirmPin = 'PINs do not match'
    if (!f.acceptTerms) e.acceptTerms = 'Please accept the terms to continue'
  }
  return e
}

export default function OpenAccount() {
  const [step, setStep] = useState(0)
  const [f, setF] = useState(EMPTY)
  const [errors, setErrors] = useState({})
  const [ref, setRef] = useState(null)
  const [serverError, setServerError] = useState('')
  const [busy, setBusy] = useState(false)
  const [challenge, setChallenge] = useState(null)
  const [done, setDone] = useState(null)

  useEffect(() => { api('/auth/reference-data').then(setRef).catch((e) => setServerError(e.message)) }, [])
  const set = (k) => (v) => setF((x) => ({ ...x, [k]: typeof v === 'object' && v?.target ? (v.target.type === 'checkbox' ? v.target.checked : v.target.value) : v }))
  const city = ref?.cities.find((c) => c.name === f.city)

  function next(e) {
    e.preventDefault()
    const errs = validate(step, f)
    setErrors(errs)
    if (Object.keys(errs).length) return
    if (step < STEPS.length - 1) { setStep(step + 1); setServerError(''); window.scrollTo(0, 0); return }
    submit()
  }

  async function submit() {
    setBusy(true); setServerError('')
    try {
      const pos = await currentPosition()
      const body = {
        ...f, pan: f.pan.toUpperCase(), fullName: f.fullName.trim(), email: f.email.trim(),
        nomineeName: f.nomineeName.trim() || null, nomineeRelation: f.nomineeRelation || null,
        deviceId: deviceId(), deviceLabel: deviceLabel(),
        latitude: pos.error ? null : pos.lat, longitude: pos.error ? null : pos.lon,
      }
      const ch = await api('/auth/register', { method: 'POST', body })
      setChallenge(ch)
    } catch (err) {
      setServerError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (done) {
    return (
      <div className="public">
        <PublicSide />
        <main className="public-main">
          <div className="wizard form">
            <div className="alert success"><span aria-hidden="true">✓</span><span><b>Your account is open.</b> We have also emailed these details to you.</span></div>
            <h2>Welcome, {done.fullName}</h2>
            <div className="details-box">
              <div className="review">
                <div className="row"><span>Customer ID</span><span className="num">{done.customerId}</span></div>
                <div className="row"><span>User ID (for login)</span><span>{done.username}</span></div>
                <div className="row"><span>Account number</span><span className="num">{groupAcc(done.accountNumber)}</span></div>
                <div className="row"><span>Account type</span><span>{label(done.accountType)}</span></div>
                <div className="row"><span>IFSC</span><span className="num">{done.ifsc}</span></div>
                <div className="row"><span>Branch</span><span>{done.branch}</span></div>
                <div className="row"><span>UPI ID</span><span>{done.upiId}</span></div>
                <div className="row"><span>Opening balance (demo)</span><span className="num">{inr(done.openingBalance)}</span></div>
              </div>
            </div>
            <div className="text-2 small">This device and your current location are now registered as your trusted device and home location.</div>
            <div><a className="btn" href="#/login">Log in to net banking</a></div>
          </div>
        </main>
      </div>
    )
  }

  const err = (k) => errors[k]
  return (
    <div className="public">
      <PublicSide />
      <main className="public-main">
        <form className="wizard form" onSubmit={next} noValidate>
          <div>
            <h2>Open an account</h2>
            <div className="text-2">Takes about 5 minutes. Keep your PAN and Aadhaar handy.</div>
          </div>
          <div className="stepper" aria-label="Progress">
            {STEPS.map((s, i) => <div key={s} className={i < step ? 'done' : i === step ? 'now' : ''}>{i + 1}. {s}</div>)}
          </div>

          {step === 0 && (
            <div className="form-grid">
              <Field full label="Full name (as on PAN card)" error={err('fullName')}>
                <input className="input" value={f.fullName} onChange={set('fullName')} autoComplete="name" />
              </Field>
              <Field label="Date of birth" error={err('dateOfBirth')}>
                <input className="input" type="date" value={f.dateOfBirth} onChange={set('dateOfBirth')} max={new Date().toISOString().slice(0, 10)} />
              </Field>
              <Field label="Gender" error={err('gender')}>
                <select className="input" value={f.gender} onChange={set('gender')}>
                  <option value="">Select</option>
                  {ref?.genders.map((g) => <option key={g} value={g}>{label(g)}</option>)}
                </select>
              </Field>
              <Field label="Mobile number" error={err('phone')}>
                <input className="input" inputMode="numeric" maxLength={10} value={f.phone} onChange={(e) => set('phone')(e.target.value.replace(/\D/g, ''))} autoComplete="tel-national" />
              </Field>
              <Field label="Email address" hint="One-time passwords are sent here" error={err('email')}>
                <input className="input" type="email" value={f.email} onChange={set('email')} autoComplete="email" />
              </Field>
            </div>
          )}

          {step === 1 && (
            <div className="form-grid">
              <Field label="PAN" hint="10 characters, e.g. ABCPD1234E" error={err('pan')}>
                <input className="input" maxLength={10} value={f.pan} onChange={(e) => set('pan')(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''))} />
              </Field>
              <Field label="Aadhaar number" hint="12 digits. We store only the last 4 digits and a secure fingerprint." error={err('aadhaar')}>
                <input className="input" inputMode="numeric" maxLength={12} value={f.aadhaar} onChange={(e) => set('aadhaar')(e.target.value.replace(/\D/g, ''))} autoComplete="off" />
              </Field>
              <div className="full">
                <label className="check"><input type="checkbox" checked={f.kycConsent} onChange={set('kycConsent')} />
                  <span>I consent to NexaPay verifying my PAN and Aadhaar for KYC as required by RBI guidelines.</span></label>
                {err('kycConsent') && <div className="field"><span className="err">{err('kycConsent')}</span></div>}
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="form-grid">
              <Field full label="Address" hint="House / flat number, street, area" error={err('addressLine')}>
                <input className="input" value={f.addressLine} onChange={set('addressLine')} autoComplete="street-address" />
              </Field>
              <Field label="City" error={err('city')}>
                <select className="input" value={f.city} onChange={set('city')}>
                  <option value="">Select city</option>
                  {ref?.cities.map((c) => <option key={c.name} value={c.name}>{c.name}</option>)}
                </select>
              </Field>
              <Field label="State">
                <input className="input" readOnly value={city ? city.state : ''} placeholder="Filled from city" />
              </Field>
              <Field label="PIN code" error={err('pincode')}>
                <input className="input" inputMode="numeric" maxLength={6} value={f.pincode} onChange={(e) => set('pincode')(e.target.value.replace(/\D/g, ''))} autoComplete="postal-code" />
              </Field>
              <Field label="Home branch">
                <input className="input" readOnly value={city ? `${city.name} Main Branch` : ''} placeholder="Filled from city" />
              </Field>
            </div>
          )}

          {step === 3 && (
            <div className="form">
              <Field label="Account type" group>
                <div className="radio-cards">
                  {[['SAVINGS', 'Savings account', 'For salary, savings and everyday payments'], ['CURRENT', 'Current account', 'For business and frequent high-value payments']].map(([v, t, d]) => (
                    <label key={v} className={'radio-card' + (f.accountType === v ? ' on' : '')}>
                      <input type="radio" name="acctype" checked={f.accountType === v} onChange={() => set('accountType')(v)} />
                      <div><b>{t}</b><span>{d}</span></div>
                    </label>
                  ))}
                </div>
              </Field>
              <div className="form-grid">
                <Field label="Occupation" error={err('occupation')}>
                  <select className="input" value={f.occupation} onChange={set('occupation')}>
                    <option value="">Select</option>
                    {ref?.occupations.map((o) => <option key={o} value={o}>{label(o)}</option>)}
                  </select>
                </Field>
                <Field label="Annual income" error={err('annualIncome')}>
                  <select className="input" value={f.annualIncome} onChange={set('annualIncome')}>
                    <option value="">Select</option>
                    {ref?.incomes.map((o) => <option key={o} value={o}>{o.replace('BELOW_', 'Below ').replace('ABOVE_', 'Above ').replace(/L/g, ' lakh')}</option>)}
                  </select>
                </Field>
                <Field label="Nominee name (optional)">
                  <input className="input" value={f.nomineeName} onChange={set('nomineeName')} />
                </Field>
                <Field label="Relation with nominee" error={err('nomineeRelation')}>
                  <select className="input" value={f.nomineeRelation} onChange={set('nomineeRelation')} disabled={!f.nomineeName.trim()}>
                    <option value="">Select</option>
                    {ref?.relations.map((o) => <option key={o} value={o}>{label(o)}</option>)}
                  </select>
                </Field>
              </div>
            </div>
          )}

          {step === 4 && (
            <div className="form-grid">
              <Field full label="Choose a User ID" hint="You will log in with this. 5 to 20 characters." error={err('username')}>
                <input className="input" value={f.username} onChange={(e) => set('username')(e.target.value.toLowerCase().replace(/\s/g, ''))} autoComplete="username" />
              </Field>
              <Field label="Password" error={err('password')}>
                <PasswordInput value={f.password} onChange={set('password')} autoComplete="new-password" />
              </Field>
              <Field label="Confirm password" error={err('confirmPassword')}>
                <PasswordInput value={f.confirmPassword} onChange={set('confirmPassword')} autoComplete="new-password" />
              </Field>
              <ul className="checklist full" aria-label="Password rules">
                {passwordChecks(f.password, f.username).map((c) => <li key={c.text} className={c.ok ? 'met' : ''}>{c.ok ? '✓' : '○'} {c.text}</li>)}
              </ul>
              <Field label="6-digit transaction PIN" hint="Needed for every payment. Not your birth date or mobile number." error={err('pin')}>
                <PinInput value={f.pin} onChange={set('pin')} />
              </Field>
              <Field label="Confirm transaction PIN" error={err('confirmPin')}>
                <PinInput value={f.confirmPin} onChange={set('confirmPin')} />
              </Field>
              <div className="full">
                <label className="check"><input type="checkbox" checked={f.acceptTerms} onChange={set('acceptTerms')} />
                  <span>I accept the terms and conditions. I understand that my device and location are used to protect my account from fraud.</span></label>
                {err('acceptTerms') && <div className="field"><span className="err">{err('acceptTerms')}</span></div>}
              </div>
            </div>
          )}

          <ErrorBox message={serverError} />
          <div className="btn-row">
            {step > 0 && <button type="button" className="btn secondary" onClick={() => { setStep(step - 1); setErrors({}) }}>Back</button>}
            <button className="btn" disabled={busy}>
              {busy ? <span className="spinner" /> : step < STEPS.length - 1 ? 'Continue' : 'Verify email and open account'}
            </button>
            <span style={{ flex: 1 }} />
            <a href="#/login" className="small">Already a customer? Log in</a>
          </div>
        </form>
      </main>
      {challenge && (
        <OtpDialog
          challenge={challenge}
          title="Verify your email"
          description={`Enter the OTP sent to ${f.email} to open your account.`}
          confirmLabel="Open my account"
          onCancel={() => setChallenge(null)}
          onVerify={async (otp, _trust, challengeId) => {
            const res = await api('/auth/register/verify', { method: 'POST', body: { challengeId, otp } })
            setChallenge(null)
            setDone(res)
          }}
        />
      )}
    </div>
  )
}
