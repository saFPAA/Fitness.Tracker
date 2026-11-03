import { useEffect } from 'react'

const IDLE_MS = 2 * 60 * 60 * 1000 // два часа без касаний: блокировку снимаем, чтобы забытый телефон не горел вечно

/** Не даёт экрану погаснуть, пока открыт экран тренировки. Если браузер не поддерживает, тихо ничего не делает. */
export function useWakeLock() {
  useEffect(() => {
    if (!('wakeLock' in navigator)) return
    let lock: WakeLockSentinel | null = null
    let acquiring = false
    let idle = false
    let cancelled = false
    let timer: ReturnType<typeof setTimeout>

    const acquire = async () => {
      if (cancelled || idle || lock || acquiring) return
      acquiring = true
      try {
        const l = await navigator.wakeLock.request('screen')
        if (cancelled || idle) {
          void l.release()
        } else {
          lock = l
          l.addEventListener('release', () => {
            if (lock === l) lock = null // система сняла блокировку (например, при сворачивании)
          })
        }
      } catch {
        /* отказ: экран гаснет как обычно */
      } finally {
        acquiring = false
      }
    }

    const armIdleTimer = () => {
      clearTimeout(timer)
      timer = setTimeout(() => {
        idle = true
        void lock?.release()
        lock = null
      }, IDLE_MS)
    }
    const onActivity = () => {
      armIdleTimer()
      if (idle) {
        idle = false
        void acquire()
      }
    }
    // при сворачивании приложения блокировка снимается сама, поэтому берём заново при возврате
    const onVisible = () => {
      if (document.visibilityState === 'visible') void acquire()
    }

    armIdleTimer()
    void acquire()
    document.addEventListener('visibilitychange', onVisible)
    window.addEventListener('pointerdown', onActivity, { passive: true })
    window.addEventListener('keydown', onActivity)
    return () => {
      cancelled = true
      clearTimeout(timer)
      document.removeEventListener('visibilitychange', onVisible)
      window.removeEventListener('pointerdown', onActivity)
      window.removeEventListener('keydown', onActivity)
      void lock?.release()
    }
  }, [])
}
