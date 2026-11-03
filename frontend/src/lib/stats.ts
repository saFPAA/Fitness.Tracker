import { addDays, mondayOf } from './dates'

export function workoutStats(dates: string[], today: string) {
  const past = dates.filter((d) => d <= today)
  const monday = mondayOf(today)
  const week = past.filter((d) => d >= monday).length
  const month = past.filter((d) => d.slice(0, 7) === today.slice(0, 7)).length

  // Серия недель: пустая текущая неделя серию не рвёт (она ещё не закончилась)
  const weeks = new Set(past.map(mondayOf))
  let cursor = monday
  if (!weeks.has(cursor)) cursor = addDays(cursor, -7)
  let streak = 0
  while (weeks.has(cursor)) {
    streak++
    cursor = addDays(cursor, -7)
  }
  return { week, month, streak }
}
