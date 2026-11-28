import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, ErrorState, Sheet, Skeleton } from '../components/ui'
import AddExerciseSheet from '../features/training/AddExerciseSheet'
import StatusBar from '../features/sync/StatusBar'
import ReorderSheet from '../features/training/ReorderSheet'
import ExerciseCard from '../features/training/ExerciseCard'
import { RestTimerBar, useRestTimer } from '../features/training/RestTimer'
import { useAddExercise } from '../features/training/hooks'
import EditWorkoutSheet from '../features/workouts/EditWorkoutSheet'
import { useDeleteWorkout, useRepeatWorkout, useWorkoutDetails } from '../features/workouts/hooks'
import { fullDate } from '../lib/dates'
import { plural } from '../lib/format'
import { getRestSeconds } from '../lib/settings'
import { useBack } from '../lib/useBack'
import { useWakeLock } from '../lib/useWakeLock'

export default function WorkoutPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const back = useBack()
  const q = useWorkoutDetails(id)
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

  const exercises = q.data?.exercises ?? []
  const setCount = exercises.reduce((total, exercise) => total + exercise.sets.length, 0)

  // По умолчанию раскрыто первое упражнение без подходов, а если все начаты, последнее.
  const firstEmpty = exercises.find((exercise) => exercise.sets.length === 0)
  const defaultOpen = (firstEmpty ?? exercises[exercises.length - 1])?.id ?? null
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
          <p className="page-kicker mt-2">ТРЕНИРОВКА · {fullDate(q.data.workout.date)}</p>
          <h1 className="page-title break-words">{q.data.workout.name}</h1>
          <button onClick={() => setEditingMeta(true)} className="-ml-2 mt-1 min-h-10 px-2 text-xs font-semibold text-accent">
            Изменить название и дату
          </button>

          {setCount > 0 && (
            <dl className="mt-4 grid grid-cols-2 gap-2 text-center">
              {[
                [exercises.length, plural(exercises.length, ['упражнение', 'упражнения', 'упражнений'])],
                [setCount, plural(setCount, ['подход', 'подхода', 'подходов'])],
              ].map(([value, label]) => (
                <div key={label} className="metric-tile rounded-2xl px-2 py-3">
                  <dd className="num text-2xl font-bold text-accent">{value}</dd>
                  <dt className="mt-1 text-xs text-mute">{label}</dt>
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
            {exercises.length === 0 && (
              <p className="rounded-2xl border border-dashed border-line bg-surface/40 px-4 py-10 text-center text-mute">
                Добавь первое упражнение — и начни фиксировать подходы.
              </p>
            )}
            {exercises.map((we) => (
              <ExerciseCard
                key={we.id}
                we={we}
                expanded={open === we.id}
                onToggle={() => setOpenId(open === we.id ? null : we.id)}
                onLogged={() => {
                  const seconds = getRestSeconds()
                  if (seconds > 0) timer.start(seconds)
                }}
              />
            ))}
          </div>

          <Button onClick={() => setPicker(true)} className="mt-4 w-full">
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
              const created = await addExercise.mutateAsync(e)
              setOpenId(created.id) // новое упражнение сразу раскрыто, остальные сворачиваются
            }}
          />

          <EditWorkoutSheet open={editingMeta} onClose={() => setEditingMeta(false)} workout={q.data.workout} />

          <ReorderSheet
            open={ordering}
            onClose={() => setOrdering(false)}
            workoutId={id}
            exercises={exercises}
            labels={new Map(exercises.map((we) => [we.id, we.name]))}
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
              «{q.data.workout.name}» будет удалена вместе со всеми упражнениями и подходами. Это нельзя отменить.
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
