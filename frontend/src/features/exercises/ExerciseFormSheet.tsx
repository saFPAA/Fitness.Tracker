import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import type { ExerciseInput } from '../../api/exercises'
import type { Exercise } from '../../api/types'
import { Button, Field, Sheet, TextAreaField } from '../../components/ui'
import { isHttpUrl, safeHttpUrl, youtubeEmbedUrl } from '../../lib/format'
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
  const imageValue = register('imageUrl')
  const videoValue = register('videoUrl')
  const [imagePreview, setImagePreview] = useState(exercise?.imageUrl ?? '')
  const [videoPreview, setVideoPreview] = useState(exercise?.videoUrl ?? '')
  const [imageFailed, setImageFailed] = useState(false)
  const previewImage = safeHttpUrl(imagePreview)
  const previewVideo = youtubeEmbedUrl(videoPreview)

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
      <Field
        label="Ссылка на видео (необязательно)"
        placeholder="https://youtu.be/..."
        type="url"
        inputMode="url"
        autoCapitalize="none"
        autoComplete="off"
        error={errors.videoUrl?.message}
        {...videoValue}
        onChange={(event) => {
          videoValue.onChange(event)
          setVideoPreview(event.target.value)
        }}
      />
      <p className="-mt-3 text-xs text-mute">Встроенный плеер доступен для ссылок YouTube. Другие ссылки откроются отдельно.</p>
      {videoPreview.trim() && (
        previewVideo ? (
          <div className="aspect-video overflow-hidden rounded-xl border border-line bg-black">
            <iframe
              src={previewVideo}
              title="Предпросмотр видео"
              loading="lazy"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              referrerPolicy="strict-origin-when-cross-origin"
              allowFullScreen
              className="size-full"
            />
          </div>
        ) : (
          <p className="-mt-3 text-xs text-mute">Ссылка сохранится, но не распознана как YouTube-видео для встроенного плеера.</p>
        )
      )}
      <Field
        label="Ссылка на фото (необязательно)"
        placeholder="https://…"
        type="url"
        inputMode="url"
        autoCapitalize="none"
        autoComplete="off"
        error={errors.imageUrl?.message}
        {...imageValue}
        onChange={(event) => {
          imageValue.onChange(event)
          setImagePreview(event.target.value)
          setImageFailed(false)
        }}
      />
      {previewImage && !imageFailed && (
        <img
          src={previewImage}
          alt="Предпросмотр фото упражнения"
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={() => setImageFailed(true)}
          className="max-h-48 w-full rounded-xl border border-line object-cover"
        />
      )}
      {previewImage && imageFailed && <p className="-mt-3 text-xs text-mute">Не удалось загрузить фото по этой ссылке.</p>}
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
