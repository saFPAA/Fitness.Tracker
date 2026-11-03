import { useQuery } from '@tanstack/react-query'
import { usersApi } from '../../api/users'
import { keepStale } from '../../lib/keepStale'
import { useUserId } from './AuthContext'

export function useUser() {
  const uid = useUserId()
  return useQuery({ queryKey: ['user', uid], queryFn: keepStale(() => usersApi.get(uid!)), enabled: uid != null })
}

