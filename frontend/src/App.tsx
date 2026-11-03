import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from './components/AppShell'
import { PublicOnly, RequireAuth } from './features/auth/guards'
import AuthPage from './pages/AuthPage'
import ExercisePage from './pages/ExercisePage'
import ExercisesPage from './pages/ExercisesPage'
import HistoryPage from './pages/HistoryPage'
import ProfilePage from './pages/ProfilePage'
import TodayPage from './pages/TodayPage'
import WorkoutPage from './pages/WorkoutPage'

export default function App() {
  return (
    <Routes>
      <Route element={<PublicOnly />}>
        <Route path="login" element={<AuthPage mode="login" />} />
        <Route path="register" element={<AuthPage mode="register" />} />
      </Route>
      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route index element={<TodayPage />} />
          <Route path="history" element={<HistoryPage />} />
          <Route path="exercises" element={<ExercisesPage />} />
          <Route path="profile" element={<ProfilePage />} />
        </Route>
        {/* экран тренировки без нижней панели */}
        <Route path="workout/:id" element={<WorkoutPage />} />
        <Route path="exercises/:id" element={<ExercisePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
