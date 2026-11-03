import { api } from './client'
import type { Page, Workout, WorkoutExercise, WorkoutSet } from './types'

export const workoutsApi = {
  byUser: (userId: number, pageNumber: number, pageSize: number) =>
    api<Page<Workout>>(`/api/workout/user/${userId}?pageNumber=${pageNumber}&pageSize=${pageSize}`),
  /** Все тренировки пользователя: страницы по 100, пока сервер не скажет, что хватит. */
  async all(userId: number): Promise<Workout[]> {
    const out: Workout[] = []
    for (let page = 0; page < 50; page++) {
      const p = await workoutsApi.byUser(userId, page, 100)
      out.push(...p.content)
      if (p.last === true || p.content.length < 100) break
    }
    return out.sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id)
  },
  get: (id: number) => api<Workout>(`/api/workout/${id}`),
  update: (id: number, body: { name: string; date: string }) =>
    api<Workout>(`/api/workout/${id}`, { method: 'PUT', body }),
  create: (body: { name: string; date: string; userId: number }) =>
    api<Workout>('/api/workout', { method: 'POST', body }),

  /**
   * В схеме БД нет ON DELETE CASCADE, поэтому сервер отказывается (409) удалять тренировку с упражнениями.
   * Удаляем снизу вверх: подходы → упражнения → тренировка. Это НЕ атомарно: при сбое посередине
   * тренировка остаётся частично очищенной, повторное удаление доделает работу.
   */
  async removeDeep(id: number) {
    const exercises = await api<WorkoutExercise[]>(`/api/workout-exercise/workout/${id}`)
    const setsPerExercise = await Promise.all(
      exercises.map((e) => api<WorkoutSet[]>(`/api/workout-set/workout-exercise/${e.id}`)),
    )
    await Promise.all(setsPerExercise.flat().map((s) => api(`/api/workout-set/${s.id}`, { method: 'DELETE' })))
    await Promise.all(exercises.map((e) => api(`/api/workout-exercise/${e.id}`, { method: 'DELETE' })))
    await api(`/api/workout/${id}`, { method: 'DELETE' })
  },
}
