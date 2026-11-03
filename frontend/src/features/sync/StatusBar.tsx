import { useState } from 'react'
import { retryFailed } from '../../lib/offlineQueue'
import { plural } from '../../lib/format'
import { useNeedRefresh, useOnline, useQueueOps } from './online'
import { applyUpdate } from './sw'

const CHANGES: [string, string, string] = ['изменение', 'изменения', 'изменений']

/** Плашка сверху страницы: нет связи, что ждёт отправки, что не отправилось, доступно обновление. */
export default function StatusBar() {
  const online = useOnline()
  const ops = useQueueOps()
  const needRefresh = useNeedRefresh()
  const [laterUpdate, setLaterUpdate] = useState(false)

  const waiting = ops.filter((o) => !o.failed).length
  const failed = ops.length - waiting
  const showUpdate = needRefresh && !laterUpdate
  if (online && waiting === 0 && failed === 0 && !showUpdate) return null

  const row = 'flex items-center gap-3 px-4 py-2.5 text-sm'
  const btn = 'min-h-11 shrink-0 rounded-lg px-3 font-semibold text-accent'
  return (
    <div className="sticky top-[env(safe-area-inset-top)] z-20 mb-3 divide-y divide-line overflow-hidden rounded-2xl border border-line bg-surface" role="status">
      {!online && (
        <p className={row}>
          Нет связи. Подходы сохраняются на телефоне и отправятся, когда связь вернётся.
        </p>
      )}
      {waiting > 0 && (
        <p className={row}>
          Ждут отправки: {waiting} {plural(waiting, CHANGES)}
        </p>
      )}
      {failed > 0 && (
        <div className={row}>
          <p className="min-w-0 flex-1 text-danger">
            Не удалось отправить: {failed} {plural(failed, CHANGES)}. Отмечены в упражнениях.
          </p>
          <button onClick={retryFailed} className={btn}>Повторить</button>
        </div>
      )}
      {showUpdate && (
        <div className={row}>
          <p className="min-w-0 flex-1">Доступна новая версия приложения.</p>
          <button onClick={() => setLaterUpdate(true)} className="min-h-11 shrink-0 px-2 text-mute">Позже</button>
          <button onClick={applyUpdate} className={btn}>Обновить</button>
        </div>
      )}
    </div>
  )
}
