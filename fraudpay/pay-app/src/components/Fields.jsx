import { Children, cloneElement, useId, useState } from 'react'

/**
 * A labelled form field: <label for=id> is linked to the input (screen readers, clicks).
 * Use group for a set of radio cards.
 */
export function Field({ label, hint, error, children, full, group }) {
  const autoId = useId()
  const child = Children.only(children)
  const id = child.props.id || autoId
  const control = group ? child : cloneElement(child, { id })
  return (
    <div className={'field' + (full ? ' full' : '')} role={group ? 'group' : undefined} aria-label={group ? label : undefined}>
      {label && (group ? <span className="lbl">{label}</span> : <label htmlFor={id}>{label}</label>)}
      {control}
      {error ? <span className="err" role="alert">{error}</span> : hint ? <span className="hint">{hint}</span> : null}
    </div>
  )
}

export function PasswordInput({ value, onChange, placeholder, autoComplete, id, invalid }) {
  const [show, setShow] = useState(false)
  return (
    <div className="pw-wrap">
      <input id={id} className={'input' + (invalid ? ' invalid' : '')} type={show ? 'text' : 'password'} value={value}
        onChange={(e) => onChange(e.target.value)} placeholder={placeholder} autoComplete={autoComplete}
        style={{ paddingRight: 60 }} />
      <button type="button" className="show" onClick={() => setShow(!show)}>{show ? 'Hide' : 'Show'}</button>
    </div>
  )
}

/** 6-digit transaction PIN - always masked, digits only. */
export function PinInput({ value, onChange, autoFocus, id }) {
  return (
    <input id={id} className="input pin-input" type="password" inputMode="numeric" autoComplete="off" maxLength={6}
      value={value} autoFocus={autoFocus} placeholder="••••••"
      onChange={(e) => onChange(e.target.value.replace(/\D/g, '').slice(0, 6))} />
  )
}

export function ErrorBox({ message }) {
  if (!message) return null
  return <div className="alert error" role="alert"><span aria-hidden="true">⚠</span><span>{message}</span></div>
}
