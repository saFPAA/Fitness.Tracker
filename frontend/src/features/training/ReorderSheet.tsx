import { useState } from 'react'
import type { WorkoutExercise } from '../../api/types'
import { Button, Sheet } from '../../components/ui'
import { useReorderExercises } from './hooks'

function List({
  workoutId,
  exercises,
  labels,
  onClose,
}: {
  workoutId: number
  exercises: WorkoutExercise[]
  labels: Map<number, string>
  onClose: () => void
}) {
  const reorder = useReorderExercises(workoutId)
  const [ids, setIds] = useState(() => exercises.map((e) => e.id))
  const [error, setError] = useState<string | null>(null)
  const changed = ids.some((id, i) => id !== exercises[i]?.id)

  const move = (i: number, d: -1 | 1) =>
    setIds((cur) => {
      const j = i + d
      if (j < 0 || j >= cur.length) return cur
      const next = [...cur]
      ;[next[i], next[j]] = [next[j], next[i]]
      return next
    })

  const save = async () => {
    setError(null)
    try {
      await reorder.mutateAsync(ids)
      onClose()
    } catch (e) {
      setError(`${e instanceof Error ? e.message : 'Не удалось изменить порядок'}. Нажмите «Сохранить» ещё раз.`)
    }
  }

  const arrow = 'size-11 shrink-0 rounded-xl border border-line bg-bg text-xl leading-none active:opacity-60 disabled:opacity-30'
  return (
    <>
      <ul>
        {ids.map((id, i) => {
          const label = labels.get(id) ?? 'Упражнение'
          return (
            <li key={id} className="flex items-center gap-2 border-t border-line py-2 first:border-t-0">
              <span className="num w-6 shrink-0 text-mute">{i + 1}</span>
              <span className="min-w-0 flex-1 font-medium break-words">{label}</span>
              <button onClick={() => move(i, -1)} disabled={i === 0 || reorder.isPending} aria-label={`Поднять «${label}» выше`} className={arrow}>↑</button>
              <button onClick={() => move(i, 1)} disabled={i === ids.length - 1 || reorder.isPending} aria-label={`Опустить «${label}» ниже`} className={arrow}>↓</button>
            </li>
          )
        })}
      </ul>
      {error && <p role="alert" className="mt-4 rounded-xl border border-danger px-4 py-3 text-sm text-danger">{error}</p>}
      <div className="mt-6 flex flex-col gap-3">
        <Button onClick={() => void save()} disabled={!changed || reorder.isPending}>
          {reorder.isPending ? 'Сохраняем…' : 'Сохранить порядок'}
        </Button>
        <Button variant="secondary" onClick={onClose} disabled={reorder.isPending}>Отмена</Button>
      </div>
    </>
  )
}

export default function ReorderSheet({
  open,
  onClose,
  workoutId,
  exercises,
  labels,
}: {
  open: boolean
  onClose: () => void
  workoutId: number
  exercises: WorkoutExercise[]
  labels: Map<number, string>
}) {
  return (
    <Sheet open={open} onClose={onClose} title="Порядок упражнений">
      <List workoutId={workoutId} exercises={exercises} labels={labels} onClose={onClose} />
    </Sheet>
  )
}
