import { api } from './client'
import type { Exercise } from './types'

/** PUT на бэкенде записывает null во все не пришедшие поля, поэтому всегда отправляем упражнение целиком. */
export interface ExerciseInput {
  name: string
  muscleGroup: string
  equipment: string | null
  description: string | null
  videoUrl: string | null
  imageUrl: string | null
}

export const exercisesApi = {
  create: (body: ExerciseInput) => api<Exercise>('/api/exercise', { method: 'POST', body }),
  update: (id: number, body: ExerciseInput) => api<Exercise>(`/api/exercise/${id}`, { method: 'PUT', body }),
  remove: (id: number) => api(`/api/exercise/${id}`, { method: 'DELETE' }),
}
