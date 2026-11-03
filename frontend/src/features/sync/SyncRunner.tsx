import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { bindQueue, fresh, queueKey, reloadQueue, setFlusher } from '../../lib/offlineQueue'
import { useUserId } from '../auth/AuthContext'
import { flushQueue } from './flush'

/** Подключает очередь к пользователю и запускает отправку: при входе, возврате связи, возвращении в приложение и по таймеру. */
export default function SyncRunner() {
  const uid = useUserId()
  const qc = useQueryClient()

  useEffect(() => {
    if (uid == null) return
    bindQueue(uid)
    const trigger = () => void flushQueue(qc)
    setFlusher(trigger)
    trigger()

    const onVisible = () => {
      if (!document.hidden) trigger()
    }
    const onStorage = (e: StorageEvent) => {
      if (e.key === queueKey(uid)) reloadQueue() // другая вкладка изменила очередь
    }
    window.addEventListener('online', trigger)
    document.addEventListener('visibilitychange', onVisible)
    window.addEventListener('storage', onStorage)
    const timer = setInterval(() => {
      if (fresh().some((o) => !o.failed)) trigger()
    }, 15_000)

    return () => {
      window.removeEventListener('online', trigger)
      document.removeEventListener('visibilitychange', onVisible)
      window.removeEventListener('storage', onStorage)
      clearInterval(timer)
      setFlusher(null)
      bindQueue(null)
    }
  }, [uid, qc])

  return null
}
