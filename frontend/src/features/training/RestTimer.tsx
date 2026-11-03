import { useCallback, useEffect, useState } from 'react'
import { mmss } from '../../lib/format'

export const REST_PRESETS = [60, 90, 120, 180]

export function useRestTimer() {
  const [endsAt, setEndsAt] = useState<number | null>(null)
  const start = useCallback((sec: number) => setEndsAt(Date.now() + sec * 1000), [])
  const adjust = useCallback((sec: number) => setEndsAt((e) => (e == null ? e : e + sec * 1000)), [])
  const stop = useCallback(() => setEndsAt(null), [])
  return { endsAt, start, adjust, stop }
}

/** Считает по абсолютному времени конца, поэтому не отстаёт, когда iOS притормаживает фоновую вкладку. */
export function RestTimerBar({
  endsAt,
  onAdjust,
  onRestart,
  onStop,
}: {
  endsAt: number
  onAdjust: (sec: number) => void
  onRestart: (sec: number) => void
  onStop: () => void
}) {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 250)
    return () => clearInterval(t)
  }, [])

  const left = Math.ceil((endsAt - now) / 1000)
  const done = left <= 0

  useEffect(() => {
    if (!done) return
    navigator.vibrate?.([200, 100, 200]) // iOS Safari вибрацию не поддерживает: там сигнал только визуальный
    const t = setTimeout(onStop, 10_000)
    return () => clearTimeout(t)
  }, [done, onStop])

  const btn = 'min-h-11 rounded-xl border border-current/25 bg-black/5 px-3 text-sm font-semibold active:opacity-60'
  return (
    <div className="fixed inset-x-0 bottom-0 z-20 border-t border-accent/30 bg-accent text-accent-ink shadow-[0_-16px_50px_#0008]" role="timer" aria-live="off">
      <div className="mx-auto max-w-xl px-4 pt-3 pb-[calc(env(safe-area-inset-bottom)+0.75rem)]">
        <div className="flex items-center gap-2">
          <div className="min-w-0 flex-1">
            <p className="text-xs opacity-70">{done ? 'Отдых окончен' : 'Отдых'}</p>
            <p className="num text-3xl leading-tight font-bold">{done ? 'Пора!' : mmss(left)}</p>
          </div>
          {!done && (
            <>
              <button onClick={() => onAdjust(-15)} className={btn}>−15</button>
              <button onClick={() => onAdjust(15)} className={btn}>+15</button>
            </>
          )}
          <button onClick={onStop} className={btn}>{done ? 'Закрыть' : 'Пропустить'}</button>
        </div>
        {!done && (
          <div className="mt-2 flex gap-2" role="group" aria-label="Начать отдых заново">
            {REST_PRESETS.map((s) => (
              <button key={s} onClick={() => onRestart(s)} className={`${btn} num flex-1`}>
                {mmss(s)}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
