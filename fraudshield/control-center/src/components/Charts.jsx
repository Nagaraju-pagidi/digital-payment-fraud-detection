import { useState } from 'react'

const SERIES = [
  { key: 'approved', label: 'Approved', icon: '✓', color: 'var(--good)' },
  { key: 'hold', label: 'On hold', icon: '⏳', color: 'var(--warning)' },
  { key: 'blocked', label: 'Blocked', icon: '⛔', color: 'var(--critical)' },
]

function niceMax(v) {
  if (v <= 4) return 4
  const p = Math.pow(10, Math.floor(Math.log10(v)))
  const n = v / p
  const step = n <= 2 ? 2 : n <= 5 ? 5 : 10
  return step * p
}

/** Decisions per minute - stacked columns with a 2px gap between segments and a hover tooltip. */
export function DecisionTimeline({ data }) {
  const [hover, setHover] = useState(null)
  const W = 640, H = 220, L = 32, R = 8, T = 8, B = 24
  const rows = data || []
  const totals = rows.map((r) => r.approved + r.hold + r.blocked)
  const max = niceMax(Math.max(1, ...totals))
  const plotW = W - L - R, plotH = H - T - B
  const slot = plotW / Math.max(1, rows.length)
  const barW = Math.max(4, slot * 0.62)
  const y = (v) => T + plotH - (v / max) * plotH
  const ticks = [0, max / 2, max]
  const label = (iso) => new Date(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })

  return (
    <div className="chart-wrap">
      <div className="legend">
        {SERIES.map((s) => (
          <span key={s.key}><i className="dot" style={{ background: s.color }} />{s.icon} {s.label}</span>
        ))}
      </div>
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label="Payments checked per minute, by decision, last 30 minutes">
        {ticks.map((t) => (
          <g key={t}>
            <line x1={L} x2={W - R} y1={y(t)} y2={y(t)} stroke={t === 0 ? 'var(--axis)' : 'var(--grid)'} strokeWidth="1" />
            <text x={L - 6} y={y(t) + 4} textAnchor="end" fontSize="10" fill="var(--muted)">{Math.round(t)}</text>
          </g>
        ))}
        {rows.map((r, i) => {
          const x = L + i * slot + (slot - barW) / 2
          let acc = 0
          const segs = SERIES.filter((s) => r[s.key] > 0)
          return (
            <g key={r.minute}>
              {segs.map((s, si) => {
                const y0 = y(acc), y1 = y(acc + r[s.key])
                acc += r[s.key]
                const top = si === segs.length - 1
                const h = Math.max(1, y0 - y1 - (top ? 0 : 2)) // 2px surface gap between stacked segments
                const rad = Math.min(4, h / 2, barW / 2) // rounded data-end
                return top ? (
                  <path key={s.key} fill={s.color}
                    d={`M${x},${y0} V${y0 - h + rad} Q${x},${y0 - h} ${x + rad},${y0 - h} H${x + barW - rad} Q${x + barW},${y0 - h} ${x + barW},${y0 - h + rad} V${y0} Z`} />
                ) : (
                  <rect key={s.key} x={x} y={y0 - h} width={barW} height={h} fill={s.color} />
                )
              })}
              {(i % 5 === 0 || i === rows.length - 1) && (
                <text x={x + barW / 2} y={H - 6} textAnchor="middle" fontSize="10" fill="var(--muted)">{label(r.minute)}</text>
              )}
              {/* hit target: the whole column, bigger than the mark */}
              <rect x={L + i * slot} y={T} width={slot} height={plotH} fill="transparent"
                onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)} />
            </g>
          )
        })}
        {hover != null && (
          <rect x={L + hover * slot} y={T} width={slot} height={plotH} fill="rgba(255,255,255,0.05)" pointerEvents="none" />
        )}
      </svg>
      {hover != null && rows[hover] && (
        <div className="tooltip" style={{ left: `calc(${((L + hover * slot + slot / 2) / W) * 100}% + 10px)`, top: 30,
          transform: hover > rows.length * 0.7 ? 'translateX(calc(-100% - 20px))' : 'none' }}>
          <div className="t">{label(rows[hover].minute)}</div>
          {SERIES.map((s) => (
            <div className="row" key={s.key}>
              <span><i className="dot" style={{ background: s.color }} />{s.label}</span>
              <b className="num">{rows[hover][s.key]}</b>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

/** Horizontal bars with direct value labels (single series - no legend needed). */
export function HBars({ rows, labelKey, valueKey, color = 'var(--accent)', format = (v) => v }) {
  if (!rows || rows.length === 0) return <div className="empty">No data yet</div>
  const max = Math.max(1, ...rows.map((r) => r[valueKey]))
  return (
    <div>
      {rows.map((r) => (
        <div className="hbar" key={r[labelKey]} title={`${r[labelKey]}: ${format(r[valueKey])}`}>
          <span className="label">{r[labelKey]}</span>
          <div className="track"><div className="fill" style={{ width: `${(r[valueKey] / max) * 100}%`, background: color }} /></div>
          <span className="val">{format(r[valueKey])}</span>
        </div>
      ))}
    </div>
  )
}

/** Score distribution - columns per score band, hover shows the count. */
export function ScoreBands({ rows }) {
  const [hover, setHover] = useState(null)
  const bands = ['0-19', '20-39', '40-69', '70-119', '120+']
  const data = bands.map((b) => ({ band: b, count: (rows || []).find((r) => r.band === b)?.count || 0 }))
  const W = 320, H = 170, L = 8, R = 8, T = 18, B = 34
  const max = Math.max(1, ...data.map((d) => d.count))
  const slot = (W - L - R) / data.length
  const bw = slot * 0.6
  const plotH = H - T - B
  const zone = ['Approve', 'Approve', 'Hold', 'Block', 'Block+freeze']
  return (
    <div className="chart-wrap">
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label="How many payments fall in each risk-score band">
        <line x1={L} x2={W - R} y1={T + plotH} y2={T + plotH} stroke="var(--axis)" />
        {data.map((d, i) => {
          const h = (d.count / max) * plotH
          const x = L + i * slot + (slot - bw) / 2
          const yTop = T + plotH - h
          return (
            <g key={d.band} onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)}>
              <rect x={L + i * slot} y={T - 14} width={slot} height={plotH + 14} fill={hover === i ? 'rgba(255,255,255,0.05)' : 'transparent'} />
              {h > 0 && (
                <path fill="var(--accent)"
                  d={`M${x},${T + plotH} V${yTop + Math.min(4, h)} Q${x},${yTop} ${x + Math.min(4, bw / 2)},${yTop} H${x + bw - Math.min(4, bw / 2)} Q${x + bw},${yTop} ${x + bw},${yTop + Math.min(4, h)} V${T + plotH} Z`} />
              )}
              <text x={x + bw / 2} y={yTop - 4} textAnchor="middle" fontSize="11" fill="var(--ink-2)">{d.count}</text>
              <text x={x + bw / 2} y={H - 18} textAnchor="middle" fontSize="10" fill="var(--ink-2)">{d.band}</text>
              <text x={x + bw / 2} y={H - 5} textAnchor="middle" fontSize="9" fill="var(--muted)">{zone[i]}</text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}
