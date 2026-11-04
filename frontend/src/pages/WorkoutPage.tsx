import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQueries } from '@tanstack/react-query'
import { trainingApi } from '../api/training'
import { Button, ErrorState, Sheet, Skeleton } from '../components/ui'
import AddExerciseSheet from '../features/training/AddExerciseSheet'
import StatusBar from '../features/sync/StatusBar'
import ReorderSheet from '../features/training/ReorderSheet'
import ExerciseCard from '../features/training/ExerciseCard'
import { RestTimerBar, useRestTimer } from '../features/training/RestTimer'
import { useAddExercise, useCatalog, useWorkoutExercises } from '../features/training/hooks'
import EditWorkoutSheet from '../features/workouts/EditWorkoutSheet'
import { useDeleteWorkout, useRepeatWorkout, useWorkout } from '../features/workouts/hooks'
import { fullDate } from '../lib/dates'
import { keepStale } from '../lib/keepStale'
import { plural } from '../lib/format'
import { getRestSeconds } from '../lib/settings'
import { useBack } from '../lib/useBack'
import { useWakeLock } from '../lib/useWakeLock'

export default function WorkoutPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const back = useBack()
  const q = useWorkout(id)
  const exQ = useWorkoutExercises(id)
  const catalog = useCatalog()
  const addExercise = useAddExercise(id)
  const del = useDeleteWorkout()
  const repeat = useRepeatWorkout()
  const timer = useRestTimer()
  useWakeLock() // экран не гаснет, пока открыта тренировка
  const [confirm, setConfirm] = useState(false)
  const [picker, setPicker] = useState(false)
  const [ordering, setOrdering] = useState(false)
  const [editingMeta, setEditingMeta] = useState(false)
  // undefined — пользователь ещё не выбирал, работает выбор по умолчанию; null — всё свёрнуто
  const [openId, setOpenId] = useState<number | null | undefined>(undefined)

  const exercises = exQ.data ?? []
  const byId = new Map((catalog.data ?? []).map((e) => [e.id, e]))

  // Подходы всех упражнений: нужны для итогов и для выбора раскрытого упражнения.
  // Ключи те же, что у useSets в карточках, поэтому запросы общие.
  const setsQs = useQueries({
    queries: exercises.map((we) => ({ queryKey: ['sets', we.id], queryFn: keepStale(() => trainingApi.setsOf(we.id)) })),
  })
  const allLoaded = exQ.isSuccess && setsQs.every((r) => r.isSuccess)
  const allSets = allLoaded ? setsQs.flatMap((r) => r.data ?? []) : []

  // По умолчанию раскрыто первое упражнение без подходов, а если все начаты, последнее
  const firstEmpty = exercises.find((_, i) => setsQs[i]?.data?.length === 0)
  const defaultOpen = allLoaded ? (firstEmpty ?? exercises[exercises.length - 1])?.id ?? null : null
  const open = openId === undefined ? defaultOpen : openId

  const remove = async () => {
    try {
      await del.mutateAsync(id)
      navigate('/', { replace: true })
    } catch {
      /* ошибка показывается в шторке через del.error */
    }
  }

  const copy = async () => {
    if (!q.data) return
    try {
      navigate(`/workout/${await repeat.mutateAsync(q.data)}`)
    } catch {
      /* ошибка показывается под кнопкой через repeat.error */
    }
  }

  return (
    <div
      className={`mx-auto max-w-xl px-4 pt-[calc(env(safe-area-inset-top)+0.75rem)] ${
        timer.endsAt != null ? 'pb-56' : 'pb-[calc(env(safe-area-inset-bottom)+2rem)]'
      }`}
    >
      <StatusBar />
      <button onClick={back} className="-ml-2 min-h-11 px-2 text-base font-medium text-accent">
        ‹ Назад
      </button>

      {q.isPending && <Skeleton className="mt-4 h-24" />}
      {q.isError && (
        <div className="mt-4">
          <ErrorState message={q.error.message} onRetry={() => void q.refetch()} />
        </div>
      )}

      {q.isSuccess && (
        <>
          <h1 className="mt-2 text-3xl font-bold tracking-tight break-words">{q.data.name}</h1>
          <p className="mt-1 text-mute">{fullDate(q.data.date)}</p>
          <button onClick={() => setEditingMeta(true)} className="-ml-2 min-h-11 px-2 text-sm font-medium text-accent">
            Изменить название и дату
          </button>

          {allSets.length > 0 && (
            <dl className="mt-4 grid grid-cols-2 gap-2 text-center">
              {[
                [exercises.length, plural(exercises.length, ['упражнение', 'упражнения', 'упражнений'])],
                [allSets.length, plural(allSets.length, ['подход', 'подхода', 'подходов'])],
              ].map(([value, label]) => (
                <div key={label} className="rounded-2xl border border-line bg-surface px-2 py-3">
                  <dd className="num text-2xl font-bold">{value}</dd>
                  <dt className="text-xs text-mute">{label}</dt>
                </div>
              ))}
            </dl>
          )}

          {exercises.length > 0 && (
            <div className="mt-4">
              <Button variant="secondary" onClick={() => void copy()} disabled={repeat.isPending} className="w-full">
                {repeat.isPending ? 'Копируем…' : 'Повторить эту тренировку'}
              </Button>
              <p className="mt-2 text-center text-sm text-mute">Новая тренировка на сегодня с теми же упражнениями, подходы пустые.</p>
              {repeat.isError && (
                <p role="alert" className="mt-3 rounded-xl border border-danger px-4 py-3 text-sm text-danger">
                  {repeat.error.message}. Новая тренировка могла создаться не полностью, проверьте историю.
                </p>
              )}
            </div>
          )}

          <div className="mt-6 flex flex-col gap-3">
            {exQ.isPending && <Skeleton className="h-20" />}
            {exQ.isError && <ErrorState message={exQ.error.message} onRetry={() => void exQ.refetch()} />}
            {exQ.isSuccess && exercises.length === 0 && (
              <p className="rounded-2xl border border-dashed border-line px-4 py-10 text-center text-mute">
                Добавьте первое упражнение.
              </p>
            )}
            {exercises.map((we) => (
              <ExerciseCard
                key={we.id}
                we={we}
                exercise={byId.get(we.exerciseId)}
                siblingIds={exercises.filter((e) => e.exerciseId === we.exerciseId).map((e) => e.id)}
                expanded={open === we.id}
                onToggle={() => setOpenId(open === we.id ? null : we.id)}
                onLogged={() => {
                  const seconds = getRestSeconds()
                  if (seconds > 0) timer.start(seconds)
                }}
              />
            ))}
          </div>

          <Button onClick={() => setPicker(true)} className="mt-4 w-full" disabled={!exQ.isSuccess}>
            Добавить упражнение
          </Button>
          {exercises.length > 1 && (
            <Button variant="secondary" onClick={() => setOrdering(true)} className="mt-3 w-full">
              Изменить порядок
            </Button>
          )}

          <Button variant="secondary" onClick={() => setConfirm(true)} className="mt-10 w-full text-danger">
            Удалить тренировку
          </Button>

          <AddExerciseSheet
            open={picker}
            onClose={() => setPicker(false)}
            onPick={async (e) => {
              const created = await addExercise.mutateAsync(e.id)
              setOpenId(created.id) // новое упражнение сразу раскрыто, остальные сворачиваются
            }}
          />

          <EditWorkoutSheet open={editingMeta} onClose={() => setEditingMeta(false)} workout={q.data} />

          <ReorderSheet
            open={ordering}
            onClose={() => setOrdering(false)}
            workoutId={id}
            exercises={exercises}
            labels={new Map(exercises.map((we) => [we.id, byId.get(we.exerciseId)?.name ?? `Упражнение #${we.exerciseId}`]))}
          />

          <Sheet
            open={confirm}
            onClose={() => {
              setConfirm(false)
              del.reset()
            }}
            title="Удалить тренировку?"
          >
            <p className="text-mute">
              «{q.data.name}» будет удалена вместе со всеми упражнениями и подходами. Это нельзя отменить.
            </p>
            {del.isError && (
              <p role="alert" className="mt-4 rounded-xl border border-danger px-4 py-3 text-sm text-danger">
                {del.error.message}. Нажмите «Удалить» ещё раз, чтобы повторить.
              </p>
            )}
            <div className="mt-6 flex flex-col gap-3">
              <Button variant="danger" onClick={() => void remove()} disabled={del.isPending}>
                {del.isPending ? 'Удаляем…' : 'Удалить'}
              </Button>
              <Button variant="secondary" onClick={() => setConfirm(false)} disabled={del.isPending}>
                Отмена
              </Button>
            </div>
          </Sheet>
        </>
      )}

      {timer.endsAt != null && (
        <RestTimerBar endsAt={timer.endsAt} onAdjust={timer.adjust} onRestart={timer.start} onStop={timer.stop} />
      )}
    </div>
  )
}
