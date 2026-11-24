import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ErrorState, Field, Skeleton } from '../components/ui'
import { useAuth } from '../features/auth/AuthContext'
import ExerciseFormSheet from '../features/exercises/ExerciseFormSheet'
import { useCatalog } from '../features/training/hooks'
import { groupDot } from '../lib/format'

export default function ExercisesPage() {
  const pageSize = 10
  const catalog = useCatalog()
  const isAdmin = useAuth().user?.role === 'ADMIN'
  const [query, setQuery] = useState('')
  const [group, setGroup] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(false)

  const all = catalog.data ?? []
  const groups = [...new Set(all.map((e) => e.muscleGroup))].sort((a, b) => a.localeCompare(b, 'ru'))
  const q = query.trim().toLowerCase()
  const items = all.filter((e) => (!group || e.muscleGroup === group) && (!q || e.name.toLowerCase().includes(q)))
  const pageCount = Math.ceil(items.length / pageSize)
  const visibleItems = items.slice(page * pageSize, (page + 1) * pageSize)

  useEffect(() => {
    setPage(0)
  }, [query, group])

  useEffect(() => {
    if (page >= pageCount && pageCount > 0) setPage(pageCount - 1)
  }, [page, pageCount])

  return (
    <>
      <div className="flex items-end gap-3">
        <div className="flex-1">
          <p className="page-kicker">БИБЛИОТЕКА ДВИЖЕНИЙ</p>
          <h1 className="page-title">Упражнения</h1>
        </div>
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
        <Field label="Поиск по каталогу" type="search" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Например, жим лёжа" autoComplete="off" />
      </div>

      {groups.length > 0 && (
        <div className="-mx-4 mt-3 flex gap-2 overflow-x-auto px-4 pb-2" role="group" aria-label="Группа мышц">
          {[null, ...groups].map((g) => (
            <button
              key={g ?? 'all'}
              onClick={() => {
                setGroup(g)
                setPage(0)
              }}
              aria-pressed={group === g}
              className={`flex min-h-10 shrink-0 items-center gap-2 rounded-full border px-4 text-xs font-semibold ${
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
        {catalog.isSuccess && items.length > 0 && (
          <div className="mb-2 flex items-center justify-between">
            <p className="page-kicker">КАТАЛОГ</p>
            <p className="text-xs text-mute">{page * pageSize + 1}–{Math.min((page + 1) * pageSize, items.length)} из {items.length}</p>
          </div>
        )}
        <ul className="overflow-hidden rounded-2xl border border-line bg-surface empty:hidden">
          {visibleItems.map((e) => (
            <li key={e.id} className="border-t border-line first:border-t-0">
              <Link to={`/exercises/${e.id}`} className="list-row flex min-h-[4.6rem] items-center gap-3 px-3 py-2.5 active:opacity-60">
                <span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-bg" aria-hidden="true">
                  <span className={`size-2.5 rounded-full ${groupDot(e.muscleGroup)}`} />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-semibold tracking-tight">{e.name}</span>
                  <span className="mt-0.5 block truncate text-xs text-mute">
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
        {pageCount > 1 && (
          <nav className="mt-4 flex items-center justify-between gap-3" aria-label="Страницы каталога">
            <button
              type="button"
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              disabled={page === 0}
              className="min-h-11 rounded-xl border border-line bg-surface px-4 text-sm font-semibold disabled:opacity-40"
            >
              ← Назад
            </button>
            <span className="num text-sm font-semibold text-mute">{page + 1} / {pageCount}</span>
            <button
              type="button"
              onClick={() => setPage((current) => Math.min(pageCount - 1, current + 1))}
              disabled={page >= pageCount - 1}
              className="min-h-11 rounded-xl border border-line bg-surface px-4 text-sm font-semibold disabled:opacity-40"
            >
              Далее →
            </button>
          </nav>
        )}
      </div>

      {isAdmin && <ExerciseFormSheet open={creating} onClose={() => setCreating(false)} groups={groups} />}
    </>
  )
}
