export default function Logo({ size = 32 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="7" fill="#24365f" />
      <path d="M8 12.5 16 7l8 5.5M9.5 13v8M14 13v8M18 13v8M22.5 13v8M7.5 23.5h17" fill="none" stroke="#e9a23b"
        strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
