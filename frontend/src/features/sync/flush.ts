import type { QueryClient } from '@tanstack/react-query'
import { ApiError } from '../../api/client'
import { trainingApi } from '../../api/training'
import type { WorkoutSet } from '../../api/types'
import { fresh, patchOp, removeOp } from '../../lib/offlineQueue'
import { findDelivered, isRetryable, type QueueOp } from '../../lib/queueLogic'

const byPosition = (a: WorkoutSet, b: WorkoutSet) => a.position - b.position
const setsKey = (weId: number) => ['sets', weId]

type Outcome = 'done' | 'failed' | 'retry'

async function attempt(op: QueueOp, qc: QueryClient): Promise<Outcome> {
  try {
    if (op.kind === 'add') {
      const server = await trainingApi.setsOf(op.weId)
      if (findDelivered(server, op)) {
        // прошлая попытка дошла до сервера, хотя ответ потерялся: второй раз не отправляем
        removeOp(op.opId)
        qc.setQueryData(setsKey(op.weId), server)
        return 'done'
      }
      patchOp(op.opId, { attempted: true, baseIds: server.map((s) => s.id) })
      const created = await trainingApi.addSet({
        workoutExerciseId: op.weId,
        position: op.position,
        repsDone: op.repsDone,
        weight: op.weight,
      })
      removeOp(op.opId)
      qc.setQueryData<WorkoutSet[]>(setsKey(op.weId), (old) => [...(old ?? server), created].sort(byPosition))
    } else if (op.kind === 'update') {
      const updated = await trainingApi.updateSet(op.setId, {
        position: op.position,
        repsDone: op.repsDone,
        weight: op.weight,
      })
      removeOp(op.opId)
      qc.setQueryData<WorkoutSet[]>(setsKey(op.weId), (old) =>
        (old ?? []).map((s) => (s.id === updated.id ? updated : s)).sort(byPosition),
      )
    } else {
      try {
        await trainingApi.deleteSet(op.setId)
      } catch (e) {
        if (!(e instanceof ApiError && e.status === 404)) throw e // уже удалён: это и нужно было
      }
      removeOp(op.opId)
      qc.setQueryData<WorkoutSet[]>(setsKey(op.weId), (old) => (old ?? []).filter((s) => s.id !== op.setId))
    }
    return 'done'
  } catch (e) {
    const status = e instanceof ApiError ? e.status : 0
    if (isRetryable(status)) {
      patchOp(op.opId, { tries: (op.tries ?? 0) + 1 })
      return 'retry'
    }
    patchOp(op.opId, { failed: e instanceof Error ? e.message : 'Сервер отказал' })
    return 'failed'
  }
}

let running = false

async function run(qc: QueryClient) {
  for (;;) {
    const op = fresh().find((o) => !o.failed)
    if (!op) return
    if ((await attempt(op, qc)) === 'retry') return // связи нет: остальное повторим позже, порядок сохраняем
  }
}

/** Отправляет очередь по порядку. Одновременно работает только одна отправка (и между вкладками тоже). */
export async function flushQueue(qc: QueryClient): Promise<void> {
  if (fresh().every((o) => o.failed)) return
  if (navigator.locks) {
    await navigator.locks.request('fitness-queue', { ifAvailable: true }, async (lock) => {
      if (lock) await run(qc)
    })
    return
  }
  if (running) return
  running = true
  try {
    await run(qc)
  } finally {
    running = false
  }
}
