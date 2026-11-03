import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import type { Workout } from '../../api/types'
import { Button, Field, Sheet } from '../../components/ui'
import NameChips from './NameChips'
import { useUpdateWorkout } from './hooks'
import { workoutSchema, type WorkoutValues } from './schema'

function Form({ workout, onDone }: { workout: Workout; onDone: () => void }) {
  const update = useUpdateWorkout(workout.id)
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, setValue, watch, formState: { errors } } = useForm<WorkoutValues>({
    resolver: zodResolver(workoutSchema),
    defaultValues: { name: workout.name, date: workout.date },
  })

  const onSubmit = handleSubmit(async (v) => {
    setError(null)
    if (v.name === workout.name && v.date === workout.date) return onDone()
    try {
      await update.mutateAsync(v)
      onDone()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось сохранить')
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
      <Field label="Название" maxLength={255} autoComplete="off" error={errors.name?.message} {...register('name')} />
      <NameChips value={watch('name')} onChange={(n) => setValue('name', n, { shouldValidate: true })} />
      <Field label="Дата" type="date" error={errors.date?.message} {...register('date')} />
      {error && <p role="alert" className="rounded-xl border border-danger px-4 py-3 text-sm text-danger">{error}</p>}
      <Button disabled={update.isPending}>{update.isPending ? 'Сохраняем…' : 'Сохранить'}</Button>
    </form>
  )
}

export default function EditWorkoutSheet({
  open,
  onClose,
  workout,
}: {
  open: boolean
  onClose: () => void
  workout: Workout
}) {
  return (
    <Sheet open={open} onClose={onClose} title="Название и дата">
      <Form workout={workout} onDone={onClose} />
    </Sheet>
  )
}
