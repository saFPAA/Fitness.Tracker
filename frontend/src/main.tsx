import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient } from '@tanstack/react-query'
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client'
import App from './App'
import { AuthProvider } from './features/auth/AuthContext'
import './features/sync/sw'
import { CACHE_MAX_AGE, persister } from './lib/persist'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      // 'always': не ждать «онлайна» от браузера (на плохой связи он врёт), а пробовать и получать ошибку
      networkMode: 'always',
      gcTime: CACHE_MAX_AGE, // кэш должен жить не меньше, чем сохраняется на диске
    },
    mutations: { networkMode: 'always' },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <PersistQueryClientProvider client={queryClient} persistOptions={{ persister, maxAge: CACHE_MAX_AGE, buster: 'v1' }}>
      <AuthProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </AuthProvider>
    </PersistQueryClientProvider>
  </StrictMode>,
)
