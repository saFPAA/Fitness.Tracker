import { useState } from 'react'
import { Button, Field, Sheet } from '../../components/ui'
import NameChips from './NameChips'

function Form({ onSubmit }: { onSubmit: (name: string) => Promise<void> }) {
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    const title = name.trim()
    if (!title) return setError('Введите название')
    setBusy(true)
    setError(null)
    try {
      await onSubmit(title)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Не удалось создать тренировку')
      setBusy(false)
    }
  }

  return (
    <form onSubmit={(e) => void submit(e)} noValidate className="flex flex-col gap-4">
      <p className="text-mute">Сегодня тренировки ещё нет. Как её назвать? Упражнение добавится в неё.</p>
      <Field label="Название" placeholder="Например, Спина + грудь" maxLength={255} autoComplete="off" value={name} onChange={(e) => setName(e.target.value)} error={error ?? undefined} />
      <NameChips value={name} onChange={setName} />
      <Button disabled={busy}>{busy ? 'Создаём…' : 'Создать и добавить'}</Button>
    </form>
  )
}

export default function NameWorkoutSheet({
  open,
  onClose,
  onSubmit,
}: {
  open: boolean
  onClose: () => void
  onSubmit: (name: string) => Promise<void>
}) {
  return (
    <Sheet open={open} onClose={onClose} title="Название тренировки">
      <Form onSubmit={onSubmit} />
    </Sheet>
  )
}
