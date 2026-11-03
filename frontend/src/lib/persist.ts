import { createSyncStoragePersister } from '@tanstack/query-sync-storage-persister'

const KEY = 'fitness.cache'
export const CACHE_MAX_AGE = 7 * 24 * 60 * 60 * 1000

/** Кэш запросов на телефоне: без связи приложение показывает последние загруженные данные. */
export const persister = createSyncStoragePersister({ storage: window.localStorage, key: KEY, throttleTime: 1000 })

/** Вызывается при выходе: данные пользователя не должны достаться следующему. */
export function clearPersistedCache() {
  try {
    localStorage.removeItem(KEY)
  } catch {
    /* хранилище недоступно */
  }
}
