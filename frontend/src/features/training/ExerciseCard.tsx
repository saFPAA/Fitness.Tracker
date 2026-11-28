import { useState } from 'react'
import type { Exercise, WorkoutExercise } from '../../api/types'
import { Button, ErrorState, Sheet, Skeleton } from '../../components/ui'
import Stepper from '../../components/Stepper'
import { fmtWeight, fromInput, groupDot, plural, toInput } from '../../lib/format'
import { overlaySets, type DisplaySet } from '../../lib/queueLogic'
import { recordOf } from '../../lib/training'
import { useQueueOps } from '../sync/online'
import SetEditSheet from './SetEditSheet'
import { previousSession, useProgress, useRemoveExercise, useSets } from './hooks'
import { useSetActions } from './setActions'

const msg = (e: unknown, fallback: string) => (e instanceof Error ? e.message : fallback)
const SETS: [string, string, string] = ['подход', 'подхода', 'подходов']

export default function ExerciseCard({
  we,
  exercise,
  siblingIds,
  expanded,
  onToggle,
  onLogged,
}: {
  we: WorkoutExercise
  exercise: Exercise | undefined
  siblingIds: number[] // все упражнения этой тренировки с тем же exerciseId
  expanded: boolean
  onToggle: () => void
  onLogged: () => void
}) {
  const setsQ = useSets(we.id)
  const progressQ = useProgress(we.exerciseId, expanded) // у свёрнутых карточек историю не грузим
  const actions = useSetActions(we.id)
  const ops = useQueueOps()
  const remove = useRemoveExercise(we.workoutId)

  const [draft, setDraft] = useState<{ reps: string; weight: string } | null>(null)
  const [editing, setEditing] = useState<DisplaySet | null>(null)
  const [confirm, setConfirm] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [record, setRecord] = useState<string | null>(null)

  // сохранённые подходы плюс ещё не отправленные изменения из очереди
  const serverSets = setsQ.data ?? []
  const sets = overlaySets(serverSets, ops, we.id)
  const past = (progressQ.data ?? []).filter((s) => !siblingIds.includes(s.workoutExerciseId))
  const prev = previousSession(progressQ.data ?? [], siblingIds)

  // Подсказка: последний подход этой тренировки, иначе первый подход прошлой сессии
  const last = sets[sets.length - 1]
  const seed = last ?? prev[0]
  const reps = draft?.reps ?? (seed ? String(seed.repsDone) : '')
  const weight = draft?.weight ?? (seed ? toInput(seed.weight) : '')
  const edit = (patch: Partial<{ reps: string; weight: string }>) => setDraft({ reps, weight, ...patch })

  const log = () => {
    setError(null)
    setRecord(null)
    const r = fromInput(reps)
    const w = weight.trim() === '' ? 0 : fromInput(weight)
    if (reps.trim() === '' || !Number.isInteger(r) || r < 0) return setError('Укажите число повторов')
    if (!Number.isFinite(w) || w < 0) return setError('Некорректный вес')
    // рекорд сравниваем с прошлыми тренировками и уже записанными подходами; без загруженной истории не сравниваем
    const kind = progressQ.isSuccess ? recordOf([...past, ...sets], r, w) : null
    try {
      actions.addSet({ position: sets.reduce((m, s) => Math.max(m, s.position), 0) + 1, repsDone: r, weight: w })
      setDraft({ reps: String(r), weight: toInput(w) })
      if (kind) setRecord(kind === 'weight' ? 'Новый рекорд веса!' : 'Новый рекорд расчётного максимума!')
      onLogged()
    } catch (e) {
      setError(msg(e, 'Не удалось записать подход'))
    }
  }

  const summary = sets.map((s) => `${s.repsDone}×${fmtWeight(s.weight)}`).join(' · ')

  return (
    <section className="overflow-hidden rounded-2xl border border-line bg-surface">
      <button
        onClick={onToggle}
        aria-expanded={expanded}
        className="flex min-h-[4.5rem] w-full items-center gap-3 p-4 text-left active:opacity-70"
      >
        <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-bg text-accent" aria-hidden="true">◉</span>
        <div className="min-w-0 flex-1">
          <h2 className={`text-base leading-snug font-semibold tracking-tight ${expanded ? 'break-words' : 'truncate'}`}>
            {exercise?.name ?? `Упражнение #${we.exerciseId}`}
          </h2>
          {exercise && (
            <p className="mt-0.5 flex items-center gap-2 text-sm text-mute">
              <span className={`size-2.5 rounded-full ${groupDot(exercise.muscleGroup)}`} aria-hidden="true" />
              {exercise.muscleGroup}
            </p>
          )}
          {!expanded && sets.length > 0 && (
            <p className="num mt-1 truncate text-sm">
              {sets.length} {plural(sets.length, SETS)} · {summary}
            </p>
          )}
        </div>
        {!expanded && sets.length > 0 && (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0 text-accent" aria-label="Есть записанные подходы" role="img">
            <path d="M5 12.5l4.5 4.5L19 7.5" />
          </svg>
        )}
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={`shrink-0 text-mute transition-transform ${expanded ? 'rotate-90' : ''}`} aria-hidden="true">
          <path d="M9 6l6 6-6 6" />
        </svg>
      </button>

      {expanded && (
        <div className="px-4 pb-4">
          {prev.length > 0 && (
            <p className="num text-sm text-mute">
              Прошлый раз: {prev.slice(0, 4).map((s) => `${s.repsDone}×${fmtWeight(s.weight)}`).join(' · ')}
              {prev.length > 4 ? ' …' : ''}
            </p>
          )}

          <div className="mt-2">
            {setsQ.isPending && <Skeleton className="h-12" />}
            {setsQ.isError && <ErrorState message={setsQ.error.message} onRetry={() => void setsQ.refetch()} />}
            {setsQ.isSuccess && sets.length === 0 && <p className="py-2 text-sm text-mute">Подходов пока нет.</p>}
            <ul>
              {sets.map((s, i) => (
                <li key={s.id} className="border-t border-line first:border-t-0">
                  <button
                    onClick={() => setEditing(s)}
                    aria-label={`Подход ${i + 1}: изменить${s.failed ? ', не отправлен' : s.pending ? ', ждёт отправки' : ''}`}
                    className="flex min-h-12 w-full items-center gap-4 py-1 text-left active:opacity-60"
                  >
                    <span className="num w-6 text-mute">{i + 1}</span>
                    <span className="num text-2xl font-bold">
                      {s.repsDone} × {fmtWeight(s.weight)}
                      <span className="ml-1 text-sm font-medium text-mute">кг</span>
                    </span>
                    {s.failed ? (
                      <span className="ml-auto text-xs text-danger">не отправлен</span>
                    ) : s.pending ? (
                      <span className="ml-auto text-xs text-mute">ждёт отправки</span>
                    ) : null}
                  </button>
                </li>
              ))}
            </ul>
          </div>

          <div className="mt-3 flex flex-col gap-3 border-t border-line pt-4">
            <Stepper label="Повторы" value={reps} onChange={(v) => edit({ reps: v })} step={1} inputMode="numeric" />
            <Stepper label="Вес, кг" value={weight} onChange={(v) => edit({ weight: v })} step={2.5} inputMode="decimal" />
            {error && <p role="alert" className="text-sm text-danger">{error}</p>}
            {record && (
              <p role="status" className="rounded-xl border border-accent px-4 py-3 text-center font-semibold text-accent">
                ★ {record}
              </p>
            )}
            <Button onClick={log} disabled={setsQ.isPending}>
              Записать подход {sets.length + 1}
            </Button>
          </div>

          <button onClick={() => setConfirm(true)} className="mt-4 min-h-11 text-sm font-medium text-danger">
            Убрать упражнение из тренировки
          </button>
        </div>
      )}

      <SetEditSheet set={editing} onClose={() => setEditing(null)} />

      <Sheet
        open={confirm}
        onClose={() => {
          setConfirm(false)
          remove.reset()
        }}
        title="Убрать упражнение?"
      >
        <p className="text-mute">Вместе с ним удалятся все подходы этого упражнения в тренировке.</p>
        {remove.isError && (
          <p role="alert" className="mt-4 rounded-xl border border-danger px-4 py-3 text-sm text-danger">
            {remove.error.message}. Нажмите «Убрать» ещё раз, чтобы повторить.
          </p>
        )}
        <div className="mt-6 flex flex-col gap-3">
          <Button variant="danger" onClick={() => remove.mutate(we.id)} disabled={remove.isPending}>
            {remove.isPending ? 'Убираем…' : 'Убрать'}
          </Button>
          <Button variant="secondary" onClick={() => setConfirm(false)} disabled={remove.isPending}>
            Отмена
          </Button>
        </div>
      </Sheet>
    </section>
  )
}
