import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { trainingApi } from '../../api/training'
import type { Workout } from '../../api/types'
import { workoutsApi } from '../../api/workouts'
import { keepStale } from '../../lib/keepStale'
import { todayISO } from '../../lib/dates'
import { useUserId } from '../auth/AuthContext'

/** Последние тренировки для вкладки «Сегодня». */
export function useRecentWorkouts() {
  const uid = useUserId()
  return useQuery({
    queryKey: ['workouts', uid, 'recent'],
    queryFn: keepStale(() => workoutsApi.byUser(uid!, 0, 10)),
    enabled: uid != null,
  })
}

/** Вся история целиком: у тренировки всего четыре поля, так что это дёшево. */
export function useAllWorkouts() {
  const uid = useUserId()
  return useQuery({
    queryKey: ['workouts', uid, 'all'],
    queryFn: keepStale(() => workoutsApi.all(uid!)),
    enabled: uid != null,
  })
}

export function useWorkout(id: number) {
  return useQuery({
    queryKey: ['workout', id],
    queryFn: keepStale(() => workoutsApi.get(id)),
    enabled: Number.isFinite(id),
  })
}

export function useCreateWorkout() {
  const qc = useQueryClient()
  const uid = useUserId()
  return useMutation({
    mutationFn: (v: { name: string; date: string }) => workoutsApi.create({ ...v, userId: uid! }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['workouts'] }),
  })
}

export function useUpdateWorkout(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { name: string; date: string }) => workoutsApi.update(id, v),
    onSuccess: (workout) => {
      qc.setQueryData(['workout', id], workout)
      void qc.invalidateQueries({ queryKey: ['workouts'] })
    },
  })
}

export function useDeleteWorkout() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: workoutsApi.remove,
    onSuccess: (_d, id) => {
      qc.removeQueries({ queryKey: ['workout', id] })
      return qc.invalidateQueries({ queryKey: ['workouts'] })
    },
  })
}

/**
 * Копия тренировки на сегодня: то же название и те же упражнения в том же порядке, подходы пустые.
 * Не атомарно: при обрыве связи новая тренировка может остаться с частью упражнений.
 */
export function useRepeatWorkout() {
  const qc = useQueryClient()
  const uid = useUserId()
  return useMutation({
    mutationFn: async (src: Workout) => {
      const items = await trainingApi.exercisesOf(src.id)
      const copy = await workoutsApi.create({ name: src.name, date: todayISO(), userId: uid! })
      for (const [i, e] of items.entries()) await trainingApi.addExerciseAt(copy.id, e.exerciseId, i + 1)
      return copy.id
    },
    onSettled: () => {
      void qc.invalidateQueries({ queryKey: ['workouts'] })
    },
  })
}
