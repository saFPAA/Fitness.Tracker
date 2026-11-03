import { useId } from 'react'
import { fromInput, toInput } from '../lib/format'

/** Поле числа с кнопками − и +: крупные зоны нажатия для одной руки в зале. */
export default function Stepper({
  label,
  value,
  onChange,
  step,
  inputMode,
}: {
  label: string
  value: string
  onChange: (v: string) => void
  step: number
  inputMode: 'numeric' | 'decimal'
}) {
  const id = useId()
  const bump = (d: number) => {
    const n = fromInput(value)
    const base = Number.isFinite(n) ? n : 0
    onChange(toInput(Math.max(0, Math.round((base + d) * 100) / 100)))
  }
  const btn =
    'size-12 shrink-0 rounded-xl border border-line bg-bg text-2xl leading-none select-none active:opacity-60'
  return (
    <div className="flex items-center gap-3">
      <label htmlFor={id} className="w-20 shrink-0 text-sm font-medium text-mute">
        {label}
      </label>
      <button type="button" aria-label={`${label}: меньше`} onClick={() => bump(-step)} className={btn}>
        −
      </button>
      <input
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        inputMode={inputMode}
        autoComplete="off"
        className="num min-h-12 min-w-0 flex-1 rounded-xl border border-line bg-bg px-1 text-center text-2xl font-bold"
      />
      <button type="button" aria-label={`${label}: больше`} onClick={() => bump(step)} className={btn}>
        +
      </button>
    </div>
  )
}
