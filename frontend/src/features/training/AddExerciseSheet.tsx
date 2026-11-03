import { useState } from 'react'
import type { Exercise } from '../../api/types'
import { ErrorState, Field, Sheet, Skeleton } from '../../components/ui'
import { groupDot } from '../../lib/format'
import { useCatalog } from './hooks'

function Picker({ onPick }: { onPick: (e: Exercise) => Promise<void> }) {
  const catalog = useCatalog()
  const [query, setQuery] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const q = query.trim().toLowerCase()
  const items = (catalog.data ?? []).filter(
    (e) => !q || e.name.toLowerCase().includes(q) || e.muscleGroup.toLowerCase().includes(q),
  )

  const pick = async (e: Exercise) => {
    setBusy(true)
    setError(null)
    try {
      await onPick(e)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Не удалось добавить упражнение')
      setBusy(false)
    }
  }

  return (
    <>
      <Field label="Поиск" type="search" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Название или группа мышц" autoComplete="off" />
      {error && <p role="alert" className="mt-3 rounded-xl border border-danger px-4 py-3 text-sm text-danger">{error}</p>}
      <div className="mt-3">
        {catalog.isPending && <Skeleton className="h-16" />}
        {catalog.isError && <ErrorState message={catalog.error.message} onRetry={() => void catalog.refetch()} />}
        {catalog.isSuccess && catalog.data.length === 0 && (
          <p className="py-8 text-center text-mute">Каталог пуст. Упражнения нужно добавить в базу.</p>
        )}
        {catalog.isSuccess && catalog.data.length > 0 && items.length === 0 && (
          <p className="py-8 text-center text-mute">Ничего не найдено.</p>
        )}
        <ul className={busy ? 'pointer-events-none opacity-60' : ''}>
          {items.map((e) => (
            <li key={e.id} className="border-t border-line first:border-t-0">
              <button onClick={() => void pick(e)} className="flex min-h-14 w-full items-center gap-3 py-2 text-left active:opacity-60">
                <span className={`size-3 shrink-0 rounded-full ${groupDot(e.muscleGroup)}`} aria-hidden="true" />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium">{e.name}</span>
                  <span className="block truncate text-sm text-mute">
                    {e.muscleGroup}{e.equipment ? ` · ${e.equipment}` : ''}
                  </span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </>
  )
}

export default function AddExerciseSheet({
  open,
  onClose,
  onPick,
}: {
  open: boolean
  onClose: () => void
  onPick: (e: Exercise) => Promise<void>
}) {
  return (
    <Sheet open={open} onClose={onClose} title="Добавить упражнение">
      <Picker
        onPick={async (e) => {
          await onPick(e)
          onClose()
        }}
      />
    </Sheet>
  )
}
