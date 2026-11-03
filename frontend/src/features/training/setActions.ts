import { ApiError } from '../../api/client'
import * as queue from '../../lib/offlineQueue'
import type { DisplaySet } from '../../lib/queueLogic'
import { useQueueOps } from '../sync/online'
import { useAddSet, useDeleteSet, useUpdateSet } from './hooks'

const noConnection = (e: unknown) => e instanceof ApiError && e.status === 0

/**
 * Запись, правка и удаление подходов. Пока связь есть, всё уходит на сервер сразу.
 * Если связи нет, изменение ставится в очередь на телефоне и отправится позже; в интерфейсе оно видно сразу.
 */
export function useSetActions(weId: number) {
  const add = useAddSet(weId)
  const update = useUpdateSet(weId)
  const del = useDeleteSet(weId)
  const ops = useQueueOps()

  const hasPendingFor = (setId: number) =>
    ops.some((o) => (o.kind === 'update' || o.kind === 'delete') && o.setId === setId)

  return {
    busy: add.isPending || update.isPending || del.isPending,
    addPending: add.isPending,

    /** knownIds: подходы на сервере на момент записи, нужны для защиты от дубля при потерянном ответе. */
    async addSet(v: { position: number; repsDone: number; weight: number }, knownIds: number[]) {
      // пока в очереди есть изменения этого упражнения, новое тоже идёт в очередь, чтобы порядок не смешался
      if (ops.some((o) => o.weId === weId)) return queue.enqueueAdd({ weId, ...v })
      try {
        await add.mutateAsync({ workoutExerciseId: weId, ...v })
      } catch (e) {
        if (noConnection(e)) return queue.enqueueAdd({ weId, ...v, attempted: true, baseIds: knownIds })
        throw e
      }
    },

    async saveSet(set: DisplaySet, v: { repsDone: number; weight: number }) {
      const body = { position: set.position, ...v }
      if (set.id < 0) return queue.editAdd(set.id, body) // ещё не отправлен: правим запись в очереди
      if (hasPendingFor(set.id)) return queue.enqueueUpdate({ weId, setId: set.id, ...body })
      try {
        await update.mutateAsync({ id: set.id, ...body })
      } catch (e) {
        if (noConnection(e)) return queue.enqueueUpdate({ weId, setId: set.id, ...body })
        throw e
      }
    },

    async removeSet(set: DisplaySet) {
      if (set.id < 0) return queue.removeAdd(set.id)
      if (hasPendingFor(set.id)) return queue.enqueueDelete({ weId, setId: set.id })
      try {
        await del.mutateAsync(set.id)
      } catch (e) {
        if (noConnection(e)) return queue.enqueueDelete({ weId, setId: set.id })
        throw e
      }
    },
  }
}
