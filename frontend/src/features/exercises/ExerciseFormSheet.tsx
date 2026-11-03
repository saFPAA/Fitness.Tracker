import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import type { ExerciseInput } from '../../api/exercises'
import type { Exercise } from '../../api/types'
import { Button, Field, Sheet, TextAreaField } from '../../components/ui'
import { isHttpUrl } from '../../lib/format'
import { useSaveExercise } from './hooks'

const long = 'Не больше 255 символов'
const url = z
  .string()
  .trim()
  .max(255, long)
  .refine((v) => v === '' || isHttpUrl(v), 'Нужна ссылка вида https://…')

const schema = z.object({
  name: z.string().trim().min(1, 'Введите название').max(255, long),
  muscleGroup: z.string().trim().min(1, 'Укажите группу мышц').max(255, long),
  equipment: z.string().trim().max(255, long),
  description: z.string().trim().max(255, long),
  videoUrl: url,
  imageUrl: url,
})
type Values = z.infer<typeof schema>

function Form({ exercise, groups, onDone }: { exercise?: Exercise; groups: string[]; onDone: () => void }) {
  const save = useSaveExercise(exercise?.id)
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors } } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: exercise?.name ?? '',
      muscleGroup: exercise?.muscleGroup ?? '',
      equipment: exercise?.equipment ?? '',
      description: exercise?.description ?? '',
      videoUrl: exercise?.videoUrl ?? '',
      imageUrl: exercise?.imageUrl ?? '',
    },
  })

  const onSubmit = handleSubmit(async (v) => {
    setError(null)
    const body: ExerciseInput = {
      name: v.name,
      muscleGroup: v.muscleGroup,
      equipment: v.equipment || null,
      description: v.description || null,
      videoUrl: v.videoUrl || null,
      imageUrl: v.imageUrl || null,
    }
    try {
      await save.mutateAsync(body)
      onDone()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось сохранить')
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
      <Field label="Название" autoComplete="off" error={errors.name?.message} {...register('name')} />
      <Field label="Группа мышц" list="muscle-groups" autoComplete="off" error={errors.muscleGroup?.message} {...register('muscleGroup')} />
      <datalist id="muscle-groups">
        {groups.map((g) => (
          <option key={g} value={g} />
        ))}
      </datalist>
      <Field label="Инвентарь (необязательно)" autoComplete="off" error={errors.equipment?.message} {...register('equipment')} />
      <TextAreaField label="Описание (необязательно)" error={errors.description?.message} {...register('description')} />
      <Field label="Ссылка на видео (необязательно)" type="url" inputMode="url" autoCapitalize="none" autoComplete="off" error={errors.videoUrl?.message} {...register('videoUrl')} />
      <Field label="Ссылка на картинку (необязательно)" type="url" inputMode="url" autoCapitalize="none" autoComplete="off" error={errors.imageUrl?.message} {...register('imageUrl')} />
      {error && <p role="alert" className="rounded-xl border border-danger px-4 py-3 text-sm text-danger">{error}</p>}
      <Button disabled={save.isPending}>{save.isPending ? 'Сохраняем…' : 'Сохранить'}</Button>
    </form>
  )
}

export default function ExerciseFormSheet({
  open,
  onClose,
  exercise,
  groups,
}: {
  open: boolean
  onClose: () => void
  exercise?: Exercise
  groups: string[]
}) {
  return (
    <Sheet open={open} onClose={onClose} title={exercise ? 'Изменить упражнение' : 'Новое упражнение'}>
      <Form exercise={exercise} groups={groups} onDone={onClose} />
    </Sheet>
  )
}
