const REST_KEY = 'fitness.restSeconds'
export const REST_DEFAULT = 180
export const REST_OPTIONS = [0, 60, 90, 180, 300]

export function getRestSeconds(): number {
  try {
    const n = Number(localStorage.getItem(REST_KEY))
    return Number.isInteger(n) && n >= 0 && n <= 900 ? n : REST_DEFAULT
  } catch {
    return REST_DEFAULT
  }
}

export function setRestSeconds(n: number) {
  try {
    localStorage.setItem(REST_KEY, String(n))
  } catch {
    /* хранилище недоступно: настройка просто не запомнится */
  }
}
