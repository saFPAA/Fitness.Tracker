import { useState } from 'react'
import { Button, Sheet } from '../../components/ui'
import Stepper from '../../components/Stepper'
import { fromInput, toInput } from '../../lib/format'
import type { DisplaySet } from '../../lib/queueLogic'
import { useSetActions } from './setActions'

function EditForm({ set, onClose }: { set: DisplaySet; onClose: () => void }) {
  const actions = useSetActions(set.workoutExerciseId)
  const [reps, setReps] = useState(String(set.repsDone))
  const [weight, setWeight] = useState(toInput(set.weight))
  const [error, setError] = useState<string | null>(null)

  const run = async (fn: () => Promise<unknown>, fallback: string) => {
    try {
      await fn()
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : fallback)
    }
  }

  const save = () => {
    const r = fromInput(reps)
    const w = weight.trim() === '' ? 0 : fromInput(weight)
    if (reps.trim() === '' || !Number.isInteger(r) || r < 0) return setError('Укажите число повторов')
    if (!Number.isFinite(w) || w < 0) return setError('Некорректный вес')
    void run(() => actions.saveSet(set, { repsDone: r, weight: w }), 'Не удалось сохранить')
  }

  return (
    <div className="flex flex-col gap-4">
      {set.failed && (
        <p role="alert" className="rounded-xl border border-danger px-4 py-3 text-sm text-danger">
          Не отправлен: {set.failed}. Сохранение попробует отправить ещё раз.
        </p>
      )}
      {set.pending && !set.failed && (
        <p className="rounded-xl border border-line px-4 py-3 text-sm text-mute">Ждёт отправки на сервер.</p>
      )}
      <Stepper label="Повторы" value={reps} onChange={setReps} step={1} inputMode="numeric" />
      <Stepper label="Вес, кг" value={weight} onChange={setWeight} step={2.5} inputMode="decimal" />
      {error && <p role="alert" className="rounded-xl border border-danger px-4 py-3 text-sm text-danger">{error}</p>}
      <Button onClick={save} disabled={actions.busy}>{actions.busy ? 'Сохраняем…' : 'Сохранить'}</Button>
      <Button
        variant="secondary"
        onClick={() => void run(() => actions.removeSet(set), 'Не удалось удалить')}
        disabled={actions.busy}
        className="text-danger"
      >
        Удалить подход
      </Button>
    </div>
  )
}

export default function SetEditSheet({ set, onClose }: { set: DisplaySet | null; onClose: () => void }) {
  return (
    <Sheet open={set != null} onClose={onClose} title="Подход">
      {set && <EditForm key={set.id} set={set} onClose={onClose} />}
    </Sheet>
  )
}
