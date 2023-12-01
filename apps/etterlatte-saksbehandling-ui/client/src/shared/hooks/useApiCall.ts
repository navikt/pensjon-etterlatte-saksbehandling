import React, { useState } from 'react'
import { ApiError, ApiResponse } from '../api/apiClient'

import { isPending, Result } from '~shared/api/apiUtils'

const initial = <A = never>(): Result<A> => ({ status: 'initial' })
const pending = <A = never>(): Result<A> => ({ status: 'pending' })
const success = <A = void>(data: A): Result<A> => ({
  status: 'success',
  data,
})

export const error = <A = never>(error: ApiError): Result<A> => ({
  status: 'error',
  error,
})

export function useApiCall<T, U>(
  fn: (req: T) => Promise<ApiResponse<U>>
): [
  Result<U>,
  (args: T, onSuccess?: (result: U, statusCode: number) => void, onError?: (error: ApiError) => void) => void,
  () => void,
] {
  const [apiResult, setApiResult] = useState<Result<U>>(initial)

  const callFn = React.useCallback(
    async (args: T, onSuccess?: (result: U, statusCode: number) => void, onError?: (error: any) => void) => {
      if (!isPending(apiResult)) {
        setApiResult(pending)

        const res = await fn(args)
        if (res.ok) {
          setApiResult(success(res.data))
          onSuccess?.(res.data, res.status)
        } else {
          setApiResult(error(res))
          onError?.(res)
        }
      }
    },
    [apiResult, fn]
  )

  const resetToInitial = React.useCallback(() => {
    setApiResult(initial)
  }, [setApiResult])

  return [apiResult, callFn, resetToInitial]
}
