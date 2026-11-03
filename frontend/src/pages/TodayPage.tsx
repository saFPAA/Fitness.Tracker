import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, ErrorState, Skeleton } from '../components/ui'
import InstallHint from '../features/sync/InstallHint'
import NewWorkoutSheet from '../features/workouts/NewWorkoutSheet'
import WorkoutCard from '../features/workouts/WorkoutCard'
import { useRecentWorkouts } from '../features/workouts/hooks'
import { fullDate, todayISO } from '../lib/dates'

export default function TodayPage() {
  const [sheet, setSheet] = useState(false)
  const q = useRecentWorkouts()
  const today = todayISO()
  const all = q.data?.content ?? []
  const todays = all.filter((w) => w.date === today)
  const earlier = all.filter((w) => w.date !== today).slice(0, 5)

  return (
    <>
      <p className="mb-2 text-xs font-bold tracking-[0.18em] text-accent">ТРЕНИРОВОЧНЫЙ ДНЕВНИК</p>
      <h1 className="text-4xl font-bold tracking-tight">Сегодня</h1>
      <p className="mt-1 text-mute">{fullDate(today)}: Сильнее, чем вчера.</p>
      <InstallHint />

      {q.isPending && <Skeleton className="mt-6 h-20" />}
      {q.isError && (
        <div className="mt-6">
          <ErrorState message={q.error.message} onRetry={() => void q.refetch()} />
        </div>
      )}

      {q.isSuccess && (
        <>
          {todays.length > 0 && (
            <ul className="mt-6 flex flex-col gap-3">
              {todays.map((w) => (
                <li key={w.id}>
                  <WorkoutCard workout={w} />
                </li>
              ))}
            </ul>
          )}

          <Button
            variant={todays.length ? 'secondary' : 'primary'}
            onClick={() => setSheet(true)}
            className={`mt-6 w-full rounded-2xl text-left ${todays.length ? 'min-h-14' : 'min-h-[5.5rem] text-lg'}`}
          >
            <span className="flex items-center justify-between gap-4">
              <span>
                {!todays.length && <span className="mb-1 block text-[10px] tracking-[0.18em] opacity-75">ГОТОВ К РАБОТЕ?</span>}
                {todays.length ? 'Ещё одна тренировка' : 'Начать тренировку'}
              </span>
              <span aria-hidden="true" className="text-2xl">↗</span>
            </span>
          </Button>

          {all.length === 0 && (
            <p className="mt-10 text-center text-mute">Пока ни одной тренировки. Начните первую!</p>
          )}

          {earlier.length > 0 && (
            <section className="mt-10">
              <div className="mb-3 flex items-baseline justify-between">
                <h2 className="text-lg font-semibold">Недавние</h2>
                <Link to="/history" className="py-2 text-sm font-medium text-accent">
                  Вся история
                </Link>
              </div>
              <ul className="flex flex-col gap-3">
                {earlier.map((w) => (
                  <li key={w.id}>
                    <WorkoutCard workout={w} />
                  </li>
                ))}
              </ul>
            </section>
          )}
        </>
      )}

      <NewWorkoutSheet open={sheet} onClose={() => setSheet(false)} />
    </>
  )
}
