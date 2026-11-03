import { useEffect, useId, useRef, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode, type TextareaHTMLAttributes } from 'react'

export function Button({
  variant = 'primary',
  className = '',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'danger' }) {
  const look =
    variant === 'primary'
      ? 'bg-accent text-accent-ink'
      : variant === 'danger'
        ? 'bg-danger text-accent-ink'
        : 'border border-line bg-surface text-ink'
  return (
    <button
      className={`min-h-13 rounded-xl px-5 text-base font-semibold transition-opacity disabled:opacity-50 active:opacity-80 ${look} ${className}`}
      {...props}
    />
  )
}

export function Field({
  label,
  error,
  trailing,
  ...input
}: InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string; trailing?: ReactNode }) {
  const id = useId()
  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium">
        {label}
      </label>
      <div className="relative">
        <input
          id={id}
          aria-invalid={!!error}
          aria-describedby={error ? `${id}-err` : undefined}
          className="min-h-13 w-full rounded-xl border border-line bg-surface px-4 pr-20 text-ink placeholder:text-mute aria-[invalid=true]:border-danger"
          {...input}
        />
        {trailing && <div className="absolute inset-y-0 right-1 flex items-center">{trailing}</div>}
      </div>
      {error && (
        <p id={`${id}-err`} className="mt-1.5 text-sm text-danger">
          {error}
        </p>
      )}
    </div>
  )
}

/** Нижняя шторка на нативном <dialog>: фокус, Escape и затемнение работают из коробки. */
export function Sheet({
  open,
  onClose,
  title,
  children,
}: {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
}) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  useEffect(() => {
    const d = ref.current
    if (!d) return
    if (open && !d.open) d.showModal()
    if (!open && d.open) d.close()
  }, [open])
  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      onClose={onClose}
      onClick={(e) => e.target === ref.current && onClose()}
      className="mx-auto mt-auto mb-0 max-h-[90dvh] w-full max-w-xl overflow-auto rounded-t-3xl bg-surface p-5 pb-[calc(env(safe-area-inset-bottom)+1.25rem)] text-ink backdrop:bg-black/50"
    >
      {open && (
        <>
          <h2 id={titleId} className="mb-4 text-xl font-bold tracking-tight">
            {title}
          </h2>
          {children}
        </>
      )}
    </dialog>
  )
}

export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse rounded-2xl bg-line/60 ${className}`} aria-hidden="true" />
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div role="alert" className="rounded-2xl border border-danger px-4 py-5 text-center">
      <p className="text-danger">{message}</p>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry} className="mt-4">
          Повторить
        </Button>
      )}
    </div>
  )
}

export function TextAreaField({
  label,
  error,
  ...area
}: TextareaHTMLAttributes<HTMLTextAreaElement> & { label: string; error?: string }) {
  const id = useId()
  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium">
        {label}
      </label>
      <textarea
        id={id}
        rows={3}
        aria-invalid={!!error}
        aria-describedby={error ? `${id}-err` : undefined}
        className="w-full resize-none rounded-xl border border-line bg-surface px-4 py-3 text-ink placeholder:text-mute aria-[invalid=true]:border-danger"
        {...area}
      />
      {error && (
        <p id={`${id}-err`} className="mt-1.5 text-sm text-danger">
          {error}
        </p>
      )}
    </div>
  )
}
