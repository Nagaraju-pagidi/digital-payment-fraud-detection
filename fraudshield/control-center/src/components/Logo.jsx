export default function ShieldLogo({ size = 30 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <path d="M16 2 4 7v8c0 7.5 5.1 13.6 12 15 6.9-1.4 12-7.5 12-15V7z" fill="#3987e5" />
      <path d="m11 16 3.5 3.5L21.5 12" fill="none" stroke="#fff" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
