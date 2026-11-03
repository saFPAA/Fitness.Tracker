// Токены живут в localStorage: так перезагрузка страницы не требует лишнего refresh,
// а несколько вкладок видят одну и ту же сессию.
const ACCESS = 'fitness.access'
const REFRESH = 'fitness.refresh'

export const REFRESH_KEY = REFRESH

export const session = {
  get access() {
    return localStorage.getItem(ACCESS)
  },
  get refresh() {
    return localStorage.getItem(REFRESH)
  },
  set(access: string, refresh: string) {
    localStorage.setItem(ACCESS, access)
    localStorage.setItem(REFRESH, refresh)
  },
  clear() {
    localStorage.removeItem(ACCESS)
    localStorage.removeItem(REFRESH)
  },
}

export interface SessionUser {
  id: number
  username: string
  role: 'USER' | 'ADMIN'
}

// Payload JWT — это UTF-8 в base64url, поэтому одного atob() для кириллических логинов мало.
function decodePayload(token: string): Record<string, unknown> | null {
  try {
    const part = token.split('.')[1]
    const b64 = part.replace(/-/g, '+').replace(/_/g, '/')
    const bin = atob(b64 + '='.repeat((4 - (b64.length % 4)) % 4))
    const bytes = Uint8Array.from(bin, (c) => c.charCodeAt(0))
    return JSON.parse(new TextDecoder().decode(bytes))
  } catch {
    return null
  }
}

/** Пользователь из access-токена. Срок действия не проверяется: просроченный токен обновит клиент API. */
export function readUser(): SessionUser | null {
  const token = session.access
  const p = token ? decodePayload(token) : null
  if (!p || typeof p.sub !== 'string' || typeof p.userId !== 'number') return null
  return { id: p.userId, username: p.sub, role: p.role === 'ADMIN' ? 'ADMIN' : 'USER' }
}
