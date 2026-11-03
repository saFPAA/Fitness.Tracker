import { api } from './client'
import type { AuthResponse, User } from './types'

export const usersApi = {
  get: (id: number) => api<User>(`/api/user/${id}`),
  changePassword: (body: { currentPassword: string; newPassword: string }) =>
    api<AuthResponse>('/api/user/me/password', { method: 'PATCH', body }),
}
