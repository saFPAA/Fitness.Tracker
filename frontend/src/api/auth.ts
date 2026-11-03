import { apiPublic } from './client'
import type { AuthResponse } from './types'

export const authApi = {
  login: (body: { username: string; password: string }) =>
    apiPublic<AuthResponse>('/api/auth/login', body),
  register: (body: { username: string; email: string; password: string }) =>
    apiPublic<AuthResponse>('/api/auth/register', body),
  /** Отзыв refresh-токена на сервере. Ошибки не важны: локально сессия уже закрыта. */
  logout: (refreshToken: string) =>
    apiPublic<void>('/api/auth/logout', { refreshToken }).catch(() => undefined),
}
