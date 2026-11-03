interface SetLike {
  repsDone: number
  weight: number
}

/** Расчётный максимум на 1 повторение по формуле Эпли. */
export const e1rm = (reps: number, weight: number) =>
  reps <= 0 || weight <= 0 ? 0 : reps === 1 ? weight : weight * (1 + reps / 30)

export const volume = (sets: SetLike[]) => sets.reduce((sum, s) => sum + s.repsDone * s.weight, 0)

/** Лучший подход: больший вес, при равном весе больше повторов. */
export function bestSet<T extends SetLike>(sets: T[]): T | null {
  return sets.reduce<T | null>(
    (b, s) => (!b || s.weight > b.weight || (s.weight === b.weight && s.repsDone > b.repsDone) ? s : b),
    null,
  )
}

/** Бьёт ли новый подход прошлые. Если прошлых нет, это не рекорд, а первый результат. */
export function recordOf(prior: SetLike[], reps: number, weight: number): 'weight' | 'e1rm' | null {
  if (prior.length === 0 || weight <= 0 || reps <= 0) return null
  if (weight > Math.max(...prior.map((s) => s.weight))) return 'weight'
  if (e1rm(reps, weight) > Math.max(...prior.map((s) => e1rm(s.repsDone, s.weight))) + 1e-9) return 'e1rm'
  return null
}
