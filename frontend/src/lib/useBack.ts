import { useNavigate } from 'react-router-dom'

/** «Назад» по истории; если страницу открыли напрямую (перезагрузка), ведёт на запасной адрес. */
export function useBack(fallback = '/') {
  const navigate = useNavigate()
  return () => (window.history.state?.idx > 0 ? navigate(-1) : navigate(fallback))
}
