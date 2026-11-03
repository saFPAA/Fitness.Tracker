import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Button, ErrorState, Sheet, Skeleton } from '../components/ui'
import { useAuth } from '../features/auth/AuthContext'
import ExerciseFormSheet from '../features/exercises/ExerciseFormSheet'
import ExerciseHistory from '../features/exercises/ExerciseHistory'
import StatusBar from '../features/sync/StatusBar'
import NameWorkoutSheet from '../features/workouts/NameWorkoutSheet'
import { NeedNameError, useAddToToday, useDeleteExercise } from '../features/exercises/hooks'
import { useCatalog } from '../features/training/hooks'
import { groupDot, safeHttpUrl } from '../lib/format'
import { useBack } from '../lib/useBack'

export default function ExercisePage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const back = useBack('/exercises')
  const catalog = useCatalog()
  const addToToday = useAddToToday()
  const del = useDeleteExercise()
  const isAdmin = useAuth().user?.role === 'ADMIN'
  const [editing, setEditing] = useState(false)
  const [confirm, setConfirm] = useState(false)
  const [imgFailed, setImgFailed] = useState(false)
  const [addError, setAddError] = useState<string | null>(null)
  const [naming, setNaming] = useState(false)

  const ex = catalog.data?.find((e) => e.id === id)
  const groups = [...new Set((catalog.data ?? []).map((e) => e.muscleGroup))].sort((a, b) => a.localeCompare(b, 'ru'))
  const image = safeHttpUrl(ex?.imageUrl)
  const video = safeHttpUrl(ex?.videoUrl)

  const add = async () => {
    if (!ex) return
    setAddError(null)
    try {
      const workoutId = await addToToday.mutateAsync({ exercise: ex })
      navigate(`/workout/${workoutId}`)
    } catch (e) {
      // сегодня тренировки нет: спрашиваем, как её назвать
      if (e instanceof NeedNameError) return setNaming(true)
      setAddError(e instanceof Error ? e.message : 'Не удалось добавить упражнение')
    }
  }

  const remove = async () => {
    try {
      await del.mutateAsync(id)
      navigate('/exercises', { replace: true })
    } catch {
      /* ошибка показывается в шторке через del.error */
    }
  }

  return (
    <div className="mx-auto max-w-xl px-4 pt-[calc(env(safe-area-inset-top)+0.75rem)] pb-[calc(env(safe-area-inset-bottom)+2rem)]">
      <StatusBar />
      <button onClick={back} className="-ml-2 min-h-11 px-2 text-base font-medium text-accent">
        ‹ Назад
      </button>

      {catalog.isPending && <Skeleton className="mt-4 h-40" />}
      {catalog.isError && (
        <div className="mt-4">
          <ErrorState message={catalog.error.message} onRetry={() => void catalog.refetch()} />
        </div>
      )}
      {catalog.isSuccess && !ex && (
        <div className="mt-4">
          <ErrorState message="Упражнение не найдено" />
          <Link to="/exercises" className="mt-4 block py-2 text-center font-medium text-accent">К каталогу</Link>
        </div>
      )}

      {ex && (
        <>
          {image && !imgFailed && (
            <img
              src={image}
              alt=""
              loading="lazy"
              referrerPolicy="no-referrer"
              onError={() => setImgFailed(true)}
              className="mt-2 max-h-64 w-full rounded-2xl border border-line object-cover"
            />
          )}
          <h1 className="mt-4 text-3xl font-bold tracking-tight break-words">{ex.name}</h1>
          <p className="mt-1 flex items-center gap-2 text-mute">
            <span className={`size-2.5 rounded-full ${groupDot(ex.muscleGroup)}`} aria-hidden="true" />
            {ex.muscleGroup}
            {ex.equipment ? ` · ${ex.equipment}` : ''}
          </p>
          {ex.description && <p className="mt-4 break-words whitespace-pre-line">{ex.description}</p>}
          {video && (
            <a href={video} target="_blank" rel="noopener noreferrer" className="mt-4 inline-flex min-h-11 items-center font-medium text-accent">
              Смотреть видео ↗
            </a>
          )}

          <Button onClick={() => void add()} disabled={addToToday.isPending} className="mt-6 w-full">
            {addToToday.isPending ? 'Добавляем…' : 'Добавить в тренировку сегодня'}
          </Button>
          <p className="mt-2 text-center text-sm text-mute">Если сегодня тренировки ещё нет, нужно будет указать её название.</p>
          {addError && <p role="alert" className="mt-3 rounded-xl border border-danger px-4 py-3 text-sm text-danger">{addError}</p>}

          <ExerciseHistory exerciseId={id} />

          {isAdmin && (
            <section className="mt-10 flex flex-col gap-3 border-t border-line pt-6">
              <Button variant="secondary" onClick={() => setEditing(true)}>Изменить упражнение</Button>
              <Button variant="secondary" onClick={() => setConfirm(true)} className="text-danger">Удалить упражнение</Button>
            </section>
          )}

          <NameWorkoutSheet
            open={naming}
            onClose={() => setNaming(false)}
            onSubmit={async (name) => {
              const workoutId = await addToToday.mutateAsync({ exercise: ex, name })
              navigate(`/workout/${workoutId}`)
            }}
          />

          {isAdmin && <ExerciseFormSheet open={editing} onClose={() => setEditing(false)} exercise={ex} groups={groups} />}

          <Sheet
            open={confirm}
            onClose={() => {
              setConfirm(false)
              del.reset()
            }}
            title="Удалить упражнение?"
          >
            <p className="text-mute">«{ex.name}» будет удалено из каталога. Если оно есть в тренировках, сервер откажет.</p>
            {del.isError && <p role="alert" className="mt-4 rounded-xl border border-danger px-4 py-3 text-sm text-danger">{del.error.message}</p>}
            <div className="mt-6 flex flex-col gap-3">
              <Button variant="danger" onClick={() => void remove()} disabled={del.isPending}>
                {del.isPending ? 'Удаляем…' : 'Удалить'}
              </Button>
              <Button variant="secondary" onClick={() => setConfirm(false)} disabled={del.isPending}>Отмена</Button>
            </div>
          </Sheet>
        </>
      )}
    </div>
  )
}
