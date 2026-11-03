import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { session } from '../../api/session'
import { Button, Field, Sheet } from '../../components/ui'
import { usersApi } from '../../api/users'

const schema = z.object({
  currentPassword: z.string().min(1, 'Введите текущий пароль'),
  newPassword: z.string()
    .min(8, 'Минимум 8 символов')
    .refine((value) => new TextEncoder().encode(value).length <= 72, 'Максимум 72 байта UTF-8'),
  confirmNewPassword: z.string().min(1, 'Повторите новый пароль'),
}).refine((value) => value.newPassword === value.confirmNewPassword, {
  message: 'Пароли не совпадают',
  path: ['confirmNewPassword'],
})

type Values = z.infer<typeof schema>

export default function ChangePasswordSheet({
  open,
  onClose,
}: {
  open: boolean
  onClose: () => void
}) {
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<Values>({ resolver: zodResolver(schema) })

  const close = () => {
    setError(null)
    setSuccess(false)
    reset()
    onClose()
  }

  const onSubmit = handleSubmit(async ({ currentPassword, newPassword }) => {
    setError(null)
    setSuccess(false)
    try {
      const tokens = await usersApi.changePassword({ currentPassword, newPassword })
      session.set(tokens.accessToken, tokens.refreshToken)
      reset()
      setSuccess(true)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось сменить пароль')
    }
  })

  return (
    <Sheet
      open={open}
      onClose={close}
      title="Сменить пароль"
    >
      {success ? (
        <div>
          <p role="status" className="rounded-xl border border-accent px-4 py-3 text-sm">
            Пароль изменён. Вы остались в аккаунте на этом устройстве.
          </p>
          <Button variant="secondary" onClick={close} className="mt-4 w-full">
            Готово
          </Button>
        </div>
      ) : (
        <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
          <Field
            label="Текущий пароль"
            type="password"
            autoComplete="current-password"
            error={errors.currentPassword?.message}
            {...register('currentPassword')}
          />
          <Field
            label="Новый пароль"
            type="password"
            autoComplete="new-password"
            error={errors.newPassword?.message}
            {...register('newPassword')}
          />
          <Field
            label="Повторите новый пароль"
            type="password"
            autoComplete="new-password"
            error={errors.confirmNewPassword?.message}
            {...register('confirmNewPassword')}
          />
          {error && <p role="alert" className="rounded-xl border border-danger px-4 py-3 text-sm text-danger">{error}</p>}
          <Button disabled={isSubmitting}>
            {isSubmitting ? 'Сохраняем…' : 'Сменить пароль'}
          </Button>
        </form>
      )}
    </Sheet>
  )
}
