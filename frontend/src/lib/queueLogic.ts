import type { WorkoutSet } from '../api/types'

// Очередь неотправленных изменений подходов. Здесь только чистая логика (без хранилища и сети),
// чтобы её можно было проверить тестами: ошибки в очереди означают потерянные или задвоенные подходы.

interface Base {
  opId: string
  weId: number // упражнение тренировки, к которому относится подход
  failed?: string // сервер отказал окончательно: ждёт решения пользователя
  tries?: number
}
export interface AddOp extends Base {
  kind: 'add'
  tempId: number // отрицательный id, под которым подход виден в интерфейсе до отправки
  position: number
  repsDone: number
  weight: number
  /** Запрос мог дойти до сервера, хотя ответ потерялся: для защиты от дубля помним, что там было до него. */
  attempted?: boolean
  baseIds?: number[]
}
export interface UpdateOp extends Base {
  kind: 'update'
  setId: number
  position: number
  repsDone: number
  weight: number
}
export interface DeleteOp extends Base {
  kind: 'delete'
  setId: number
}
export type QueueOp = AddOp | UpdateOp | DeleteOp

export type DisplaySet = WorkoutSet & { pending?: boolean; failed?: string }

/** Серверные подходы плюс ещё не отправленные изменения: то, что пользователь должен видеть. */
export function overlaySets(server: WorkoutSet[], ops: QueueOp[], weId: number): DisplaySet[] {
  let list: DisplaySet[] = server.map((s) => ({ ...s }))
  for (const op of ops) {
    if (op.weId !== weId) continue
    if (op.kind === 'add') {
      list.push({
        id: op.tempId,
        workoutExerciseId: weId,
        position: op.position,
        repsDone: op.repsDone,
        weight: op.weight,
        pending: true,
        failed: op.failed,
      })
    } else if (op.kind === 'update') {
      list = list.map((s) =>
        s.id === op.setId
          ? { ...s, position: op.position, repsDone: op.repsDone, weight: op.weight, pending: true, failed: op.failed }
          : s,
      )
    } else if (op.failed) {
      // удаление не удалось: подход остаётся на сервере, показываем его с отметкой, а не прячем
      list = list.map((s) => (s.id === op.setId ? { ...s, pending: true, failed: op.failed } : s))
    } else {
      list = list.filter((s) => s.id !== op.setId)
    }
  }
  // при равной позиции сохранённые идут раньше неотправленных
  return list.sort((a, b) => a.position - b.position || Number(a.id < 0) - Number(b.id < 0))
}

/** Повторная правка того же подхода заменяет прежнюю, а не копится. */
export function upsertUpdate(
  ops: QueueOp[],
  e: { opId: string; weId: number; setId: number; position: number; repsDone: number; weight: number },
): QueueOp[] {
  const idx = ops.findIndex((o) => o.kind === 'update' && o.setId === e.setId)
  const op: UpdateOp = { kind: 'update', ...e }
  return idx >= 0 ? ops.map((o, i) => (i === idx ? { ...op, opId: o.opId } : o)) : [...ops, op]
}

/** Удаление отменяет ожидающие правки этого подхода. */
export function enqueueDelete(ops: QueueOp[], e: { opId: string; weId: number; setId: number }): QueueOp[] {
  const rest = ops.filter((o) => !(o.kind === 'update' && o.setId === e.setId))
  if (rest.some((o) => o.kind === 'delete' && o.setId === e.setId)) {
    // повторное удаление = «повторить»: снимаем отметку об отказе
    return rest.map((o) => (o.kind === 'delete' && o.setId === e.setId ? { ...o, failed: undefined, tries: 0 } : o))
  }
  return [...rest, { kind: 'delete', ...e }]
}

/**
 * Правка ещё не отправленного подхода. Сбрасываем признак «мог дойти до сервера»: иначе после правки
 * по сигнатуре нельзя узнать доставленный подход. Лучше видимый дубль, чем молча потерянная правка.
 */
export function editAdd(
  ops: QueueOp[],
  tempId: number,
  v: { position: number; repsDone: number; weight: number },
): QueueOp[] {
  return ops.map((o) =>
    o.kind === 'add' && o.tempId === tempId
      ? { ...o, ...v, failed: undefined, tries: 0, attempted: false, baseIds: undefined }
      : o,
  )
}

export function removeAdd(ops: QueueOp[], tempId: number): QueueOp[] {
  return ops.filter((o) => !(o.kind === 'add' && o.tempId === tempId))
}

/** Дошёл ли подход до сервера в прошлую попытку (появился новый подход с теми же значениями). */
export function findDelivered(server: WorkoutSet[], op: AddOp): boolean {
  if (!op.attempted || !op.baseIds) return false
  const base = op.baseIds
  return server.some(
    (s) =>
      !base.includes(s.id) &&
      s.position === op.position &&
      s.repsDone === op.repsDone &&
      Number(s.weight) === op.weight,
  )
}

/** Временный сбой: операцию оставляем в очереди и повторяем позже. Остальное считается отказом. */
export function isRetryable(status: number): boolean {
  return status === 0 || status === 401 || status === 408 || status === 429 || status >= 500
}
