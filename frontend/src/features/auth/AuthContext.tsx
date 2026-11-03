import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { authApi } from '../../api/auth'
import { onSessionExpired } from '../../api/client'
import { clearPersistedCache } from '../../lib/persist'
import { REFRESH_KEY, readUser, session, type SessionUser } from '../../api/session'

interface AuthContextValue {
  user: SessionUser | null
  login: (v: { username: string; password: string }) => Promise<void>
  register: (v: { username: string; email: string; password: string }) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient()
  const [user, setUser] = useState<SessionUser | null>(() => (session.refresh ? readUser() : null))

  useEffect(() => {
    const drop = () => {
      setUser(null)
      qc.clear()
      clearPersistedCache()
    }
    const off = onSessionExpired(drop)
    // выход в другой вкладке
    const onStorage = (e: StorageEvent) => {
      if (e.key === REFRESH_KEY && !e.newValue) drop()
    }
    window.addEventListener('storage', onStorage)
    return () => {
      off()
      window.removeEventListener('storage', onStorage)
    }
  }, [qc])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      async login(v) {
        const r = await authApi.login(v)
        session.set(r.accessToken, r.refreshToken)
        setUser(readUser())
      },
      async register(v) {
        const r = await authApi.register(v)
        session.set(r.accessToken, r.refreshToken)
        setUser(readUser())
      },
      async logout() {
        const rt = session.refresh
        session.clear()
        setUser(null)
        qc.clear()
        clearPersistedCache()
        if (rt) await authApi.logout(rt)
      },
    }),
    [user, qc],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth нужно вызывать внутри AuthProvider')
  return ctx
}

export function useUserId(): number | null {
  return useAuth().user?.id ?? null
}
