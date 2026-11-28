import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from '../api/client'
import { trainingApi } from '../api/training'
import { workoutsApi } from '../api/workouts'

vi.mock('../api/client', () => ({ api: vi.fn() }))

const request = vi.mocked(api)

describe.each([
  { resource: 'workout', remove: workoutsApi.remove, path: '/api/workout/42' },
  { resource: 'workout exercise', remove: trainingApi.removeExercise, path: '/api/workout-exercise/42' },
])('deleting a $resource', ({ remove, path }) => {
  beforeEach(() => request.mockReset())

  it('sends only the parent DELETE', async () => {
    request.mockResolvedValueOnce(undefined)

    await remove(42)

    expect(request).toHaveBeenCalledExactlyOnceWith(path, { method: 'DELETE' })
  })

  it('propagates a failed request without sending any other requests', async () => {
    const error = new Error('Connection lost')
    request.mockRejectedValueOnce(error)

    await expect(remove(42)).rejects.toBe(error)

    expect(request).toHaveBeenCalledExactlyOnceWith(path, { method: 'DELETE' })
  })
})
