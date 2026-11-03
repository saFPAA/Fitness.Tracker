import { useCatalog } from '../training/hooks'
import { groupDot } from '../../lib/format'
import { groupsIn, toggleGroup } from '../../lib/workoutName'

const FALLBACK = ['Грудь', 'Спина', 'Ноги', 'Плечи', 'Руки']

/** Необязательные подсказки: нажатия складывают группы мышц в название («Спина + Грудь»). */
export default function NameChips({ value, onChange }: { value: string; onChange: (name: string) => void }) {
  const catalog = useCatalog()
  const fromCatalog = [...new Set((catalog.data ?? []).map((e) => e.muscleGroup))].sort((a, b) => a.localeCompare(b, 'ru'))
  const groups = fromCatalog.length > 0 ? fromCatalog : FALLBACK
  const active = groupsIn(value)

  return (
    <div className="flex flex-wrap gap-2" role="group" aria-label="Добавить группу мышц в название">
      {groups.map((g) => {
        const on = active.has(g.toLowerCase())
        return (
          <button
            key={g}
            type="button"
            aria-pressed={on}
            onClick={() => onChange(toggleGroup(value, g))}
            className={`flex min-h-11 items-center gap-2 rounded-full border px-4 text-sm font-medium active:opacity-70 ${
              on ? 'border-accent bg-accent text-accent-ink' : 'border-line'
            }`}
          >
            <span className={`size-2.5 rounded-full ${groupDot(g)}`} aria-hidden="true" />
            {g}
          </button>
        )
      })}
    </div>
  )
}
