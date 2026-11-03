import { z } from 'zod'

export const workoutSchema = z.object({
  name: z.string().trim().min(1, 'Введите название').max(255, 'Не больше 255 символов'),
  date: z.string().min(1, 'Выберите дату'),
})
export type WorkoutValues = z.infer<typeof workoutSchema>
