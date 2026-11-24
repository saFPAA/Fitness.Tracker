import { NavLink, Outlet } from 'react-router-dom'
import StatusBar from '../features/sync/StatusBar'
import type { ReactNode } from 'react'

const icon = (d: ReactNode) => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{d}</svg>
)

const tabs = [
  { to: '/', label: 'Сегодня', end: true, icon: icon(<><path d="M6 8v8M18 8v8M3 10v4M21 10v4M6 12h12" /></>) },
  { to: '/history', label: 'История', icon: icon(<><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></>) },
  { to: '/exercises', label: 'Упражнения', icon: icon(<><path d="M4 6h16M4 12h16M4 18h10" /></>) },
  { to: '/profile', label: 'Профиль', icon: icon(<><circle cx="12" cy="8.5" r="3.5" /><path d="M5 20c.8-3.5 3.6-5 7-5s6.2 1.5 7 5" /></>) },
]

export default function AppShell() {
  return (
    <div className="mx-auto flex min-h-full max-w-xl flex-col">
      <main className="flex-1 px-4 pt-[calc(env(safe-area-inset-top)+1rem)] pb-28">
        <StatusBar />
        <Outlet />
      </main>
      <nav
        aria-label="Основная навигация"
        className="fixed inset-x-0 bottom-0 z-10 mx-auto max-w-xl border-line bg-surface/95 pb-[env(safe-area-inset-bottom)] backdrop-blur-xl"
      >
        <ul className="mx-auto flex max-w-xl">
          {tabs.map((t) => (
            <li key={t.to} className="flex-1">
              <NavLink
                to={t.to}
                end={t.end}
                className={({ isActive }) =>
                  `flex min-h-[4.1rem] flex-col items-center justify-center gap-1 text-[11px] font-semibold ${isActive ? 'text-accent' : 'text-mute'}`
                }
              >
                {t.icon}
                {t.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}
