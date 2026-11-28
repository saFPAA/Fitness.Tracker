import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { trainingApi } from '../../api/training'
import { keepStale } from '../../lib/keepStale'
import type { Exercise, WorkoutDetails, WorkoutSet } from '../../api/types'
import { useUserId } from '../auth/AuthContext'
import { exerciseHistoryKey, updateWorkoutSets, workoutDetailsKey } from '../workouts/cache'

const byPosition = (a: WorkoutSet, b: WorkoutSet) => a.position - b.position

export function useCatalog() {
  return useQuery({
    queryKey: ['exercises', 'catalog'],
    queryFn: keepStale(() => trainingApi.catalog()),
    staleTime: 10 * 60_000,
  })
}

export function usePreviousWorkout(exerciseId: number, workoutId: number, enabled: boolean) {
  const uid = useUserId()
  return useQuery({
    queryKey: [...exerciseHistoryKey, uid, exerciseId, 'previous', workoutId],
    queryFn: keepStale(() => trainingApi.history(uid!, exerciseId, 0, 1, workoutId)),
    enabled: uid != null && enabled,
    staleTime: 60_000,
  })
}

export function useAddExercise(workoutId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (exercise: Exercise) => trainingApi.addExercise(workoutId, exercise.id),
    onSuccess: async (created, exercise) => {
      const queryKey = workoutDetailsKey(workoutId)
      await qc.cancelQueries({ queryKey })
      qc.setQueryData<WorkoutDetails>(queryKey, (old) => old && {
        ...old,
        exercises: [...old.exercises, { ...created, name: exercise.name, muscleGroup: exercise.muscleGroup, sets: [] }]
          .sort((a, b) => a.orderIndex - b.orderIndex),
      })
      void qc.invalidateQueries({ queryKey, refetchType: 'none' })
    },
  })
}

export function useRemoveExercise(workoutId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => trainingApi.removeExercise(id),
    onSuccess: async (_d, id) => {
      const queryKey = workoutDetailsKey(workoutId)
      await qc.cancelQueries({ queryKey })
      qc.setQueryData<WorkoutDetails>(queryKey, (old) => old && {
        ...old,
        exercises: old.exercises.filter((exercise) => exercise.id !== id),
      })
      qc.removeQueries({ queryKey: ['sets', id] })
      void qc.invalidateQueries({ queryKey: exerciseHistoryKey, refetchType: 'none' })
    },
  })
}

export function useUpdateSet(workoutExerciseId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { id: number; position: number; repsDone: number; weight: number }) =>
      trainingApi.updateSet(v.id, { position: v.position, repsDone: v.repsDone, weight: v.weight }),
    onSuccess: async (updated) => {
      await updateWorkoutSets(qc, workoutExerciseId, (sets) =>
        sets.map((set) => set.id === updated.id ? updated : set).sort(byPosition),
      )
      void qc.invalidateQueries({ queryKey: exerciseHistoryKey, refetchType: 'none' })
    },
  })
}

export function useDeleteSet(workoutExerciseId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => trainingApi.deleteSet(id),
    onSuccess: async (_d, id) => {
      await updateWorkoutSets(qc, workoutExerciseId, (sets) => sets.filter((set) => set.id !== id))
      void qc.invalidateQueries({ queryKey: exerciseHistoryKey, refetchType: 'none' })
    },
  })
}

export function useReorderExercises(workoutId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (orderedIds: number[]) => trainingApi.reorderExercises(workoutId, orderedIds),
    // и при успехе, и при сбое перечитываем список; промис возвращаем, чтобы шторка закрылась уже с новым порядком
    onSettled: () => qc.invalidateQueries({ queryKey: workoutDetailsKey(workoutId) }),
  })
}
