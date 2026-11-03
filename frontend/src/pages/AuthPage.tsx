import { useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import PlateMark from '../components/PlateMark'
import { Button, Field } from '../components/ui'
import { useAuth } from '../features/auth/AuthContext'

const loginSchema = z.object({
  username: z.string().trim().min(1, 'Введите логин'),
  password: z.string().min(1, 'Введите пароль'),
})
const registerSchema = z.object({
  username: z.string().trim().min(1, 'Введите логин'),
  email: z.email('Введите корректную почту'),
  password: z.string().min(8, 'Минимум 8 символов'),
})
type LoginValues = z.infer<typeof loginSchema>
type RegisterValues = z.infer<typeof registerSchema>

function Frame({ title, lead, children, footer }: { title: string; lead: string; children: ReactNode; footer: ReactNode }) {
  return (
    <div className="mx-auto max-w-md px-5 pt-[calc(env(safe-area-inset-top)+3rem)] pb-[calc(env(safe-area-inset-bottom)+2rem)]">
      <PlateMark />
      <h1 className="mt-8 text-4xl font-bold tracking-tight">{title}</h1>
      <p className="mt-2 text-mute">{lead}</p>
      <div className="mt-8">{children}</div>
      <p className="mt-6 text-center text-mute">{footer}</p>
    </div>
  )
}

function ShowToggle({ shown, onToggle }: { shown: boolean; onToggle: () => void }) {
  return (
    <button type="button" onClick={onToggle} className="min-h-11 rounded-lg px-3 text-sm font-medium text-accent">
      {shown ? 'Скрыть' : 'Показать'}
    </button>
  )
}

function FormError({ message }: { message: string | null }) {
  return message ? (
    <p role="alert" className="rounded-xl border border-danger px-4 py-3 text-sm text-danger">
      {message}
    </p>
  ) : null
}

const errText = (e: unknown) => (e instanceof Error ? e.message : 'Не удалось выполнить запрос')

function LoginForm() {
  const { login } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const [shown, setShown] = useState(false)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
  })
  const onSubmit = handleSubmit(async (v) => {
    setError(null)
    try {
      await login(v)
    } catch (e) {
      setError(errText(e))
    }
  })
  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
      <Field label="Логин" autoComplete="username" autoCapitalize="none" autoCorrect="off" error={errors.username?.message} {...register('username')} />
      <Field
        label="Пароль"
        type={shown ? 'text' : 'password'}
        autoComplete="current-password"
        error={errors.password?.message}
        trailing={<ShowToggle shown={shown} onToggle={() => setShown((s) => !s)} />}
        {...register('password')}
      />
      <FormError message={error} />
      <Button disabled={isSubmitting}>{isSubmitting ? 'Входим…' : 'Войти'}</Button>
    </form>
  )
}

function RegisterForm() {
  const { register: signUp } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const [shown, setShown] = useState(false)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
  })
  const onSubmit = handleSubmit(async (v) => {
    setError(null)
    try {
      await signUp(v)
    } catch (e) {
      setError(errText(e))
    }
  })
  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
      <Field label="Логин" autoComplete="username" autoCapitalize="none" autoCorrect="off" error={errors.username?.message} {...register('username')} />
      <Field label="Почта" type="email" inputMode="email" autoComplete="email" autoCapitalize="none" error={errors.email?.message} {...register('email')} />
      <Field
        label="Пароль"
        type={shown ? 'text' : 'password'}
        autoComplete="new-password"
        error={errors.password?.message}
        trailing={<ShowToggle shown={shown} onToggle={() => setShown((s) => !s)} />}
        {...register('password')}
      />
      <FormError message={error} />
      <Button disabled={isSubmitting}>{isSubmitting ? 'Создаём…' : 'Создать аккаунт'}</Button>
    </form>
  )
}

export default function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  return mode === 'login' ? (
    <Frame
      title="Вход"
      lead="Войдите, чтобы вести дневник тренировок."
      footer={<>Нет аккаунта? <Link to="/register" className="font-medium text-accent">Создать</Link></>}
    >
      <LoginForm />
    </Frame>
  ) : (
    <Frame
      title="Новый аккаунт"
      lead="Логин и почта должны быть уникальными."
      footer={<>Уже есть аккаунт? <Link to="/login" className="font-medium text-accent">Войти</Link></>}
    >
      <RegisterForm />
    </Frame>
  )
}
