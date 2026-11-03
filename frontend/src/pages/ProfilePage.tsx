import { useState } from 'react'
import { Button, Skeleton } from '../components/ui'
import { useAuth } from '../features/auth/AuthContext'
import { useUser } from '../features/auth/profile'
import ChangePasswordSheet from '../features/auth/ChangePasswordSheet'
import { useQueueOps } from '../features/sync/online'
import { mmss } from '../lib/format'
import { plural } from '../lib/format'
import { REST_OPTIONS, getRestSeconds, setRestSeconds } from '../lib/settings'

export default function ProfilePage() {
  const { user, logout } = useAuth()
  const account = useUser()
  const [rest, setRest] = useState(getRestSeconds)
  const [changingPassword, setChangingPassword] = useState(false)
  const unsent = useQueueOps().length
  if (!user) return null

  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight">Профиль</h1>
      <dl className="mt-6 rounded-2xl border border-line bg-surface px-4">
        <div className="py-3">
          <dt className="text-sm text-mute">Логин</dt>
          <dd className="text-lg font-semibold break-words">{user.username}</dd>
        </div>
        <div className="border-t border-line py-3">
          <dt className="text-sm text-mute">Почта</dt>
          <dd className="text-lg font-semibold break-words">
            {account.isPending && <Skeleton className="mt-1 h-6 w-48" />}
            {account.isError && <span className="text-sm font-normal text-danger">Не удалось загрузить</span>}
            {account.isSuccess && account.data.email}
          </dd>
        </div>
      </dl>
      <Button variant="secondary" onClick={() => setChangingPassword(true)} className="mt-3 w-full">
        Сменить пароль
      </Button>
      <ChangePasswordSheet open={changingPassword} onClose={() => setChangingPassword(false)} />

      <section className="mt-8">
        <h2 className="text-lg font-semibold">Отдых между подходами</h2>
        <p className="mt-1 text-sm text-mute">Таймер запускается после каждого записанного подхода.</p>
        <div className="mt-3 flex flex-wrap gap-2" role="group" aria-label="Время отдыха по умолчанию">
          {REST_OPTIONS.map((s) => (
            <button
              key={s}
              onClick={() => {
                setRest(s)
                setRestSeconds(s)
              }}
              aria-pressed={rest === s}
              className={`num min-h-11 min-w-16 rounded-full border px-4 text-sm font-medium ${
                rest === s ? 'border-accent bg-accent text-accent-ink' : 'border-line bg-surface'
              }`}
            >
              {mmss(s)}
            </button>
          ))}
        </div>
      </section>

      {unsent > 0 && (
        <p className="mt-8 rounded-2xl border border-line px-4 py-3 text-sm text-mute">
          Не отправлено на сервер: {unsent} {plural(unsent, ['изменение', 'изменения', 'изменений'])}. После выхода они останутся на телефоне и отправятся при следующем входе в этот аккаунт.
        </p>
      )}

      <Button variant="secondary" onClick={() => void logout()} className="mt-8 w-full">
        Выйти
      </Button>
    </>
  )
}
