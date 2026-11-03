import { beforeEach, describe, expect, it, vi } from 'vitest'
import { QueryClient } from '@tanstack/react-query'

// --- окружение: localStorage и «сервер» в памяти ---
const store = new Map<string, string>()
vi.stubGlobal('localStorage', {
  getItem: (k: string) => store.get(k) ?? null,
  setItem: (k: string, v: string) => void store.set(k, v),
  removeItem: (k: string) => void store.delete(k),
})

interface Set_ { id: number; workoutExerciseId: number; position: number; repsDone: number; weight: number }
let server: Set_[] = []
let nextId = 100
let posts = 0
// сценарии сбоев
let netDown = false // любой запрос падает как «нет связи»
let dropNextPostResponse = false // сервер выполнит POST, но ответ потеряется
let failNextPostWith: number | null = null

const json = (status: number, body: unknown) =>
  new Response(body === undefined ? '' : JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

vi.stubGlobal('fetch', async (url: string, init: RequestInit = {}) => {
  if (netDown) throw new TypeError('network')
  const method = init.method ?? 'GET'
  const body = init.body ? JSON.parse(init.body as string) : undefined
  let m: RegExpMatchArray | null
  if (method === 'GET' && (m = url.match(/workout-set\/workout-exercise\/(\d+)$/)))
    return json(200, server.filter((s) => s.workoutExerciseId === +m![1]))
  if (method === 'POST' && url.endsWith('/api/workout-set')) {
    if (failNextPostWith) { const st = failNextPostWith; failNextPostWith = null; return json(st, { message: 'boom' }) }
    posts++
    const created = { id: nextId++, ...body }
    server.push(created)
    if (dropNextPostResponse) { dropNextPostResponse = false; throw new TypeError('response lost') }
    return json(200, created)
  }
  if (method === 'PUT' && (m = url.match(/workout-set\/(\d+)$/))) {
    const s = server.find((x) => x.id === +m![1])
    if (!s) return json(404, { message: 'Workout set not found' })
    Object.assign(s, body)
    return json(200, s)
  }
  if (method === 'DELETE' && (m = url.match(/workout-set\/(\d+)$/))) {
    const i = server.findIndex((x) => x.id === +m![1])
    if (i < 0) return json(404, { message: 'Workout set not found' })
    server.splice(i, 1)
    return json(200, undefined)
  }
  return json(500, { message: 'unexpected ' + method + ' ' + url })
})

const queue = await import('../lib/offlineQueue')
const { flushQueue } = await import('../features/sync/flush')

let qc: QueryClient
beforeEach(() => {
  store.clear()
  server = []
  nextId = 100
  posts = 0
  netDown = false
  dropNextPostResponse = false
  failNextPostWith = null
  qc = new QueryClient()
  queue.bindQueue(1)
})

const add = (position: number, repsDone: number, weight: number, extra = {}) =>
  queue.enqueueAdd({ weId: 7, position, repsDone, weight, ...extra })

describe('отправка очереди', () => {
  it('отправляет накопленные подходы по порядку и очищает очередь', async () => {
    add(1, 10, 60); add(2, 10, 60); add(3, 8, 62.5)
    await flushQueue(qc)
    expect(server.map((s) => s.position)).toEqual([1, 2, 3])
    expect(queue.fresh()).toEqual([])
    expect(JSON.parse(store.get('fitness.queue.1')!)).toEqual([])
    expect((qc.getQueryData(['sets', 7]) as Set_[]).length).toBe(3)
  })

  it('при обрыве связи сохраняет порядок: следующие не отправляются раньше первого', async () => {
    add(1, 10, 60); add(2, 10, 60)
    netDown = true
    await flushQueue(qc)
    expect(server).toEqual([])
    expect(queue.fresh().length).toBe(2)
    netDown = false
    await flushQueue(qc)
    expect(server.map((s) => s.position)).toEqual([1, 2])
    expect(queue.fresh()).toEqual([])
  })

  it('очередь переживает перезапуск приложения', async () => {
    add(1, 10, 60)
    queue.bindQueue(null)
    queue.bindQueue(1) // заново читаем из хранилища
    expect(queue.fresh().length).toBe(1)
    await flushQueue(qc)
    expect(server.length).toBe(1)
  })

  it('не создаёт дубль, если запрос дошёл, а ответ потерялся', async () => {
    add(1, 10, 60)
    dropNextPostResponse = true
    await flushQueue(qc) // POST выполнен на сервере, ответа нет
    expect(server.length).toBe(1)
    expect(queue.fresh().length).toBe(1) // клиент не знает об успехе
    await flushQueue(qc) // повтор должен заметить доставленный подход
    expect(posts).toBe(1)
    expect(server.length).toBe(1)
    expect(queue.fresh()).toEqual([])
  })

  it('не создаёт дубль, если первая попытка шла напрямую и оборвалась (attempted из карточки)', async () => {
    // напрямую: сервер получил подход, ответ потерян; карточка кладёт его в очередь с baseIds
    server.push({ id: 100, workoutExerciseId: 7, position: 1, repsDone: 10, weight: 60 })
    nextId = 101
    add(1, 10, 60, { attempted: true, baseIds: [] })
    await flushQueue(qc)
    expect(posts).toBe(0)
    expect(server.length).toBe(1)
    expect(queue.fresh()).toEqual([])
  })

  it('два одинаковых подхода подряд не принимаются за дубль друг друга', async () => {
    add(1, 10, 60); add(2, 10, 60)
    await flushQueue(qc)
    expect(server.length).toBe(2)
  })

  it('временный сбой сервера (503) оставляет операцию в очереди', async () => {
    add(1, 10, 60)
    failNextPostWith = 503
    await flushQueue(qc)
    expect(queue.fresh().length).toBe(1)
    expect(queue.fresh()[0].failed).toBeUndefined()
    await flushQueue(qc)
    expect(server.length).toBe(1)
  })

  it('окончательный отказ помечает операцию и не блокирует остальные', async () => {
    queue.enqueueUpdate({ weId: 7, setId: 999, position: 1, repsDone: 5, weight: 50 }) // такого подхода нет: 404
    add(1, 10, 60)
    await flushQueue(qc)
    const left = queue.fresh()
    expect(left.length).toBe(1)
    expect(left[0].kind).toBe('update')
    expect(left[0].failed).toBeTruthy()
    expect(server.length).toBe(1) // подход после отказавшей правки всё же ушёл
    await flushQueue(qc) // отказавшая не повторяется сама
    expect(left[0].failed).toBeTruthy()
  })

  it('удаление уже удалённого подхода считается успехом', async () => {
    queue.enqueueDelete({ weId: 7, setId: 555 })
    await flushQueue(qc)
    expect(queue.fresh()).toEqual([])
  })

  it('правка и удаление существующих подходов доходят до сервера', async () => {
    server.push({ id: 1, workoutExerciseId: 7, position: 1, repsDone: 10, weight: 60 })
    server.push({ id: 2, workoutExerciseId: 7, position: 2, repsDone: 10, weight: 60 })
    queue.enqueueUpdate({ weId: 7, setId: 1, position: 1, repsDone: 9, weight: 62.5 })
    queue.enqueueUpdate({ weId: 7, setId: 1, position: 1, repsDone: 8, weight: 65 }) // сливается с предыдущей
    queue.enqueueDelete({ weId: 7, setId: 2 })
    expect(queue.fresh().length).toBe(2)
    await flushQueue(qc)
    expect(server).toEqual([{ id: 1, workoutExerciseId: 7, position: 1, repsDone: 8, weight: 65 }])
  })

  it('«повторить» снимает отметку отказа и пробует снова', async () => {
    queue.enqueueUpdate({ weId: 7, setId: 999, position: 1, repsDone: 5, weight: 50 })
    await flushQueue(qc)
    expect(queue.fresh()[0].failed).toBeTruthy()
    server.push({ id: 999, workoutExerciseId: 7, position: 1, repsDone: 1, weight: 1 })
    queue.retryFailed()
    await flushQueue(qc)
    expect(queue.fresh()).toEqual([])
    expect(server[0].repsDone).toBe(5)
  })

  it('очереди разных пользователей не смешиваются', async () => {
    add(1, 10, 60)
    queue.bindQueue(2)
    expect(queue.fresh()).toEqual([])
    queue.bindQueue(1)
    expect(queue.fresh().length).toBe(1)
  })
})
