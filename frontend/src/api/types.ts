export interface AuthResponse {
  accessToken: string
  refreshToken: string
}

export interface Page<T> {
  content: T[]
  last?: boolean
}

export interface Workout {
  id: number
  name: string
  date: string // YYYY-MM-DD
  userId: number
}

export interface WorkoutExercise {
  id: number
  workoutId: number
  exerciseId: number
  orderIndex: number
}

export interface WorkoutSet {
  id: number
  workoutExerciseId: number
  position: number
  repsDone: number
  weight: number
}

export interface Exercise {
  id: number
  name: string
  muscleGroup: string
  equipment: string | null
  description: string | null
  videoUrl: string | null
  imageUrl: string | null
}

export interface User {
  id: number
  username: string
  email: string
  createdAt: string | null
}
