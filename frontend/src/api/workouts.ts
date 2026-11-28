import { api } from './client'
import type { Page, Workout } from './types'

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

  remove: (id: number) => api(`/api/workout/${id}`, { method: 'DELETE' }),
}
