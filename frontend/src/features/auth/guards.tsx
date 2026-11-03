import { Navigate, Outlet } from 'react-router-dom'
import SyncRunner from '../sync/SyncRunner'
import { useAuth } from './AuthContext'

export function RequireAuth() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  return (
    <>
      <SyncRunner />
      <Outlet />
    </>
  )
}

export function PublicOnly() {
  const { user } = useAuth()
  return user ? <Navigate to="/" replace /> : <Outlet />
}
