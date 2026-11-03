import { Link } from 'react-router-dom'
import { ErrorState, Skeleton } from '../../components/ui'
import { dayNum, fullDate, monthShort } from '../../lib/dates'
import { fmtWeight, plural } from '../../lib/format'
import { bestSet, volume } from '../../lib/training'
import { useExerciseHistory, type Session } from './hooks'

/** Сравнение с предыдущей тренировкой: сначала по лучшему весу, при равенстве по объёму. */
function delta(cur: Session, prev: Session | undefined): string | null {
  if (!prev) return null
  const dw = bestSet(cur.sets)!.weight - bestSet(prev.sets)!.weight
  if (dw > 0) return `↑ +${fmtWeight(dw)} кг к прошлой`
  if (dw < 0) return `↓ −${fmtWeight(-dw)} кг к прошлой`
  const dv = volume(cur.sets) - volume(prev.sets)
  if (dv > 0) return `↑ объём +${fmtWeight(Math.round(dv))} кг`
  if (dv < 0) return `↓ объём −${fmtWeight(Math.round(-dv))} кг`
  return '= без изменений'
}

export default function ExerciseHistory({ exerciseId }: { exerciseId: number }) {
  const history = useExerciseHistory(exerciseId)

  return (
    <section className="mt-10">
      <h2 className="mb-3 text-lg font-semibold">Мои результаты</h2>
      {history.isPending && <Skeleton className="h-24" />}
      {history.isError && <ErrorState message={history.error.message} onRetry={() => void history.refetch()} />}
      {history.isSuccess && history.data.totalSets === 0 && (
        <p className="rounded-2xl border border-dashed border-line px-4 py-8 text-center text-mute">
          Вы ещё не делали это упражнение.
        </p>
      )}
      {history.isSuccess && history.data.totalSets > 0 && (
        <>
          <dl className="grid grid-cols-3 gap-2 text-center">
            <div className="rounded-2xl border border-line bg-surface px-2 py-3">
              <dd className="num text-2xl font-bold">
                {history.data.maxSet ? fmtWeight(history.data.maxSet.weight) : '–'}
                <span className="text-sm font-medium text-mute"> кг</span>
              </dd>
              <dt className="num text-xs text-mute">рекорд × {history.data.maxSet?.repsDone}</dt>
            </div>
            <div className="rounded-2xl border border-line bg-surface px-2 py-3">
              <dd className="num text-2xl font-bold">{history.data.totalSets}</dd>
              <dt className="text-xs text-mute">{plural(history.data.totalSets, ['подход', 'подхода', 'подходов'])}</dt>
            </div>
            <div className="rounded-2xl border border-line bg-surface px-2 py-3">
              <dd className="num text-2xl font-bold">{history.data.sessionCount}</dd>
              <dt className="text-xs text-mute">{plural(history.data.sessionCount, ['тренировка', 'тренировки', 'тренировок'])}</dt>
            </div>
          </dl>
          {history.data.bestE1rm > 0 && (
            <p className="num mt-3 text-sm text-mute">
              Расчётный максимум на 1 повторение: ≈ {fmtWeight(Math.round(history.data.bestE1rm * 10) / 10)} кг
            </p>
          )}

          <ul className="mt-4 flex flex-col gap-3">
            {history.data.sessions.map((s, i) => {
              const d = delta(s, history.data.sessions[i + 1])
              return (
                <li key={`${s.workout.id}-${s.sets[0].workoutExerciseId}`}>
                  <Link to={`/workout/${s.workout.id}`} className="flex gap-4 rounded-2xl border border-line bg-surface p-3 active:opacity-80">
                    <div className="flex size-14 shrink-0 flex-col items-center justify-center rounded-xl bg-bg">
                      <span className="num text-xl leading-none font-bold">{dayNum(s.workout.date)}</span>
                      <span className="mt-1 text-xs text-mute uppercase">{monthShort(s.workout.date)}</span>
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm text-mute">{fullDate(s.workout.date)} · {s.workout.name}</p>
                      <p className="num mt-0.5 font-semibold break-words">
                        {s.sets.map((x) => `${x.repsDone}×${fmtWeight(x.weight)}`).join(' · ')}
                      </p>
                      <p className="num mt-1 text-sm text-mute">
                        Объём {fmtWeight(Math.round(volume(s.sets)))} кг{d ? ` · ${d}` : ''}
                      </p>
                    </div>
                  </Link>
                </li>
              )
            })}
          </ul>
          {history.data.sessionCount > history.data.sessions.length && (
            <p className="mt-3 text-center text-sm text-mute">Показаны последние тренировки. Рекорды считаются по всем.</p>
          )}
        </>
      )}
    </section>
  )
}
