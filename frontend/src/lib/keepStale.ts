import type { QueryFunctionContext } from '@tanstack/react-query'
import { ApiError } from '../api/client'


export function keepStale<T>(fn: () => Promise<T>) {
  return async (ctx: QueryFunctionContext): Promise<T> => {
    try {
      return await fn()
    } catch (e) {
      const prev = ctx.client.getQueryData<T>(ctx.queryKey)
      if (prev !== undefined && e instanceof ApiError && (e.status === 0 || e.status >= 500)) return prev
      throw e
    }
  }
}
