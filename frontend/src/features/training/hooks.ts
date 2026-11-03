import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { trainingApi } from '../../api/training'
import { keepStale } from '../../lib/keepStale'
import type { WorkoutExercise, WorkoutSet } from '../../api/types'
import { useUserId } from '../auth/AuthContext'

const byOrder = (a: WorkoutExercise, b: WorkoutExercise) => a.orderIndex - b.orderIndex
const byPosition = (a: WorkoutSet, b: WorkoutSet) => a.position - b.position

export function useCatalog() {
  return useQuery({
    queryKey: ['exercises', 'catalog'],
    queryFn: keepStale(() => trainingApi.catalog()),
    staleTime: 10 * 60_000,
  })
}

export const exercisesKey = (workoutId: number) => ['workout', workoutId, 'exercises']

export function useWorkoutExercises(workoutId: number) {
  return useQuery({
    queryKey: exercisesKey(workoutId),
    queryFn: keepStale(() => trainingApi.exercisesOf(workoutId)),
    enabled: Number.isFinite(workoutId),
  })
}

export function useSets(workoutExerciseId: number) {
  return useQuery({
    queryKey: ['sets', workoutExerciseId],
    queryFn: keepStale(() => trainingApi.setsOf(workoutExerciseId)),
  })
}

/** Подходы прошлых тренировок для подсказки. Некритично: при ошибке подсказки просто нет. */
export function useProgress(exerciseId: number, enabled = true) {
  const uid = useUserId()
  return useQuery({
    queryKey: ['progress', uid, exerciseId],
    queryFn: keepStale(() => trainingApi.progress(uid!, exerciseId)),
    enabled: uid != null && enabled,
    staleTime: 60_000,
  })
}

export function useAddExercise(workoutId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (exerciseId: number) => trainingApi.addExercise(workoutId, exerciseId),
    onSuccess: (created) =>
      qc.setQueryData<WorkoutExercise[]>(exercisesKey(workoutId), (old) => [...(old ?? []), created].sort(byOrder)),
  })
}

export function useRemoveExercise(workoutId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => trainingApi.removeExercise(id),
    onSuccess: (_d, id) => {
      qc.setQueryData<WorkoutExercise[]>(exercisesKey(workoutId), (old) => (old ?? []).filter((e) => e.id !== id))
      qc.removeQueries({ queryKey: ['sets', id] })
    },
  })
}

export function useAddSet(workoutExerciseId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: trainingApi.addSet,
    onSuccess: (created) =>
      qc.setQueryData<WorkoutSet[]>(['sets', workoutExerciseId], (old) => [...(old ?? []), created].sort(byPosition)),
  })
}

export function useUpdateSet(workoutExerciseId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { id: number; position: number; repsDone: number; weight: number }) =>
      trainingApi.updateSet(v.id, { position: v.position, repsDone: v.repsDone, weight: v.weight }),
    onSuccess: (updated) =>
      qc.setQueryData<WorkoutSet[]>(['sets', workoutExerciseId], (old) =>
        (old ?? []).map((s) => (s.id === updated.id ? updated : s)).sort(byPosition),
      ),
  })
}

export function useDeleteSet(workoutExerciseId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => trainingApi.deleteSet(id),
    onSuccess: (_d, id) =>
      qc.setQueryData<WorkoutSet[]>(['sets', workoutExerciseId], (old) => (old ?? []).filter((s) => s.id !== id)),
  })
}

/**
 * Прошлая сессия по этому упражнению. В ответе /api/progress нет даты, но записи идут по возрастанию даты,
 * поэтому берём упражнение тренировки, которому принадлежит последняя запись, и исключаем текущую тренировку.
 */
export function previousSession(all: WorkoutSet[], currentIds: number[]): WorkoutSet[] {
  const others = all.filter((s) => !currentIds.includes(s.workoutExerciseId))
  const last = others[others.length - 1]
  if (!last) return []
  return others.filter((s) => s.workoutExerciseId === last.workoutExerciseId).sort(byPosition)
}

export function useReorderExercises(workoutId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (orderedIds: number[]) => trainingApi.reorderExercises(workoutId, orderedIds),
    // и при успехе, и при сбое перечитываем список; промис возвращаем, чтобы шторка закрылась уже с новым порядком
    onSettled: () => qc.invalidateQueries({ queryKey: exercisesKey(workoutId) }),
  })
}
