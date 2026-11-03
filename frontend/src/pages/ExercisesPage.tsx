import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ErrorState, Field, Skeleton } from '../components/ui'
import { useAuth } from '../features/auth/AuthContext'
import ExerciseFormSheet from '../features/exercises/ExerciseFormSheet'
import { useCatalog } from '../features/training/hooks'
import { groupDot } from '../lib/format'

export default function ExercisesPage() {
  const catalog = useCatalog()
  const isAdmin = useAuth().user?.role === 'ADMIN'
  const [query, setQuery] = useState('')
  const [group, setGroup] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)

  const all = catalog.data ?? []
  const groups = [...new Set(all.map((e) => e.muscleGroup))].sort((a, b) => a.localeCompare(b, 'ru'))
  const q = query.trim().toLowerCase()
  const items = all.filter((e) => (!group || e.muscleGroup === group) && (!q || e.name.toLowerCase().includes(q)))

  return (
    <>
      <div className="flex items-center gap-3">
        <h1 className="flex-1 text-3xl font-bold tracking-tight">Упражнения</h1>
        {isAdmin && (
          <button
            onClick={() => setCreating(true)}
            aria-label="Новое упражнение"
            className="grid size-11 place-items-center rounded-full bg-accent text-2xl leading-none text-accent-ink active:opacity-80"
          >
            +
          </button>
        )}
      </div>

      <div className="mt-5">
        <Field label="Поиск" type="search" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Название упражнения" autoComplete="off" />
      </div>

      {groups.length > 0 && (
        <div className="-mx-4 mt-3 flex gap-2 overflow-x-auto px-4 pb-1" role="group" aria-label="Группа мышц">
          {[null, ...groups].map((g) => (
            <button
              key={g ?? 'all'}
              onClick={() => setGroup(g)}
              aria-pressed={group === g}
              className={`flex min-h-11 shrink-0 items-center gap-2 rounded-full border px-4 text-sm font-medium ${
                group === g ? 'border-accent bg-accent text-accent-ink' : 'border-line bg-surface'
              }`}
            >
              {g && <span className={`size-2.5 rounded-full ${groupDot(g)}`} aria-hidden="true" />}
              {g ?? 'Все'}
            </button>
          ))}
        </div>
      )}

      <div className="mt-4">
        {catalog.isPending && (
          <div className="flex flex-col gap-3">
            <Skeleton className="h-16" />
            <Skeleton className="h-16" />
            <Skeleton className="h-16" />
          </div>
        )}
        {catalog.isError && <ErrorState message={catalog.error.message} onRetry={() => void catalog.refetch()} />}
        {catalog.isSuccess && all.length === 0 && (
          <p className="py-10 text-center text-mute">
            Каталог пуст.{isAdmin ? ' Нажмите «+», чтобы добавить упражнение.' : ''}
          </p>
        )}
        {catalog.isSuccess && all.length > 0 && items.length === 0 && (
          <p className="py-10 text-center text-mute">Ничего не найдено.</p>
        )}
        <ul className="overflow-hidden rounded-2xl border border-line bg-surface empty:hidden">
          {items.map((e) => (
            <li key={e.id} className="border-t border-line first:border-t-0">
              <Link to={`/exercises/${e.id}`} className="flex min-h-16 items-center gap-3 px-4 py-2 active:opacity-60">
                <span className={`size-3 shrink-0 rounded-full ${groupDot(e.muscleGroup)}`} aria-hidden="true" />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium">{e.name}</span>
                  <span className="block truncate text-sm text-mute">
                    {e.muscleGroup}
                    {e.equipment ? ` · ${e.equipment}` : ''}
                  </span>
                </span>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0 text-mute" aria-hidden="true">
                  <path d="M9 6l6 6-6 6" />
                </svg>
              </Link>
            </li>
          ))}
        </ul>
      </div>

      {isAdmin && <ExerciseFormSheet open={creating} onClose={() => setCreating(false)} groups={groups} />}
    </>
  )
}
