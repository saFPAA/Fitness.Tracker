import { useState } from 'react'
import { ErrorState, Field, Skeleton } from '../components/ui'
import WorkoutCard from '../features/workouts/WorkoutCard'
import { useAllWorkouts } from '../features/workouts/hooks'
import { mondayOf, monthYear, todayISO } from '../lib/dates'
import { plural } from '../lib/format'
import { workoutStats } from '../lib/stats'

type Period = 'all' | 'week' | 'month'
const PERIODS: { id: Period; label: string }[] = [
  { id: 'all', label: 'Всё время' },
  { id: 'week', label: 'Эта неделя' },
  { id: 'month', label: 'Этот месяц' },
]

export default function HistoryPage() {
  const q = useAllWorkouts()
  const [query, setQuery] = useState('')
  const [period, setPeriod] = useState<Period>('all')

  const today = todayISO()
  const all = q.data ?? []
  const stats = workoutStats(all.map((w) => w.date), today)

  const from = period === 'week' ? mondayOf(today) : period === 'month' ? `${today.slice(0, 7)}-01` : null
  const needle = query.trim().toLowerCase()
  const items = all.filter(
    (w) => (!from || (w.date >= from && w.date <= today)) && (!needle || w.name.toLowerCase().includes(needle)),
  )

  // История отсортирована по дате, поэтому группы по месяцам идут подряд
  const groups: { key: string; title: string; items: typeof items }[] = []
  for (const w of items) {
    const key = w.date.slice(0, 7)
    const last = groups[groups.length - 1]
    if (last?.key === key) last.items.push(w)
    else groups.push({ key, title: monthYear(w.date), items: [w] })
  }

  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight">История</h1>

      {q.isPending && (
        <div className="mt-6 flex flex-col gap-3">
          <Skeleton className="h-20" />
          <Skeleton className="h-20" />
          <Skeleton className="h-20" />
        </div>
      )}
      {q.isError && (
        <div className="mt-6">
          <ErrorState message={q.error.message} onRetry={() => void q.refetch()} />
        </div>
      )}
      {q.isSuccess && all.length === 0 && <p className="mt-10 text-center text-mute">Здесь появятся ваши тренировки.</p>}

      {q.isSuccess && all.length > 0 && (
        <>
          <dl className="mt-5 grid grid-cols-3 gap-2 text-center">
            {[
              [stats.week, 'на этой неделе'],
              [stats.month, 'в этом месяце'],
              [stats.streak, `${plural(stats.streak, ['неделя', 'недели', 'недель'])} подряд`],
            ].map(([value, label]) => (
              <div key={label} className="rounded-2xl border border-line bg-surface px-2 py-3">
                <dd className="num text-2xl font-bold">{value}</dd>
                <dt className="text-xs text-mute">{label}</dt>
              </div>
            ))}
          </dl>

          <div className="mt-5">
            <Field label="Поиск" type="search" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Название тренировки" autoComplete="off" />
          </div>
          <div className="mt-3 flex gap-2 overflow-x-auto" role="group" aria-label="Период">
            {PERIODS.map((p) => (
              <button
                key={p.id}
                onClick={() => setPeriod(p.id)}
                aria-pressed={period === p.id}
                className={`min-h-11 shrink-0 rounded-full border px-4 text-sm font-medium ${
                  period === p.id ? 'border-accent bg-accent text-accent-ink' : 'border-line bg-surface'
                }`}
              >
                {p.label}
              </button>
            ))}
          </div>

          {items.length === 0 && <p className="mt-10 text-center text-mute">Ничего не найдено.</p>}
          {groups.map((g) => (
            <section key={g.key} className="mt-6">
              <h2 className="mb-3 text-sm font-semibold text-mute uppercase">{g.title}</h2>
              <ul className="flex flex-col gap-3">
                {g.items.map((w) => (
                  <li key={w.id}>
                    <WorkoutCard workout={w} />
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </>
      )}
    </>
  )
}
