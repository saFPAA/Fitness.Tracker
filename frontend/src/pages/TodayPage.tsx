import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, ErrorState, Skeleton } from '../components/ui'
import InstallHint from '../features/sync/InstallHint'
import NewWorkoutSheet from '../features/workouts/NewWorkoutSheet'
import WorkoutCard from '../features/workouts/WorkoutCard'
import { useRecentWorkouts } from '../features/workouts/hooks'
import { fullDate, mondayOf, todayISO } from '../lib/dates'
import { useAuth } from '../features/auth/AuthContext'
import { plural } from '../lib/format'
import ProfileIcon from '../components/ProfileIcon'

export default function TodayPage() {
  const [sheet, setSheet] = useState(false)
  const q = useRecentWorkouts()
  const username = useAuth().user?.username ?? ''
  const today = todayISO()
  const all = q.data?.content ?? []
  const todays = all.filter((w) => w.date === today)
  const earlier = all.filter((w) => w.date !== today).slice(0, 5)
  const weekCount = all.filter((w) => w.date >= mondayOf(today) && w.date <= today).length

  return (
    <>
      <div className="mb-5 flex items-center justify-between gap-3">
        <div>
          <p className="page-kicker">{fullDate(today)}</p>
          <h1 className="page-title">Привет, {username}</h1>
        </div>
        <Link to="/profile" aria-label="Открыть профиль" className="grid size-11 shrink-0 place-items-center rounded-2xl bg-surface text-accent">
          <ProfileIcon />
        </Link>
      </div>
      <div className="dashboard-hero mb-5 p-4">
        <div className="flex items-center justify-between gap-4">
          <div>
            <p className="page-kicker">НЕДЕЛЬНАЯ АКТИВНОСТЬ</p>
            <p className="mt-2 text-xl font-bold tracking-tight">{weekCount} {plural(weekCount, ['тренировка', 'тренировки', 'тренировок'])}</p>
            <p className="mt-1 text-sm text-mute">Сильнее, чем вчера.</p>
          </div>
          <div className="grid size-[4.25rem] shrink-0 place-items-center rounded-full border-[5px] border-accent/80 text-lg font-bold text-accent">
            {weekCount}
          </div>
        </div>
      </div>
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
            <section className="mt-5">
              <div className="mb-3 flex items-baseline justify-between">
                <h2 className="text-lg font-semibold">Сегодня</h2>
                <span className="text-xs text-mute">{todays.length} {plural(todays.length, ['тренировка', 'тренировки', 'тренировок'])}</span>
              </div>
              <ul className="flex flex-col gap-3">
              {todays.map((w) => (
                <li key={w.id}>
                  <WorkoutCard workout={w} />
                </li>
              ))}
              </ul>
            </section>
          )}

          <Button
            variant={todays.length ? 'secondary' : 'primary'}
            onClick={() => setSheet(true)}
            className={`mt-4 w-full rounded-2xl text-left ${todays.length ? 'min-h-14' : 'min-h-[4.5rem] text-base'}`}
          >
            <span className="flex items-center justify-between gap-4">
              <span>
                {!todays.length && <span className="mb-1 block text-[10px] tracking-[0.16em] opacity-75">ПЛАН НА ДЕНЬ</span>}
                {todays.length ? 'Добавить тренировку' : 'Начать тренировку'}
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
