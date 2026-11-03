import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { exercisesApi, type ExerciseInput } from '../../api/exercises'
import { trainingApi } from '../../api/training'
import type { Exercise, Workout, WorkoutSet } from '../../api/types'
import { workoutsApi } from '../../api/workouts'
import { todayISO } from '../../lib/dates'
import { keepStale } from '../../lib/keepStale'
import { bestSet, e1rm } from '../../lib/training'
import { useUserId } from '../auth/AuthContext'
import { exercisesKey } from '../training/hooks'

export function useSaveExercise(id?: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (b: ExerciseInput) => (id == null ? exercisesApi.create(b) : exercisesApi.update(id, b)),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['exercises'] })
    },
  })
}

export function useDeleteExercise() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => exercisesApi.remove(id),
    // промис не возвращаем: иначе страница успеет показать «не найдено» до перехода назад
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['exercises'] })
    },
  })
}

/** Сегодня тренировки ещё нет, а название не указано: страница должна его спросить. */
export class NeedNameError extends Error {
  constructor() {
    super('Нужно название тренировки')
  }
}

/** Добавляет упражнение в сегодняшнюю тренировку; если её нет, создаёт с названием, которое ввёл пользователь. */
export function useAddToToday() {
  const qc = useQueryClient()
  const uid = useUserId()
  return useMutation({
    mutationFn: async ({ exercise: ex, name }: { exercise: Exercise; name?: string }) => {
      const today = todayISO()
      const page = await workoutsApi.byUser(uid!, 0, 10)
      let workout = page.content.find((w) => w.date === today)
      if (!workout) {
        const title = name?.trim()
        if (!title) throw new NeedNameError()
        workout = await workoutsApi.create({ name: title, date: today, userId: uid! })
      }
      await trainingApi.addExercise(workout.id, ex.id)
      return workout.id
    },
    onSuccess: (workoutId) => {
      void qc.invalidateQueries({ queryKey: ['workouts'] })
      void qc.invalidateQueries({ queryKey: exercisesKey(workoutId) })
    },
  })
}

export interface Session {
  workout: Workout
  sets: WorkoutSet[]
}

/**
 * История по упражнению. В /api/progress нет дат, поэтому для последних 10 сессий
 * дозапрашиваем упражнение тренировки и саму тренировку. Рекорды считаются по всем подходам.
 */
export function useExerciseHistory(exerciseId: number) {
  const uid = useUserId()
  return useQuery({
    queryKey: ['exerciseHistory', uid, exerciseId],
    enabled: uid != null,
    staleTime: 60_000,
    queryFn: keepStale(async () => {
      const all = await trainingApi.progress(uid!, exerciseId)

      const groups = new Map<number, WorkoutSet[]>()
      for (const s of all) {
        const list = groups.get(s.workoutExerciseId)
        if (list) list.push(s)
        else groups.set(s.workoutExerciseId, [s])
      }

      const recentIds = [...groups.keys()].slice(-10)
      const links = await Promise.allSettled(recentIds.map((id) => trainingApi.workoutExercise(id)))
      const workoutIds = [
        ...new Set(links.flatMap((r) => (r.status === 'fulfilled' ? [r.value.workoutId] : []))),
      ]
      const found = await Promise.allSettled(workoutIds.map((id) => workoutsApi.get(id)))
      const workouts = new Map(found.flatMap((r) => (r.status === 'fulfilled' ? [[r.value.id, r.value] as const] : [])))

      const sessions: Session[] = []
      links.forEach((r, i) => {
        if (r.status !== 'fulfilled') return
        const workout = workouts.get(r.value.workoutId)
        if (!workout) return
        sessions.push({ workout, sets: [...groups.get(recentIds[i])!].sort((a, b) => a.position - b.position) })
      })
      sessions.sort((a, b) => b.workout.date.localeCompare(a.workout.date) || b.workout.id - a.workout.id)

      return {
        maxSet: bestSet(all),
        bestE1rm: all.reduce((m, s) => Math.max(m, e1rm(s.repsDone, s.weight)), 0),
        totalSets: all.length,
        sessionCount: groups.size,
        sessions,
      }
    }),
  })
}
