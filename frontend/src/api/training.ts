import { ApiError, api } from './client'
import { planReorder } from '../lib/reorder'
import type { Exercise, Page, WorkoutExercise, WorkoutSet } from './types'

export const trainingApi = {
  /** Весь каталог: страницы по 100, пока сервер не скажет, что хватит. */
  async catalog(): Promise<Exercise[]> {
    const out: Exercise[] = []
    for (let page = 0; page < 50; page++) {
      const p = await api<Page<Exercise>>(`/api/exercise?pageNumber=${page}&pageSize=100`)
      out.push(...p.content)
      if (p.last === true || p.content.length < 100) break
    }
    return out.sort((a, b) => a.name.localeCompare(b.name, 'ru'))
  },

  workoutExercise: (id: number) => api<WorkoutExercise>(`/api/workout-exercise/${id}`),

  exercisesOf: (workoutId: number) => api<WorkoutExercise[]>(`/api/workout-exercise/workout/${workoutId}`),

  /** order_index уникален внутри тренировки: берём следующий и один раз повторяем при гонке. */
  async addExercise(workoutId: number, exerciseId: number): Promise<WorkoutExercise> {
    for (let attempt = 0; ; attempt++) {
      const list = await trainingApi.exercisesOf(workoutId)
      const orderIndex = list.reduce((m, e) => Math.max(m, e.orderIndex), 0) + 1
      try {
        return await api<WorkoutExercise>('/api/workout-exercise', {
          method: 'POST',
          body: { workoutId, exerciseId, orderIndex },
        })
      } catch (e) {
        if (!(e instanceof ApiError && e.status === 409 && attempt === 0)) throw e
      }
    }
  },

  addExerciseAt: (workoutId: number, exerciseId: number, orderIndex: number) =>
    api<WorkoutExercise>('/api/workout-exercise', { method: 'POST', body: { workoutId, exerciseId, orderIndex } }),

  /** Смена порядка в две фазы (см. planReorder). Не атомарно: при сбое повтор пересчитает план с нуля. */
  async reorderExercises(workoutId: number, orderedIds: number[]) {
    const current = await trainingApi.exercisesOf(workoutId)
    const { park, place } = planReorder(current, orderedIds)
    const put = (u: { id: number; orderIndex: number }) =>
      api<WorkoutExercise>(`/api/workout-exercise/${u.id}`, { method: 'PUT', body: { orderIndex: u.orderIndex } })
    await Promise.all(park.map(put))
    await Promise.all(place.map(put))
  },

  removeExercise: (id: number) => api(`/api/workout-exercise/${id}`, { method: 'DELETE' }),

  setsOf: (workoutExerciseId: number) =>
    api<WorkoutSet[]>(`/api/workout-set/workout-exercise/${workoutExerciseId}`),
  addSet: (body: { workoutExerciseId: number; position: number; repsDone: number; weight: number; requestId: string }) =>
    api<WorkoutSet>('/api/workout-set', { method: 'POST', body }),
  updateSet: (id: number, body: { position: number; repsDone: number; weight: number }) =>
    api<WorkoutSet>(`/api/workout-set/${id}`, { method: 'PUT', body }),
  deleteSet: (id: number) => api(`/api/workout-set/${id}`, { method: 'DELETE' }),

  /** Все подходы пользователя по упражнению, по возрастанию даты тренировки (самой даты в ответе нет). */
  progress: (userId: number, exerciseId: number) =>
    api<WorkoutSet[]>(`/api/progress?userId=${userId}&exerciseId=${exerciseId}`),
}
