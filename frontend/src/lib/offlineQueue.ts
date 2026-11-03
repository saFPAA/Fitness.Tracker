import {
  editAdd as editAddOp,
  enqueueDelete as enqueueDeleteOp,
  removeAdd as removeAddOp,
  upsertUpdate as upsertUpdateOp,
  type AddOp,
  type QueueOp,
} from './queueLogic'

// Очередь лежит в localStorage отдельно для каждого пользователя и переживает перезапуск приложения.
// Перед каждой записью читаем актуальное состояние из хранилища: вкладок может быть несколько.
let uid: number | null = null
let ops: QueueOp[] = []
const listeners = new Set<() => void>()
let flusher: (() => void) | null = null

const key = (id: number) => `fitness.queue.${id}`
export const queueKey = (id: number) => key(id)

const newId = () => Math.random().toString(36).slice(2) + Date.now().toString(36) // randomUUID нужен HTTPS
const newTempId = () => -(Math.floor(Math.random() * 1_000_000_000) + 1)

function read(id: number): QueueOp[] {
  try {
    const parsed: unknown = JSON.parse(localStorage.getItem(key(id)) ?? '[]')
    return Array.isArray(parsed) ? (parsed as QueueOp[]) : []
  } catch {
    return []
  }
}

function emit() {
  listeners.forEach((l) => l())
}

function commit(next: QueueOp[]) {
  ops = next
  if (uid != null) {
    try {
      localStorage.setItem(key(uid), JSON.stringify(next))
    } catch {
      /* хранилище переполнено или недоступно: очередь останется только в памяти */
    }
  }
  emit()
}

/** Актуальное состояние из хранилища (другая вкладка могла его изменить). */
export function fresh(): QueueOp[] {
  if (uid != null) {
    const next = read(uid)
    if (JSON.stringify(next) !== JSON.stringify(ops)) {
      ops = next // другая вкладка изменила очередь
      emit()
    }
  }
  return ops
}

export function bindQueue(id: number | null) {
  uid = id
  ops = id == null ? [] : read(id)
  emit()
}
export function reloadQueue() {
  if (uid != null) {
    ops = read(uid)
    emit()
  }
}

export const getOps = () => ops
export const subscribeQueue = (l: () => void) => {
  listeners.add(l)
  return () => {
    listeners.delete(l)
  }
}

export function setFlusher(fn: (() => void) | null) {
  flusher = fn
}
/** Попробовать отправить очередь чуть позже: сразу после сбоя связи это бессмысленно. */
export function requestFlush(delayMs = 1500) {
  setTimeout(() => flusher?.(), delayMs)
}

// ---- изменения очереди ----
export function enqueueAdd(v: {
  weId: number
  position: number
  repsDone: number
  weight: number
  attempted?: boolean
  baseIds?: number[]
}) {
  const op: AddOp = { kind: 'add', opId: newId(), tempId: newTempId(), ...v }
  commit([...fresh(), op])
  requestFlush()
}

export function enqueueUpdate(v: { weId: number; setId: number; position: number; repsDone: number; weight: number }) {
  commit(upsertUpdateOp(fresh(), { opId: newId(), ...v }))
  requestFlush()
}

export function enqueueDelete(v: { weId: number; setId: number }) {
  commit(enqueueDeleteOp(fresh(), { opId: newId(), ...v }))
  requestFlush()
}

export function editAdd(tempId: number, v: { position: number; repsDone: number; weight: number }) {
  commit(editAddOp(fresh(), tempId, v))
  requestFlush()
}

export function removeAdd(tempId: number) {
  commit(removeAddOp(fresh(), tempId))
}

export function removeOp(opId: string) {
  commit(fresh().filter((o) => o.opId !== opId))
}

export function patchOp(opId: string, patch: Partial<AddOp> & { failed?: string; tries?: number }) {
  commit(fresh().map((o) => (o.opId === opId ? ({ ...o, ...patch } as QueueOp) : o)))
}

/** «Повторить»: снимаем отметку об отказе со всех операций. */
export function retryFailed() {
  commit(fresh().map((o) => ({ ...o, failed: undefined, tries: 0 })))
  requestFlush(0)
}
