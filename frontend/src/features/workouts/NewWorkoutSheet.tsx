import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button, Field, Sheet } from '../../components/ui'
import { todayISO } from '../../lib/dates'
import NameChips from './NameChips'
import { useCreateWorkout } from './hooks'
import { workoutSchema, type WorkoutValues } from './schema'

function Form({ onDone }: { onDone: () => void }) {
  const navigate = useNavigate()
  const create = useCreateWorkout()
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, setValue, watch, formState: { errors } } = useForm<WorkoutValues>({
    resolver: zodResolver(workoutSchema),
    defaultValues: { name: '', date: todayISO() },
  })

  const onSubmit = handleSubmit(async (v) => {
    setError(null)
    try {
      const w = await create.mutateAsync(v)
      onDone()
      navigate(`/workout/${w.id}`)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось создать тренировку')
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
      <Field label="Название" placeholder="Например, Спина + грудь" maxLength={255} autoComplete="off" error={errors.name?.message} {...register('name')} />
      <NameChips value={watch('name')} onChange={(n) => setValue('name', n, { shouldValidate: true })} />
      <Field label="Дата" type="date" error={errors.date?.message} {...register('date')} />
      {error && (
        <p role="alert" className="rounded-xl border border-danger px-4 py-3 text-sm text-danger">
          {error}
        </p>
      )}
      <Button disabled={create.isPending}>{create.isPending ? 'Создаём…' : 'Начать'}</Button>
    </form>
  )
}

export default function NewWorkoutSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <Sheet open={open} onClose={onClose} title="Новая тренировка">
      <Form onDone={onClose} />
    </Sheet>
  )
}
