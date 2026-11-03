import { session } from './session'
import type { AuthResponse } from './types'

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

// Бэкенд отвечает английскими сообщениями, показываем русские
const RU: Record<string, string> = {
  'Invalid username or password': 'Неверный логин или пароль',
  'Current password is incorrect': 'Неверный текущий пароль',
  'Password must be no longer than 72 UTF-8 bytes': 'Пароль слишком длинный',
  'Username already taken': 'Этот логин уже занят',
  'Email already registered': 'Эта почта уже зарегистрирована',
  'Invalid refresh token': 'Сессия недействительна, войдите снова',
  'Refresh token expired': 'Сессия истекла, войдите снова',
  'Access denied': 'Нет доступа',
  'Something went wrong': 'Что-то пошло не так на сервере',
  'Cannot delete exercise: it is used in existing workouts': 'Упражнение нельзя удалить: оно используется в тренировках',
  'Workout not found': 'Тренировка не найдена',
  'Exercise not found': 'Упражнение не найдено',
  'Workout exercise not found': 'Упражнение в тренировке не найдено',
  'Workout set not found': 'Подход не найден',
  'Order index already used in this workout': 'Это место в тренировке уже занято',
  'Operation violates data integrity constraints': 'Операция нарушает связи данных',
  'You do not have permission to access this resource': 'Нет доступа',
}

const expiredListeners = new Set<() => void>()
/** Вызывается, когда refresh-токен отвергнут и сессию нужно завершить. */
export function onSessionExpired(cb: () => void) {
  expiredListeners.add(cb)
  return () => {
    expiredListeners.delete(cb)
  }
}

const TIMEOUT_MS = 15_000

async function send(path: string, method: string, body?: unknown, token?: string | null) {
  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`
  // На плохой связи запрос иначе может висеть минутами. По таймауту считаем, что связи нет (status 0).
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), TIMEOUT_MS)
  try {
    return await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: ctrl.signal,
    })
  } catch {
    throw new ApiError(0, 'Нет связи с сервером')
  } finally {
    clearTimeout(timer)
  }
}

async function parse<T>(res: Response): Promise<T> {
  const text = await res.text()
  let data: unknown = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    /* тело не JSON */
  }
  if (!res.ok) {
    const raw = (data as { message?: string } | null)?.message
    throw new ApiError(res.status, (raw && RU[raw]) || raw || `Ошибка ${res.status}`)
  }
  return data as T
}

/** Запрос без токена (вход, регистрация, выход). */
export async function apiPublic<T>(path: string, body: unknown): Promise<T> {
  return parse<T>(await send(path, 'POST', body))
}

// ---- обновление токена ----
// Бэкенд считает повторное использование refresh-токена кражей и отзывает ВСЕ токены пользователя.
// Поэтому обновление идёт строго по одному: внутри вкладки через общий промис, между вкладками через Web Locks.
let inflight: Promise<boolean> | null = null

async function doRefresh(usedAccess: string | null): Promise<boolean> {
  if (session.access !== usedAccess) return true // другая вкладка уже обновила токены
  const rt = session.refresh
  if (!rt) return false
  const res = await send('/api/auth/refresh', 'POST', { refreshToken: rt })
  if (res.ok) {
    const j = (await res.json()) as AuthResponse
    session.set(j.accessToken, j.refreshToken)
    return true
  }
  if (res.status === 401 || res.status === 400) return false
  throw new ApiError(res.status, `Ошибка ${res.status}`)
}

function refreshTokens(usedAccess: string | null): Promise<boolean> {
  inflight ??= (async () => {
    try {
      return navigator.locks
        ? await navigator.locks.request('fitness-refresh', () => doRefresh(usedAccess))
        : await doRefresh(usedAccess)
    } finally {
      inflight = null
    }
  })()
  return inflight
}

/** Запрос с токеном. При 401 один раз обновляет токен и повторяет запрос. */
export async function api<T = void>(
  path: string,
  opts: { method?: string; body?: unknown } = {},
): Promise<T> {
  const method = opts.method ?? 'GET'
  const used = session.access
  let res = await send(path, method, opts.body, used)

  if (res.status === 401 && session.refresh) {
    if (await refreshTokens(used)) {
      res = await send(path, method, opts.body, session.access)
    } else {
      session.clear()
      expiredListeners.forEach((f) => f())
      throw new ApiError(401, 'Сессия истекла, войдите снова')
    }
  }
  return parse<T>(res)
}
