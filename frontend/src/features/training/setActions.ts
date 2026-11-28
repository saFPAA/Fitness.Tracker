import { ApiError } from '../../api/client'
import * as queue from '../../lib/offlineQueue'
import type { DisplaySet } from '../../lib/queueLogic'
import { useQueueOps } from '../sync/online'
import { useDeleteSet, useUpdateSet } from './hooks'

const noConnection = (e: unknown) => e instanceof ApiError && e.status === 0

/**
 * Новый подход сохраняется в очереди до первой отправки. Правки и удаления серверных
 * подходов отправляются сразу, а при потере связи попадают в очередь.
 */
export function useSetActions(weId: number) {
  const update = useUpdateSet(weId)
  const del = useDeleteSet(weId)
  const ops = useQueueOps()

  const hasPendingFor = (setId: number) =>
    ops.some((o) => (o.kind === 'update' || o.kind === 'delete') && o.setId === setId)

  return {
    busy: update.isPending || del.isPending,

    addSet(v: { position: number; repsDone: number; weight: number }) {
      queue.enqueueAdd({ weId, ...v })
    },

    async saveSet(set: DisplaySet, v: { repsDone: number; weight: number }) {
      const body = { position: set.position, ...v }
      if (set.id < 0) return queue.editAdd(set.id, body) // создание ещё не подтверждено
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
