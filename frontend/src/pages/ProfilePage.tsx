import { useState } from 'react'
import { Button, Skeleton } from '../components/ui'
import { useAuth } from '../features/auth/AuthContext'
import { useUser } from '../features/auth/profile'
import ChangePasswordSheet from '../features/auth/ChangePasswordSheet'
import { useQueueOps } from '../features/sync/online'
import { mmss } from '../lib/format'
import { plural } from '../lib/format'
import { REST_OPTIONS, getRestSeconds, setRestSeconds } from '../lib/settings'
import ProfileIcon from '../components/ProfileIcon'

export default function ProfilePage() {
  const { user, logout } = useAuth()
  const account = useUser()
  const [rest, setRest] = useState(getRestSeconds)
  const [changingPassword, setChangingPassword] = useState(false)
  const unsent = useQueueOps().length
  if (!user) return null

  return (
    <>
      <p className="page-kicker">ТВОЙ АККАУНТ</p>
      <h1 className="page-title">Профиль</h1>
      <div className="surface-card mt-5 p-4">
        <div className="mb-4 flex items-center gap-3">
          <div className="grid size-14 shrink-0 place-items-center rounded-2xl bg-bg text-accent"><ProfileIcon className="size-8" /></div>
          <div className="min-w-0">
            <p className="truncate text-lg font-semibold">{user.username}</p>
            <p className="mt-0.5 text-xs text-mute">Участник тренировочного дневника</p>
          </div>
        </div>
        <dl className="border-t border-line pt-3">
          <div>
            <dt className="text-[10px] font-bold tracking-[0.12em] text-mute">ПОЧТА</dt>
            <dd className="mt-1 break-words font-medium">
            {account.isPending && <Skeleton className="mt-1 h-6 w-48" />}
            {account.isError && <span className="text-sm font-normal text-danger">Не удалось загрузить</span>}
            {account.isSuccess && account.data.email}
          </dd>
          </div>
        </dl>
      </div>
      <section className="mt-6">
        <div className="mb-3"><h2 className="text-lg font-semibold">Безопасность</h2><p className="mt-1 text-xs text-mute">Защити доступ к своему дневнику.</p></div>
        <Button variant="secondary" onClick={() => setChangingPassword(true)} className="w-full justify-between rounded-2xl text-left">
          <span>Сменить пароль</span>
        </Button>
      </section>
      <ChangePasswordSheet open={changingPassword} onClose={() => setChangingPassword(false)} />

      <section className="mt-7">
        <h2 className="text-lg font-semibold">Тренировка</h2>
        <div className="surface-card mt-3 p-4">
          <h3 className="font-semibold">Отдых между подходами</h3>
          <p className="mt-1 text-xs text-mute">Таймер запускается после каждого записанного подхода.</p>
          <div className="mt-3 flex flex-wrap gap-2" role="group" aria-label="Время отдыха по умолчанию">
          {REST_OPTIONS.map((s) => (
            <button
              key={s}
              onClick={() => {
                setRest(s)
                setRestSeconds(s)
              }}
              aria-pressed={rest === s}
              className={`num min-h-10 min-w-16 rounded-full border px-4 text-xs font-semibold ${
                rest === s ? 'border-accent bg-accent text-accent-ink' : 'border-line bg-surface'
              }`}
            >
              {s === 0 ? 'Выкл.' : mmss(s)}
            </button>
          ))}
          </div>
        </div>
      </section>

      {unsent > 0 && (
        <p className="mt-6 rounded-2xl border border-accent/30 bg-accent/5 px-4 py-3 text-sm text-mute">
          Не отправлено на сервер: {unsent} {plural(unsent, ['изменение', 'изменения', 'изменений'])}. После выхода они останутся на телефоне и отправятся при следующем входе в этот аккаунт.
        </p>
      )}

      <Button variant="secondary" onClick={() => void logout()} className="mt-7 w-full rounded-2xl">
        Выйти
      </Button>
    </>
  )
}
