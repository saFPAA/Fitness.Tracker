/** Знак приложения: олимпийский блин, вид сбоку. */
export default function PlateMark({ size = 56 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 100 100" aria-hidden="true">
      <circle cx="50" cy="50" r="46" className="fill-accent" />
      <circle cx="50" cy="50" r="33" fill="none" stroke="var(--accent-ink)" strokeOpacity=".35" strokeWidth="3" />
      <circle cx="50" cy="50" r="11" className="fill-bg" />
    </svg>
  )
}
