import { Link } from 'react-router-dom'
import type { Workout } from '../../api/types'
import { dayNum, fullDate, monthShort } from '../../lib/dates'

export default function WorkoutCard({ workout }: { workout: Workout }) {
  return (
    <Link
      to={`/workout/${workout.id}`}
      className="list-row flex items-center gap-3 rounded-2xl border border-line bg-surface p-3 active:opacity-80"
    >
      <div className="flex size-[3.35rem] shrink-0 flex-col items-center justify-center rounded-2xl bg-bg">
        <span className="num text-xl leading-none font-bold text-accent">{dayNum(workout.date)}</span>
        <span className="mt-1 text-[10px] font-semibold text-mute uppercase">{monthShort(workout.date)}</span>
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate font-semibold tracking-tight">{workout.name}</p>
        <p className="mt-0.5 truncate text-xs text-mute">{fullDate(workout.date)} · открыть тренировку</p>
      </div>
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0 text-mute" aria-hidden="true">
        <path d="M9 6l6 6-6 6" />
      </svg>
    </Link>
  )
}
