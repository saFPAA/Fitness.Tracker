// Даты тренировок — LocalDate без часового пояса ("2026-10-16"). Всё считаем в локальном времени,
// иначе toISOString() вечером сдвинет «сегодня» на другой день.
export function toISO(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export function todayISO(): string {
  return toISO(new Date())
}

export function parseISO(s: string): Date {
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function addDays(iso: string, n: number): string {
  const d = parseISO(iso)
  d.setDate(d.getDate() + n)
  return toISO(d)
}

/** Понедельник недели, в которую попадает дата (неделя с понедельника). */
export function mondayOf(iso: string): string {
  const d = parseISO(iso)
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7))
  return toISO(d)
}

const fmt = (o: Intl.DateTimeFormatOptions) => new Intl.DateTimeFormat('ru-RU', o)
const fFull = fmt({ weekday: 'long', day: 'numeric', month: 'long' })
const fMonthShort = fmt({ month: 'short' })
const fMonthYear = fmt({ month: 'long', year: 'numeric' })

export const cap = (s: string) => (s ? s[0].toUpperCase() + s.slice(1) : s)
export const dayNum = (s: string) => parseISO(s).getDate()
export const monthShort = (s: string) => fMonthShort.format(parseISO(s)).replace('.', '')
export const fullDate = (s: string) => cap(fFull.format(parseISO(s)))
export const monthYear = (s: string) => cap(fMonthYear.format(parseISO(s)))
