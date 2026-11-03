import { useSyncExternalStore } from 'react'
import { getOps, subscribeQueue } from '../../lib/offlineQueue'
import type { QueueOp } from '../../lib/queueLogic'
import { getNeedRefresh, subscribeUpdate } from './sw'

const subscribeOnline = (cb: () => void) => {
  window.addEventListener('online', cb)
  window.addEventListener('offline', cb)
  return () => {
    window.removeEventListener('online', cb)
    window.removeEventListener('offline', cb)
  }
}
/** navigator.onLine на плохой связи часто врёт «онлайн»: это лишь подсказка для плашки, не условие отправки. */
export const useOnline = () => useSyncExternalStore(subscribeOnline, () => navigator.onLine)

export const useQueueOps = (): QueueOp[] => useSyncExternalStore(subscribeQueue, getOps)
export const useNeedRefresh = () => useSyncExternalStore(subscribeUpdate, getNeedRefresh)
