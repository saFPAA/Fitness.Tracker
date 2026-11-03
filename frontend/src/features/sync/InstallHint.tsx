import { useState } from 'react'

const KEY = 'fitness.installHintHidden'

function shouldShow(): boolean {
  try {
    if (localStorage.getItem(KEY)) return false
  } catch {
    /* без хранилища просто покажем */
  }
  const ua = navigator.userAgent
  const ios = /iPad|iPhone|iPod/.test(ua) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
  const standalone =
    (navigator as Navigator & { standalone?: boolean }).standalone === true ||
    window.matchMedia('(display-mode: standalone)').matches
  return ios && !standalone
}

/** iOS сам не предлагает «Установить»: показываем подсказку, пока приложение не на домашнем экране. */
export default function InstallHint() {
  const [show, setShow] = useState(shouldShow)
  if (!show) return null
  const hide = () => {
    try {
      localStorage.setItem(KEY, '1')
    } catch {
      /* не запомнится */
    }
    setShow(false)
  }
  return (
    <div className="mt-4 rounded-2xl border border-line bg-surface p-4">
      <p className="font-semibold">Установите приложение на iPhone</p>
      <p className="mt-1 text-sm text-mute">
        В Safari нажмите «Поделиться» (квадрат со стрелкой), затем «На экран "Домой"». Приложение откроется без адресной строки и будет работать при плохой связи.
      </p>
      <button onClick={hide} className="mt-2 min-h-11 text-sm font-medium text-accent">Скрыть</button>
    </div>
  )
}
