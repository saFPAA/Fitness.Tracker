import { registerSW } from 'virtual:pwa-register'

// Новая версия ставится только по кнопке, чтобы приложение не перезагрузилось посреди тренировки.
let needRefresh = false
const listeners = new Set<() => void>()

const update = registerSW({
  onNeedRefresh() {
    needRefresh = true
    listeners.forEach((l) => l())
  },
  onRegisteredSW(_url, reg) {
    // раз в час проверяем, не вышла ли новая версия
    if (reg) setInterval(() => void reg.update().catch(() => undefined), 60 * 60 * 1000)
  },
})

export const subscribeUpdate = (l: () => void) => {
  listeners.add(l)
  return () => {
    listeners.delete(l)
  }
}
export const getNeedRefresh = () => needRefresh
export const applyUpdate = () => void update(true)
